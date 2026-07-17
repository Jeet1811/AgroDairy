package com.agrodairy.inventory.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InventoryAlertsResponse(
        List<ExpiringSoon> expiringSoon,
        List<LowStock> lowStock,
        List<OutOfStock> outOfStock
) {
    public record ExpiringSoon(UUID batchId, UUID productId, String productName, String batchCode,
                                LocalDate expiryDate, int quantityAvailable) {}

    public record LowStock(UUID productId, String productName, int totalAvailable) {}

    public record OutOfStock(UUID productId, String productName) {}
}
