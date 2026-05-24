package com.kilgore.fooddeliveryapp.identity.dto.response;

import com.kilgore.fooddeliveryapp.identity.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginAuthResponse {
    private String token;
    private String firstName;
    private String email;
    private UserRole role;
    private long expiresAt;
}
