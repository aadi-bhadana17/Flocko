package com.kilgore.fooddeliveryapp.identity.dto.summary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddressSummary {

    private Long addressId;
    private String buildingNo;
    private String street;
    private String city;
    private String pincode;
    private String landmark;

    private UserSummary user;
}
