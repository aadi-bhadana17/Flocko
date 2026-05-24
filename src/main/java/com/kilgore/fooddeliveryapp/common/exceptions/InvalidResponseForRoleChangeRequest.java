package com.kilgore.fooddeliveryapp.common.exceptions;

public class InvalidResponseForRoleChangeRequest extends RuntimeException{
    public InvalidResponseForRoleChangeRequest(String message) {
        super(message);
    }
}
