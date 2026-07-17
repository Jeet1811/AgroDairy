package com.agrodairy.delivery.service;

import com.agrodairy.common.exception.NotFoundException;
import com.agrodairy.delivery.dto.DeliveryResponse;
import com.agrodairy.delivery.dto.DeliverySummaryResponse;
import com.agrodairy.delivery.dto.UpdateDeliveryStatusRequest;
import com.agrodairy.delivery.entity.Delivery;
import com.agrodairy.delivery.entity.DeliveryStatus;
import com.agrodairy.delivery.repository.DeliveryRepository;
import com.agrodairy.product.entity.Product;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;

    public DeliveryService(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    public Page<DeliveryResponse> list(LocalDate date, DeliveryStatus status, Pageable pageable) {
        Specification<Delivery> spec = (root, query, cb) -> cb.conjunction();
        if (date != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("deliveryDate"), date));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return deliveryRepository.findAll(spec, pageable).map(DeliveryResponse::from);
    }

    public Page<DeliveryResponse> mine(UUID userId, Pageable pageable) {
        Specification<Delivery> spec = (root, query, cb) -> {
            var subscriptionUser = root.join("subscription", JoinType.LEFT).join("user", JoinType.LEFT);
            var orderUser = root.join("order", JoinType.LEFT).join("user", JoinType.LEFT);
            return cb.or(cb.equal(subscriptionUser.get("id"), userId), cb.equal(orderUser.get("id"), userId));
        };
        return deliveryRepository.findAll(spec, pageable).map(DeliveryResponse::from);
    }

    @Transactional
    public DeliveryResponse updateStatus(UUID id, UpdateDeliveryStatusRequest request) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Delivery not found"));
        delivery.setStatus(request.status());
        if (request.notes() != null) {
            delivery.setNotes(request.notes());
        }
        if (request.status() == DeliveryStatus.DELIVERED) {
            delivery.setDeliveredAt(Instant.now());
        }
        deliveryRepository.save(delivery);
        return DeliveryResponse.from(delivery);
    }

    @Transactional(readOnly = true)
    public DeliverySummaryResponse summary(LocalDate date) {
        List<Delivery> deliveries = deliveryRepository.findByDeliveryDate(date);

        Map<UUID, DeliverySummaryResponse.ProductQuantity> byProduct = new LinkedHashMap<>();
        for (Delivery delivery : deliveries) {
            if (delivery.getSubscription() == null) {
                continue;
            }
            Product product = delivery.getSubscription().getProduct();
            int quantity = delivery.getSubscription().getQuantity();
            byProduct.merge(product.getId(),
                    new DeliverySummaryResponse.ProductQuantity(product.getId(), product.getName(), quantity),
                    (existing, added) -> new DeliverySummaryResponse.ProductQuantity(
                            existing.productId(), existing.name(), existing.totalQuantity() + added.totalQuantity()));
        }

        return new DeliverySummaryResponse(deliveries.size(), List.copyOf(byProduct.values()));
    }
}
