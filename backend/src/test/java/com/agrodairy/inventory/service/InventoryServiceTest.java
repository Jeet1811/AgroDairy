package com.agrodairy.inventory.service;

import com.agrodairy.common.exception.ApiException;
import com.agrodairy.inventory.entity.BatchStatus;
import com.agrodairy.inventory.entity.InventoryBatch;
import com.agrodairy.inventory.repository.InventoryBatchRepository;
import com.agrodairy.product.entity.Product;
import com.agrodairy.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryBatchRepository batchRepository;

    @Mock
    private ProductRepository productRepository;

    private InventoryService newService() {
        return new InventoryService(batchRepository, productRepository);
    }

    private static Product testProduct() {
        return Product.builder().id(UUID.randomUUID()).name("Cow Milk 1L").active(true).build();
    }

    private static InventoryBatch batch(Product product, String code, LocalDate expiry, int available) {
        return InventoryBatch.builder()
                .id(UUID.randomUUID())
                .product(product)
                .batchCode(code)
                .manufactureDate(LocalDate.now().minusDays(10))
                .expiryDate(expiry)
                .quantityProduced(available)
                .quantityAvailable(available)
                .quantitySold(0)
                .status(BatchStatus.ACTIVE)
                .build();
    }

    @Test
    void allocatesEarliestExpiryFirstAndSplitsAcrossBatches() {
        InventoryService service = newService();
        Product product = testProduct();
        InventoryBatch earlier = batch(product, "B-EARLY", LocalDate.now().plusDays(5), 5);
        InventoryBatch later = batch(product, "B-LATE", LocalDate.now().plusDays(30), 10);

        when(batchRepository.findAllocatable(product.getId(), BatchStatus.ACTIVE))
                .thenReturn(List.of(earlier, later));

        List<FefoAllocation> allocations = service.allocateFefo(product.getId(), 8);

        assertThat(allocations).hasSize(2);
        assertThat(allocations.get(0).batch()).isEqualTo(earlier);
        assertThat(allocations.get(0).quantity()).isEqualTo(5);
        assertThat(allocations.get(1).batch()).isEqualTo(later);
        assertThat(allocations.get(1).quantity()).isEqualTo(3);

        assertThat(earlier.getQuantityAvailable()).isEqualTo(0);
        assertThat(earlier.getStatus()).isEqualTo(BatchStatus.SOLD_OUT);
        assertThat(earlier.getQuantitySold()).isEqualTo(5);

        assertThat(later.getQuantityAvailable()).isEqualTo(7);
        assertThat(later.getStatus()).isEqualTo(BatchStatus.ACTIVE);
        assertThat(later.getQuantitySold()).isEqualTo(3);

        verify(batchRepository, times(2)).save(any());
    }

    @Test
    void insufficientStockRejectsWithoutMutatingAnyBatch() {
        InventoryService service = newService();
        Product product = testProduct();
        InventoryBatch onlyBatch = batch(product, "B-1", LocalDate.now().plusDays(10), 5);

        when(batchRepository.findAllocatable(product.getId(), BatchStatus.ACTIVE))
                .thenReturn(List.of(onlyBatch));

        assertThatThrownBy(() -> service.allocateFefo(product.getId(), 10))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("INSUFFICIENT_STOCK"));

        assertThat(onlyBatch.getQuantityAvailable()).isEqualTo(5);
        verify(batchRepository, never()).save(any());
    }

    @Test
    void fullyConsumesSingleBatchExactly() {
        InventoryService service = newService();
        Product product = testProduct();
        InventoryBatch onlyBatch = batch(product, "B-1", LocalDate.now().plusDays(10), 6);

        when(batchRepository.findAllocatable(product.getId(), BatchStatus.ACTIVE))
                .thenReturn(List.of(onlyBatch));

        List<FefoAllocation> allocations = service.allocateFefo(product.getId(), 6);

        assertThat(allocations).hasSize(1);
        assertThat(allocations.get(0).quantity()).isEqualTo(6);
        assertThat(onlyBatch.getQuantityAvailable()).isEqualTo(0);
        assertThat(onlyBatch.getStatus()).isEqualTo(BatchStatus.SOLD_OUT);
    }
}
