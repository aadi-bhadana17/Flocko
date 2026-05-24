package com.kilgore.fooddeliveryapp.deals.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GroupDealParticipationRequest {
    private int quantity;
    private Long addressId;
}
