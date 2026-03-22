package com.kilgore.fooddeliveryapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GroupDealTierResponse {
    private int thresholdPercent;
    private int discountPercent;
}