package com.kilgore.fooddeliveryapp.service;

import com.kilgore.fooddeliveryapp.dto.request.CreateCategoryRequest;
import com.kilgore.fooddeliveryapp.dto.response.CreateCategoryResponse;
import com.kilgore.fooddeliveryapp.dto.summary.AddonSummary;
import com.kilgore.fooddeliveryapp.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.exceptions.EntityAlreadyExistsException;
import com.kilgore.fooddeliveryapp.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.exceptions.RestaurantNotFoundException;
import com.kilgore.fooddeliveryapp.model.Category;
import com.kilgore.fooddeliveryapp.model.Restaurant;
import com.kilgore.fooddeliveryapp.repository.CategoryRepository;
import com.kilgore.fooddeliveryapp.repository.RestaurantRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private RestaurantRepository restaurantRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    @Transactional
    @CacheEvict(value = "restaurantMenu", key = "#restaurantId")
    public CreateCategoryResponse createCategory(Long restaurantId,
                                                 CreateCategoryRequest request) {
        Restaurant restaurant = verifyOwnerAccess(restaurantId);

        if(categoryRepository
                .findCategoryByCategoryNameAndRestaurant_RestaurantId(
                        request.getCategoryName(),
                        restaurantId
                ).isPresent()) {
            throw new EntityAlreadyExistsException("This Category already exists in this restaurant");
        }

        Category category = new Category();

        category.setCategoryName(request.getCategoryName());
        category.setRestaurant(restaurant);
        category.setDescription(request.getDescription());
        category.setDisplayOrder(request.getDisplayOrder());

        categoryRepository.save(category);

        return createResponse(category);
    }

    public List<CreateCategoryResponse> getAllCategories(Long restaurantId) {
        Restaurant restaurant = verifyOwnerAccess(restaurantId);

        return restaurant.getCategories()
                .stream()
                .map(this::createResponse)
                .toList();
    }


    @Transactional
    @CacheEvict(value = "restaurantMenu", key = "#restaurantId")
    public CreateCategoryResponse updateCategory(Long restaurantId,
                                                 Long categoryId,
                                                 CreateCategoryRequest request) {
        Restaurant restaurant = verifyOwnerAccess(restaurantId);

        Category category = verifyCategory(restaurant, categoryId);

        category.setCategoryName(request.getCategoryName());
        category.setDescription(request.getDescription());
        category.setDisplayOrder(request.getDisplayOrder());
        categoryRepository.save(category);

        return createResponse(category);
    }

    @Transactional
    @CacheEvict(value = "restaurantMenu", key = "#restaurantId")
    public void deleteCategory(Long restaurantId, Long categoryId) {
        Restaurant restaurant = verifyOwnerAccess(restaurantId);

        Category category = verifyCategory(restaurant, categoryId);
        categoryRepository.delete(category);
    }

    /* HELPING METHODS */

    private CreateCategoryResponse createResponse(Category category){

        RestaurantSummary restaurant = new RestaurantSummary();
        restaurant.setRestaurantId(category.getRestaurant().getRestaurantId());
        restaurant.setRestaurantName(category.getRestaurant().getRestaurantName());

        List<AddonSummary> addons = category.getAvailableAddons().stream()
                .map(addon -> new AddonSummary(
                        addon.getAddonId(),
                        addon.getAddonName()
                ))
                .toList();

        return new CreateCategoryResponse(
                category.getCategoryId(),
                category.getCategoryName(),
                category.getDescription(),
                restaurant,
                category.getDisplayOrder(),
                addons
        );
    }

    private Restaurant verifyOwnerAccess(Long restaurantId) {
        String username =  SecurityContextHolder.getContext()
                .getAuthentication().getName();

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(restaurantId));

        if(!restaurant.getOwner().getEmail().equals(username)){
            throw new AccessDeniedException("You are not allowed to perform this action of accessing Category");
        }
        return restaurant;
    }

    private Category verifyCategory(Restaurant restaurant, Long categoryId) {
        Category category =  categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));

        if(!category.getRestaurant().getRestaurantId().equals(restaurant.getRestaurantId())){
            throw new AccessDeniedException("Category does not belong to this restaurant");
        }

        return category;
    }

}
