package com.kilgore.fooddeliveryapp.catalog.controller;

import com.kilgore.fooddeliveryapp.catalog.dto.request.AppendAddonRequest;
import com.kilgore.fooddeliveryapp.catalog.dto.response.AppendAddonsResponse;
import com.kilgore.fooddeliveryapp.catalog.service.CategoryAddonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasAnyAuthority('RESTAURANT_OWNER')")
@RequestMapping("/api/restaurants/{restaurantId}/categories/{categoryId}/addons")
public class CategoryAddonController {

    @Autowired
    private CategoryAddonService categoryAddonService;

    @PostMapping
    public AppendAddonsResponse appendAddons(@PathVariable Long restaurantId,
                                             @PathVariable Long categoryId,
                                             @RequestBody AppendAddonRequest request
                                             ) {
        return categoryAddonService.appendAddon(restaurantId, categoryId, request);
    }

    @GetMapping
    public AppendAddonsResponse getAddons(@PathVariable Long restaurantId,
                                          @PathVariable Long categoryId) {
        return categoryAddonService.getAddons(restaurantId, categoryId);
    }

    @DeleteMapping()
    public AppendAddonsResponse removeAddons(@PathVariable Long restaurantId,
                                              @PathVariable Long categoryId,
                                              @RequestBody AppendAddonRequest request) {
        return categoryAddonService.removeAddons(restaurantId, categoryId, request);
    }
}
