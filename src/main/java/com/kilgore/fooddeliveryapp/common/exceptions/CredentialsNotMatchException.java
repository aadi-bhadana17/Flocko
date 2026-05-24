package com.kilgore.fooddeliveryapp.common.exceptions;

public class CredentialsNotMatchException extends RuntimeException {
    public CredentialsNotMatchException(String message) {
        super(message);
    }
}
