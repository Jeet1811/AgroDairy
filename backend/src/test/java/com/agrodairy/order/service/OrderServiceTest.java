package com.agrodairy.order.service;

import com.agrodairy.auth.entity.Role;
import com.agrodairy.auth.entity.User;
import com.agrodairy.auth.repository.UserRepository;
import com.agrodairy.auth.security.AuthenticatedUser;
import com.agrodairy.common.exception.ApiException;
import com.agrodairy.common.exception.NotFoundException;
import com.agrodairy.inventory.service.InventoryService;
import com.agrodairy.order.dto.UpdateOrderStatusRequest;
import com.agrodairy.order.entity.Order;
import com.agrodairy.order.entity.OrderStatus;
import com.agrodairy.order.entity.PaymentStatus;
import com.agrodairy.order.repository.OrderItemRepository;
import com.agrodairy.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartService cartService;

    @Mock
    private InventoryService inventoryService;

    private OrderService newService() {
        return new OrderService(orderRepository, orderItemRepository, userRepository, cartService, inventoryService);
    }

    private static Order orderWithStatus(UUID ownerId, OrderStatus status) {
        User owner = User.builder().id(ownerId).role(Role.CUSTOMER).build();
        return Order.builder()
                .id(UUID.randomUUID())
                .user(owner)
                .status(status)
                .paymentStatus(PaymentStatus.PENDING)
                .totalAmount(BigDecimal.TEN)
                .deliveryAddress("123 Main St")
                .build();
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING, CONFIRMED",
            "CONFIRMED, PROCESSING",
            "PROCESSING, PACKED",
            "PACKED, OUT_FOR_DELIVERY",
            "OUT_FOR_DELIVERY, DELIVERED",
            "DELIVERED, REFUNDED",
            "CANCELLED, REFUNDED",
            "PENDING, CANCELLED",
            "CONFIRMED, CANCELLED"
    })
    void validForwardTransitionsAreAccepted(OrderStatus from, OrderStatus to) {
        OrderService service = newService();
        Order order = orderWithStatus(UUID.randomUUID(), from);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(order.getId())).thenReturn(List.of());

        service.updateStatus(order.getId(), new UpdateOrderStatusRequest(to));

        assertThat(order.getStatus()).isEqualTo(to);
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING, PACKED",
            "PENDING, DELIVERED",
            "CONFIRMED, PENDING",
            "PROCESSING, CANCELLED",
            "PACKED, CANCELLED",
            "DELIVERED, CANCELLED",
            "REFUNDED, CANCELLED"
    })
    void invalidTransitionsAreRejected(OrderStatus from, OrderStatus to) {
        OrderService service = newService();
        Order order = orderWithStatus(UUID.randomUUID(), from);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.updateStatus(order.getId(), new UpdateOrderStatusRequest(to)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("INVALID_STATUS_TRANSITION"));
        assertThat(order.getStatus()).isEqualTo(from);
    }

    @Test
    void ownerCanCancelOnlyWhilePending() {
        OrderService service = newService();
        UUID ownerId = UUID.randomUUID();
        Order pendingOrder = orderWithStatus(ownerId, OrderStatus.PENDING);
        when(orderRepository.findById(pendingOrder.getId())).thenReturn(Optional.of(pendingOrder));
        when(orderItemRepository.findByOrderId(pendingOrder.getId())).thenReturn(List.of());

        AuthenticatedUser owner = new AuthenticatedUser(ownerId, "owner@example.com", Role.CUSTOMER);
        service.cancel(pendingOrder.getId(), owner);
        assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        Order confirmedOrder = orderWithStatus(ownerId, OrderStatus.CONFIRMED);
        when(orderRepository.findById(confirmedOrder.getId())).thenReturn(Optional.of(confirmedOrder));
        assertThatThrownBy(() -> service.cancel(confirmedOrder.getId(), owner))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void staffCanCancelAnyPreDeliveredStatusButNotDelivered() {
        OrderService service = newService();
        UUID ownerId = UUID.randomUUID();
        AuthenticatedUser staff = new AuthenticatedUser(UUID.randomUUID(), "staff@example.com", Role.STAFF);

        Order processingOrder = orderWithStatus(ownerId, OrderStatus.PROCESSING);
        when(orderRepository.findById(processingOrder.getId())).thenReturn(Optional.of(processingOrder));
        when(orderItemRepository.findByOrderId(processingOrder.getId())).thenReturn(List.of());
        service.cancel(processingOrder.getId(), staff);
        assertThat(processingOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        Order deliveredOrder = orderWithStatus(ownerId, OrderStatus.DELIVERED);
        when(orderRepository.findById(deliveredOrder.getId())).thenReturn(Optional.of(deliveredOrder));
        assertThatThrownBy(() -> service.cancel(deliveredOrder.getId(), staff))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void nonOwnerCustomerCannotSeeOrCancelAnotherCustomersOrder() {
        OrderService service = newService();
        Order order = orderWithStatus(UUID.randomUUID(), OrderStatus.PENDING);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        AuthenticatedUser otherCustomer = new AuthenticatedUser(UUID.randomUUID(), "other@example.com", Role.CUSTOMER);

        assertThatThrownBy(() -> service.cancel(order.getId(), otherCustomer))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.get(order.getId(), otherCustomer))
                .isInstanceOf(NotFoundException.class);
    }
}
