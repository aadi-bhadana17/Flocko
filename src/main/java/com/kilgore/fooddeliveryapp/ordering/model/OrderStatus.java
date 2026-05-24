package com.kilgore.fooddeliveryapp.ordering.model;

import java.util.List;
import java.util.Map;

public enum OrderStatus {
    CREATED,
    CONFIRMED,
    PREPARING,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED;

    private static final Map<OrderStatus, List<OrderStatus>> VALID_TRANSITIONS = Map.of(
            CREATED, List.of(CONFIRMED, CANCELLED),
            CONFIRMED, List.of(PREPARING, CANCELLED),
            PREPARING, List.of(OUT_FOR_DELIVERY),
            OUT_FOR_DELIVERY, List.of(DELIVERED),
            DELIVERED, List.of(),
            CANCELLED, List.of()
    );

    public boolean canTransitionTo(OrderStatus next) {
        if(next == null) return false;
        return VALID_TRANSITIONS.getOrDefault(this, List.of()).contains(next);
    }
}
