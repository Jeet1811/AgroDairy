package com.agrodairy.order.dto;

import com.agrodairy.order.entity.Order;
import com.agrodairy.order.entity.OrderStatus;
import com.agrodairy.order.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        OrderStatus status,
        BigDecimal totalAmount,
        PaymentStatus paymentStatus,
        String deliveryAddress,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrderResponse from(Order order, List<OrderItemResponse> items) {
        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getPaymentStatus(),
                order.getDeliveryAddress(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}
