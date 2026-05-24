package com.kilgore.fooddeliveryapp.common.exceptions;

public class RestaurantAlreadyExistsException extends RuntimeException {
    public RestaurantAlreadyExistsException() {
        super("Restaurant with the same name already exists in the same city!");
    }
}
