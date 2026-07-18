package com.agrodairy.inventory.scheduler;

import com.agrodairy.auth.entity.Role;
import com.agrodairy.inventory.entity.BatchStatus;
import com.agrodairy.inventory.entity.InventoryBatch;
import com.agrodairy.inventory.repository.InventoryBatchRepository;
import com.agrodairy.notification.entity.NotificationType;
import com.agrodairy.notification.service.NotificationService;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * §12: notifies STAFF/ADMIN once per batch when it enters the "expiring within 3 days" window
 * (same threshold as §11.1's read-computed inventory alerts). Runs once daily — the spec doesn't
 * pin an exact time for this trigger, so 07:00 was chosen to land before the start of business.
 * expiryNotifiedAt marks a batch as already-notified so re-running (or the next day's run while
 * still in the window) doesn't re-notify for the same batch.
 *
 * Opted out of the app-wide lazy-initialization default — a lazy bean's @Scheduled methods
 * never get registered with the scheduler until something else forces its creation first.
 */
@Lazy(false)
@Component
public class ExpiringBatchNotificationJob {

    private static final int EXPIRING_SOON_THRESHOLD_DAYS = 3;

    private final InventoryBatchRepository inventoryBatchRepository;
    private final NotificationService notificationService;

    public ExpiringBatchNotificationJob(InventoryBatchRepository inventoryBatchRepository,
                                         NotificationService notificationService) {
        this.inventoryBatchRepository = inventoryBatchRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 7 * * *")
    public void notifyExpiringBatches() {
        checkExpiringBatchesAsOf(LocalDate.now());
    }

    /** Exposed separately from the cron trigger so it can be invoked directly and deterministically (e.g. by tests). */
    @Transactional
    public void checkExpiringBatchesAsOf(LocalDate today) {
        LocalDate cutoff = today.plusDays(EXPIRING_SOON_THRESHOLD_DAYS);
        List<InventoryBatch> expiringSoon =
                inventoryBatchRepository.findExpiringSoonNotYetNotified(BatchStatus.ACTIVE, cutoff);
        for (InventoryBatch batch : expiringSoon) {
            String message = "Inventory batch %s for %s expires on %s — %d unit(s) remaining."
                    .formatted(batch.getBatchCode(), batch.getProduct().getName(), batch.getExpiryDate(),
                            batch.getQuantityAvailable());
            notificationService.notifyRoles(List.of(Role.STAFF, Role.ADMIN), NotificationType.INVENTORY_EXPIRY, message);
            batch.setExpiryNotifiedAt(Instant.now());
            inventoryBatchRepository.save(batch);
        }
    }
}
