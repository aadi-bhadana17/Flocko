package com.kilgore.fooddeliveryapp.identity.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignupRequest {
    @NotBlank
    private String firstName;
    private String lastName;
    @NotBlank
    @Email
    private String email;
    @NotBlank
    @Length(min = 8)
    private String password;
    @NotBlank
    private String confirmPassword;

    @AssertTrue
    public boolean isPasswordsMatch() {
        return password.equals(confirmPassword);
    }
}
