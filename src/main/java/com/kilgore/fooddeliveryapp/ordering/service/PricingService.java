package com.kilgore.fooddeliveryapp.ordering.service;

import com.kilgore.fooddeliveryapp.catalog.api.CatalogFacade;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.AddonSummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.FoodSummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantExtendedSummary;
import com.kilgore.fooddeliveryapp.ordering.model.Cart;
import com.kilgore.fooddeliveryapp.ordering.model.CartItem;
import com.kilgore.fooddeliveryapp.ordering.repository.CartItemRepository;
import com.kilgore.fooddeliveryapp.ordering.repository.CartRepository;
import com.kilgore.fooddeliveryapp.ordering.repository.OrderRepository;
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
    private final CatalogFacade catalogFacade;

    public PricingService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                          OrderRepository orderRepository, CatalogFacade catalogFacade) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.catalogFacade = catalogFacade;
    }

    public BigDecimal calculateItemTotal(CartItem item) {
        FoodSummary food = catalogFacade.getFoodById(item.getFoodId());

        BigDecimal base = food.getFoodPrice();
        RestaurantExtendedSummary restaurant = catalogFacade.getRestaurantExtendedById(food.getRestaurantId());
        BigDecimal dynamicPrice = base.multiply(pricingMultiplier(food.getFoodId(), restaurant));

        BigDecimal addonTotal = catalogFacade.getAddonsByIds(item.getAddonIds()).stream()
                .map(AddonSummary::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return dynamicPrice.add(addonTotal)
                .multiply(BigDecimal.valueOf(item.getQuantity()));
    }

    public BigDecimal calculatePriceAtAddition(CartItem item) {
        FoodSummary food = catalogFacade.getFoodById(item.getFoodId());
        BigDecimal base = food.getFoodPrice();

        BigDecimal addonTotal = catalogFacade.getAddonsByIds(item.getAddonIds()).stream()
                .map(AddonSummary::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return base.add(addonTotal);
    }

    public BigDecimal calculateCartTotal(Cart cart) {
        return cart.getItems().stream()
                .map(CartItem::getItemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateCurrentPrice(FoodSummary food, List<AddonSummary> addons) {
        BigDecimal base = food.getFoodPrice();

        BigDecimal addonTotal = addons.stream()
                .map(AddonSummary::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return base.add(addonTotal);
    }

    public void recalculateCartTotals(Cart cart) {
        cart.setTotalPrice(calculateCartTotal(cart));
        cart.setTotalQuantity(calculateTotalQuantity(cart));
    }

    public boolean refreshExpiredPrices(Cart cart) {
        boolean anyPriceUpdated = false;

        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        for(CartItem item : cart.getItems()){
            if(item.getAddedTime().isBefore(oneHourAgo)){

                FoodSummary food = catalogFacade.getFoodById(item.getFoodId());
                List<AddonSummary> addons = catalogFacade.getAddonsByIds(item.getAddonIds());

                BigDecimal newPrice = calculateCurrentPrice(food, addons);

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

    private BigDecimal pricingMultiplier(Long foodId, RestaurantExtendedSummary restaurant) {

        int ordersLastHour = ordersInLastHour(foodId, restaurant.getRestaurantId());
        BigDecimal demandMultiplier = demandBasedMultiplier(ordersLastHour);
        BigDecimal timeMultiplier = timeBasedMultiplier(restaurant);

        return demandMultiplier.multiply(timeMultiplier);
    }

    private int ordersInLastHour(Long foodId, Long restaurantId) {

        return orderRepository.countFoodQuantityInLastHour(
                restaurantId,
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

    private BigDecimal timeBasedMultiplier(RestaurantExtendedSummary restaurant) {

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
