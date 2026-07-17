package com.agrodairy.review.service;

import com.agrodairy.auth.entity.User;
import com.agrodairy.auth.repository.UserRepository;
import com.agrodairy.common.exception.ApiException;
import com.agrodairy.common.exception.NotFoundException;
import com.agrodairy.product.entity.Product;
import com.agrodairy.product.repository.ProductRepository;
import com.agrodairy.review.dto.CreateReviewRequest;
import com.agrodairy.review.dto.ReviewResponse;
import com.agrodairy.review.entity.Review;
import com.agrodairy.review.repository.ReviewRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository, UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public Page<ReviewResponse> listByProduct(UUID productId, Pageable pageable) {
        return reviewRepository.findByProductId(productId, pageable).map(ReviewResponse::from);
    }

    @Transactional
    public ReviewResponse create(UUID productId, UUID userId, CreateReviewRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (reviewRepository.existsByProductIdAndUserId(productId, userId)) {
            throw new ApiException(HttpStatus.CONFLICT, "CONFLICT", "You have already reviewed this product");
        }

        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(request.rating())
                .comment(request.comment())
                .build();
        try {
            reviewRepository.saveAndFlush(review);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "CONFLICT", "You have already reviewed this product");
        }
        return ReviewResponse.from(review);
    }
}
