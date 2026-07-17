package com.agrodairy.delivery.dto;

import com.agrodairy.delivery.entity.Delivery;
import com.agrodairy.delivery.entity.DeliveryStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DeliveryResponse(
        UUID id,
        UUID orderId,
        UUID subscriptionId,
        LocalDate deliveryDate,
        DeliveryStatus status,
        Instant deliveredAt,
        String notes
) {
    public static DeliveryResponse from(Delivery delivery) {
        return new DeliveryResponse(
                delivery.getId(),
                delivery.getOrder() != null ? delivery.getOrder().getId() : null,
                delivery.getSubscription() != null ? delivery.getSubscription().getId() : null,
                delivery.getDeliveryDate(),
                delivery.getStatus(),
                delivery.getDeliveredAt(),
                delivery.getNotes());
    }
}
