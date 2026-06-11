package com.kilgore.fooddeliveryapp.ordering.service;

import com.kilgore.fooddeliveryapp.catalog.api.CatalogFacade;
import com.kilgore.fooddeliveryapp.common.util.UserAuthorization;
import com.kilgore.fooddeliveryapp.ordering.dto.request.CreateReviewRequest;
import com.kilgore.fooddeliveryapp.ordering.dto.response.ReviewResponse;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.common.exceptions.RestaurantNotFoundException;
import com.kilgore.fooddeliveryapp.catalog.model.Restaurant;
import com.kilgore.fooddeliveryapp.ordering.model.Review;
import com.kilgore.fooddeliveryapp.identity.api.UserFacade;
import com.kilgore.fooddeliveryapp.ordering.repository.OrderRepository;
import com.kilgore.fooddeliveryapp.catalog.repository.RestaurantRepository;
import com.kilgore.fooddeliveryapp.ordering.repository.ReviewRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserAuthorization userAuthorization;
    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserFacade userFacade;
    private final CatalogFacade catalogFacade;

    public ReviewService(ReviewRepository reviewRepository, UserAuthorization userAuthorization, OrderRepository orderRepository, RestaurantRepository restaurantRepository, UserFacade userFacade, CatalogFacade catalogFacade) {
        this.reviewRepository = reviewRepository;
        this.userAuthorization = userAuthorization;
        this.orderRepository = orderRepository;
        this.restaurantRepository = restaurantRepository;

        this.userFacade = userFacade;
        this.catalogFacade = catalogFacade;
    }

    public List<ReviewResponse> getReviewsForRestaurant(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(restaurantId));

        List<Review> reviews = reviewRepository.findByRestaurantId(restaurantId);
        if(reviews.isEmpty()) {
            throw new EntityNotFoundException("There are no reviews posted for this restaurant.");
        }

        return reviews.stream()
                .map(this::mapToReviewResponse)
                .toList();
    }

    public ReviewResponse getReview(Long restaurantId, Long reviewId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(restaurantId));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review with id " + reviewId + " not found."));

        return mapToReviewResponse(review);
    }

    public List<ReviewResponse> getReviewsForUser() {
        Long userId = userAuthorization.authorizeUserId();

        List<Review> reviews = reviewRepository.findByUserId(userId);

        return reviews.stream()
                .map(this::mapToReviewResponse)
                .toList();
    }

    @Transactional
    public ReviewResponse createReview(Long restaurantId, CreateReviewRequest request) {
        Long userId = userAuthorization.authorizeUserId();

        LocalDateTime fifteenDaysAgo = LocalDateTime.now().minusDays(15);

        if(reviewRepository.existsByUserIdAndRestaurantIdAndPostedAtAfter(
                userId, restaurantId, fifteenDaysAgo)) {
            throw new IllegalStateException("You have already reviewed this restaurant within the last 15 days.");
        }

        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);

        if(!orderRepository.existsByUserIdAndRestaurantIdAndCreatedAtAfter(
                userId, restaurantId, ninetyDaysAgo)) {
            throw new IllegalStateException("You can only review a restaurant if you have placed an order within the last 90 days.");
        }

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(restaurantId));

        Review review = new Review();

        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setUserId(userId);
        review.setRestaurantId(restaurant.getRestaurantId());
        review.setPostedAt(LocalDateTime.now());

        reviewRepository.save(review);

        long totalReviews;
        if(restaurant.getTotalReviews() == null) {
            totalReviews = 1;
        } else {
            totalReviews = restaurant.getTotalReviews() + 1;
        }

        BigDecimal avgRating;

        if(restaurant.getAvgRating() == null) {
            avgRating = BigDecimal.ZERO;
        } else {
            avgRating = restaurant.getAvgRating();
        }

        BigDecimal updatedRating  = avgRating.add(BigDecimal.valueOf(review.getRating()))
                .divide(BigDecimal.valueOf(totalReviews), 2, RoundingMode.HALF_UP);


        restaurant.setTotalReviews(totalReviews);
        restaurant.setAvgRating(updatedRating);

        restaurantRepository.save(restaurant);

        return mapToReviewResponse(review);
    }

    private ReviewResponse mapToReviewResponse(Review review) {
        return new ReviewResponse(
                review.getReviewId(),
                review.getRating(),
                review.getComment(),
                userFacade.getUserById(review.getUserId()),
                catalogFacade.getRestaurantById(review.getRestaurantId()),
                review.getPostedAt()
        );
    }
}
