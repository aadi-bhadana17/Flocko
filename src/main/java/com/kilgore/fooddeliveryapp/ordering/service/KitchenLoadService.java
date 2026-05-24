package com.kilgore.fooddeliveryapp.ordering.service;

import com.kilgore.fooddeliveryapp.ordering.model.KitchenLoadIndicator;
import com.kilgore.fooddeliveryapp.ordering.model.OrderStatus;
import com.kilgore.fooddeliveryapp.catalog.model.Restaurant;
import com.kilgore.fooddeliveryapp.ordering.repository.OrderRepository;
import org.springframework.stereotype.Service;

@Service
public class KitchenLoadService {

    private final OrderRepository orderRepository;

    public KitchenLoadService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public KitchenLoadIndicator getKitchenStatus(Restaurant restaurant) {
        int currentOrders = getCurrentOrders(restaurant);

        if (currentOrders < 10) {
            return KitchenLoadIndicator.LOW;
        } else if (currentOrders < 20) {
            return KitchenLoadIndicator.MEDIUM;
        } else {
            return KitchenLoadIndicator.HIGH;
        }
    }

    public int getCurrentOrders(Restaurant restaurant) {
        long orders = orderRepository.findAll().stream()
                .filter(order -> order.getRestaurant().equals(restaurant)
                && (order.getOrderStatus() == OrderStatus.PREPARING || order.getOrderStatus() == OrderStatus.CONFIRMED))
                .count();

        return (int) orders;
    }
}
