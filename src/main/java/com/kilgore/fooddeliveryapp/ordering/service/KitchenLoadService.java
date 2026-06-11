package com.kilgore.fooddeliveryapp.ordering.service;

import com.kilgore.fooddeliveryapp.common.enums.KitchenLoadIndicator;
import com.kilgore.fooddeliveryapp.ordering.repository.OrderRepository;
import org.springframework.stereotype.Service;

@Service
public class KitchenLoadService {

    private final OrderRepository orderRepository;

    public KitchenLoadService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public KitchenLoadIndicator getKitchenStatus(Long restaurantId) {
        long currentOrders = getCurrentOrders(restaurantId);

        if (currentOrders < 10) {
            return KitchenLoadIndicator.LOW;
        } else if (currentOrders < 20) {
            return KitchenLoadIndicator.MEDIUM;
        } else {
            return KitchenLoadIndicator.HIGH;
        }
    }

    public long getCurrentOrders(Long restaurantId) {
        return orderRepository.countCurrentOrdersByRestaurantId(restaurantId);
    }
}
