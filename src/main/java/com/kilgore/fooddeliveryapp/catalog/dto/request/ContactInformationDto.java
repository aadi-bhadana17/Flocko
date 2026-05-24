package com.kilgore.fooddeliveryapp.catalog.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContactInformationDto {
    @NotBlank
    @Email
    private String email;
    @NotBlank
    @Pattern(
            regexp = "^(\\+91)?[6-9][0-9]{9}$"
    )
    private String mobile;

    private String instagram;
    private String facebook;
    private String twitter;
}
