package com.kilgore.fooddeliveryapp.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor

public class AddressRequest {
    @NotBlank
    private String buildingNo;
    @NotBlank
    private String street;
    @NotBlank
    private String city;
    @NotBlank
    private String pincode;
    @NotBlank
    private  String state;
    private String landmark;
}