package com.kilgore.fooddeliveryapp.catalog.dto.summary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddonSummary {

    private Long addonId;
    private String addonName;
    private BigDecimal price;
}
