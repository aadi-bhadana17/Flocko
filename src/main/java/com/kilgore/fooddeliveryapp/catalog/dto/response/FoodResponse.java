package com.kilgore.fooddeliveryapp.catalog.dto.response;

import com.kilgore.fooddeliveryapp.catalog.dto.summary.CategorySummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantSummary;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FoodResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private List<String> images;

    private CategorySummary category;
    private RestaurantSummary restaurant;

    private boolean vegetarian;
    private boolean available;

    private LocalDateTime createdAt;
}
