package com.kilgore.fooddeliveryapp.dto.response;

import com.kilgore.fooddeliveryapp.dto.summary.AddressSummary;
import com.kilgore.fooddeliveryapp.dto.summary.OrderItemSummary;
import com.kilgore.fooddeliveryapp.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.dto.summary.UserSummary;
import com.kilgore.fooddeliveryapp.model.OrderStatus;
import com.kilgore.fooddeliveryapp.model.PaymentStatus;
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

    private String message;

}
