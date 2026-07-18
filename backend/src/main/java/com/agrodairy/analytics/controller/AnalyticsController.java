package com.agrodairy.analytics.controller;

import com.agrodairy.analytics.dto.DashboardResponse;
import com.agrodairy.analytics.service.AnalyticsService;
import com.agrodairy.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> dashboard() {
        return ResponseEntity.ok(ApiResponse.of(analyticsService.dashboard()));
    }
}
