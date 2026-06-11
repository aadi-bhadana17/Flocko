package com.kilgore.fooddeliveryapp.catalog.service;

import com.kilgore.fooddeliveryapp.catalog.dto.request.CreateCategoryRequest;
import com.kilgore.fooddeliveryapp.catalog.dto.response.CreateCategoryResponse;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.AddonSummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.catalog.util.CatalogMapper;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityAlreadyExistsException;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.common.exceptions.RestaurantNotFoundException;
import com.kilgore.fooddeliveryapp.catalog.model.Category;
import com.kilgore.fooddeliveryapp.catalog.model.Restaurant;
import com.kilgore.fooddeliveryapp.catalog.repository.CategoryRepository;
import com.kilgore.fooddeliveryapp.catalog.repository.RestaurantRepository;
import com.kilgore.fooddeliveryapp.common.util.UserAuthorization;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final RestaurantRepository restaurantRepository;
    private final CategoryRepository categoryRepository;
    private final CatalogMapper catalogMapper;
    private final UserAuthorization userAuthorization;

    public CategoryService(RestaurantRepository restaurantRepository, CategoryRepository categoryRepository, CatalogMapper catalogMapper, UserAuthorization userAuthorization) {
        this.restaurantRepository = restaurantRepository;
        this.categoryRepository = categoryRepository;
        this.catalogMapper = catalogMapper;
        this.userAuthorization = userAuthorization;
    }

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
                .map(catalogMapper::toAddonSummary)
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
        Long userId = userAuthorization.authorizeUserId();

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(restaurantId));

        if(!restaurant.getOwnerUserId().equals(userId)) {
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
