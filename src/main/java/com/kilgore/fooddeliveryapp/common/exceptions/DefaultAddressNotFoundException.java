package com.kilgore.fooddeliveryapp.common.exceptions;

public class DefaultAddressNotFoundException extends RuntimeException {
    public DefaultAddressNotFoundException(String message) {
        super(message);
    }
}
