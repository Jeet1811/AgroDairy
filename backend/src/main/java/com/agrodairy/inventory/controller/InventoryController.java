package com.agrodairy.inventory.controller;

import com.agrodairy.common.dto.ApiResponse;
import com.agrodairy.common.dto.PageResponse;
import com.agrodairy.inventory.dto.CreateInventoryBatchRequest;
import com.agrodairy.inventory.dto.InventoryAlertsResponse;
import com.agrodairy.inventory.dto.InventoryBatchResponse;
import com.agrodairy.inventory.dto.UpdateInventoryBatchRequest;
import com.agrodairy.inventory.entity.BatchStatus;
import com.agrodairy.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/inventory")
@PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/batches")
    public ResponseEntity<ApiResponse<List<InventoryBatchResponse>>> listBatches(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) BatchStatus status,
            @RequestParam(required = false) Integer expiringWithinDays,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<InventoryBatchResponse> page = inventoryService.listBatches(productId, status, expiringWithinDays, pageable);
        return ResponseEntity.ok(ApiResponse.of(page.getContent(), PageResponse.of(page)));
    }

    @PostMapping("/batches")
    public ResponseEntity<ApiResponse<InventoryBatchResponse>> createBatch(@Valid @RequestBody CreateInventoryBatchRequest request) {
        InventoryBatchResponse response = inventoryService.createBatch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @PatchMapping("/batches/{id}")
    public ResponseEntity<ApiResponse<InventoryBatchResponse>> updateBatch(
            @PathVariable UUID id, @RequestBody UpdateInventoryBatchRequest request) {
        return ResponseEntity.ok(ApiResponse.of(inventoryService.updateBatchStatus(id, request)));
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<InventoryAlertsResponse>> alerts() {
        return ResponseEntity.ok(ApiResponse.of(inventoryService.alerts()));
    }
}
