package com.kilgore.fooddeliveryapp.identity.dto.response;

import com.kilgore.fooddeliveryapp.identity.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StaffCreationResponse {
    private String name;
    private String email;
    private String phone;
    private UserRole role;
    private String password;
}
