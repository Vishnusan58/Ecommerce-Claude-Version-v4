package com.ecommerce.ecommerce_backend.repository;

import com.ecommerce.ecommerce_backend.model.Product;
import com.ecommerce.ecommerce_backend.model.Review;
import com.ecommerce.ecommerce_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProduct(Product product);

    boolean existsByUserAndProduct(User user, Product product);

    long countByProduct(Product product);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.product = :product")
    double getAverageRatingByProduct(@Param("product") Product product);
}
