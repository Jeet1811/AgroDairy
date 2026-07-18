package com.agrodairy.ai.dto;

import java.util.List;
import java.util.UUID;

/** Shape returned by ai-service, and re-returned as-is to our own API callers. */
public record DemandForecastResponse(
        UUID productId,
        String modelVersion,
        List<DemandPrediction> predictions
) {}
