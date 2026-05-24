package com.kilgore.fooddeliveryapp.catalog.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FoodStatusRequest {
    private boolean available;
}
