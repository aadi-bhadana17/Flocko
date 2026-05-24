package com.kilgore.fooddeliveryapp.ordering.service;

import com.kilgore.fooddeliveryapp.common.util.UserAuthorization;
import com.kilgore.fooddeliveryapp.ordering.dto.request.UpdateKitchenStatusRequest;
import com.kilgore.fooddeliveryapp.ordering.dto.response.KitchenLoadResponse;
import com.kilgore.fooddeliveryapp.common.exceptions.RestaurantNotFoundException;
import com.kilgore.fooddeliveryapp.catalog.model.Restaurant;
import com.kilgore.fooddeliveryapp.identity.model.User;
import com.kilgore.fooddeliveryapp.identity.model.UserRole;
import com.kilgore.fooddeliveryapp.catalog.repository.RestaurantRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class KitchenService {

    private final UserAuthorization userAuthorization;
    private final RestaurantRepository restaurantRepository;
    private final KitchenLoadService kitchenLoadService;

    public KitchenService(UserAuthorization userAuthorization, RestaurantRepository restaurantRepository, KitchenLoadService kitchenLoadService) {
        this.userAuthorization = userAuthorization;
        this.restaurantRepository = restaurantRepository;
        this.kitchenLoadService = kitchenLoadService;
    }


    public KitchenLoadResponse updateKitchenStatus(Long restaurantId, UpdateKitchenStatusRequest request) {
        User user = userAuthorization.authorizeUser();

        Restaurant restaurant =  restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(restaurantId));

        if(user.getRole().equals(UserRole.RESTAURANT_STAFF) && !Objects.equals(user.getEmployedAt(), restaurantId))
            throw new AccessDeniedException("You are not allowed to perform this action, as you are not employed at this restaurant.");
        else if(user.getRole().equals(UserRole.RESTAURANT_OWNER) && !restaurant.getOwner().getUserId().equals(user.getUserId()))
            throw new AccessDeniedException("You are not allowed to perform this action, as you do not own this restaurant.");


        restaurant.setKitchenStatus(request.getKitchenLoadIndicator());
        restaurantRepository.save(restaurant);

        return createKitchenLoadResponse(restaurant);
    }

    private KitchenLoadResponse createKitchenLoadResponse(Restaurant restaurant) {
        int currentOrders = kitchenLoadService.getCurrentOrders(restaurant);

        return new KitchenLoadResponse(
                restaurant.getRestaurantId(),
                restaurant.getRestaurantName(),
                restaurant.getKitchenStatus(),
                currentOrders,
                "The kitchen load status has been updated to " + restaurant.getKitchenStatus()
                        + " according to the request made by the restaurant staff or owner." +
                        " The current number of orders in the kitchen is " + currentOrders + "."
        );
    }

}
