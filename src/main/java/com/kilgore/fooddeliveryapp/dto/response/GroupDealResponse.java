package com.kilgore.fooddeliveryapp.dto.response;

import com.kilgore.fooddeliveryapp.dto.summary.FoodSummary;
import com.kilgore.fooddeliveryapp.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.model.GroupDealStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GroupDealResponse {
    private Long dealId;
    private String dealName;

    private RestaurantSummary restaurant;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private List<FoodSummary> foodList;

    private BigDecimal originalPrice;
    private int maxDiscount;

    private int targetParticipation;
    private int currentParticipation;
    private BigDecimal currentPrice;

    private GroupDealStatus status;

    private LocalDateTime confirmationWindowEndTime;
    private List<GroupDealTierResponse> discountList;
}
