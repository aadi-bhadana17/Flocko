package com.kilgore.fooddeliveryapp.catalog.dto.response;

import com.kilgore.fooddeliveryapp.catalog.dto.summary.AddonSummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantSummary;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCategoryResponse {
    private Long categoryId;
    private String categoryName;
    private String description;
    private RestaurantSummary restaurant;
    private Integer displayOrder;
    private List<AddonSummary> addons;
}
