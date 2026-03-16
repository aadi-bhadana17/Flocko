package com.kilgore.fooddeliveryapp.dto.response;

import com.kilgore.fooddeliveryapp.dto.summary.RestaurantSummary;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessPlanResponse {
    private Long messPlanId;
    private String messPlanName;
    private String messPlanDescription;
    private BigDecimal price;
    private RestaurantSummary restaurant;
    private List<MessPlanSlotResponse> slots;
    private boolean isActive;
}
