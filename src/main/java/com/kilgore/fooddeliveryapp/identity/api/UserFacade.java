package com.kilgore.fooddeliveryapp.identity.api;

import com.kilgore.fooddeliveryapp.identity.dto.summary.UserSummary;
import com.kilgore.fooddeliveryapp.identity.dto.summary.AddressSummary;

import java.math.BigDecimal;

public interface UserFacade {
    UserSummary getUserById(Long userId);
    boolean isUserActive(Long userId);
    AddressSummary getDefaultAddress(Long userId);
    void deductWalletBalance(Long userId, BigDecimal amount);
}