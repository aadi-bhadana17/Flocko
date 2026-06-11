package com.kilgore.fooddeliveryapp.catalog.dto.summary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class RestaurantExtendedSummary {
    @NotNull
    private Long restaurantId;
    @NotBlank
    private String restaurantName;
    @NotNull
    private LocalTime openingTime;
    @NotNull
    private LocalTime closingTime;
    @NotNull
    private Long ownerUserId;
}
