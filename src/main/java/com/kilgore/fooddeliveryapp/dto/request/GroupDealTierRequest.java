package com.kilgore.fooddeliveryapp.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GroupDealTierRequest {
    @NotNull
    @Positive
    private int thresholdPercent;
    @NotNull
    @Positive
    private int discountPercent;
}
