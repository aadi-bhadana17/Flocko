package com.kilgore.fooddeliveryapp.deals.model;

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
    @Column(name = "restaurant_restaurant_id", nullable = false)
    private Long restaurantId;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private BigDecimal originalPrice;
    private int maxDiscount;

    @ElementCollection
    @CollectionTable(
            name = "group_deal_food_ids",
            joinColumns = @JoinColumn(name = "deal_id")
    )
    @Column(name = "food_id")
    private List<Long> foodIds = new ArrayList<>();

    private int targetParticipation;

    @Enumerated(EnumType.STRING)
    private GroupDealStatus status;

    private LocalDateTime confirmationWindowEndTime;

    @OneToMany(mappedBy = "groupDeal", cascade = CascadeType.ALL,  orphanRemoval = true)
    private List<GroupDealTier>  discountList = new ArrayList<>();
}
