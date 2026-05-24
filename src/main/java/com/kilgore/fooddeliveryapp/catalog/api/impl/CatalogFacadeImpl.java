package com.kilgore.fooddeliveryapp.catalog.api.impl;

import com.kilgore.fooddeliveryapp.catalog.api.CatalogFacade;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.AddonSummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.FoodSummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.catalog.repository.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CatalogFacadeImpl implements CatalogFacade {
    
    private final RestaurantRepository restaurantRepository;
    private final FoodRepository foodRepository;

    public CatalogFacadeImpl(RestaurantRepository restaurantRepository, FoodRepository foodRepository) {
        this.restaurantRepository = restaurantRepository;
        this.foodRepository = foodRepository;
    }

    @Override
    public RestaurantSummary getRestaurant(Long restaurantId) {
        // Implementation
        return null;
    }

    @Override
    public FoodSummary getFood(Long foodId) {
        return null;
    }

    @Override
    public List<AddonSummary> getAddons(List<Long> addonIds) {
        return List.of();
    }

    @Override
    public boolean isFoodAvailable(Long foodId) {
        // Implementation
        return false;
    }

    @Override
    public boolean isRestaurantOpen(Long restaurantId) {
        return false;
    }

    // ... other methods
}