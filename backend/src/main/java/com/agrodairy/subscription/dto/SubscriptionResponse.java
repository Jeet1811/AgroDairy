package com.agrodairy.subscription.dto;

import com.agrodairy.subscription.entity.Subscription;
import com.agrodairy.subscription.entity.SubscriptionFrequency;
import com.agrodairy.subscription.entity.SubscriptionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        UUID userId,
        UUID productId,
        String productName,
        int quantity,
        SubscriptionFrequency frequency,
        String weekdays,
        String deliveryTimeSlot,
        LocalDate startDate,
        LocalDate endDate,
        SubscriptionStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getUser().getId(),
                subscription.getProduct().getId(),
                subscription.getProduct().getName(),
                subscription.getQuantity(),
                subscription.getFrequency(),
                subscription.getWeekdays(),
                subscription.getDeliveryTimeSlot(),
                subscription.getStartDate(),
                subscription.getEndDate(),
                subscription.getStatus(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt());
    }
}
