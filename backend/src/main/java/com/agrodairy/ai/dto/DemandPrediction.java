package com.agrodairy.ai.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DemandPrediction(
        LocalDate date,
        BigDecimal predictedQuantity,
        BigDecimal confidenceLow,
        BigDecimal confidenceHigh
) {}
