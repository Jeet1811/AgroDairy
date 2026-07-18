package com.agrodairy.inventory.repository;

import com.agrodairy.inventory.entity.BatchStatus;
import com.agrodairy.inventory.entity.InventoryBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, UUID>,
        JpaSpecificationExecutor<InventoryBatch> {

    boolean existsByBatchCode(String batchCode);

    @Query("select b from InventoryBatch b where b.product.id = :productId and b.status = :status "
            + "and b.quantityAvailable > 0 order by b.expiryDate asc nulls last")
    List<InventoryBatch> findAllocatable(@Param("productId") UUID productId, @Param("status") BatchStatus status);

    @Query("select b from InventoryBatch b where b.status = :status and b.expiryDate is not null and b.expiryDate <= :cutoff")
    List<InventoryBatch> findExpiringSoon(@Param("status") BatchStatus status, @Param("cutoff") LocalDate cutoff);

    @Query("select b from InventoryBatch b where b.status = :status and b.expiryDate is not null "
            + "and b.expiryDate <= :cutoff and b.expiryNotifiedAt is null")
    List<InventoryBatch> findExpiringSoonNotYetNotified(@Param("status") BatchStatus status, @Param("cutoff") LocalDate cutoff);

    @Query("select b from InventoryBatch b where b.product.id = :productId and b.status = :status")
    List<InventoryBatch> findActiveByProduct(@Param("productId") UUID productId, @Param("status") BatchStatus status);

    @Query("select coalesce(sum(b.quantityAvailable * b.product.price), 0) from InventoryBatch b where b.status = :status")
    BigDecimal sumInventoryValue(@Param("status") BatchStatus status);
}
