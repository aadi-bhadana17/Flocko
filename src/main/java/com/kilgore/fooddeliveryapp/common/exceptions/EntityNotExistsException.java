package com.kilgore.fooddeliveryapp.common.exceptions;

public class EntityNotExistsException extends RuntimeException {
    public EntityNotExistsException(String message) {
        super(message);
    }
}
