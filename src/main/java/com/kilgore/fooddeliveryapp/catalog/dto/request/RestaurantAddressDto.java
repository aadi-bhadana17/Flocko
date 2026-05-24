package com.kilgore.fooddeliveryapp.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantAddressDto {
    @NotBlank
    private String buildingNo;
    @NotBlank
    private String street;
    @NotBlank
    private String city;
    @NotBlank
    private String state;
    @NotBlank
    @Pattern(regexp = "^[0-9]{6}$", message = "Pin code must be of 6 digits")
    private String pincode;
    private String landmark;
}
