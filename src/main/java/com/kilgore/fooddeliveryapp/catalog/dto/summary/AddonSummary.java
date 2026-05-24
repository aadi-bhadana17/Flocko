package com.kilgore.fooddeliveryapp.catalog.dto.summary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddonSummary {

    private Long addonId;
    private String addonName;
}
