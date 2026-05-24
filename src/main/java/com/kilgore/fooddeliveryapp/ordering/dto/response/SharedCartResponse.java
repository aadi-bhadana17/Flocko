package com.kilgore.fooddeliveryapp.ordering.dto.response;

import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.ordering.dto.summary.SharedCartMemberSummary;
import com.kilgore.fooddeliveryapp.identity.dto.summary.UserSummary;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SharedCartResponse {

    private Long sharedCartId;
    private UserSummary host;
    private RestaurantSummary restaurant;
    private List<SharedCartMemberSummary> memberList;
    private String joinCode;

    private BigDecimal totalPrice;
    private BigDecimal amountPaid;

    private boolean hostPaysAll;
    private boolean isActive;
    private boolean viewerIsMember;
    private boolean viewerIsHost;
}
