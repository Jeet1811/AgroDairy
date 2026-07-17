package com.agrodairy.animal.dto;

import com.agrodairy.animal.entity.Session;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateProductionRecordRequest(
        @NotNull LocalDate productionDate,
        @NotNull Session session,
        @NotNull @DecimalMin(value = "0.0") BigDecimal quantityLitres
) {}
