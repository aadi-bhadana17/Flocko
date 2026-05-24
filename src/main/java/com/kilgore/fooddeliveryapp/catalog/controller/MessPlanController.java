package com.kilgore.fooddeliveryapp.catalog.controller;

import com.kilgore.fooddeliveryapp.catalog.dto.request.AddMessPlanRequest;
import com.kilgore.fooddeliveryapp.catalog.dto.response.MessPlanResponse;
import com.kilgore.fooddeliveryapp.catalog.service.MessPlanService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants/{restaurantId}/mess-plans")
public class MessPlanController {

    private final MessPlanService messPlanService;

    public MessPlanController(MessPlanService messPlanService) {
        this.messPlanService = messPlanService;
    }

    @GetMapping
    public List<MessPlanResponse> getMessPlansForRestaurant(@PathVariable Long restaurantId) {
        return messPlanService.getMessPlansForRestaurant(restaurantId);
    }

    @GetMapping("/{messPlanId}")
    public MessPlanResponse getMessPlanById(@PathVariable Long restaurantId,
                                            @PathVariable Long messPlanId) {
        return messPlanService.getMessPlanById(restaurantId, messPlanId);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('RESTAURANT_OWNER')")
    public MessPlanResponse addMessPlanToRestaurant(@PathVariable Long restaurantId,
                                                    @RequestBody AddMessPlanRequest request) {
        return messPlanService.addMessPlanToRestaurant(restaurantId, request);
    }

    @PutMapping("{messPlanId}")
    @PreAuthorize("hasAnyAuthority('RESTAURANT_OWNER')")
    public MessPlanResponse updateMessPlan(@PathVariable Long restaurantId,
                                            @PathVariable Long messPlanId,
                                            @RequestBody AddMessPlanRequest request) {
        return messPlanService.updateMessPlan(restaurantId, messPlanId, request);
    }

        @DeleteMapping("{messPlanId}")
        @PreAuthorize("hasAnyAuthority('RESTAURANT_OWNER')")
        public String deleteMessPlan(@PathVariable Long restaurantId,
                                    @PathVariable Long messPlanId) {
            return messPlanService.deleteMessPlan(restaurantId, messPlanId);
        }



}
