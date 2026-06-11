package com.kilgore.fooddeliveryapp.deals.model;

import com.kilgore.fooddeliveryapp.common.enums.PaymentStatus;
import com.kilgore.fooddeliveryapp.common.enums.RefundReason;
import com.kilgore.fooddeliveryapp.common.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupDealParticipation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long participantId;
    @Column(name = "user_user_id" ,nullable = false)
    private Long userId;
    @ManyToOne
    private GroupDeal groupDeal;
    private int quantity;

    private PaymentStatus paymentStatus;
    private BigDecimal amountPaid;

    @Enumerated(EnumType.STRING)
    private RefundStatus refundStatus = RefundStatus.NONE;
    @Enumerated(EnumType.STRING)
    private RefundReason refundReason  = RefundReason.NONE;
    private LocalDateTime refundAt = null;
    private BigDecimal refundAmount = BigDecimal.ZERO;

    private boolean isConfirmed = true;
    @Column(name = "address_to_deliver_address_id", nullable = false)
    private Long addressIdToDeliver;
}
