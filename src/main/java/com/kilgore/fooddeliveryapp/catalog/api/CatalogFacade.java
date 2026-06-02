package com.kilgore.fooddeliveryapp.catalog.api;

import com.kilgore.fooddeliveryapp.catalog.dto.summary.*;
import com.kilgore.fooddeliveryapp.common.shared.kitchen.KitchenLoadIndicator;

import java.util.List;

public interface CatalogFacade {
    RestaurantSummary getRestaurant(Long restaurantId);
    RestaurantExtendedSummary getRestaurantExtended(Long restaurantId);
    FoodSummary getFood(Long foodId);
    List<AddonSummary> getAddons(List<Long> addonIds);
    boolean isFoodAvailable(Long foodId);
    boolean isRestaurantOpen(Long restaurantId);
    String getRestaurantName(Long restaurantId);
    String getFoodName(Long foodId);
    void setKitchenLoadIndicator(Long restaurantId, KitchenLoadIndicator indicator);
    boolean isAddonAvailable(Long addonId);
}