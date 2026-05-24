package com.kilgore.fooddeliveryapp.catalog.annotations;


import com.kilgore.fooddeliveryapp.catalog.dto.request.RestaurantRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RestaurantTimeValidator
        implements ConstraintValidator<RestaurantRequest.ValidRestaurantTime, RestaurantRequest> {

    @Override
    public boolean isValid(RestaurantRequest req, ConstraintValidatorContext ctx) {
        if (req.getOpeningTime() == null || req.getClosingTime() == null) {
            return true; // let @NotNull handle it
        }
        if(req.getOpeningTime().equals(req.getClosingTime())) {
            return true; // restaurant is opening 24 hours, so we don't need to check the validation
        }
        return true;
    }
}

