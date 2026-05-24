package com.kilgore.fooddeliveryapp.ordering.repository;

import com.kilgore.fooddeliveryapp.ordering.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review,Long> {

    boolean existsByUserUserIdAndRestaurantRestaurantIdAndPostedAtAfter(Long userId, Long restaurantId, LocalDateTime postedAt);

    List<Review> findByRestaurantRestaurantId(Long restaurantId);

    List<Review> findByUserUserId(Long userId);
}
