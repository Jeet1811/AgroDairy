package com.agrodairy.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateInventoryBatchRequest(
        @NotNull UUID productId,
        @NotBlank String batchCode,
        @NotNull LocalDate manufactureDate,
        LocalDate expiryDate,
        @NotNull @Min(1) Integer quantityProduced
) {}
