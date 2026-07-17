package com.agrodairy.review.repository;

import com.agrodairy.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Page<Review> findByProductId(UUID productId, Pageable pageable);

    boolean existsByProductIdAndUserId(UUID productId, UUID userId);
}
