package com.kilgore.fooddeliveryapp.ordering.dto.summary;

import com.kilgore.fooddeliveryapp.identity.dto.summary.UserSummary;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SharedCartMemberSummary {
    private Long memberId;
    private UserSummary user;
    private CartSummary cart;
    private BigDecimal walletContribution;
}
