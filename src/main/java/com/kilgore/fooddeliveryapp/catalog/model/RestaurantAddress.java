package com.kilgore.fooddeliveryapp.catalog.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantAddress {
    private String buildingNo;
    private String street;
    private String city;
    private String state;
    private String pincode;
    private String landmark;
}