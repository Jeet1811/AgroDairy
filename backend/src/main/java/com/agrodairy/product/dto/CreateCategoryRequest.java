package com.agrodairy.product.dto;

import com.agrodairy.product.entity.CategoryKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCategoryRequest(
        @NotBlank String name,
        @NotNull CategoryKind kind
) {}
