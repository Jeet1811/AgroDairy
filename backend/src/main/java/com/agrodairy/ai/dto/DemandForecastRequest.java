package com.agrodairy.ai.dto;

import java.util.UUID;

/** Outgoing request body to ai-service's POST /predict/demand. */
public record DemandForecastRequest(UUID productId, int horizonDays) {
}
