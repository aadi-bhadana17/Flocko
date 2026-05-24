package com.kilgore.fooddeliveryapp.ordering.controller;

import com.kilgore.fooddeliveryapp.ordering.dto.request.UpdateKitchenStatusRequest;
import com.kilgore.fooddeliveryapp.ordering.dto.response.KitchenLoadResponse;
import com.kilgore.fooddeliveryapp.ordering.service.KitchenService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/restaurant/{restaurantId}/kitchen")
@PreAuthorize("hasAnyAuthority('RESTAURANT_OWNER', 'RESTAURANT_STAFF')")
public class KitchenController {

    private final KitchenService kitchenService;

    public KitchenController(KitchenService kitchenService) {
        this.kitchenService = kitchenService;
    }

    @PutMapping("/status")
    public KitchenLoadResponse updateKitchenStatus(@PathVariable Long restaurantId,
                                                   @RequestBody UpdateKitchenStatusRequest request) {
        return kitchenService.updateKitchenStatus(restaurantId, request);
    }
}
