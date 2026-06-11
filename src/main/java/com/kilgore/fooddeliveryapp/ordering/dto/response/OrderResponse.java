package com.kilgore.fooddeliveryapp.ordering.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kilgore.fooddeliveryapp.identity.dto.summary.AddressSummary;
import com.kilgore.fooddeliveryapp.ordering.dto.summary.OrderItemSummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.identity.dto.summary.UserSummary;
import com.kilgore.fooddeliveryapp.ordering.model.OrderStatus;
import com.kilgore.fooddeliveryapp.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {

    private Long orderId;
    private UserSummary user;
    private RestaurantSummary restaurant;
    private OrderStatus orderStatus;
    private LocalDateTime createdAt;
    private AddressSummary address;

    private List<OrderItemSummary> orderItems;
    private PaymentStatus paymentStatus;
    private BigDecimal totalPrice;
    private int totalQuantity;

    private LocalDateTime scheduledAt;

    @JsonProperty("isSpecial")
    private boolean isSpecial;

    private String message;

}
