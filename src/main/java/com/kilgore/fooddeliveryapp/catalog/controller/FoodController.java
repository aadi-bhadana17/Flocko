package com.kilgore.fooddeliveryapp.catalog.controller;

import com.kilgore.fooddeliveryapp.catalog.dto.request.FoodRequest;
import com.kilgore.fooddeliveryapp.catalog.dto.request.FoodStatusRequest;
import com.kilgore.fooddeliveryapp.catalog.dto.response.FoodResponse;
import com.kilgore.fooddeliveryapp.catalog.service.FoodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@PreAuthorize("hasAnyAuthority('RESTAURANT_OWNER')")
@RequestMapping("/api/restaurants/{restaurantId}/foods")
public class FoodController {

    @Autowired
    private FoodService foodService;

    @PostMapping
    public FoodResponse createFood(@PathVariable Long restaurantId,
                                   @RequestBody FoodRequest request) {
        return foodService.createFood(restaurantId, request);
    }

    @GetMapping
    public List<FoodResponse> findAllFoods(@PathVariable Long restaurantId) {
        return foodService.findAllFoods(restaurantId);
    }

    @GetMapping("/{foodId}")
    public FoodResponse findFoodById(@PathVariable Long restaurantId,
                                     @PathVariable Long foodId) {
        return foodService.findFoodById(restaurantId, foodId);
    }

    @PutMapping("/{foodId}")
    public FoodResponse updateFood(@PathVariable Long restaurantId,
                                   @PathVariable Long foodId,
                                   @RequestBody FoodRequest request) {
        return foodService.updateFood(restaurantId, foodId, request);
    }

    @PatchMapping("/{foodId}")
    public FoodResponse updateFoodStatus(@PathVariable Long restaurantId,
                                         @PathVariable Long foodId,
                                         @RequestBody FoodStatusRequest request) {
        return foodService.updateFoodStatus(restaurantId, foodId, request);
    }

    @DeleteMapping("/{foodId}")
    public String deleteFood(@PathVariable Long restaurantId,
                                   @PathVariable Long foodId) {
        return foodService.deleteFood(restaurantId, foodId);
    }
}
