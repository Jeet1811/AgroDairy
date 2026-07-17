package com.agrodairy.review.dto;

import com.agrodairy.review.entity.Review;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID productId,
        UUID userId,
        int rating,
        String comment,
        Instant createdAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getProduct().getId(),
                review.getUser().getId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt());
    }
}
