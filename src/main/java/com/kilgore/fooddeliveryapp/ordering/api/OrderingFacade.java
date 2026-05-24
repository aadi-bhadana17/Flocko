package com.kilgore.fooddeliveryapp.ordering.api;

public interface OrderingFacade {
    boolean hasUserOrderedFrom(Long userId, Long restaurantId);
    boolean isOrderActive(Long orderId);
}