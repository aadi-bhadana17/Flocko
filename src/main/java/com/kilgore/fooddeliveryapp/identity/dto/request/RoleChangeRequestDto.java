package com.kilgore.fooddeliveryapp.identity.dto.request;

import com.kilgore.fooddeliveryapp.identity.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleChangeRequestDto {
    private UserRole requestedRole;
    private String requestReason;
}
