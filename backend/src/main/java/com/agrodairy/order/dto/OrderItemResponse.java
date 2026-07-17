package com.agrodairy.order.dto;

import com.agrodairy.order.entity.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID productId,
        String productName,
        UUID inventoryBatchId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
    public static OrderItemResponse from(OrderItem item) {
        BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new OrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getInventoryBatch() != null ? item.getInventoryBatch().getId() : null,
                item.getQuantity(),
                item.getUnitPrice(),
                subtotal);
    }
}
