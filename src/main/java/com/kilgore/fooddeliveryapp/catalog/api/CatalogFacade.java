package com.kilgore.fooddeliveryapp.catalog.api;

import com.kilgore.fooddeliveryapp.catalog.dto.summary.*;

import java.util.List;

public interface CatalogFacade {
    RestaurantSummary getRestaurant(Long restaurantId);
    FoodSummary getFood(Long foodId);
    List<AddonSummary> getAddons(List<Long> addonIds);
    boolean isFoodAvailable(Long foodId);
    boolean isRestaurantOpen(Long restaurantId);
}