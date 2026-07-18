package com.agrodairy.notification.service;

import com.agrodairy.auth.entity.Role;
import com.agrodairy.auth.entity.User;
import com.agrodairy.auth.repository.UserRepository;
import com.agrodairy.common.exception.NotFoundException;
import com.agrodairy.notification.entity.Notification;
import com.agrodairy.notification.entity.NotificationType;
import com.agrodairy.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    private NotificationService newService() {
        return new NotificationService(notificationRepository, userRepository);
    }

    private static User userWithId(UUID id) {
        return User.builder().id(id).role(Role.CUSTOMER).build();
    }

    @Test
    void ownerCanMarkTheirOwnNotificationRead() {
        NotificationService service = newService();
        UUID ownerId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .user(userWithId(ownerId))
                .type(NotificationType.WELCOME)
                .message("hi")
                .read(false)
                .build();
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        service.markRead(notification.getId(), ownerId);

        assertThat(notification.isRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void nonOwnerCannotMarkAnotherUsersNotificationRead() {
        NotificationService service = newService();
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .user(userWithId(ownerId))
                .type(NotificationType.WELCOME)
                .message("hi")
                .read(false)
                .build();
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> service.markRead(notification.getId(), otherUserId))
                .isInstanceOf(NotFoundException.class);
        assertThat(notification.isRead()).isFalse();
    }

    @Test
    void notifyRolesCreatesOneNotificationPerMatchingUser() {
        NotificationService service = newService();
        User staff = userWithId(UUID.randomUUID());
        User admin = userWithId(UUID.randomUUID());
        when(userRepository.findByRoleIn(List.of(Role.STAFF, Role.ADMIN))).thenReturn(List.of(staff, admin));

        service.notifyRoles(List.of(Role.STAFF, Role.ADMIN), NotificationType.ANOMALY_ALERT, "test message");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Notification::getUser).containsExactlyInAnyOrder(staff, admin);
        assertThat(captor.getAllValues()).allSatisfy(n -> {
            assertThat(n.getType()).isEqualTo(NotificationType.ANOMALY_ALERT);
            assertThat(n.getMessage()).isEqualTo("test message");
            assertThat(n.isRead()).isFalse();
        });
    }

    @Test
    void notificationNotFoundThrows() {
        NotificationService service = newService();
        UUID missingId = UUID.randomUUID();
        when(notificationRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(missingId, UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }
}
