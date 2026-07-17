package com.agrodairy.subscription.dto;

import com.agrodairy.subscription.entity.SubscriptionFrequency;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateSubscriptionRequest(
        @NotNull UUID productId,
        @NotNull @Min(1) Integer quantity,
        @NotNull SubscriptionFrequency frequency,
        String weekdays,
        String deliveryTimeSlot,
        @NotNull LocalDate startDate,
        LocalDate endDate
) {}
