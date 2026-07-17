package com.agrodairy.order.repository;

import com.agrodairy.order.entity.Order;
import com.agrodairy.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query("select o from Order o where (:userId is null or o.user.id = :userId) "
            + "and (:status is null or o.status = :status)")
    Page<Order> search(@Param("userId") UUID userId, @Param("status") OrderStatus status, Pageable pageable);
}
