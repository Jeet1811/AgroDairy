package com.agrodairy.order.service;

import com.agrodairy.auth.entity.Role;
import com.agrodairy.auth.entity.User;
import com.agrodairy.auth.repository.UserRepository;
import com.agrodairy.auth.security.AuthenticatedUser;
import com.agrodairy.common.exception.ApiException;
import com.agrodairy.common.exception.NotFoundException;
import com.agrodairy.common.exception.ValidationException;
import com.agrodairy.inventory.service.FefoAllocation;
import com.agrodairy.inventory.service.InventoryService;
import com.agrodairy.notification.entity.NotificationType;
import com.agrodairy.notification.service.NotificationService;
import com.agrodairy.order.dto.CreateOrderRequest;
import com.agrodairy.order.dto.OrderItemResponse;
import com.agrodairy.order.dto.OrderResponse;
import com.agrodairy.order.dto.UpdateOrderStatusRequest;
import com.agrodairy.order.entity.CartItem;
import com.agrodairy.order.entity.Order;
import com.agrodairy.order.entity.OrderItem;
import com.agrodairy.order.entity.OrderStatus;
import com.agrodairy.order.entity.PaymentStatus;
import com.agrodairy.order.repository.OrderItemRepository;
import com.agrodairy.order.repository.OrderRepository;
import com.agrodairy.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class OrderService {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING, Set.of(OrderStatus.PACKED),
            OrderStatus.PACKED, Set.of(OrderStatus.OUT_FOR_DELIVERY),
            OrderStatus.OUT_FOR_DELIVERY, Set.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, Set.of(OrderStatus.REFUNDED),
            OrderStatus.CANCELLED, Set.of(OrderStatus.REFUNDED),
            OrderStatus.REFUNDED, Set.of());

    /** Source statuses a STAFF/ADMIN may cancel from — "any time before DELIVERED" per §8. */
    private static final Set<OrderStatus> STAFF_CANCELLABLE_FROM = Set.of(
            OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PROCESSING,
            OrderStatus.PACKED, OrderStatus.OUT_FOR_DELIVERY);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;

    public OrderService(OrderRepository orderRepository,
                         OrderItemRepository orderItemRepository,
                         UserRepository userRepository,
                         CartService cartService,
                         InventoryService inventoryService,
                         NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.cartService = cartService;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
    }

    @Transactional
    public OrderResponse createOrder(UUID userId, CreateOrderRequest request) {
        List<CartItem> cartItems = cartService.getCartItemEntities(userId);
        if (cartItems.isEmpty()) {
            throw new ValidationException("Cart is empty");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .deliveryAddress(request.deliveryAddress())
                .build();
        orderRepository.save(order);

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            List<FefoAllocation> allocations = inventoryService.allocateFefo(product.getId(), cartItem.getQuantity());
            for (FefoAllocation allocation : allocations) {
                OrderItem orderItem = OrderItem.builder()
                        .order(order)
                        .product(product)
                        .inventoryBatch(allocation.batch())
                        .quantity(allocation.quantity())
                        .unitPrice(product.getPrice())
                        .build();
                orderItemRepository.save(orderItem);
                orderItems.add(orderItem);
                total = total.add(product.getPrice().multiply(BigDecimal.valueOf(allocation.quantity())));
            }
        }
        order.setTotalAmount(total);
        orderRepository.saveAndFlush(order);

        cartService.clearCart(userId);

        List<OrderItemResponse> itemResponses = orderItems.stream().map(OrderItemResponse::from).toList();
        return OrderResponse.from(order, itemResponses);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> list(AuthenticatedUser principal, OrderStatus status, Pageable pageable) {
        UUID scopedUserId = principal.role() == Role.CUSTOMER ? principal.id() : null;
        Specification<Order> spec = (root, query, cb) -> cb.conjunction();
        if (scopedUserId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("user").get("id"), scopedUserId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return orderRepository.findAll(spec, pageable).map(this::toResponseWithItems);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID id, AuthenticatedUser principal) {
        Order order = findOrderOrThrow(id);
        if (principal.role() == Role.CUSTOMER && !order.getUser().getId().equals(principal.id())) {
            throw new NotFoundException("Order not found");
        }
        return toResponseWithItems(order);
    }

    @Transactional
    public OrderResponse updateStatus(UUID id, UpdateOrderStatusRequest request) {
        Order order = findOrderOrThrow(id);
        OrderStatus current = order.getStatus();
        OrderStatus target = request.status();
        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION",
                    "Cannot transition order from " + current + " to " + target);
        }
        order.setStatus(target);
        orderRepository.save(order);
        notifyOwnerOfStatusChange(order);
        return toResponseWithItems(order);
    }

    @Transactional
    public OrderResponse cancel(UUID id, AuthenticatedUser principal) {
        Order order = findOrderOrThrow(id);
        boolean isOwner = order.getUser().getId().equals(principal.id());
        boolean isStaffOrAdmin = principal.role() == Role.STAFF || principal.role() == Role.ADMIN;
        if (!isOwner && !isStaffOrAdmin) {
            throw new NotFoundException("Order not found");
        }

        boolean allowed = isStaffOrAdmin
                ? STAFF_CANCELLABLE_FROM.contains(order.getStatus())
                : order.getStatus() == OrderStatus.PENDING;
        if (!allowed) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION",
                    "Order cannot be cancelled from its current status");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        notifyOwnerOfStatusChange(order);
        return toResponseWithItems(order);
    }

    private void notifyOwnerOfStatusChange(Order order) {
        String message = "Your order " + order.getId() + " status changed to " + order.getStatus() + ".";
        notificationService.notifyUser(order.getUser(), NotificationType.ORDER_STATUS_CHANGE, message);
    }

    private OrderResponse toResponseWithItems(Order order) {
        List<OrderItemResponse> items = orderItemRepository.findByOrderId(order.getId()).stream()
                .map(OrderItemResponse::from)
                .toList();
        return OrderResponse.from(order, items);
    }

    private Order findOrderOrThrow(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found"));
    }
}
