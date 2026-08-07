package com.kilgore.fooddeliveryapp.catalog.api;

import com.kilgore.fooddeliveryapp.catalog.dto.summary.*;
import com.kilgore.fooddeliveryapp.common.enums.KitchenLoadIndicator;
import com.kilgore.fooddeliveryapp.ordering.dto.response.KitchenLoadResponse;

import java.math.BigDecimal;
import java.util.List;

public interface CatalogFacade {
    RestaurantSummary getRestaurantById(Long restaurantId);
    RestaurantExtendedSummary getRestaurantExtendedById(Long restaurantId);
    FoodSummary getFoodById(Long foodId);
    List<FoodSummary> getFoodListByIds(List<Long> foodIds);
    List<AddonSummary> getAddonsByIds(List<Long> addonIds);
    boolean isFoodAvailable(Long foodId);
    boolean isRestaurantOpen(Long restaurantId);
    String getRestaurantName(Long restaurantId);
    String getFoodName(Long foodId);
    void setKitchenLoadIndicator(Long restaurantId, KitchenLoadIndicator indicator);
    boolean isAddonAvailable(Long addonId);
    KitchenLoadResponse getKitchenLoadResponse(Long restaurantId, long currentOrders);
    BigDecimal getFoodPriceById(Long foodId);

    String suspendRestaurant(Long restaurantId);
    String reactivateRestaurant(Long restaurantId);

    List<Long> getOwnedRestaurantIds(Long userId);
}