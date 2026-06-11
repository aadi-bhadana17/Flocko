package com.kilgore.fooddeliveryapp.catalog.model;

import com.kilgore.fooddeliveryapp.common.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class MessSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long subscriptionId;

    @Column(name = "user_id")
    private Long userId;

    @ManyToOne
    private MessPlan messPlan;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;
    private PaymentStatus paymentStatus;
}
