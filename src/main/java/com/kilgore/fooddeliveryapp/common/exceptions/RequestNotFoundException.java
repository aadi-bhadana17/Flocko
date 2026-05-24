package com.kilgore.fooddeliveryapp.common.exceptions;

public class RequestNotFoundException extends RuntimeException {
    public RequestNotFoundException(Long id) {
        super("Dear Admin, no role change request was found for ID " + id +
                ". Please verify the ID and try again.");
    }
}
