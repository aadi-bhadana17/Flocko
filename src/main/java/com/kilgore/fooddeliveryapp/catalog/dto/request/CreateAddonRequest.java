package com.kilgore.fooddeliveryapp.catalog.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateAddonRequest {

    private String addonName;
    private String category;
    private boolean available;
    private List<Long> categoryIds;
    private BigDecimal price;
}
