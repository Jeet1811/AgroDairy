package com.agrodairy.order.repository;

import com.agrodairy.order.entity.OrderItem;
import com.agrodairy.order.entity.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrderId(UUID orderId);

    @Query("select oi.product.id as productId, oi.product.name as name, sum(oi.quantity) as unitsSold "
            + "from OrderItem oi where oi.order.status <> :excludedStatus "
            + "group by oi.product.id, oi.product.name order by sum(oi.quantity) desc")
    List<TopSellingProductProjection> findTopSellingProducts(@Param("excludedStatus") OrderStatus excludedStatus, Pageable pageable);

    interface TopSellingProductProjection {
        UUID getProductId();
        String getName();
        Long getUnitsSold();
    }
}
