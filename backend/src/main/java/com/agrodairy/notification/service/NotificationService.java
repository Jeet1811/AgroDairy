package com.agrodairy.notification.service;

import com.agrodairy.auth.entity.Role;
import com.agrodairy.auth.entity.User;
import com.agrodairy.auth.repository.UserRepository;
import com.agrodairy.common.exception.NotFoundException;
import com.agrodairy.notification.dto.NotificationResponse;
import com.agrodairy.notification.entity.Notification;
import com.agrodairy.notification.entity.NotificationType;
import com.agrodairy.notification.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(UUID userId, Boolean isRead, Pageable pageable) {
        Specification<Notification> spec = (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
        if (isRead != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("read"), isRead));
        }
        return notificationRepository.findAll(spec, pageable).map(NotificationResponse::from);
    }

    @Transactional
    public NotificationResponse markRead(UUID id, UUID userId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        if (!notification.getUser().getId().equals(userId)) {
            throw new NotFoundException("Notification not found");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
        return NotificationResponse.from(notification);
    }

    @Transactional
    public void notifyUser(User user, NotificationType type, String message) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .message(message)
                .read(false)
                .build();
        notificationRepository.save(notification);
    }

    @Transactional
    public void notifyRoles(List<Role> roles, NotificationType type, String message) {
        for (User user : userRepository.findByRoleIn(roles)) {
            notifyUser(user, type, message);
        }
    }
}
