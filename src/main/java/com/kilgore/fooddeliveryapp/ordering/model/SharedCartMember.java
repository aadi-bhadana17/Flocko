package com.kilgore.fooddeliveryapp.ordering.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SharedCartMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberId;

    @Column(name = "user_user_id", nullable = false)
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Cart cart;
    @ManyToOne(fetch = FetchType.LAZY)
    private SharedCart sharedCart;

    private BigDecimal walletContribution =  BigDecimal.ZERO;
    private boolean isActive = true;
}
