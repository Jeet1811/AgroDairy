package com.agrodairy.subscription.repository;

import com.agrodairy.subscription.entity.Subscription;
import com.agrodairy.subscription.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID>,
        JpaSpecificationExecutor<Subscription> {

    List<Subscription> findByStatus(SubscriptionStatus status);

    long countByStatus(SubscriptionStatus status);
}
