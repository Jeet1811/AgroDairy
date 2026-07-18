package com.agrodairy.subscription.scheduler;

import com.agrodairy.delivery.entity.Delivery;
import com.agrodairy.delivery.entity.DeliveryStatus;
import com.agrodairy.delivery.repository.DeliveryRepository;
import com.agrodairy.notification.entity.NotificationType;
import com.agrodairy.notification.service.NotificationService;
import com.agrodairy.subscription.entity.Subscription;
import com.agrodairy.subscription.entity.SubscriptionStatus;
import com.agrodairy.subscription.repository.SubscriptionRepository;
import com.agrodairy.subscription.repository.SubscriptionSkipDateRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

/** Opted out of the app-wide lazy-initialization default — a lazy bean's @Scheduled methods
 * never get registered with the scheduler until something else forces its creation first. */
@Lazy(false)
@Component
public class DailyDeliveryGeneratorJob {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionSkipDateRepository skipDateRepository;
    private final DeliveryRepository deliveryRepository;
    private final NotificationService notificationService;

    public DailyDeliveryGeneratorJob(SubscriptionRepository subscriptionRepository,
                                      SubscriptionSkipDateRepository skipDateRepository,
                                      DeliveryRepository deliveryRepository,
                                      NotificationService notificationService) {
        this.subscriptionRepository = subscriptionRepository;
        this.skipDateRepository = skipDateRepository;
        this.deliveryRepository = deliveryRepository;
        this.notificationService = notificationService;
    }

    /** Runs at 20:00 the evening before, generating tomorrow's deliveries. */
    @Scheduled(cron = "0 0 20 * * *")
    public void generateTomorrowsDeliveries() {
        generateDeliveriesFor(LocalDate.now().plusDays(1));
    }

    /** Exposed separately from the cron trigger so it can be invoked directly and deterministically (e.g. by tests). */
    @Transactional
    public void generateDeliveriesFor(LocalDate targetDate) {
        List<Subscription> activeSubscriptions = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE);
        for (Subscription subscription : activeSubscriptions) {
            if (!isEligible(subscription, targetDate)) {
                continue;
            }
            if (skipDateRepository.existsBySubscriptionIdAndSkipDate(subscription.getId(), targetDate)) {
                continue;
            }
            if (deliveryRepository.existsBySubscriptionIdAndDeliveryDate(subscription.getId(), targetDate)) {
                continue;
            }
            Delivery delivery = Delivery.builder()
                    .subscription(subscription)
                    .deliveryDate(targetDate)
                    .status(DeliveryStatus.PENDING)
                    .build();
            deliveryRepository.save(delivery);

            String message = "A delivery has been scheduled for tomorrow (" + targetDate + ").";
            notificationService.notifyUser(subscription.getUser(), NotificationType.DELIVERY_GENERATED, message);
        }
    }

    private static boolean isEligible(Subscription subscription, LocalDate targetDate) {
        if (targetDate.isBefore(subscription.getStartDate())) {
            return false;
        }
        if (subscription.getEndDate() != null && targetDate.isAfter(subscription.getEndDate())) {
            return false;
        }
        return switch (subscription.getFrequency()) {
            case DAILY -> true;
            case ALTERNATE_DAY -> ChronoUnit.DAYS.between(subscription.getStartDate(), targetDate) % 2 == 0;
            case CUSTOM -> matchesWeekday(subscription.getWeekdays(), targetDate);
        };
    }

    private static boolean matchesWeekday(String weekdays, LocalDate targetDate) {
        if (weekdays == null || weekdays.isBlank()) {
            return false;
        }
        String isoDay = String.valueOf(targetDate.getDayOfWeek().getValue());
        return Arrays.stream(weekdays.split(","))
                .map(String::trim)
                .anyMatch(isoDay::equals);
    }
}
