package com.kilgore.fooddeliveryapp.service;

import com.kilgore.fooddeliveryapp.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.model.*;
import com.kilgore.fooddeliveryapp.repository.CartItemRepository;
import com.kilgore.fooddeliveryapp.repository.CartRepository;
import com.kilgore.fooddeliveryapp.repository.FoodRepository;
import com.kilgore.fooddeliveryapp.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class PricingService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final FoodRepository foodRepository;

    public PricingService(CartRepository cartRepository, CartItemRepository cartItemRepository, OrderRepository orderRepository, FoodRepository foodRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.foodRepository = foodRepository;
    }

    public BigDecimal calculateItemTotal(CartItem item) {
        Food food = foodRepository.findById(item.getFood().getFoodId())
                .orElseThrow(() -> new EntityNotFoundException("Food item not found with ID: " + item.getFood().getFoodId()));

        BigDecimal base = food.getFoodPrice();
        BigDecimal dynamicPrice = base.multiply(pricingMultiplier(food.getFoodId(), food.getRestaurant()));

        BigDecimal addonTotal = item.getAddons().stream()
                .map(Addon::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return dynamicPrice.add(addonTotal)
                .multiply(BigDecimal.valueOf(item.getQuantity()));
    }

    public BigDecimal calculatePriceAtAddition(CartItem item) {
        BigDecimal base = item.getFood().getFoodPrice();

        BigDecimal addonTotal = item.getAddons().stream()
                .map(Addon::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return base.add(addonTotal);
    }

    public BigDecimal calculateCartTotal(Cart cart) {
        return cart.getItems().stream()
                .map(CartItem::getItemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateCurrentPrice(Food food, List<Addon> addons) {
        BigDecimal base = food.getFoodPrice();

        BigDecimal addonTotal = addons.stream()
                .map(Addon::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return base.add(addonTotal);
    }

    public void updateCartTotal(Cart cart) {
        cart.setTotalPrice(calculateCartTotal(cart));
        cart.setTotalQuantity(calculateTotalQuantity(cart));
        cartRepository.save(cart);
    }

    public boolean refreshExpiredPrices(Cart cart) {
        boolean anyPriceUpdated = false;

        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        for(CartItem item : cart.getItems()){
            if(item.getAddedTime().isBefore(oneHourAgo)){
                BigDecimal newPrice = calculateCurrentPrice(
                        item.getFood(),
                        item.getAddons());

                if(!item.getPriceAtAddition().equals(newPrice)){
                    item.setPriceAtAddition(newPrice);
                    item.setItemTotal(newPrice.multiply
                            (BigDecimal.valueOf(item.getQuantity())));
                    item.setAddedTime(LocalDateTime.now());
                    anyPriceUpdated = true;

                    cartItemRepository.save(item);
                }
            }
        }
        return anyPriceUpdated;
    }

    private int calculateTotalQuantity(Cart  cart) {
        return cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    private BigDecimal pricingMultiplier(Long foodId, Restaurant restaurant) {

        int ordersLastHour = ordersInLastHour(foodId, restaurant);
        BigDecimal demandMultiplier = demandBasedMultiplier(ordersLastHour);
        BigDecimal timeMultiplier = timeBasedMultiplier(restaurant);

        return demandMultiplier.multiply(timeMultiplier);
    }

    private int ordersInLastHour(Long foodId, Restaurant restaurant) {

        return orderRepository.countFoodQuantityInLastHour(
                restaurant,
                LocalDateTime.now().minusHours(1),
                foodId
        );
    }

    private BigDecimal demandBasedMultiplier(int ordersLastHour) {
        if (ordersLastHour <= 20) return BigDecimal.ONE;
        if (ordersLastHour < 50) return BigDecimal.valueOf(1.08);
        if(ordersLastHour < 100) return BigDecimal.valueOf(1.15);
        return BigDecimal.valueOf(1.18);
    }

    private BigDecimal timeBasedMultiplier(Restaurant restaurant) {

        LocalTime now = LocalTime.now();
        LocalTime opening = restaurant.getOpeningTime();
        LocalTime closing = restaurant.getClosingTime();

        if (now.isBefore(opening) || now.isAfter(closing)) {
            return BigDecimal.ONE;
        }

        // Opening discounts
        if (now.isBefore(opening.plusMinutes(30))) return BigDecimal.valueOf(0.95);
        if (now.isBefore(opening.plusHours(1))) return BigDecimal.valueOf(0.98);

        // Lunch peak hours
        if (isBetween(now, LocalTime.NOON, LocalTime.of(14, 0)))
            return BigDecimal.valueOf(1.05);

        // Dinner peak hours
        if (isBetween(now, LocalTime.of(19, 30), LocalTime.of(22, 30)))
            return BigDecimal.valueOf(1.10);

        // Closing discounts
        if (now.isAfter(closing.minusMinutes(30))) return BigDecimal.valueOf(0.92);
        if (now.isAfter(closing.minusHours(1))) return BigDecimal.valueOf(0.97);

        return BigDecimal.ONE;
    }
    private boolean isBetween(LocalTime time, LocalTime start, LocalTime end) {
        return !time.isBefore(start) && !time.isAfter(end);
    }
}
