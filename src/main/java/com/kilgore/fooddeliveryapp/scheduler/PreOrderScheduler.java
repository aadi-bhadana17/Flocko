package com.kilgore.fooddeliveryapp.scheduler;

import com.kilgore.fooddeliveryapp.model.Order;
import com.kilgore.fooddeliveryapp.model.OrderStatus;
import com.kilgore.fooddeliveryapp.repository.OrderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PreOrderScheduler {

    private final OrderRepository orderRepository;

    public PreOrderScheduler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Scheduled(fixedRate = 60000)
    // This method runs every minute to check for pre-orders that are scheduled to be confirmed
    public void scheduledOrders() {
        LocalDateTime now = LocalDateTime.now();

       List<Order> orders = orderRepository.findDuePreOrders(now, OrderStatus.CREATED);

       orders.forEach(order -> order.setOrderStatus(OrderStatus.CONFIRMED));
       orderRepository.saveAll(orders);
    }
}
