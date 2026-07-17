package com.agrodairy.inventory.service;

import com.agrodairy.common.exception.ApiException;
import com.agrodairy.common.exception.NotFoundException;
import com.agrodairy.inventory.dto.CreateInventoryBatchRequest;
import com.agrodairy.inventory.dto.InventoryAlertsResponse;
import com.agrodairy.inventory.dto.InventoryBatchResponse;
import com.agrodairy.inventory.dto.UpdateInventoryBatchRequest;
import com.agrodairy.inventory.entity.BatchStatus;
import com.agrodairy.inventory.entity.InventoryBatch;
import com.agrodairy.inventory.repository.InventoryBatchRepository;
import com.agrodairy.product.entity.Product;
import com.agrodairy.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InventoryService {

    private static final int LOW_STOCK_THRESHOLD = 10;
    private static final int EXPIRING_SOON_DAYS = 3;

    private final InventoryBatchRepository batchRepository;
    private final ProductRepository productRepository;

    public InventoryService(InventoryBatchRepository batchRepository, ProductRepository productRepository) {
        this.batchRepository = batchRepository;
        this.productRepository = productRepository;
    }

    public Page<InventoryBatchResponse> listBatches(UUID productId, BatchStatus status, Integer expiringWithinDays, Pageable pageable) {
        LocalDate expiryCutoff = expiringWithinDays != null ? LocalDate.now().plusDays(expiringWithinDays) : null;
        return batchRepository.search(productId, status, expiryCutoff, pageable).map(InventoryBatchResponse::from);
    }

    @Transactional
    public InventoryBatchResponse createBatch(CreateInventoryBatchRequest request) {
        if (batchRepository.existsByBatchCode(request.batchCode())) {
            throw new ApiException(HttpStatus.CONFLICT, "CONFLICT", "A batch with this code already exists");
        }
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new NotFoundException("Product not found"));

        InventoryBatch batch = InventoryBatch.builder()
                .product(product)
                .batchCode(request.batchCode())
                .manufactureDate(request.manufactureDate())
                .expiryDate(request.expiryDate())
                .quantityProduced(request.quantityProduced())
                .quantityAvailable(request.quantityProduced())
                .quantitySold(0)
                .status(BatchStatus.ACTIVE)
                .build();
        batchRepository.saveAndFlush(batch);
        return InventoryBatchResponse.from(batch);
    }

    @Transactional
    public InventoryBatchResponse updateBatchStatus(UUID id, UpdateInventoryBatchRequest request) {
        InventoryBatch batch = batchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Inventory batch not found"));
        if (request.status() != null) {
            batch.setStatus(request.status());
        }
        batchRepository.saveAndFlush(batch);
        return InventoryBatchResponse.from(batch);
    }

    /**
     * FEFO (first-expiry-first-out) allocation: draws {@code requestedQuantity} units from the
     * product's ACTIVE batches with stock, earliest expiry first, splitting across batches as
     * needed. Mutates and persists the consumed batches (decrementing availability, marking
     * SOLD_OUT once depleted) and returns what was drawn from each so a caller — e.g. order
     * creation — can build its own line items against the exact batches consumed.
     */
    @Transactional
    public List<FefoAllocation> allocateFefo(UUID productId, int requestedQuantity) {
        List<InventoryBatch> batches = batchRepository.findAllocatable(productId, BatchStatus.ACTIVE);
        int totalAvailable = batches.stream().mapToInt(InventoryBatch::getQuantityAvailable).sum();
        if (totalAvailable < requestedQuantity) {
            throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK",
                    "Insufficient stock to fulfill the requested quantity");
        }

        List<FefoAllocation> allocations = new ArrayList<>();
        int remaining = requestedQuantity;
        for (InventoryBatch batch : batches) {
            if (remaining <= 0) {
                break;
            }
            int take = Math.min(remaining, batch.getQuantityAvailable());
            batch.setQuantityAvailable(batch.getQuantityAvailable() - take);
            batch.setQuantitySold(batch.getQuantitySold() + take);
            if (batch.getQuantityAvailable() == 0) {
                batch.setStatus(BatchStatus.SOLD_OUT);
            }
            batchRepository.save(batch);
            allocations.add(new FefoAllocation(batch, take));
            remaining -= take;
        }
        return allocations;
    }

    @Transactional(readOnly = true)
    public InventoryAlertsResponse alerts() {
        LocalDate expiryCutoff = LocalDate.now().plusDays(EXPIRING_SOON_DAYS);
        List<InventoryBatch> expiringSoonBatches = batchRepository.findExpiringSoon(BatchStatus.ACTIVE, expiryCutoff);
        List<InventoryAlertsResponse.ExpiringSoon> expiringSoon = expiringSoonBatches.stream()
                .map(b -> new InventoryAlertsResponse.ExpiringSoon(
                        b.getId(), b.getProduct().getId(), b.getProduct().getName(), b.getBatchCode(),
                        b.getExpiryDate(), b.getQuantityAvailable()))
                .toList();

        Map<UUID, Integer> availableByProduct = new HashMap<>();
        Map<UUID, Product> productById = new HashMap<>();
        for (Product product : productRepository.findAll()) {
            if (!product.isActive()) {
                continue;
            }
            List<InventoryBatch> activeBatches = batchRepository.findActiveByProduct(product.getId(), BatchStatus.ACTIVE);
            int total = activeBatches.stream().mapToInt(InventoryBatch::getQuantityAvailable).sum();
            availableByProduct.put(product.getId(), total);
            productById.put(product.getId(), product);
        }

        List<InventoryAlertsResponse.LowStock> lowStock = new ArrayList<>();
        List<InventoryAlertsResponse.OutOfStock> outOfStock = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : availableByProduct.entrySet()) {
            Product product = productById.get(entry.getKey());
            int total = entry.getValue();
            if (total == 0) {
                outOfStock.add(new InventoryAlertsResponse.OutOfStock(product.getId(), product.getName()));
            } else if (total < LOW_STOCK_THRESHOLD) {
                lowStock.add(new InventoryAlertsResponse.LowStock(product.getId(), product.getName(), total));
            }
        }

        return new InventoryAlertsResponse(expiringSoon, lowStock, outOfStock);
    }
}
