package com.kilgore.fooddeliveryapp.dto.response;

import com.kilgore.fooddeliveryapp.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponse{
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private UserRole role;

}