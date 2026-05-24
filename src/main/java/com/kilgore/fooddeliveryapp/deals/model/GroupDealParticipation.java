package com.kilgore.fooddeliveryapp.deals.model;

import com.kilgore.fooddeliveryapp.identity.model.Address;
import com.kilgore.fooddeliveryapp.ordering.model.PaymentStatus;
import com.kilgore.fooddeliveryapp.identity.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupDealParticipation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long participantId;
    @ManyToOne
    private User user;
    @ManyToOne
    private GroupDeal groupDeal;
    private int quantity;
    private PaymentStatus paymentStatus;
    private BigDecimal amountPaid;
    private boolean isConfirmed = true;
    @ManyToOne
    private Address addressToDeliver;
}
