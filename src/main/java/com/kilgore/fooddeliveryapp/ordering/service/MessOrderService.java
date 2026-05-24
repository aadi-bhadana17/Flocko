package com.kilgore.fooddeliveryapp.ordering.service;

import com.kilgore.fooddeliveryapp.catalog.model.Food;
import com.kilgore.fooddeliveryapp.catalog.model.MealType;
import com.kilgore.fooddeliveryapp.catalog.model.MessPlanSlot;
import com.kilgore.fooddeliveryapp.catalog.model.MessSubscription;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.identity.model.Address;
import com.kilgore.fooddeliveryapp.identity.model.User;
import com.kilgore.fooddeliveryapp.ordering.model.Order;
import com.kilgore.fooddeliveryapp.ordering.model.OrderItem;
import com.kilgore.fooddeliveryapp.ordering.model.OrderStatus;
import com.kilgore.fooddeliveryapp.ordering.model.OrderType;
import com.kilgore.fooddeliveryapp.ordering.repository.OrderItemRepository;
import com.kilgore.fooddeliveryapp.ordering.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public MessOrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }


    public MessPlanSlot processSubscription(MessSubscription subscription, DayOfWeek dayOfWeek, MealType mealType) {
        // Implement the logic to create an order for the subscriber based on their subscription details
        // This may involve creating an Order entity, setting its properties, and saving it to the database

        return subscription.getMessPlan().getSlots().stream()
                .filter(slot -> slot.getDayOfWeek() == dayOfWeek && slot.getMealType() == mealType)
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Mess plan slot not found for the specified day and meal type"));
    }

    @Transactional
    public void placeOrder(List<Food> foodList, User user, MessSubscription subscription) {

        Address address = user.getAddresses().stream()
                .filter(Address::isDefault)
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Default address not found for user: " + user.getUserId()));



        Order order = new Order();
        order.setUser(user);
        order.setRestaurant(subscription.getMessPlan().getRestaurant());
        order.setOrderStatus(OrderStatus.CREATED);
        order.setCreatedAt(LocalDateTime.now());
        order.setDeliveryAddress(address);
        order.setOrderType(OrderType.MESS);

        orderRepository.save(order);

        foodList.stream()
                .map(food -> extractOrderItems(order, food))
                .forEach(order.getOrderItems()::add);

        BigDecimal orderAmount = order.getOrderItems().stream()
                .map(OrderItem::getItemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalPrice(orderAmount);
        orderRepository.save(order);

    }

    //----------------------------------------------Helper Methods------------------------------------------------------

    private OrderItem extractOrderItems(Order order, Food food) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setFood(food);
        orderItem.setQuantity(1); // Assuming quantity is 1 for each food item in the mess plan
        orderItem.setItemTotal(food.getFoodPrice());
        orderItem.setPriceAtOrder(food.getFoodPrice());

        orderItemRepository.save(orderItem);
        return orderItem;
    }


}
