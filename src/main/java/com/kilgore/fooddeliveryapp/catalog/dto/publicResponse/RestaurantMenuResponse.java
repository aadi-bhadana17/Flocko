package com.kilgore.fooddeliveryapp.catalog.dto.publicResponse;

import com.kilgore.fooddeliveryapp.catalog.model.CuisineType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantMenuResponse {
    private Long restaurantId;
    private String restaurantName;
    private CuisineType cuisineType;
    private boolean isOpen;
    private List<MenuCategoryResponse> categories;
}

