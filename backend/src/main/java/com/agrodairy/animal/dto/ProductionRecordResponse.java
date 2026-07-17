package com.agrodairy.animal.dto;

import com.agrodairy.animal.entity.MilkProductionRecord;
import com.agrodairy.animal.entity.Session;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProductionRecordResponse(
        UUID id,
        UUID animalId,
        LocalDate productionDate,
        Session session,
        BigDecimal quantityLitres,
        UUID recordedBy,
        Instant createdAt
) {
    public static ProductionRecordResponse from(MilkProductionRecord record) {
        return new ProductionRecordResponse(
                record.getId(),
                record.getAnimal().getId(),
                record.getProductionDate(),
                record.getSession(),
                record.getQuantityLitres(),
                record.getRecordedBy() != null ? record.getRecordedBy().getId() : null,
                record.getCreatedAt());
    }
}
