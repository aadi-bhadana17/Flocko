package com.kilgore.fooddeliveryapp.identity.dto.summary;

import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.identity.model.AccountStatus;
import com.kilgore.fooddeliveryapp.identity.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserExtendedSummary {

    private Long id;
    private String name;
    private String email;
    private UserRole role;
    private AccountStatus status;
    private boolean isOnline;
    private LocalDateTime restrictedUntil;
    private String restrictionReason;
    private List<RestaurantSummary> ownedRestaurants;

}
