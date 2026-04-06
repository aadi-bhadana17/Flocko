package com.kilgore.fooddeliveryapp.dto.request;

import com.kilgore.fooddeliveryapp.annotations.RestaurantTimeValidator;
import com.kilgore.fooddeliveryapp.model.CuisineType;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@RestaurantRequest.ValidRestaurantTime
public class RestaurantRequest {

    @NotBlank
    private String restaurantName;
    @NotBlank
    private String restaurantDescription;
    @NotNull
    private CuisineType cuisineType;
    @NotNull
    @Valid
    private RestaurantAddressDto address;
    @NotNull
    @Valid
    private ContactInformationDto contactInformation;
    @NotNull
    private LocalTime openingTime;
    @NotNull
    private LocalTime closingTime;
    private List<String> images;


    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = RestaurantTimeValidator.class)
    public @interface ValidRestaurantTime {
        String message() default "Opening time must be before closing time";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }

}
