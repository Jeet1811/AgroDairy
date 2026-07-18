package com.agrodairy.notification.repository;

import com.agrodairy.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID>,
        JpaSpecificationExecutor<Notification> {
}
