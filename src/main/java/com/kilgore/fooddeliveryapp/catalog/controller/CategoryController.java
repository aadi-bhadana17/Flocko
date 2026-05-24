package com.kilgore.fooddeliveryapp.catalog.controller;

import com.kilgore.fooddeliveryapp.catalog.dto.request.CreateCategoryRequest;
import com.kilgore.fooddeliveryapp.catalog.dto.response.CreateCategoryResponse;
import com.kilgore.fooddeliveryapp.catalog.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants/{restaurantId}/categories")
@PreAuthorize("hasAnyAuthority('RESTAURANT_OWNER')")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @PostMapping
    public CreateCategoryResponse createCategory(@PathVariable Long restaurantId,
                                                 @RequestBody CreateCategoryRequest request) {

        return categoryService.createCategory(restaurantId, request);
    }

    @GetMapping
    public List<CreateCategoryResponse> getAllCategories(@PathVariable Long restaurantId) {
        return categoryService.getAllCategories(restaurantId);
    }

    @PutMapping("/{categoryId}")
    public CreateCategoryResponse updateCategory(@PathVariable Long restaurantId,
                                                 @PathVariable Long categoryId,
                                                 @RequestBody CreateCategoryRequest request) {
        return categoryService.updateCategory(restaurantId, categoryId, request);
    }

    @DeleteMapping("/{categoryId}")
    public void deleteCategory(@PathVariable Long restaurantId, @PathVariable Long categoryId) {
        categoryService.deleteCategory(restaurantId, categoryId);
    }

}
