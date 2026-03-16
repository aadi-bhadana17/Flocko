package com.kilgore.fooddeliveryapp.controller;

import com.kilgore.fooddeliveryapp.dto.request.AddMessPlanRequest;
import com.kilgore.fooddeliveryapp.dto.request.RestaurantRequest;
import com.kilgore.fooddeliveryapp.dto.request.RestaurantStatusRequest;
import com.kilgore.fooddeliveryapp.dto.response.MessPlanResponse;
import com.kilgore.fooddeliveryapp.dto.response.RestaurantResponse;
import com.kilgore.fooddeliveryapp.service.RestaurantService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN' , 'RESTAURANT_OWNER') ")
    public RestaurantResponse addRestaurant(@RequestBody RestaurantRequest request) {

        return restaurantService.createRestaurant(request);
    }

    @GetMapping
    public List<RestaurantResponse> getAllRestaurants() {
        return restaurantService.getAllRestaurants();
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyAuthority('RESTAURANT_OWNER')")
    public List<RestaurantResponse> getMyRestaurants() {
        return restaurantService.getMyRestaurants();
    }

    @GetMapping("/{id}")
    public RestaurantResponse getRestaurant(@PathVariable Long id) {
        return restaurantService.getRestaurant(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'RESTAURANT_OWNER')")
    public RestaurantResponse updateRestaurant(@PathVariable Long id, @RequestBody RestaurantRequest request) {
        return  restaurantService.updateRestaurant(request, id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'RESTAURANT_OWNER')")
    public RestaurantResponse updateRestaurantStatus(@PathVariable Long id, @RequestBody RestaurantStatusRequest request) {
        return restaurantService.updateRestaurantStatus(id, request);
    }

    @PostMapping("{id}/mess-plans")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'RESTAURANT_OWNER')")
    public MessPlanResponse addMessPlanToRestaurant(@PathVariable Long id,
                                                    @RequestBody AddMessPlanRequest request) {
        return restaurantService.addMessPlanToRestaurant(id, request);
    }
}
