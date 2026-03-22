package com.kilgore.fooddeliveryapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GroupDeal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dealId;
    private String dealName;
    @ManyToOne
    private Restaurant restaurant;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private BigDecimal originalPrice;
    private int maxDiscount;

    @ManyToMany
    private List<Food> foodList = new ArrayList<>();
    private int targetParticipation;

    @Enumerated(EnumType.STRING)
    private GroupDealStatus status;

    private LocalDateTime confirmationWindowEndTime;

    @OneToMany(mappedBy = "groupDeal", cascade = CascadeType.ALL,  orphanRemoval = true)
    private List<GroupDealTier>  discountList = new ArrayList<>();
}
