package com.kilgore.fooddeliveryapp.service;

import com.kilgore.fooddeliveryapp.dto.publicResponse.MenuAddonResponse;
import com.kilgore.fooddeliveryapp.dto.publicResponse.MenuCategoryResponse;
import com.kilgore.fooddeliveryapp.dto.publicResponse.MenuItemResponse;
import com.kilgore.fooddeliveryapp.dto.publicResponse.RestaurantMenuResponse;
import com.kilgore.fooddeliveryapp.dto.publicResponse.RestaurantPublicResponse;
import com.kilgore.fooddeliveryapp.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.exceptions.RestaurantNotFoundException;
import com.kilgore.fooddeliveryapp.model.*;
import com.kilgore.fooddeliveryapp.repository.FoodRepository;
import com.kilgore.fooddeliveryapp.repository.RestaurantRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;

@Service
public class PublicService {
    // This service class will be used by PublicController to handle business logic related to public endpoints, 
    // such as fetching restaurant information, menu items, etc.

    private final RestaurantRepository restaurantRepository;
    private final FoodRepository foodRepository;

    public PublicService(RestaurantRepository restaurantRepository, FoodRepository foodRepository) {
        this.restaurantRepository = restaurantRepository;
        this.foodRepository = foodRepository;
    }

    //(filters: city, cuisineType, isOpen)
    public List<RestaurantPublicResponse> getRestaurants(String city, String cuisineType) {
        List<Restaurant> restaurants = restaurantRepository.findAll().stream()
                .filter(restaurant -> ((city == null) || restaurant.getAddress().getCity().equalsIgnoreCase(city)) &&
                        ((cuisineType == null) || restaurant.getCuisineType().name().equalsIgnoreCase(cuisineType)) &&
                        isRestaurantAvailable(restaurant))
                .toList();

        return restaurants.stream()
                .map(this::createRestaurantPublicResponse)
                .toList();
    }

    private RestaurantPublicResponse createRestaurantPublicResponse(Restaurant restaurant) {
        return new RestaurantPublicResponse(
                restaurant.getRestaurantId(),
                restaurant.getRestaurantName(),
                restaurant.getCuisineType(),
                restaurant.getAddress(),
                restaurant.getAvgRating(),
                restaurant.getImages(),
                restaurant.isOpen()
        );
    }

    public List<RestaurantPublicResponse> getRestaurantByName(String name) {

        return restaurantRepository.findAll().stream()
                .filter(r -> r.getRestaurantName().toLowerCase().contains(name.toLowerCase()) && isRestaurantAvailable(r))
                .map(this::createRestaurantPublicResponse)
                .toList();
    }

    public RestaurantPublicResponse getRestaurantById(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant with id " + id + " not found"));

        if (!isRestaurantAvailable(restaurant)) throw new RestaurantNotFoundException(id);

        return createRestaurantPublicResponse(restaurant);
    }

    @Cacheable(value = "restaurantMenu", key = "#restaurantId")
    public RestaurantMenuResponse getRestaurantMenu(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findMenuById(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant with id " + restaurantId + " not found"));

        if (!isRestaurantAvailable(restaurant)) throw new RestaurantNotFoundException(restaurantId);


        List<Food> allFoods = foodRepository.findAllByRestaurant_RestaurantId(restaurantId);

        List<Category> uniqueCategories = restaurant.getCategories().stream()
                .collect(LinkedHashMap<Long, Category>::new,
                        (map, category) -> map.putIfAbsent(category.getCategoryId(), category),
                        LinkedHashMap::putAll)
                .values().stream()
                .toList();

        List<MenuCategoryResponse> categories = uniqueCategories.stream()
                .map(category -> createMenuCategoryResponse(category, allFoods))
                .toList();

        return createRestaurantMenuResponse(restaurant, categories);
    }

    private MenuItemResponse createMenuItemResponse(Food food) {
        return new MenuItemResponse(
                food.getFoodId(),
                food.getFoodName(),
                food.getFoodDescription(),
                food.getFoodPrice(),
                food.getImages(),
                food.isVegetarian(),
                food.isAvailable()
        );
    }

    private MenuCategoryResponse createMenuCategoryResponse(Category category, List<Food> allFoods) {
        List<MenuItemResponse> foods = allFoods.stream()
                .filter(food -> food.getFoodCategory().getCategoryId().equals(category.getCategoryId()))
                .map(this::createMenuItemResponse)
                .toList();

        List<MenuAddonResponse> addons = category.getAvailableAddons().stream()
                .map(this::createMenuAddonResponse)
                .toList();

        return new MenuCategoryResponse(
                category.getCategoryId(),
                category.getCategoryName(),
                category.getDescription(),
                category.getDisplayOrder(),
                foods,
                addons
        );
    }

    private MenuAddonResponse createMenuAddonResponse(Addon addon) {
        return new MenuAddonResponse(
                addon.getAddonId(),
                addon.getAddonName(),
                addon.getPrice(),
                addon.isAvailable()
        );
    }

    private RestaurantMenuResponse createRestaurantMenuResponse(Restaurant restaurant,
                                                                 List<MenuCategoryResponse> categories) {
        return new RestaurantMenuResponse(
                restaurant.getRestaurantId(),
                restaurant.getRestaurantName(),
                restaurant.getCuisineType(),
                restaurant.isOpen(),
                categories
        );
    }

    private boolean isRestaurantAvailable(Restaurant restaurant) {
        return (restaurant.getRestaurantStatus() == null || restaurant.getRestaurantStatus() == RestaurantStatus.ACTIVE)
                && restaurant.isOpen();
    }
}
