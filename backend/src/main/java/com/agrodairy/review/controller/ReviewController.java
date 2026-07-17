package com.agrodairy.review.controller;

import com.agrodairy.auth.security.AuthenticatedUser;
import com.agrodairy.common.dto.ApiResponse;
import com.agrodairy.common.dto.PageResponse;
import com.agrodairy.review.dto.CreateReviewRequest;
import com.agrodairy.review.dto.ReviewResponse;
import com.agrodairy.review.service.ReviewService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> list(
            @PathVariable UUID productId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ReviewResponse> page = reviewService.listByProduct(productId, pageable);
        return ResponseEntity.ok(ApiResponse.of(page.getContent(), PageResponse.of(page)));
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ReviewResponse>> create(
            @PathVariable UUID productId,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateReviewRequest request) {
        ReviewResponse response = reviewService.create(productId, principal.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }
}
