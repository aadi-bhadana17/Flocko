package com.kilgore.fooddeliveryapp.identity.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long addressId;

    private String buildingNo;
    private String street;
    private String city;
    private String state;
    private String pincode;
    private String landmark;

    @ManyToOne
    @JsonIgnore
    private User user;

    public boolean isDefault = false;
}