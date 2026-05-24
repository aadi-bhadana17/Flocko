package com.kilgore.fooddeliveryapp.catalog.dto.response;

import com.kilgore.fooddeliveryapp.catalog.dto.request.ContactInformationDto;
import com.kilgore.fooddeliveryapp.catalog.dto.request.RestaurantAddressDto;
import com.kilgore.fooddeliveryapp.catalog.model.CuisineType;
import com.kilgore.fooddeliveryapp.catalog.model.RestaurantStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantResponse {
    private Long restaurantId;
    private String restaurantName;
    private String restaurantDescription;
    private OwnerResponse owner;
    private CuisineType cuisineType;
    private RestaurantAddressDto address;
    private ContactInformationDto contactInformation;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private List<String> images;
    private boolean open;
    private RestaurantStatus restaurantStatus;
    private LocalDate registrationDate;

}
