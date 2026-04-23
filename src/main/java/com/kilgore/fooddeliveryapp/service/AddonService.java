package com.kilgore.fooddeliveryapp.service;

import com.kilgore.fooddeliveryapp.dto.request.CreateAddonRequest;
import com.kilgore.fooddeliveryapp.dto.request.AddonAvailableStatusRequest;
import com.kilgore.fooddeliveryapp.dto.response.AddonResponse;
import com.kilgore.fooddeliveryapp.dto.summary.CategorySummary;
import com.kilgore.fooddeliveryapp.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.exceptions.RestaurantNotFoundException;
import com.kilgore.fooddeliveryapp.model.Addon;
import com.kilgore.fooddeliveryapp.model.Category;
import com.kilgore.fooddeliveryapp.model.Restaurant;
import com.kilgore.fooddeliveryapp.repository.AddonRepository;
import com.kilgore.fooddeliveryapp.repository.CategoryRepository;
import com.kilgore.fooddeliveryapp.repository.RestaurantRepository;
import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddonService {

    private final RestaurantRepository restaurantRepository;
    private final AddonRepository addonRepository;
    private final CategoryRepository categoryRepository;

    public AddonService(RestaurantRepository restaurantRepository, AddonRepository addonRepository, CategoryRepository categoryRepository) {
        this.restaurantRepository = restaurantRepository;
        this.addonRepository = addonRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    @CacheEvict(value = "restaurantMenu", key = "#restaurantId")
    public AddonResponse createAddon(Long restaurantId,
                                               CreateAddonRequest request) {
        Restaurant restaurant = verifyOwnerAccess(restaurantId);

        Addon addon = new Addon();
        addon.setRestaurant(restaurant);
        addon.setAddonName(request.getAddonName());
        addon.setAvailable(request.isAvailable());
        addon.setPrice(request.getPrice());

        addonRepository.save(addon);

        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            linkCategories(addon, request.getCategoryIds(), restaurant);
        }

        return createResponse(addon);
    }

    public List<AddonResponse> getAddons(Long restaurantId) {
        verifyOwnerAccess(restaurantId);

        return addonRepository
                .findAllByRestaurant_RestaurantId(restaurantId)
                .stream()
                .map(this::createResponse)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "restaurantMenu", key = "#restaurantId")
    public AddonResponse updateAddon(Long restaurantId, Long addonId,
                                               CreateAddonRequest request) {
        Restaurant restaurant = verifyOwnerAccess(restaurantId);

        Addon addon = checkAddon(restaurant,  addonId);

        addon.setAddonName(request.getAddonName());
        addon.setAvailable(request.isAvailable());
        addon.setPrice(request.getPrice());

        addonRepository.save(addon);

        unlinkAllCategories(addon);
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            linkCategories(addon, request.getCategoryIds(), restaurant);
        }

        return createResponse(addon);
    }

    @Transactional
    @CacheEvict(value = "restaurantMenu", key = "#restaurantId")
    public AddonResponse updateAvailability(Long restaurantId, Long addonId,
                                                 AddonAvailableStatusRequest request) {
        Restaurant restaurant = verifyOwnerAccess(restaurantId);
        Addon addon = checkAddon(restaurant,  addonId);

        addon.setAvailable(request.isAvailable());

        return createResponse(addon);
    }

    @Transactional
    @CacheEvict(value = "restaurantMenu", key = "#restaurantId")
    public String deleteAddon(Long restaurantId, Long addonId) {
        Restaurant restaurant = verifyOwnerAccess(restaurantId);
        Addon addon = checkAddon(restaurant, addonId);

        addonRepository.delete(addon);

        return "deleted";
    }

    //------------------------------------------------HELPER METHODS----------------------------------------------------

    private Restaurant verifyOwnerAccess(Long restaurantId) {
        String username =  SecurityContextHolder.getContext()
                .getAuthentication().getName();

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(restaurantId));

        if(!restaurant.getOwner().getEmail().equals(username)){
            throw new AccessDeniedException("You are not allowed to perform this action of accessing Addons for this restaurant.");
        }
        return restaurant;
    }

    private AddonResponse createResponse(Addon addon) {

        RestaurantSummary restaurant = new RestaurantSummary();
        restaurant.setRestaurantId(addon.getRestaurant().getRestaurantId());
        restaurant.setRestaurantName(addon.getRestaurant().getRestaurantName());

        List<CategorySummary> categories = addon.getCategories().stream()
                .map(category -> new CategorySummary(
                        category.getCategoryId(),
                        category.getCategoryName()
                ))
                .toList();


        return new AddonResponse(
                addon.getAddonId(),
                addon.getAddonName(),
                categories,
                restaurant,
                addon.isAvailable(),
                addon.getPrice()
        );
    }

    private Addon checkAddon(Restaurant restaurant, Long addonId) {
        Addon addon = addonRepository.findById(addonId)
                .orElseThrow(() -> new EntityNotFoundException("Addon not found with id " + addonId));

        if(!addon.getRestaurant().getRestaurantId().equals(restaurant.getRestaurantId())) {
            throw new AccessDeniedException("You are not allowed to perform this action of updating Addons for this restaurant.");
        }

        return addon;
    }

    private void linkCategories(Addon addon, List<Long> categoryIds, Restaurant restaurant) {
        List<Category> categories = categoryRepository.findAllById(categoryIds);
        for (Category category : categories) {
            if (!category.getRestaurant().getRestaurantId().equals(restaurant.getRestaurantId())) {
                throw new AccessDeniedException("Category does not belong to this restaurant.");
            }
            category.addAddon(addon);
        }
    }

    private void unlinkAllCategories(Addon addon) {
        for (Category category : List.copyOf(addon.getCategories())) {
            category.removeAddon(addon);
        }
    }
}
