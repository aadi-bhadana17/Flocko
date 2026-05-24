package com.kilgore.fooddeliveryapp.catalog.dto.publicResponse;

import com.kilgore.fooddeliveryapp.catalog.model.CuisineType;
import com.kilgore.fooddeliveryapp.catalog.model.RestaurantAddress;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantPublicResponse {
    private Long id;
    private String name;
    private CuisineType cuisineType;
    private RestaurantAddress address;
    private BigDecimal avgRating;
    private List<String> images;
    private boolean isOpen;

}
