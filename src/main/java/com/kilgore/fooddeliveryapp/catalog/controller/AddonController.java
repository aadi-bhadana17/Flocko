package com.kilgore.fooddeliveryapp.catalog.controller;

import com.kilgore.fooddeliveryapp.catalog.dto.request.CreateAddonRequest;
import com.kilgore.fooddeliveryapp.catalog.dto.request.AddonAvailableStatusRequest;
import com.kilgore.fooddeliveryapp.catalog.dto.response.AddonResponse;
import com.kilgore.fooddeliveryapp.catalog.service.AddonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants/{restaurantId}/addons")
@PreAuthorize("hasAnyAuthority('RESTAURANT_OWNER')")
public class AddonController {

    @Autowired
    private AddonService addonService;

    @PostMapping
    public AddonResponse createAddon(@PathVariable Long restaurantId,
                                               @RequestBody CreateAddonRequest request) {
        return addonService.createAddon(restaurantId, request);
    }

    @GetMapping
    public List<AddonResponse> getAddons(@PathVariable Long restaurantId) {
        return addonService.getAddons(restaurantId);
    }

    @PutMapping("/{addonId}")
    public AddonResponse updateAddon(@PathVariable Long restaurantId,
                                               @PathVariable Long addonId,
                                               @RequestBody CreateAddonRequest request) {
        return addonService.updateAddon(restaurantId, addonId, request);
    }

    @PatchMapping("/{addonId}")
    public AddonResponse updateAvailability(@PathVariable Long restaurantId,
                                                 @PathVariable Long addonId,
                                                 @RequestBody AddonAvailableStatusRequest request) {
        return addonService.updateAvailability(restaurantId, addonId, request);
    }

    @DeleteMapping("/{addonId}")
    public String deleteAddon(@PathVariable Long restaurantId,
                                               @PathVariable Long addonId) {
        return addonService.deleteAddon(restaurantId, addonId);
    }
}
