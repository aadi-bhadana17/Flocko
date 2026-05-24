package com.kilgore.fooddeliveryapp.catalog.dto.response;

import com.kilgore.fooddeliveryapp.catalog.dto.summary.MessPlanSummary;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessSubscriptionResponse {

    private Long subscriptionId;
    private MessPlanSummary messPlan;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;
}
