package com.agrodairy.animal.controller;

import com.agrodairy.animal.dto.AnimalResponse;
import com.agrodairy.animal.dto.CreateAnimalRequest;
import com.agrodairy.animal.dto.CreateProductionRecordRequest;
import com.agrodairy.animal.dto.ProductionRecordResponse;
import com.agrodairy.animal.dto.UpdateAnimalRequest;
import com.agrodairy.animal.entity.AnimalStatus;
import com.agrodairy.animal.entity.AnimalType;
import com.agrodairy.animal.service.AnimalService;
import com.agrodairy.auth.security.AuthenticatedUser;
import com.agrodairy.common.dto.ApiResponse;
import com.agrodairy.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/animals")
@PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
public class AnimalController {

    private final AnimalService animalService;

    public AnimalController(AnimalService animalService) {
        this.animalService = animalService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AnimalResponse>>> list(
            @RequestParam(required = false) AnimalStatus status,
            @RequestParam(required = false) AnimalType type,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AnimalResponse> page = animalService.list(status, type, pageable);
        return ResponseEntity.ok(ApiResponse.of(page.getContent(), PageResponse.of(page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AnimalResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(animalService.get(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AnimalResponse>> create(@Valid @RequestBody CreateAnimalRequest request) {
        AnimalResponse response = animalService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AnimalResponse>> update(@PathVariable UUID id, @RequestBody UpdateAnimalRequest request) {
        return ResponseEntity.ok(ApiResponse.of(animalService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        animalService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/production")
    public ResponseEntity<ApiResponse<List<ProductionRecordResponse>>> listProduction(
            @PathVariable UUID id,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProductionRecordResponse> page = animalService.listProduction(id, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.of(page.getContent(), PageResponse.of(page)));
    }

    @PostMapping("/{id}/production")
    public ResponseEntity<ApiResponse<ProductionRecordResponse>> recordProduction(
            @PathVariable UUID id,
            @Valid @RequestBody CreateProductionRecordRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        ProductionRecordResponse response = animalService.recordProduction(id, request, principal.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }
}
