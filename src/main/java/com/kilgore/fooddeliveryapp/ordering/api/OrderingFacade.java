package com.kilgore.fooddeliveryapp.ordering.api;

import com.kilgore.fooddeliveryapp.deals.dto.request.CreateGroupDealOrderRequest;

public interface OrderingFacade {
    boolean hasUserOrderedFrom(Long userId, Long restaurantId);
    boolean isOrderActive(Long orderId);
    Long placeGroupDealOrder(CreateGroupDealOrderRequest request);
}