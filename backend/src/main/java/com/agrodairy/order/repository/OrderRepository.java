package com.agrodairy.order.repository;

import com.agrodairy.order.entity.Order;
import com.agrodairy.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    @Query("select coalesce(sum(o.totalAmount), 0) from Order o where o.status <> :excludedStatus and o.createdAt >= :start")
    BigDecimal sumRevenueSince(@Param("excludedStatus") OrderStatus excludedStatus, @Param("start") Instant start);

    long countByStatusNot(OrderStatus excludedStatus);

    @Query(value = "SELECT CAST(created_at AS date) AS d, SUM(total_amount) AS amount FROM orders "
            + "WHERE status <> :excludedStatus AND created_at >= :start GROUP BY d ORDER BY d", nativeQuery = true)
    List<Object[]> findDailyRevenueSince(@Param("excludedStatus") String excludedStatus, @Param("start") Instant start);
}
