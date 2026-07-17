package com.agrodairy.subscription.service;

import com.agrodairy.auth.entity.Role;
import com.agrodairy.auth.entity.User;
import com.agrodairy.auth.repository.UserRepository;
import com.agrodairy.auth.security.AuthenticatedUser;
import com.agrodairy.common.exception.ApiException;
import com.agrodairy.common.exception.NotFoundException;
import com.agrodairy.common.exception.ValidationException;
import com.agrodairy.product.entity.Product;
import com.agrodairy.product.repository.ProductRepository;
import com.agrodairy.subscription.dto.CreateSubscriptionRequest;
import com.agrodairy.subscription.dto.SkipSubscriptionRequest;
import com.agrodairy.subscription.dto.SubscriptionResponse;
import com.agrodairy.subscription.dto.UpdateSubscriptionRequest;
import com.agrodairy.subscription.entity.Subscription;
import com.agrodairy.subscription.entity.SubscriptionFrequency;
import com.agrodairy.subscription.entity.SubscriptionSkipDate;
import com.agrodairy.subscription.entity.SubscriptionStatus;
import com.agrodairy.subscription.repository.SubscriptionRepository;
import com.agrodairy.subscription.repository.SubscriptionSkipDateRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.UUID;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionSkipDateRepository skipDateRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                                SubscriptionSkipDateRepository skipDateRepository,
                                ProductRepository productRepository,
                                UserRepository userRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.skipDateRepository = skipDateRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public SubscriptionResponse create(UUID userId, CreateSubscriptionRequest request) {
        if (request.frequency() == SubscriptionFrequency.CUSTOM) {
            validateWeekdays(request.weekdays());
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new NotFoundException("Product not found"));

        Subscription subscription = Subscription.builder()
                .user(user)
                .product(product)
                .quantity(request.quantity())
                .frequency(request.frequency())
                .weekdays(request.frequency() == SubscriptionFrequency.CUSTOM ? request.weekdays() : null)
                .deliveryTimeSlot(request.deliveryTimeSlot())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(SubscriptionStatus.ACTIVE)
                .build();
        subscriptionRepository.saveAndFlush(subscription);
        return SubscriptionResponse.from(subscription);
    }

    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> list(AuthenticatedUser principal, SubscriptionStatus status, Pageable pageable) {
        UUID scopedUserId = principal.role() == Role.CUSTOMER ? principal.id() : null;
        Specification<Subscription> spec = (root, query, cb) -> cb.conjunction();
        if (scopedUserId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("user").get("id"), scopedUserId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return subscriptionRepository.findAll(spec, pageable).map(SubscriptionResponse::from);
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse get(UUID id, AuthenticatedUser principal) {
        Subscription subscription = findOrThrow(id);
        assertOwnerOrStaff(subscription, principal);
        return SubscriptionResponse.from(subscription);
    }

    @Transactional
    public SubscriptionResponse update(UUID id, AuthenticatedUser principal, UpdateSubscriptionRequest request) {
        Subscription subscription = findOrThrow(id);
        assertOwnerOrStaff(subscription, principal);

        if (request.quantity() != null) {
            subscription.setQuantity(request.quantity());
        }
        if (request.weekdays() != null) {
            validateWeekdays(request.weekdays());
            subscription.setWeekdays(request.weekdays());
        }
        if (request.deliveryTimeSlot() != null) {
            subscription.setDeliveryTimeSlot(request.deliveryTimeSlot());
        }
        if (request.endDate() != null) {
            subscription.setEndDate(request.endDate());
        }
        subscriptionRepository.save(subscription);
        return SubscriptionResponse.from(subscription);
    }

    @Transactional
    public SubscriptionResponse pause(UUID id, AuthenticatedUser principal) {
        Subscription subscription = findOrThrow(id);
        assertOwnerOrStaff(subscription, principal);
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw invalidTransition(subscription.getStatus(), SubscriptionStatus.PAUSED);
        }
        subscription.setStatus(SubscriptionStatus.PAUSED);
        subscriptionRepository.save(subscription);
        return SubscriptionResponse.from(subscription);
    }

    @Transactional
    public SubscriptionResponse resume(UUID id, AuthenticatedUser principal) {
        Subscription subscription = findOrThrow(id);
        assertOwnerOrStaff(subscription, principal);
        if (subscription.getStatus() != SubscriptionStatus.PAUSED) {
            throw invalidTransition(subscription.getStatus(), SubscriptionStatus.ACTIVE);
        }
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(subscription);
        return SubscriptionResponse.from(subscription);
    }

    @Transactional
    public SubscriptionResponse cancel(UUID id, AuthenticatedUser principal) {
        Subscription subscription = findOrThrow(id);
        assertOwnerOrStaff(subscription, principal);
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE && subscription.getStatus() != SubscriptionStatus.PAUSED) {
            throw invalidTransition(subscription.getStatus(), SubscriptionStatus.CANCELLED);
        }
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(subscription);
        return SubscriptionResponse.from(subscription);
    }

    @Transactional
    public SubscriptionResponse skip(UUID id, AuthenticatedUser principal, SkipSubscriptionRequest request) {
        Subscription subscription = findOrThrow(id);
        assertOwnerOrStaff(subscription, principal);

        if (skipDateRepository.existsBySubscriptionIdAndSkipDate(id, request.skipDate())) {
            throw new ApiException(HttpStatus.CONFLICT, "CONFLICT", "This date is already marked as skipped");
        }
        SubscriptionSkipDate skip = SubscriptionSkipDate.builder()
                .subscription(subscription)
                .skipDate(request.skipDate())
                .build();
        try {
            skipDateRepository.saveAndFlush(skip);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "CONFLICT", "This date is already marked as skipped");
        }
        return SubscriptionResponse.from(subscription);
    }

    private static void validateWeekdays(String weekdays) {
        if (weekdays == null || weekdays.isBlank()) {
            throw new ValidationException("weekdays is required for CUSTOM frequency");
        }
        boolean valid = Arrays.stream(weekdays.split(","))
                .map(String::trim)
                .allMatch(s -> s.matches("[1-7]"));
        if (!valid) {
            throw new ValidationException("weekdays must be a comma-separated list of ISO day numbers (1=Monday..7=Sunday)");
        }
    }

    private static ApiException invalidTransition(SubscriptionStatus from, SubscriptionStatus to) {
        return new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION",
                "Cannot transition subscription from " + from + " to " + to);
    }

    private void assertOwnerOrStaff(Subscription subscription, AuthenticatedUser principal) {
        if (principal.role() == Role.CUSTOMER && !subscription.getUser().getId().equals(principal.id())) {
            throw new NotFoundException("Subscription not found");
        }
    }

    private Subscription findOrThrow(UUID id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Subscription not found"));
    }
}
