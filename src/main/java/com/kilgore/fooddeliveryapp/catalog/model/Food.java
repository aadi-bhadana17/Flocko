package com.kilgore.fooddeliveryapp.catalog.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Food {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long foodId;
    private String foodName;
    private String foodDescription;
    private BigDecimal foodPrice;

    @Column(name = "restaurant_restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "food_category_category_id", nullable = false)
    private Long categoryId;

    @Column(length = 1000)
    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> images = new ArrayList<>();


    private boolean vegetarian;
    private boolean available;

    private LocalDateTime createdAt;

}
