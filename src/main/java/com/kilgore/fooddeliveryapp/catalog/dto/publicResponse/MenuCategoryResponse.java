package com.kilgore.fooddeliveryapp.catalog.dto.publicResponse;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MenuCategoryResponse {
    private Long categoryId;
    private String categoryName;
    private String description;
    private Integer displayOrder;
    private List<MenuItemResponse> foods;
    private List<MenuAddonResponse> availableAddons;
}

