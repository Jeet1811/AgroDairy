package com.agrodairy.ai.controller;

import com.agrodairy.ai.dto.AnomalyAlertResponse;
import com.agrodairy.ai.dto.DemandForecastResponse;
import com.agrodairy.ai.entity.Severity;
import com.agrodairy.ai.service.AnomalyAlertService;
import com.agrodairy.ai.service.DemandForecastService;
import com.agrodairy.common.dto.ApiResponse;
import com.agrodairy.common.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
@PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
public class AiController {

    private final AnomalyAlertService anomalyAlertService;
    private final DemandForecastService demandForecastService;

    public AiController(AnomalyAlertService anomalyAlertService, DemandForecastService demandForecastService) {
        this.anomalyAlertService = anomalyAlertService;
        this.demandForecastService = demandForecastService;
    }

    @GetMapping("/anomalies")
    public ResponseEntity<ApiResponse<List<AnomalyAlertResponse>>> listAnomalies(
            @RequestParam(required = false) Boolean acknowledged,
            @RequestParam(required = false) Severity severity,
            @PageableDefault(size = 20, sort = "detectedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AnomalyAlertResponse> page = anomalyAlertService.list(acknowledged, severity, pageable);
        return ResponseEntity.ok(ApiResponse.of(page.getContent(), PageResponse.of(page)));
    }

    @PatchMapping("/anomalies/{id}/ack")
    public ResponseEntity<ApiResponse<AnomalyAlertResponse>> acknowledgeAnomaly(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(anomalyAlertService.acknowledge(id)));
    }

    @GetMapping("/demand-forecast")
    public ResponseEntity<ApiResponse<DemandForecastResponse>> demandForecast(
            @RequestParam UUID productId,
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(ApiResponse.of(demandForecastService.forecast(productId, days)));
    }
}
