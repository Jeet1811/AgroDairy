package com.agrodairy.subscription.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SkipSubscriptionRequest(
        @NotNull LocalDate skipDate
) {}
