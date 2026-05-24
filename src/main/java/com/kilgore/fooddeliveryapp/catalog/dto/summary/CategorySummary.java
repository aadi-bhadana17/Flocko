package com.kilgore.fooddeliveryapp.catalog.dto.summary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategorySummary {
    private Long categoryId;
    private String categoryName;
}
