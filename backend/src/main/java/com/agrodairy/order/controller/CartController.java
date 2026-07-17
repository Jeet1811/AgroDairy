package com.agrodairy.order.controller;

import com.agrodairy.auth.security.AuthenticatedUser;
import com.agrodairy.common.dto.ApiResponse;
import com.agrodairy.order.dto.AddCartItemRequest;
import com.agrodairy.order.dto.CartResponse;
import com.agrodairy.order.dto.UpdateCartItemRequest;
import com.agrodairy.order.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> get(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(ApiResponse.of(cartService.getCart(principal.id())));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(@AuthenticationPrincipal AuthenticatedUser principal,
                                                              @Valid @RequestBody AddCartItemRequest request) {
        return ResponseEntity.ok(ApiResponse.of(cartService.addItem(principal.id(), request)));
    }

    @PatchMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(@AuthenticationPrincipal AuthenticatedUser principal,
                                                                 @PathVariable UUID productId,
                                                                 @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(ApiResponse.of(cartService.updateItem(principal.id(), productId, request)));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(@AuthenticationPrincipal AuthenticatedUser principal,
                                                                 @PathVariable UUID productId) {
        return ResponseEntity.ok(ApiResponse.of(cartService.removeItem(principal.id(), productId)));
    }

    @DeleteMapping
    public ResponseEntity<Void> clear(@AuthenticationPrincipal AuthenticatedUser principal) {
        cartService.clearCart(principal.id());
        return ResponseEntity.noContent().build();
    }
}
