package com.agrodairy.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductRequest(
        @NotNull UUID categoryId,
        @NotBlank String name,
        String description,
        @NotNull @DecimalMin(value = "0.0") BigDecimal price,
        @NotBlank String unit,
        Integer shelfLifeDays,
        String imageUrl
) {}
