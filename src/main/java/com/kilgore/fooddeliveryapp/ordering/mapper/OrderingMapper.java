package com.kilgore.fooddeliveryapp.ordering.mapper;

import com.kilgore.fooddeliveryapp.catalog.api.CatalogFacade;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.AddonSummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.FoodSummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.identity.api.UserFacade;
import com.kilgore.fooddeliveryapp.identity.dto.summary.AddressSummary;
import com.kilgore.fooddeliveryapp.identity.dto.summary.UserSummary;
import com.kilgore.fooddeliveryapp.ordering.dto.response.CartResponse;
import com.kilgore.fooddeliveryapp.ordering.dto.response.OrderResponse;
import com.kilgore.fooddeliveryapp.ordering.dto.response.SharedCartResponse;
import com.kilgore.fooddeliveryapp.ordering.dto.summary.CartItemSummary;
import com.kilgore.fooddeliveryapp.ordering.dto.summary.CartSummary;
import com.kilgore.fooddeliveryapp.ordering.dto.summary.OrderItemSummary;
import com.kilgore.fooddeliveryapp.ordering.dto.summary.SharedCartMemberSummary;
import com.kilgore.fooddeliveryapp.ordering.model.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class OrderingMapper {

    private final CatalogFacade catalogFacade;
    private final UserFacade userFacade;

    public OrderingMapper(CatalogFacade catalogFacade, UserFacade userFacade) {
        this.catalogFacade = catalogFacade;
        this.userFacade = userFacade;
    }


    public OrderResponse toOrderResponse(Order order, String message) {
        UserSummary user = userFacade.getUserById(order.getUserId());
        RestaurantSummary restaurant = catalogFacade.getRestaurantById(order.getRestaurantId());
        AddressSummary address = userFacade.getAddressById(order.getDeliveryAddressId());

        return new OrderResponse(
                order.getOrderId(),
                user,
                restaurant,
                order.getOrderStatus(),
                order.getCreatedAt(),
                address,
                createOrderItemsSummaries(order),
                order.getPaymentStatus(),
                order.getTotalPrice(),
                order.getTotalQuantity(),
                order.getScheduledAt(),
                order.isSpecial(),
                message
        );
    }

    public CartResponse toCartResponse(Cart cart, UserSummary user, RestaurantSummary restaurant, String message) {
        return new CartResponse(
                cart.getCartId(),
                user,
                toCartItemSummaries(cart.getItems()),
                cart.getTotalQuantity(),
                cart.getTotalPrice(),
                restaurant,
                message
        );
    }

    public SharedCartResponse toSharedCartResponse(SharedCart sharedCart, Long viewerUserId) {
        boolean viewerIsHost = sharedCart.getHostUserId().equals(viewerUserId);

        List<SharedCartMember> activeMembers = sharedCart.getMemberList().stream()
                .filter(SharedCartMember::isActive)
                .toList();

        BigDecimal recalculatedTotal = activeMembers.stream()
                .map(member -> member.getCart() != null && member.getCart().getTotalPrice() != null
                        ? member.getCart().getTotalPrice()
                        : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        System.out.println("Shared Cart ID: " + sharedCart.getSharedCartId());
        System.out.println("Restaurant ID: " + sharedCart.getRestaurantId());

        return new SharedCartResponse(
                sharedCart.getSharedCartId(),
                userFacade.getUserById(sharedCart.getHostUserId()),
                catalogFacade.getRestaurantById(sharedCart.getRestaurantId()),
                createSharedCartMemberSummaries(activeMembers),
                sharedCart.getJoinCode(),
                recalculatedTotal,
                sharedCart.getAmountPaid(),
                sharedCart.isHostPaysAll(),
                sharedCart.isActive(),
                !viewerIsHost,
                viewerIsHost
        );
    }

    private List<OrderItemSummary> createOrderItemsSummaries(Order order) {
        return order.getOrderItems().stream()
                .map(this::createOrderItemSummary)
                .toList();
    }

    private OrderItemSummary createOrderItemSummary(OrderItem orderItem) {

        List<AddonSummary> addons = new ArrayList<>();

        if(orderItem.getAddonIds() != null && !orderItem.getAddonIds().isEmpty()) {
            addons = catalogFacade.getAddonsByIds(orderItem.getAddonIds());
        }

        return new OrderItemSummary(
                orderItem.getOrderItemId(),
                orderItem.getFoodId(),
                catalogFacade.getFoodName(orderItem.getFoodId()),
                orderItem.getQuantity(),
                orderItem.getPriceAtOrder(),
                addons,
                orderItem.getItemTotal()
        );
    }

    private List<CartItemSummary> toCartItemSummaries(List<CartItem> cartItems) {
        return cartItems.stream().map(this::toCartItemSummary).toList();
    }

    private CartItemSummary toCartItemSummary(CartItem item) {
        FoodSummary food = catalogFacade.getFoodById(item.getFoodId());
        List<AddonSummary> addons = catalogFacade.getAddonsByIds(item.getAddonIds());

        return new CartItemSummary(
                item.getCartItemId(),
                food.getFoodName(),
                item.getQuantity(),
                addons,
                item.getItemTotal()
        );
    }

    private List<SharedCartMemberSummary> createSharedCartMemberSummaries(List<SharedCartMember> members) {
        return members.stream()
                .map(this::createSharedCartMemberSummary)
                .toList();
    }

    private SharedCartMemberSummary createSharedCartMemberSummary(SharedCartMember member) {
        return new SharedCartMemberSummary(
                member.getMemberId(),
                userFacade.getUserById(member.getUserId()),
                toCartSummary(member.getCart()),
                member.getWalletContribution()
        );
    }

    private CartSummary toCartSummary(Cart cart) {
        Objects.requireNonNull(cart, "Cart cannot be null");

        return new CartSummary(
                cart.getCartId(),
                toCartItemSummaries(cart.getItems()),
                cart.getTotalPrice()
        );
    }
}
