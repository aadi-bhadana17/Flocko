package com.kilgore.fooddeliveryapp.ordering.dto.response;

import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.identity.dto.summary.UserSummary;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReviewResponse {
    private Long reviewId;
    private int rating;
    private String comment;
    private UserSummary reviewer;
    private RestaurantSummary restaurant;
    private LocalDateTime postedAt;
}
