package com.kilgore.fooddeliveryapp.identity.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChangePasswordRequest {

    @NotBlank
    private String oldPassword;
    @NotBlank
    @Length(min = 8)
    private String newPassword;
    private String confirmNewPassword;

    @AssertTrue
    public boolean isPasswordsMatch() {
        return confirmNewPassword.equals(newPassword);
    }

    @AssertTrue
    public boolean isPasswordChanged() {
        return !oldPassword.equals(newPassword);
    }
}
