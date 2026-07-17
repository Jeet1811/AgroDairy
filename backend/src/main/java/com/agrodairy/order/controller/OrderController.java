package com.agrodairy.order.controller;

import com.agrodairy.auth.security.AuthenticatedUser;
import com.agrodairy.common.dto.ApiResponse;
import com.agrodairy.common.dto.PageResponse;
import com.agrodairy.order.dto.CreateOrderRequest;
import com.agrodairy.order.dto.OrderResponse;
import com.agrodairy.order.dto.UpdateOrderStatusRequest;
import com.agrodairy.order.entity.OrderStatus;
import com.agrodairy.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderResponse>> create(@AuthenticationPrincipal AuthenticatedUser principal,
                                                              @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(principal.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<OrderResponse> page = orderService.list(principal, status, pageable);
        return ResponseEntity.ok(ApiResponse.of(page.getContent(), PageResponse.of(page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> get(@AuthenticationPrincipal AuthenticatedUser principal,
                                                           @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(orderService.get(id, principal)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(@PathVariable UUID id,
                                                                    @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.of(orderService.updateStatus(id, request)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(@AuthenticationPrincipal AuthenticatedUser principal,
                                                              @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(orderService.cancel(id, principal)));
    }
}
