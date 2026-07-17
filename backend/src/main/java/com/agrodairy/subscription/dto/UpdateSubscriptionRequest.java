package com.agrodairy.subscription.dto;

import java.time.LocalDate;

/** Partial update — any field left null is left unchanged on the existing Subscription. */
public record UpdateSubscriptionRequest(
        Integer quantity,
        String weekdays,
        String deliveryTimeSlot,
        LocalDate endDate
) {}
