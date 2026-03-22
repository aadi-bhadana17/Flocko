package com.kilgore.fooddeliveryapp.dto.response;

import com.kilgore.fooddeliveryapp.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GroupDealParticipationResponse {
    private Long participantId;
    private Long userId;
    private String participantName;
    private String participantEmail;
    private String groupDealName;
    private int quantity;
    private PaymentStatus paymentStatus;
    private BigDecimal amountPaid;
    private boolean isConfirmed;
}
