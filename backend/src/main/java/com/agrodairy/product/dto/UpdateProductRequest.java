package com.agrodairy.product.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Partial update — any field left null is left unchanged on the existing Product. */
public record UpdateProductRequest(
        UUID categoryId,
        String name,
        String description,
        BigDecimal price,
        String unit,
        Integer shelfLifeDays,
        String imageUrl
) {}
