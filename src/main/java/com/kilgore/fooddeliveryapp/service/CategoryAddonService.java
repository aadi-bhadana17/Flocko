package com.kilgore.fooddeliveryapp.service;

import com.kilgore.fooddeliveryapp.dto.request.AppendAddonRequest;
import com.kilgore.fooddeliveryapp.dto.response.AppendAddonsResponse;
import com.kilgore.fooddeliveryapp.dto.summary.AddonSummary;
import com.kilgore.fooddeliveryapp.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.exceptions.RestaurantNotFoundException;
import com.kilgore.fooddeliveryapp.model.Addon;
import com.kilgore.fooddeliveryapp.model.Category;
import com.kilgore.fooddeliveryapp.model.Restaurant;
import com.kilgore.fooddeliveryapp.repository.AddonRepository;
import com.kilgore.fooddeliveryapp.repository.CategoryRepository;
import com.kilgore.fooddeliveryapp.repository.RestaurantRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CategoryAddonService {

    @Autowired
    private RestaurantRepository restaurantRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private AddonRepository addonRepository;

    @Transactional
    @CacheEvict(value = "restaurantMenu", key = "#restaurantId")
    public AppendAddonsResponse appendAddon(Long restaurantId,
                                            Long categoryId,
                                            AppendAddonRequest request) {
        Restaurant restaurant = verifyOwnerAccess(restaurantId);
        Category category = checkCategory(restaurant, categoryId);

        List<Addon> addons = extractingAddons(request);

        verifyAddons(addons, restaurantId);

        addons.forEach(category::addAddon);


        return createAppendAddonsResponse(category);
    }

    public AppendAddonsResponse getAddons(Long restaurantId, Long categoryId) {
        Restaurant restaurant = verifyOwnerAccess(restaurantId);
        Category category = checkCategory(restaurant, categoryId);

        return createAppendAddonsResponse(category);
    }

    @Transactional
    @CacheEvict(value = "restaurantMenu", key = "#restaurantId")
    public AppendAddonsResponse removeAddons(Long restaurantId,
                                             Long categoryId,
                                             AppendAddonRequest  request) {
        Restaurant restaurant = verifyOwnerAccess(restaurantId);
        Category category = checkCategory(restaurant, categoryId);

        List<Addon> addons = extractingAddons(request);
        verifyAddons(addons, restaurantId);

        addons.forEach(category::removeAddon);

        // No need to save, because it is @Transactional method and the entity has been managed while fetching
        // so hibernate knows something will change, no need to save, it will happen automatically

        return createAppendAddonsResponse(category);
    }


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

    private Category checkCategory(Restaurant restaurant, Long categoryId) {
        Category category =  categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));

        if(!category.getRestaurant().getRestaurantId().equals(restaurant.getRestaurantId())){
            throw new AccessDeniedException("Category does not belong to this restaurant");
        }

        return category;
    }

    private List<Addon> extractingAddons(AppendAddonRequest request) {
        List<Addon> addonsList = addonRepository.findAllById(request.getAddonIds());

        Set<Long> foundIds = addonsList.stream()
                .map(Addon::getAddonId)
                .collect(Collectors.toSet());  // O(1)

        List<Long> invalidIds = request.getAddonIds().stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        if(!invalidIds.isEmpty()) {
            throw new EntityNotFoundException("Addons not found :  " + invalidIds);
        }

        return addonsList;
    }

    private void verifyAddons(List<Addon> addons, Long restaurantId) {
        for (Addon addon : addons) {
            if(!addon.getRestaurant().getRestaurantId().equals(restaurantId)){
                throw new AccessDeniedException("The Addons you are using does not belong to this restaurant.");
            }
        }
    }

    private AppendAddonsResponse createAppendAddonsResponse(Category category) {

        Set<Addon> addons = category.getAvailableAddons();

        List<AddonSummary> addonSummaries = addons.stream()
                .map(addon -> new AddonSummary(
                        addon.getAddonId(),
                        addon.getAddonName()
                ))
                .toList();

        return new AppendAddonsResponse(
                category.getCategoryId(),
                addonSummaries
        );
    }

}
