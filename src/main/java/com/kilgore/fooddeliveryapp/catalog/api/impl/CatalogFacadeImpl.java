package com.kilgore.fooddeliveryapp.catalog.api.impl;

import com.kilgore.fooddeliveryapp.catalog.api.CatalogFacade;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.AddonSummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.FoodSummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantExtendedSummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.catalog.model.Addon;
import com.kilgore.fooddeliveryapp.catalog.model.Food;
import com.kilgore.fooddeliveryapp.catalog.model.Restaurant;
import com.kilgore.fooddeliveryapp.catalog.model.RestaurantStatus;
import com.kilgore.fooddeliveryapp.catalog.repository.*;
import com.kilgore.fooddeliveryapp.catalog.util.CatalogMapper;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.common.exceptions.RestaurantNotFoundException;
import com.kilgore.fooddeliveryapp.common.enums.KitchenLoadIndicator;
import com.kilgore.fooddeliveryapp.ordering.dto.response.KitchenLoadResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
    public RestaurantSummary getRestaurantById(Long restaurantId) {
        Restaurant restaurant = fetchRestaurant(restaurantId);
        return catalogMapper.toRestaurantSummary(restaurant);
    }

    @Override
    public RestaurantExtendedSummary getRestaurantExtendedById(Long restaurantId) {
        Restaurant restaurant = fetchRestaurant(restaurantId);
        return catalogMapper.toRestaurantExtendedSummary(restaurant);
    }

    @Override
    public FoodSummary getFoodById(Long foodId) {
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new EntityNotFoundException("Food with id " + foodId + " not found"));

        return catalogMapper.toFoodSummary(food);
    }

    @Override
    public List<FoodSummary> getFoodListByIds(List<Long> foodIds) {
        return foodIds.stream()
                .map(this::getFoodById)
                .toList();
    }

    @Override
    public List<AddonSummary> getAddonsByIds(List<Long> addonIds) {
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

    @Override
    public KitchenLoadResponse getKitchenLoadResponse(Long restaurantId, long currentOrders) {
        Restaurant restaurant = fetchRestaurant(restaurantId);

        return new KitchenLoadResponse(
                restaurantId,
                restaurant.getRestaurantName(),
                restaurant.getKitchenStatus(),
                currentOrders,
                "The kitchen load status has been updated to " + restaurant.getKitchenStatus()
                        + " according to the request made by the restaurant staff or owner." +
                        " The current number of orders in the kitchen is " + currentOrders + "."
        );
    }

    @Override
    public BigDecimal getFoodPriceById(Long foodId) {
        Food food = fetchFood(foodId);
        return food.getFoodPrice();
    }

    @Override
    public String suspendRestaurant(Long restaurantId) {
        Restaurant restaurant = fetchRestaurant(restaurantId);

        restaurant.setOpen(false);
        restaurant.setRestaurantStatus(RestaurantStatus.SUSPENDED);
        restaurantRepository.save(restaurant);

        return "Restaurant " + restaurant.getRestaurantName() + " has been suspended.";
    }

    @Override
    public String reactivateRestaurant(Long restaurantId) {
        Restaurant restaurant = fetchRestaurant(restaurantId);

        restaurant.setOpen(true);
        restaurant.setRestaurantStatus(RestaurantStatus.ACTIVE);
        restaurantRepository.save(restaurant);

        return "Restaurant " + restaurant.getRestaurantName() + " has been activated.";
    }

    @Override
    public List<Long> getOwnedRestaurantIds(Long userId) {

        return restaurantRepository.findByOwnerUserId(userId)
                .stream()
                .map(Restaurant::getRestaurantId)
                .toList();
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