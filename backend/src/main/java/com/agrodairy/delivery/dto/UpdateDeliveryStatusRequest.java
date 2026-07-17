package com.agrodairy.delivery.dto;

import com.agrodairy.delivery.entity.DeliveryStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDeliveryStatusRequest(
        @NotNull DeliveryStatus status,
        String notes
) {}
