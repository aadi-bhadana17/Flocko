package com.kilgore.fooddeliveryapp.catalog.api.impl;

import com.kilgore.fooddeliveryapp.catalog.api.CatalogFacade;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.AddonSummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.FoodSummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantExtendedSummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.catalog.model.Addon;
import com.kilgore.fooddeliveryapp.catalog.model.Food;
import com.kilgore.fooddeliveryapp.catalog.model.Restaurant;
import com.kilgore.fooddeliveryapp.catalog.repository.*;
import com.kilgore.fooddeliveryapp.catalog.util.CatalogMapper;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.common.exceptions.RestaurantNotFoundException;
import com.kilgore.fooddeliveryapp.common.shared.kitchen.KitchenLoadIndicator;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CatalogFacadeImpl implements CatalogFacade {
    
    private final RestaurantRepository restaurantRepository;
    private final FoodRepository foodRepository;
    private final AddonRepository addonRepository;
    private final CatalogMapper catalogMapper;

    public CatalogFacadeImpl(RestaurantRepository restaurantRepository, FoodRepository foodRepository, AddonRepository addonRepository, CatalogMapper catalogMapper) {
        this.restaurantRepository = restaurantRepository;
        this.foodRepository = foodRepository;
        this.addonRepository = addonRepository;
        this.catalogMapper = catalogMapper;
    }

    @Override
    public RestaurantSummary getRestaurant(Long restaurantId) {
        Restaurant restaurant = fetchRestaurant(restaurantId);
        return catalogMapper.toRestaurantSummary(restaurant);
    }

    @Override
    public RestaurantExtendedSummary getRestaurantExtended(Long restaurantId) {
        Restaurant restaurant = fetchRestaurant(restaurantId);
        return catalogMapper.toRestaurantExtendedSummary(restaurant);
    }

    @Override
    public FoodSummary getFood(Long foodId) {
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new EntityNotFoundException("Food with id " + foodId + " not found"));

        return catalogMapper.toFoodSummary(food);
    }

    @Override
    public List<AddonSummary> getAddons(List<Long> addonIds) {
            return addonIds.stream()
                    .map(this::fetchAddon)
                    .map(catalogMapper::toAddonSummary)
                    .toList();
    }

    @Override
    public boolean isFoodAvailable(Long foodId) {
        Food food = fetchFood(foodId);
        return food.isAvailable();
    }

    @Override
    public boolean isRestaurantOpen(Long restaurantId) {
        Restaurant restaurant = fetchRestaurant(restaurantId);
        return restaurant.isOpen();
    }


    @Override
    public String getRestaurantName(Long restaurantId) {
        Restaurant restaurant = fetchRestaurant(restaurantId);
        return restaurant.getRestaurantName();
    }

    @Override
    public String getFoodName(Long foodId) {
        Food food = fetchFood(foodId);
        return food.getFoodName();
    }

    @Override
    public void setKitchenLoadIndicator(Long restaurantId, KitchenLoadIndicator indicator) {
        Restaurant restaurant = fetchRestaurant(restaurantId);
        restaurant.setKitchenStatus(indicator);
        restaurantRepository.save(restaurant);
    }

    @Override
    public boolean isAddonAvailable(Long addonId) {
        Addon addon = fetchAddon(addonId);
        return addon.isAvailable();
    }

    private Restaurant fetchRestaurant(Long restaurantId) {
        return restaurantRepository.findById(restaurantId).orElseThrow(() ->
                new RestaurantNotFoundException(restaurantId));
    }

    private Food fetchFood(Long foodId) {
        return foodRepository.findById(foodId).orElseThrow(() ->
                new EntityNotFoundException("Food not found with id: " + foodId));
    }

    private Addon fetchAddon(Long addonId) {
        return addonRepository.findById(addonId)
                .orElseThrow(() -> new EntityNotFoundException("Addon not found with id: " + addonId));
    }
}