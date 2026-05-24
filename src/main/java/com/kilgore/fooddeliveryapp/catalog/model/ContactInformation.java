package com.kilgore.fooddeliveryapp.catalog.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ContactInformation {
    private String email;
    private String mobile;
    private String instagram;
    private String facebook;
    private String twitter;

}