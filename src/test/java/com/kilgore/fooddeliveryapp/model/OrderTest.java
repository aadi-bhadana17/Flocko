package com.kilgore.fooddeliveryapp.model;

import com.kilgore.fooddeliveryapp.ordering.model.Order;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void shouldCreateValidOrder() {
        Order order = new Order();

        order.setOrderId(1L);
        order.setTotalPrice(new BigDecimal("500"));

        assertEquals(1L, order.getOrderId());
        assertEquals(new BigDecimal("500"), order.getTotalPrice());
    }
}