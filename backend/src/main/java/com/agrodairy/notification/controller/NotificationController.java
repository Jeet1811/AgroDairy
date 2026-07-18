package com.agrodairy.notification.controller;

import com.agrodairy.auth.security.AuthenticatedUser;
import com.agrodairy.common.dto.ApiResponse;
import com.agrodairy.common.dto.PageResponse;
import com.agrodairy.notification.dto.NotificationResponse;
import com.agrodairy.notification.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) Boolean isRead,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<NotificationResponse> page = notificationService.list(principal.id(), isRead, pageable);
        return ResponseEntity.ok(ApiResponse.of(page.getContent(), PageResponse.of(page)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(notificationService.markRead(id, principal.id())));
    }
}
