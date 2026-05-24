package com.kilgore.fooddeliveryapp.identity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
public  class AddressResponse {
    private Long addressId;
    private String buildingNo;
    private String street;
    private String city;
    private String state;
    private String pincode;
    private String landmark;
    private boolean isDefault;
}