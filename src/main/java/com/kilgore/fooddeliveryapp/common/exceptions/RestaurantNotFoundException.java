package com.kilgore.fooddeliveryapp.common.exceptions;

public class RestaurantNotFoundException extends RuntimeException {
    public RestaurantNotFoundException(Long id) {
        super("Restaurant with this id "+ id + " does not exist");
    }
}
