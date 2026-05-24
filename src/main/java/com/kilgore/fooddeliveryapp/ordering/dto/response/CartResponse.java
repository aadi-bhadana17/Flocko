package com.kilgore.fooddeliveryapp.ordering.dto.response;

import com.kilgore.fooddeliveryapp.ordering.dto.summary.CartItemSummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.identity.dto.summary.UserSummary;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    private Long cartId;
    private UserSummary user;
    private List<CartItemSummary> cartItems;
    private int totalQuantity;
    private BigDecimal totalPrice;
    private RestaurantSummary restaurant;
    private String message;
}
