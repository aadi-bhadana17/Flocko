package com.kilgore.fooddeliveryapp.dto.summary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FoodSummary {
    private Long foodId;
    private String foodName;
    private String foodDescription;
    private BigDecimal foodPrice;
    private boolean vegetarian;
}
