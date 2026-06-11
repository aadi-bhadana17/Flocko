package com.kilgore.fooddeliveryapp.identity.api;

import com.kilgore.fooddeliveryapp.catalog.dto.request.AddStaffRequest;
import com.kilgore.fooddeliveryapp.identity.dto.response.StaffCreationResponse;
import com.kilgore.fooddeliveryapp.identity.dto.summary.StaffSummary;
import com.kilgore.fooddeliveryapp.identity.dto.summary.UserExtendedSummary;
import com.kilgore.fooddeliveryapp.identity.dto.summary.UserSummary;
import com.kilgore.fooddeliveryapp.identity.dto.summary.AddressSummary;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface UserFacade {
    Long getUserIdByEmail(String email);
    UserSummary getUserById(Long userId);
    UserExtendedSummary getUserExtendedById(Long userId);
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
    LocalDateTime userRestrictedUntil(Long userId);

    StaffCreationResponse createStaff(AddStaffRequest request, Long restaurantId);
    List<StaffSummary> getRestaurantStaff(Long restaurantId);
    boolean canUserManageRestaurant(Long userId, Long restaurantId);

    void checkDefaultAddress(Long userId);
}