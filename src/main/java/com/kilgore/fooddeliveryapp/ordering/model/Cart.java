package com.kilgore.fooddeliveryapp.ordering.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartId;

    @Column(name = "user_user_id", nullable = false)
    private Long userId;

    @OneToMany(mappedBy = "cart",  cascade = CascadeType.ALL,  fetch = FetchType.EAGER, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    private int totalQuantity;
    private BigDecimal totalPrice;

    @Column(name = "restaurant_restaurant_id")
    private Long restaurantId;

    @ManyToOne
    private SharedCart sharedCart; // if this cart is part of a shared cart, otherwise null,
    // and it will keep change as user might switch between shared carts

}
