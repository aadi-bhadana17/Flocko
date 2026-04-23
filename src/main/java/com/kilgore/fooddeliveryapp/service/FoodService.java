package com.kilgore.fooddeliveryapp.service;

import com.kilgore.fooddeliveryapp.dto.request.FoodRequest;
import com.kilgore.fooddeliveryapp.dto.request.FoodStatusRequest;
import com.kilgore.fooddeliveryapp.dto.response.FoodResponse;
import com.kilgore.fooddeliveryapp.dto.summary.CategorySummary;
import com.kilgore.fooddeliveryapp.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.exceptions.EntityAlreadyExistsException;
import com.kilgore.fooddeliveryapp.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.exceptions.RestaurantNotFoundException;
import com.kilgore.fooddeliveryapp.model.Category;
import com.kilgore.fooddeliveryapp.model.Food;
import com.kilgore.fooddeliveryapp.model.Restaurant;
import com.kilgore.fooddeliveryapp.repository.CategoryRepository;
import com.kilgore.fooddeliveryapp.repository.FoodRepository;
import com.kilgore.fooddeliveryapp.repository.RestaurantRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FoodService {

    private final FoodRepository foodRepository;
    private final CategoryRepository categoryRepository;
    private final RestaurantRepository restaurantRepository;

    @Transactional
    @CacheEvict(value = "restaurantMenu", key = "#restaurantId")
    public FoodResponse createFood(Long restaurantId, FoodRequest request) {

        Restaurant restaurant = verifyOwnerAccess(restaurantId);
        Category category = verifyCategory(restaurant, request.getCategoryId());

        if(isFoodExists(restaurantId, request)) {
            throw new EntityAlreadyExistsException("Food already exists in this restaurant");
        }

        Food food = new Food();

        food.setFoodName(request.getFoodName());
        food.setFoodDescription(request.getFoodDescription());
        food.setFoodPrice(request.getFoodPrice());

        food.setRestaurant(restaurant);
        food.setFoodCategory(category);
        food.setImages(request.getImages());

        food.setVegetarian(request.isVegetarian());
        food.setAvailable(request.isAvailable());

        food.setCreatedAt(LocalDateTime.now());

        foodRepository.save(food);

        return createFoodResponse(food);
    }

    public List<FoodResponse> findAllFoods(Long restaurantId) {
        verifyOwnerAccess(restaurantId);

        List<Food> foods = foodRepository.findByRestaurant_RestaurantId(restaurantId);

        return   foods.stream()
                .map(this::createFoodResponse)
                .toList();
    }

    public FoodResponse findFoodById(Long restaurantId, Long foodId) {
        verifyOwnerAccess(restaurantId);

        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new EntityNotFoundException("Food not found"));

        return createFoodResponse(food);
    }

    @Transactional
    @CacheEvict(value = "restaurantMenu", key = "#restaurantId")
    public FoodResponse updateFood(Long restaurantId, Long foodId, FoodRequest request) {
        Restaurant restaurant = verifyOwnerAccess(restaurantId);
        Category category = verifyCategory(restaurant, request.getCategoryId());

        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new EntityNotFoundException("Food not found"));

        if(isFoodNameDuplicated(restaurantId, foodId, request.getFoodName())) {
            throw new EntityAlreadyExistsException("Food with this name already exists");
        }

        food.setFoodName(request.getFoodName());
        food.setFoodDescription(request.getFoodDescription());
        food.setFoodPrice(request.getFoodPrice());
        food.setFoodCategory(category);
        food.setImages(request.getImages());
        food.setVegetarian(request.isVegetarian());
        food.setAvailable(request.isAvailable());

        return createFoodResponse(food);
    }

    @Transactional
    @CacheEvict(value = "restaurantMenu", key = "#restaurantId")
    public FoodResponse updateFoodStatus(Long restaurantId, Long foodId, FoodStatusRequest request) {

        verifyOwnerAccess(restaurantId);
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new EntityNotFoundException("Food not found"));

        food.setAvailable(request.isAvailable());

        return createFoodResponse(food);
    }

    @Transactional
    @CacheEvict(value = "restaurantMenu", key = "#restaurantId")
    public String deleteFood(Long restaurantId, Long foodId) {
        verifyOwnerAccess(restaurantId);
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new EntityNotFoundException("Food not found"));

        foodRepository.delete(food);

        return "Food with id : " + foodId + " has been deleted";
    }

    private Restaurant verifyOwnerAccess(Long restaurantId) {
        String username =  SecurityContextHolder.getContext()
                .getAuthentication().getName();

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(restaurantId));

        if(!restaurant.getOwner().getEmail().equals(username)){
            throw new AccessDeniedException("You are not owner of this restaurant, " +
                    "so you are not allowed to perform this action of accessing Foods");
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

    private FoodResponse createFoodResponse(Food food) {

        RestaurantSummary restaurant = new RestaurantSummary(
                food.getRestaurant().getRestaurantId(),
                food.getRestaurant().getRestaurantName(),
                food.getRestaurant().getCuisineType(),
                food.getRestaurant().getAvgRating()
        );

        CategorySummary category = new CategorySummary(
                food.getFoodCategory().getCategoryId(),
                food.getFoodCategory().getCategoryName()
        );

        return new FoodResponse(
                food.getFoodId(),
                food.getFoodName(),
                food.getFoodDescription(),
                food.getFoodPrice(),
                food.getImages(),
                category,
                restaurant,
                food.isVegetarian(),
                food.isAvailable(),
                food.getCreatedAt()
        );
    }

    private boolean isFoodExists(Long restaurantId, FoodRequest request) {
        return foodRepository.findByFoodNameAndRestaurant_RestaurantId(
                request.getFoodName(),
                restaurantId
        ).isPresent();
    }

    private boolean isFoodNameDuplicated(Long restaurantId, Long foodId, String newFoodName) {
        Optional<Food> food = foodRepository.findByFoodNameAndRestaurant_RestaurantId(newFoodName,  restaurantId);

        return food.filter(value -> !value.getFoodId().equals(foodId)).isPresent();
        // if the food we are getting from param, and food from repo here, have same ID then we are fine not otherwise
    }

}
