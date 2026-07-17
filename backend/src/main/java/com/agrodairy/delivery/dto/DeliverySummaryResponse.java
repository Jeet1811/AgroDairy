package com.agrodairy.delivery.dto;

import java.util.List;
import java.util.UUID;

public record DeliverySummaryResponse(
        int totalStops,
        List<ProductQuantity> totalItemsByProduct
) {
    public record ProductQuantity(UUID productId, String name, int totalQuantity) {}
}
