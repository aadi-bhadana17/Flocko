package com.kilgore.fooddeliveryapp.ordering.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SharedCart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sharedCartId;

    @Column(name = "host_user_id", nullable = false)
    private Long hostUserId;

    @Column(name = "restaurant_restaurant_id", nullable = false)
    private Long restaurantId;

    @OneToMany(mappedBy = "sharedCart")
    private List<SharedCartMember> memberList = new ArrayList<>();

    @Column(unique = true)
    private String joinCode;


    private BigDecimal totalPrice = BigDecimal.ZERO;
    private BigDecimal amountPaid = BigDecimal.ZERO; // paid by users already

    private boolean hostPaysAll = true; // on false every user has to pay money for the foods they add,
    // and the amount should present in their wallet - no upi

    private boolean isActive =  true;
}
