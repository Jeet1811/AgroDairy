package com.agrodairy.inventory.dto;

import com.agrodairy.inventory.entity.BatchStatus;
import com.agrodairy.inventory.entity.InventoryBatch;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InventoryBatchResponse(
        UUID id,
        UUID productId,
        String batchCode,
        LocalDate manufactureDate,
        LocalDate expiryDate,
        int quantityProduced,
        int quantityAvailable,
        int quantitySold,
        BatchStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static InventoryBatchResponse from(InventoryBatch batch) {
        return new InventoryBatchResponse(
                batch.getId(),
                batch.getProduct().getId(),
                batch.getBatchCode(),
                batch.getManufactureDate(),
                batch.getExpiryDate(),
                batch.getQuantityProduced(),
                batch.getQuantityAvailable(),
                batch.getQuantitySold(),
                batch.getStatus(),
                batch.getCreatedAt(),
                batch.getUpdatedAt());
    }
}
