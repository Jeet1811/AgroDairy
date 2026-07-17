package com.agrodairy.delivery.repository;

import com.agrodairy.delivery.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID>, JpaSpecificationExecutor<Delivery> {

    boolean existsBySubscriptionIdAndDeliveryDate(UUID subscriptionId, LocalDate deliveryDate);

    List<Delivery> findByDeliveryDate(LocalDate date);
}
