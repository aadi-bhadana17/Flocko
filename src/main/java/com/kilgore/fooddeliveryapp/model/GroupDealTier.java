package com.kilgore.fooddeliveryapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GroupDealTier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dealTierId;
    @ManyToOne
    private GroupDeal groupDeal;

    private int thresholdPercent;
    private int discountPercent;
}
