package com.kilgore.fooddeliveryapp.identity.util;

import com.kilgore.fooddeliveryapp.identity.dto.summary.AddressSummary;
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

}
