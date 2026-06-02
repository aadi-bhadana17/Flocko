package com.kilgore.fooddeliveryapp.identity.api;

import com.kilgore.fooddeliveryapp.identity.dto.summary.UserSummary;
import com.kilgore.fooddeliveryapp.identity.dto.summary.AddressSummary;

import java.math.BigDecimal;

public interface UserFacade {
    Long getUserIdByEmail(String email);
    UserSummary getUserById(Long userId);
    boolean isUserActive(Long userId);
    boolean isUserRestricted(Long userId);
    AddressSummary getDefaultAddress(Long userId);
    AddressSummary getAddressById(Long addressId);
    boolean isAddressOwnedByUser(Long userId, Long addressId);
    BigDecimal getWalletBalance(Long userId);
    void setWalletBalance(Long userId, BigDecimal balance);
    void addWalletBalance(Long userId, BigDecimal amount);
    void deductWalletBalance(Long userId, BigDecimal amount);
    boolean isEmployedAt(Long userId, Long restaurantId);
    boolean isUserRestaurantStaff(Long userId);
    boolean isUserRestaurantOwner(Long userId);
    boolean isUserAdmin(Long userId);
    boolean isOwnerOfRestaurant(Long userId, Long restaurantId);
    boolean isUserCustomer(Long userId);
}