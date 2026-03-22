package com.kilgore.fooddeliveryapp.exceptions;

public class DefaultAddressNotFoundException extends RuntimeException {
    public DefaultAddressNotFoundException(String message) {
        super(message);
    }
}
