package com.kilgore.fooddeliveryapp.ordering.dto.summary;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CartSummary {

    private Long cartId;
    List<CartItemSummary> cartItems;
    private BigDecimal totalPrice;
}
