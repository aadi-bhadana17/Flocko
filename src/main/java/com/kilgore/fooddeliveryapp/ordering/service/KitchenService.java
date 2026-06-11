package com.kilgore.fooddeliveryapp.ordering.service;

import com.kilgore.fooddeliveryapp.catalog.api.CatalogFacade;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.common.util.UserAuthorization;
import com.kilgore.fooddeliveryapp.identity.api.UserFacade;
import com.kilgore.fooddeliveryapp.identity.dto.summary.UserExtendedSummary;
import com.kilgore.fooddeliveryapp.ordering.dto.request.UpdateKitchenStatusRequest;
import com.kilgore.fooddeliveryapp.ordering.dto.response.KitchenLoadResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class KitchenService {

    private final UserAuthorization userAuthorization;
    private final KitchenLoadService kitchenLoadService;
    private final CatalogFacade catalogFacade;
    private final UserFacade userFacade;

    public KitchenService(UserAuthorization userAuthorization, KitchenLoadService kitchenLoadService, CatalogFacade catalogFacade, UserFacade userFacade) {
        this.userAuthorization = userAuthorization;
        this.kitchenLoadService = kitchenLoadService;
        this.catalogFacade = catalogFacade;
        this.userFacade = userFacade;
    }


    public KitchenLoadResponse updateKitchenStatus(Long restaurantId, UpdateKitchenStatusRequest request) {
        Long userId = userAuthorization.authorizeUserId();

        RestaurantSummary restaurant = catalogFacade.getRestaurantById(restaurantId);

        if(userFacade.canUserManageRestaurant(userId, restaurantId))
            throw new AccessDeniedException("You are not allowed to perform this action. Because you don't have authority for that");

        catalogFacade.setKitchenLoadIndicator(restaurantId, request.getKitchenLoadIndicator());

        return createKitchenLoadResponse(restaurant.getRestaurantId());
    }

    private KitchenLoadResponse createKitchenLoadResponse(Long restaurantId) {
        long currentOrders = kitchenLoadService.getCurrentOrders(restaurantId);
        return catalogFacade.getKitchenLoadResponse(restaurantId, currentOrders);
    }
}
