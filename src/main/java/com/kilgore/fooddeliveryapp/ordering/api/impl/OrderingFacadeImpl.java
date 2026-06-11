package com.kilgore.fooddeliveryapp.ordering.api.impl;

import com.kilgore.fooddeliveryapp.catalog.api.CatalogFacade;
import com.kilgore.fooddeliveryapp.common.enums.PaymentStatus;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.deals.dto.request.CreateGroupDealOrderRequest;
import com.kilgore.fooddeliveryapp.ordering.api.OrderingFacade;
import com.kilgore.fooddeliveryapp.ordering.model.Order;
import com.kilgore.fooddeliveryapp.ordering.model.OrderItem;
import com.kilgore.fooddeliveryapp.ordering.model.OrderStatus;
import com.kilgore.fooddeliveryapp.ordering.model.OrderType;
import com.kilgore.fooddeliveryapp.ordering.repository.OrderItemRepository;
import com.kilgore.fooddeliveryapp.ordering.repository.OrderRepository;
import com.kilgore.fooddeliveryapp.ordering.service.OrderService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class OrderingFacadeImpl implements OrderingFacade {

    private final OrderRepository orderRepository;
    private final CatalogFacade catalogFacade;
    private final OrderItemRepository orderItemRepository;
    private final OrderService orderService;

    public OrderingFacadeImpl(OrderRepository orderRepository, CatalogFacade catalogFacade, OrderItemRepository orderItemRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.catalogFacade = catalogFacade;
        this.orderItemRepository = orderItemRepository;
        this.orderService = orderService;
    }


    @Override
    public boolean hasUserOrderedFrom(Long userId, Long restaurantId) {
        return false;
    }

    @Override
    public boolean isOrderActive(Long orderId) {
        Order order  = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId));
        return order.getOrderStatus() != OrderStatus.CANCELLED && order.getOrderStatus() != OrderStatus.DELIVERED;
    }

    @Override
    public Long placeGroupDealOrder(CreateGroupDealOrderRequest request) {

        orderService.validateWalletBalanceAndDeduct(request.getUserId(), request.getPrice());

        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setRestaurantId(request.getRestaurantId());
        order.setDeliveryAddressId(request.getDeliveryAddressId());
        order.setTotalPrice(request.getPrice());
        order.setTotalQuantity(request.getQuantity());
        order.setOrderStatus(OrderStatus.CREATED);
        order.setOrderType(OrderType.GROUP_DEAL);
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setCreatedAt(LocalDateTime.now());

        orderRepository.save(order);
        order.setOrderItems(createOrderItems(request.getFoodIds(), order));
        orderRepository.save(order);

        return order.getOrderId();
    }

    private List<OrderItem> createOrderItems(List<Long> foodIds, Order order) {
        List<OrderItem> orderItems = new ArrayList<>();

        foodIds.forEach(foodId -> {
            OrderItem item = new OrderItem();
            BigDecimal price = catalogFacade.getFoodPriceById(foodId);

            item.setFoodId(foodId);
            item.setQuantity(1);
            item.setPriceAtOrder(price);
            item.setItemTotal(price);
            item.setOrder(order);

            orderItems.add(item);
        });
        orderItemRepository.saveAll(orderItems);
        return orderItems;
    }
}
