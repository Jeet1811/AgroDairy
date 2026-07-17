package com.agrodairy.subscription.controller;

import com.agrodairy.auth.security.AuthenticatedUser;
import com.agrodairy.common.dto.ApiResponse;
import com.agrodairy.common.dto.PageResponse;
import com.agrodairy.subscription.dto.CreateSubscriptionRequest;
import com.agrodairy.subscription.dto.SkipSubscriptionRequest;
import com.agrodairy.subscription.dto.SubscriptionResponse;
import com.agrodairy.subscription.dto.UpdateSubscriptionRequest;
import com.agrodairy.subscription.entity.SubscriptionStatus;
import com.agrodairy.subscription.service.SubscriptionService;
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
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) SubscriptionStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<SubscriptionResponse> page = subscriptionService.list(principal, status, pageable);
        return ResponseEntity.ok(ApiResponse.of(page.getContent(), PageResponse.of(page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> get(@AuthenticationPrincipal AuthenticatedUser principal,
                                                                  @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(subscriptionService.get(id, principal)));
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> create(@AuthenticationPrincipal AuthenticatedUser principal,
                                                                     @Valid @RequestBody CreateSubscriptionRequest request) {
        SubscriptionResponse response = subscriptionService.create(principal.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> update(@AuthenticationPrincipal AuthenticatedUser principal,
                                                                     @PathVariable UUID id,
                                                                     @RequestBody UpdateSubscriptionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(subscriptionService.update(id, principal, request)));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> pause(@AuthenticationPrincipal AuthenticatedUser principal,
                                                                     @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(subscriptionService.pause(id, principal)));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> resume(@AuthenticationPrincipal AuthenticatedUser principal,
                                                                      @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(subscriptionService.resume(id, principal)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> cancel(@AuthenticationPrincipal AuthenticatedUser principal,
                                                                      @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(subscriptionService.cancel(id, principal)));
    }

    @PostMapping("/{id}/skip")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> skip(@AuthenticationPrincipal AuthenticatedUser principal,
                                                                    @PathVariable UUID id,
                                                                    @Valid @RequestBody SkipSubscriptionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(subscriptionService.skip(id, principal, request)));
    }
}
