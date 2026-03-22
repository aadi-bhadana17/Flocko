package com.kilgore.fooddeliveryapp.controller;

import com.kilgore.fooddeliveryapp.dto.publicResponse.RestaurantMenuResponse;
import com.kilgore.fooddeliveryapp.dto.publicResponse.RestaurantPublicResponse;
import com.kilgore.fooddeliveryapp.dto.response.GroupDealResponse;
import com.kilgore.fooddeliveryapp.service.GroupDealService;
import com.kilgore.fooddeliveryapp.service.PublicService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/public/restaurants")
public class PublicController {
    /*
        This controller can be used for endpoints that do not require authentication, such as viewing the menu
        or available restaurants.
        Also, it will be used to land users after login, and to provide access to the home page and other public resources.
    */

    private final PublicService publicService;
    private final GroupDealService groupDealService;

    public PublicController(PublicService publicService, GroupDealService groupDealService) {
        this.publicService = publicService;
        this.groupDealService = groupDealService;
    }

    @GetMapping
    public List<RestaurantPublicResponse> getRestaurants(
                                    @RequestParam(required = false) String city,
                                    @RequestParam(required = false) String cuisineType) {

        return publicService.getRestaurants(city, cuisineType);
    }

    @GetMapping("/search")
    public List<RestaurantPublicResponse> getRestaurantByName(@RequestParam("q") String name) {
        return publicService.getRestaurantByName(name);
    }

    @GetMapping("/{restaurantId}")
    public RestaurantPublicResponse getRestaurantById(@PathVariable Long restaurantId) {
        return publicService.getRestaurantById(restaurantId);
    }

    @GetMapping("/{restaurantId}/menu")
    public RestaurantMenuResponse getRestaurantMenu(@PathVariable Long restaurantId) {
        return publicService.getRestaurantMenu(restaurantId);
    }

    @GetMapping("/{restaurantId}/group-deals")
    public List<GroupDealResponse> getDealsForRestaurant(@PathVariable Long restaurantId) {
        return groupDealService.getActiveDealsForRestaurant(restaurantId);
    }
}
