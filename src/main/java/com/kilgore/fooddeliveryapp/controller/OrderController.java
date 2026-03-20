package com.kilgore.fooddeliveryapp.controller;

import com.kilgore.fooddeliveryapp.dto.request.PlaceOrderRequest;
import com.kilgore.fooddeliveryapp.dto.request.UpdateOrderStatusRequest;
import com.kilgore.fooddeliveryapp.dto.response.OrderResponse;
import com.kilgore.fooddeliveryapp.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }


    @PostMapping
    public OrderResponse placeOrder(@RequestBody PlaceOrderRequest request) {
        return orderService.placeOrder(request);
    }

    @GetMapping
    public List<OrderResponse> getMyOrders() {
        return orderService.getMyOrders();
    }

    @GetMapping("/restaurant")
    @PreAuthorize("hasAnyAuthority('RESTAURANT_OWNER', 'RESTAURANT_STAFF')")
    public List<OrderResponse> getRestaurantOrders() {
        return orderService.getRestaurantOrders();
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable Long orderId) {
        return orderService.getOrder(orderId);
    }

    @PatchMapping("/{orderId}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'RESTAURANT_STAFF')")
    public String cancelOrder(@PathVariable Long orderId) {
        return orderService.cancelOrder(orderId);
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAnyAuthority('RESTAURANT_OWNER', 'RESTAURANT_STAFF')")
    public OrderResponse updateOrderStatus(@PathVariable Long orderId,
                                           @RequestBody UpdateOrderStatusRequest request) {
        return orderService.updateOrderStatus(orderId, request);
    }

    @GetMapping("/restaurant/{restaurantId}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public List<OrderResponse> getAdminRestaurantOrders(@PathVariable Long restaurantId) {
        return orderService.getAdminRestaurantOrders(restaurantId);
    }
}
