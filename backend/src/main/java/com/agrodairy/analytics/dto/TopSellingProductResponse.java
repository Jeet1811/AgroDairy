package com.agrodairy.analytics.dto;

import java.util.UUID;

public record TopSellingProductResponse(UUID productId, String name, long unitsSold) {
}
