package com.kilgore.fooddeliveryapp.catalog.util;

import com.kilgore.fooddeliveryapp.catalog.dto.summary.AddonSummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.FoodSummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantExtendedSummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.catalog.model.Addon;
import com.kilgore.fooddeliveryapp.catalog.model.Food;
import com.kilgore.fooddeliveryapp.catalog.model.Restaurant;

import org.springframework.stereotype.Component;

@Component
public class CatalogMapper {

    public RestaurantSummary toRestaurantSummary(Restaurant restaurant) {
        return new RestaurantSummary(
                restaurant.getRestaurantId(),
                restaurant.getRestaurantName(),
                restaurant.getCuisineType(),
                restaurant.getAvgRating()
        );
    }

    public RestaurantExtendedSummary toRestaurantExtendedSummary(Restaurant restaurant) {
        return new RestaurantExtendedSummary(
                restaurant.getRestaurantId(),
                restaurant.getRestaurantName(),
                restaurant.getOpeningTime(),
                restaurant.getClosingTime()
        );
    }

    public FoodSummary toFoodSummary(Food food) {
        return new FoodSummary(
                food.getFoodId(),
                food.getFoodName(),
                food.getFoodDescription(),
                food.getFoodPrice(),
                food.isVegetarian(),
                food.getRestaurantId()
        );
    }

    public AddonSummary toAddonSummary(Addon addon) {
        return new AddonSummary(
                addon.getAddonId(),
                addon.getAddonName(),
                addon.getPrice()
        );
    }

}
