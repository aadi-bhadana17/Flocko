package com.kilgore.fooddeliveryapp.catalog.dto.summary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessPlanSummary {

    private Long messPlanId;
    private String name;
    private String description;
    private BigDecimal price;
}
