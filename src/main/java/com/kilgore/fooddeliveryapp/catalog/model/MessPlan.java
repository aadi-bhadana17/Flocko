package com.kilgore.fooddeliveryapp.catalog.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class MessPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messPlanId;
    private String messPlanName;
    private String description;
    @ManyToOne
    private Restaurant restaurant;
    private BigDecimal price;

    @OneToMany(mappedBy = "messPlan", cascade = CascadeType.ALL,  orphanRemoval = true)
    private List<MessPlanSlot> slots = new ArrayList<>();

    private boolean isActive = true;
    private LocalDateTime deletionScheduledAt;
}
