package com.kilgore.fooddeliveryapp.catalog.dto.response;

import com.kilgore.fooddeliveryapp.catalog.dto.summary.CategorySummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantSummary;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddonResponse {
    private Long addonId;
    private String addonName;
    private List<CategorySummary> categories;
    private RestaurantSummary restaurant;
    private boolean available;
    private BigDecimal price;
}
