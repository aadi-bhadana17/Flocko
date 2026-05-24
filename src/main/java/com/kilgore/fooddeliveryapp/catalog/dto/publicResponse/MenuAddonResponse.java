package com.kilgore.fooddeliveryapp.catalog.dto.publicResponse;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MenuAddonResponse {
    private Long addonId;
    private String addonName;
    private BigDecimal price;
    private boolean available;
}

