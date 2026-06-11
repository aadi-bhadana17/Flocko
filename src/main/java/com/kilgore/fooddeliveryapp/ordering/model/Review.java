package com.kilgore.fooddeliveryapp.ordering.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    private int rating;
    private String comment;

    @Column(name = "user_user_id", nullable = false)
    private Long userId;

    @Column(name = "restaurant_restaurant_id", nullable = false)
    private Long restaurantId;

    private LocalDateTime postedAt;
}
