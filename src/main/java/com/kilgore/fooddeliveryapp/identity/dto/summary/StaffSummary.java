package com.kilgore.fooddeliveryapp.identity.dto.summary;

import com.kilgore.fooddeliveryapp.identity.model.AccountStatus;
import com.kilgore.fooddeliveryapp.identity.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StaffSummary {
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private UserRole role;
    private boolean isOnline;
}
