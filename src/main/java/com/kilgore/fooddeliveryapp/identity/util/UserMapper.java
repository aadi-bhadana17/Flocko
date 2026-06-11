package com.kilgore.fooddeliveryapp.identity.util;

import com.kilgore.fooddeliveryapp.identity.dto.response.StaffCreationResponse;
import com.kilgore.fooddeliveryapp.identity.dto.summary.AddressSummary;
import com.kilgore.fooddeliveryapp.identity.dto.summary.StaffSummary;
import com.kilgore.fooddeliveryapp.identity.dto.summary.UserExtendedSummary;
import com.kilgore.fooddeliveryapp.identity.dto.summary.UserSummary;
import com.kilgore.fooddeliveryapp.identity.model.Address;
import com.kilgore.fooddeliveryapp.identity.model.User;
import org.springframework.stereotype.Component;


@Component
public class UserMapper {


    public UserSummary toUserSummary(User user) {
        return new UserSummary(
                user.getUserId(),
                user.getFirstName() + " " + user.getLastName()
        );
    }

    public UserExtendedSummary toUserExtendedSummary(User user) {
        return UserExtendedSummary.builder()
                .id(user.getUserId())
                .name(user.getFirstName() + " " + user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getAccountStatus())
                .isOnline(user.isOnline())
                .restrictedUntil(user.getRestrictedUntil())
                .restrictionReason(user.getRestrictionReason())
                .build();
    }

    public AddressSummary toAddressSummary(Address address, User user) {
        return new AddressSummary(
                address.getAddressId(),
                address.getBuildingNo(),
                address.getStreet(),
                address.getCity(),
                address.getPincode(),
                address.getLandmark(),
                toUserSummary(user)
        );
    }

    public StaffCreationResponse toStaffCreationResponse(User user, String tempPassword) {
        return new StaffCreationResponse(
                user.getFirstName() + " " + user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                tempPassword
        );
    }

    public StaffSummary toStaffSummary(User user) {
        return new StaffSummary(
                user.getUserId(),
                user.getFirstName() + " " + user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.isOnline()
        );
    }

}
