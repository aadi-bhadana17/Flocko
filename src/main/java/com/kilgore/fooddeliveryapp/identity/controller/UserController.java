package com.kilgore.fooddeliveryapp.identity.controller;

import com.kilgore.fooddeliveryapp.identity.dto.request.AddressRequest;
import com.kilgore.fooddeliveryapp.identity.dto.request.ChangePasswordRequest;
import com.kilgore.fooddeliveryapp.identity.dto.request.RoleChangeRequestDto;
import com.kilgore.fooddeliveryapp.identity.dto.request.UpdateProfileRequest;
import com.kilgore.fooddeliveryapp.identity.dto.response.AddressResponse;
import com.kilgore.fooddeliveryapp.catalog.dto.response.MessSubscriptionResponse;
import com.kilgore.fooddeliveryapp.identity.dto.response.RoleChangeRequestResponse;
import com.kilgore.fooddeliveryapp.identity.dto.response.UserProfileResponse;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.identity.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {


    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    //------------------------------------------Profile Management------------------------------------------------------

    @GetMapping("/profile")
    public UserProfileResponse getUserProfile() {
        return userService.getUserProfile();
    }

    @PutMapping("/profile")
    public UserProfileResponse updateUserProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateUserProfile(request);
    }

    @PutMapping("/change-password")
    public String changePassword(@RequestBody ChangePasswordRequest request) {
        return userService.changePassword(request);
    }

    //------------------------------------------Address Management------------------------------------------------------


    @GetMapping("/addresses")
    public List<AddressResponse> getAddresses() {
        return userService.getAddresses();
    }

    @PostMapping("/addresses")
    public AddressResponse addAddress(@Valid @RequestBody AddressRequest request) {
        return userService.addAddress(request);
    }

    @PutMapping("/addresses/{addressId}")
    public AddressResponse updateAddress(@PathVariable Long addressId,
                                         @Valid @RequestBody AddressRequest request) {
        return userService.updateAddress(addressId, request);
    }

    @PutMapping("/addresses/{addressId}/default")
    public AddressResponse setDefaultAddress(@PathVariable Long addressId) {
        return userService.setDefaultAddress(addressId);
    }

    @DeleteMapping("/addresses/{addressId}")
    public String deleteAddress(@PathVariable Long addressId) {
        return userService.deleteAddress(addressId);
    }


    //------------------------------------------Favourite Restaurants---------------------------------------------------

    @GetMapping("/favourites")
    public List<RestaurantSummary> getFavouriteRestaurants() {
        return userService.getFavouriteRestaurants();
    }

    @PostMapping("/favourites/{restaurantId}")
    public String addFavouriteRestaurant(@PathVariable Long restaurantId) {
        return userService.addFavouriteRestaurant(restaurantId);
    }

    @DeleteMapping("/favourites/{restaurantId}")
    public String removeFavouriteRestaurant(@PathVariable Long restaurantId) {
        return userService.removeFavouriteRestaurant(restaurantId);
    }

    //------------------------------------------Role Change Request-----------------------------------------------------


    @PostMapping("/role-change-request")
    public RoleChangeRequestResponse roleChangeRequest(@RequestBody RoleChangeRequestDto request) {
        return userService.createRoleChangeRequest(request);
    }

}
