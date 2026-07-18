package com.agrodairy.ai.dto;

import com.agrodairy.ai.entity.AnomalyAlert;
import com.agrodairy.ai.entity.Severity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AnomalyAlertResponse(
        UUID id,
        UUID animalId,
        String animalTag,
        Instant detectedAt,
        LocalDate productionDate,
        BigDecimal baselineMean,
        BigDecimal baselineStddev,
        BigDecimal actualValue,
        BigDecimal zScore,
        Severity severity,
        boolean acknowledged
) {
    public static AnomalyAlertResponse from(AnomalyAlert alert) {
        return new AnomalyAlertResponse(
                alert.getId(),
                alert.getAnimal().getId(),
                alert.getAnimal().getTag(),
                alert.getDetectedAt(),
                alert.getProductionDate(),
                alert.getBaselineMean(),
                alert.getBaselineStddev(),
                alert.getActualValue(),
                alert.getZScore(),
                alert.getSeverity(),
                alert.isAcknowledged());
    }
}
