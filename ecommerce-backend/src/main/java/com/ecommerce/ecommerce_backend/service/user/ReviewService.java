package com.ecommerce.ecommerce_backend.service.user;

import com.ecommerce.ecommerce_backend.model.*;
import com.ecommerce.ecommerce_backend.repository.ProductRepository;
import com.ecommerce.ecommerce_backend.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public Review addReview(User user, Product product, int rating, String comment) {
        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(rating);
        review.setComment(comment);
        review.setVerifiedPurchase(true);
        review.setCreatedAt(LocalDateTime.now());

        Review savedReview = reviewRepository.save(review);

        // Update product's review count and average rating
        int currentCount = product.getReviewCount();
        double currentAvg = product.getAverageRating();

        // Calculate new average: ((oldAvg * oldCount) + newRating) / newCount
        int newCount = currentCount + 1;
        double newAverage = ((currentAvg * currentCount) + rating) / newCount;

        product.setReviewCount(newCount);
        product.setAverageRating(newAverage);
        productRepository.save(product);

        return savedReview;
    }

    public List<Review> getProductReviews(Product product) {
        return reviewRepository.findByProduct(product);
    }

    public boolean canUserReview(User user, Product product) {
        // User can review if they haven't already reviewed this product
        return !reviewRepository.existsByUserAndProduct(user, product);
    }
}
