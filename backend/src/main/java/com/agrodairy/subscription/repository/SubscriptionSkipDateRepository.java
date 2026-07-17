package com.agrodairy.subscription.repository;

import com.agrodairy.subscription.entity.SubscriptionSkipDate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.UUID;

public interface SubscriptionSkipDateRepository extends JpaRepository<SubscriptionSkipDate, UUID> {

    boolean existsBySubscriptionIdAndSkipDate(UUID subscriptionId, LocalDate skipDate);
}
