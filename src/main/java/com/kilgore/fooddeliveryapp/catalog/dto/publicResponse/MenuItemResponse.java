package com.kilgore.fooddeliveryapp.catalog.dto.publicResponse;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MenuItemResponse {
    private Long foodId;
    private String foodName;
    private String foodDescription;
    private BigDecimal foodPrice;
    private List<String> images;
    private boolean vegetarian;
    private boolean available;
}

