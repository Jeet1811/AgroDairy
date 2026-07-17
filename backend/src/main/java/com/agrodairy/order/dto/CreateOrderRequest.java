package com.agrodairy.order.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(
        @NotBlank String deliveryAddress
) {}
