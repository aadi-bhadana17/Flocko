package com.kilgore.fooddeliveryapp.ordering.controller;

import com.kilgore.fooddeliveryapp.ordering.dto.request.CreateReviewRequest;
import com.kilgore.fooddeliveryapp.ordering.dto.response.ReviewResponse;
import com.kilgore.fooddeliveryapp.ordering.service.ReviewService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/reviews/restaurant/{restaurantId}")
    public List<ReviewResponse> getReviewsForRestaurant(@PathVariable Long restaurantId) {
        return reviewService.getReviewsForRestaurant(restaurantId);
    }

    @GetMapping("/reviews/restaurant/{restaurantId}/{reviewId}")
    public ReviewResponse getReview(@PathVariable Long restaurantId, @PathVariable Long reviewId) {
        return reviewService.getReview(restaurantId, reviewId);
    }

    @GetMapping("/user/reviews")
    public List<ReviewResponse> getReviewsForUser() {
        return reviewService.getReviewsForUser();
    }

    @PostMapping("/restaurant/{restaurantId}")
    public ReviewResponse createReview(@PathVariable Long restaurantId,
                                     @RequestBody CreateReviewRequest request) {
        return reviewService.createReview(restaurantId, request);
    }
}
