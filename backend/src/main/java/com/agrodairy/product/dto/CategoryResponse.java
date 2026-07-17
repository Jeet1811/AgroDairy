package com.agrodairy.product.dto;

import com.agrodairy.product.entity.CategoryKind;
import com.agrodairy.product.entity.ProductCategory;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        CategoryKind kind
) {
    public static CategoryResponse from(ProductCategory category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getKind());
    }
}
