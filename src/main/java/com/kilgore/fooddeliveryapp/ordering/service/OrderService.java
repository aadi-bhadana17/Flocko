package com.kilgore.fooddeliveryapp.ordering.service;

import com.kilgore.fooddeliveryapp.catalog.api.CatalogFacade;
import com.kilgore.fooddeliveryapp.common.util.UserAuthorization;
import com.kilgore.fooddeliveryapp.common.exceptions.*;
import com.kilgore.fooddeliveryapp.identity.api.UserFacade;
import com.kilgore.fooddeliveryapp.identity.dto.summary.AddressSummary;
import com.kilgore.fooddeliveryapp.ordering.mapper.OrderingMapper;
import com.kilgore.fooddeliveryapp.common.enums.PaymentStatus;
import com.kilgore.fooddeliveryapp.ordering.dto.request.PlaceOrderRequest;
import com.kilgore.fooddeliveryapp.ordering.dto.request.UpdateOrderStatusRequest;
import com.kilgore.fooddeliveryapp.ordering.dto.response.OrderResponse;
import com.kilgore.fooddeliveryapp.ordering.model.*;
import com.kilgore.fooddeliveryapp.ordering.repository.CartRepository;
import com.kilgore.fooddeliveryapp.ordering.repository.OrderItemRepository;
import com.kilgore.fooddeliveryapp.ordering.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final OrderItemRepository orderItemRepository;
    private final PricingService pricingService;
    private final UserAuthorization userAuthorization;
    private final KitchenLoadService kitchenLoadService;
    private final UserFacade userFacade;
    private final CatalogFacade catalogFacade;
    private final OrderingMapper orderingMapper;

    public OrderService(OrderRepository orderRepository, CartRepository cartRepository,
                         OrderItemRepository orderItemRepository, PricingService pricingService,
                        UserAuthorization userAuthorization, KitchenLoadService kitchenLoadService,
                        UserFacade userFacade, CatalogFacade catalogFacade, OrderingMapper orderingMapper) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.orderItemRepository = orderItemRepository;
        this.pricingService = pricingService;
        this.userAuthorization = userAuthorization;
        this.kitchenLoadService = kitchenLoadService;
        this.userFacade = userFacade;
        this.catalogFacade = catalogFacade;
        this.orderingMapper = orderingMapper;
    }

    //------------------------------------------- MAJOR ORDER HANDLING METHODS---------------------------------------------------

    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {

        Long userId = userAuthorization.authorizeUserId();
        validateUserCanPlaceOrder(userId);

        Cart cart = getValidatedCart();

        AddressSummary address = getValidatedUserAddress(userId, request);

        String message = refreshCartAndGetMessage(cart);

        Long restaurantId = cart.getRestaurantId();

        Order order = createBaseOrder(cart, address, request);

        validateWalletBalanceAndDeduct(userId, cart.getTotalPrice());

        orderRepository.save(order);

        List<OrderItem> orderItems = createOrderItems(cart, order);

        order.setOrderItems(orderItems);

        finalizeCartAfterOrder(cart);

        if(request.getScheduledAt() != null) {
            order.setOrderType(OrderType.PRE_ORDER);
            order.setScheduledAt(request.getScheduledAt());
        }

        updateKitchenLoadIndicator(restaurantId);

        return orderingMapper.toOrderResponse(order, message);
    }

    public List<OrderResponse> getMyOrders() {
        Long userId = userAuthorization.authorizeUserId();

        return orderRepository.findByUserId(userId).stream()
                .map(order -> orderingMapper.toOrderResponse(order, "Your order"))
                .toList();
    }

    public OrderResponse getOrder(Long orderId) {
        userAuthorization.authorizeUserId();

        return orderingMapper.toOrderResponse(fetchOrder(orderId), "");
    }

    @Transactional
    public String cancelOrder(Long orderId) {
        Long userId = userAuthorization.authorizeUserId();
        Order order = fetchOrder(orderId);

        validateOrderAccess(order, userId);

        if(isOrderCancelable(order)) {
            order.setOrderStatus(OrderStatus.CANCELLED);

            BigDecimal refund = processRefund(order, userId);

            orderRepository.save(order);
            return "Order has  been cancelled, and " + refund + " amount has been refunded to your wallet";
        }

        throw new EntityUnavailableException("The order is " + order.getOrderStatus() + " now, So it can't be cancelled");
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        Long userId = userAuthorization.authorizeUserId();

        Order order = fetchOrder(orderId);

        validateRestaurantAccess(userId, order.getRestaurantId());

        OrderStatus newStatus = validateOrderStatusTransition(request, order);

        order.setOrderStatus(newStatus);
        orderRepository.save(order);

        return orderingMapper.toOrderResponse(order, "Order status has been updated");
    }

    public List<OrderResponse> getRestaurantOrders(Long restaurantId) {

        Long userId = userAuthorization.authorizeUserId();

        boolean allow =
                userFacade.isUserAdmin(userId)
                        || userFacade.isOwnerOfRestaurant(userId, restaurantId)
                        || userFacade.isEmployedAt(userId, restaurantId);

        if (!allow) {
            throw new AccessDeniedException("Access denied, you don't have permission to access orders of this restaurant");
        }

        return orderRepository.findByRestaurantId(restaurantId).stream()
                .map(order -> orderingMapper.toOrderResponse(order, "Your Restaurant Order"))
                .toList();
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(order -> orderingMapper.toOrderResponse(order, "All Orders"))
                .toList();
    }


    //--------------------------------------ACCESS CONTROL AND AUTHORIZATION ------------------------------------------------------


    private void validateUserCanPlaceOrder(Long userId) {
        if(userFacade.isUserRestricted(userId)) {
            throw new UserStatusException("Your account is currently restricted, you can't place orders right now. Please contact support for more info.");
        }
    }

    private void validateRestaurantAccess(Long userId, Long restaurantId) {
        if(!userFacade.isOwnerOfRestaurant(userId, restaurantId) &&
                !userFacade.isEmployedAt(userId, restaurantId)) {
            throw new AccessDeniedException("Access denied, this order doesn't belong to your restaurant");
        }
    }

    private void validateOrderAccess(Order order, Long userId) {
        if(userFacade.isUserAdmin(userId))
            return;
        if(userFacade.isUserCustomer(userId)) {
            if (!Objects.equals(order.getUserId(), userId))
                throw new AccessDeniedException("Access denied, this order doesn't belong to you");
            return;
        }

        if(userFacade.isOwnerOfRestaurant(userId, order.getRestaurantId())
                || userFacade.isEmployedAt(userId, order.getRestaurantId()))
            return;

        throw new AccessDeniedException("Access denied, you don't have permission to access this order");
    }


    //---------------------------------------VALIDATION AND INTEGRITY CHECK---------------------------------------------


    private void validateCartItems(CartItem cartItem) {
        if(!catalogFacade.isFoodAvailable(cartItem.getFoodId())) {
            throw new EntityUnavailableException("One or more Foods from cart is not available at this time");
        }

        if(cartItem.getAddonIds() != null && !cartItem.getAddonIds().isEmpty()) {
            if (cartItem.getAddonIds().stream()
                    .anyMatch(addonId -> !catalogFacade.isAddonAvailable(addonId))) {
                throw new EntityUnavailableException("One or more Addons from cart is not available at this time");
            }
        }

    }

    private Cart getValidatedCart() {
        Long userId = userAuthorization.authorizeUserId();

        Cart cart = cartRepository.findByUserId(userId);
        if (cart == null) {
            throw new EntityUnavailableException("Cart not found");
        } else if(cart.getItems().isEmpty())
            throw new EntityUnavailableException("Cart is empty");

        return cart;
    }

    private boolean isOrderCancelable(Order order) {
        return order.getOrderStatus() == OrderStatus.CREATED ||
                order.getOrderStatus() == OrderStatus.CONFIRMED;
    }

    private AddressSummary getValidatedUserAddress(Long userId, PlaceOrderRequest request) {
        AddressSummary address = userFacade.getAddressById(request.getAddressId());

        if (!userFacade.isAddressOwnedByUser(userId, request.getAddressId()))
            throw new AccessDeniedException("This address doesn't belongs to you, choose another address");

        return address;
    }

    private OrderStatus validateOrderStatusTransition(UpdateOrderStatusRequest request, Order order) {
        OrderStatus currentStatus = order.getOrderStatus();
        OrderStatus newStatus = request.getOrderStatus();

        if(newStatus == null)
            throw new InvalidOrderStateException("Cannot transition order from " + currentStatus + " to null");
        if(!currentStatus.canTransitionTo(newStatus))
            throw new InvalidOrderStateException("Cannot transition order from " + currentStatus + " to " + newStatus);

        return newStatus;
    }


    //------------------------------------------- PAYMENT HANDLING -----------------------------------------------------


    private BigDecimal processRefund(Order order, Long userId) {
        BigDecimal refund = order.getTotalPrice();

        if(order.getOrderType() == OrderType.PRE_ORDER) {
            refund = order.getTotalPrice().multiply(BigDecimal.valueOf(0.75));
        }
        order.setRefundAmount(refund);
        userFacade.addWalletBalance(userId, refund);

        return refund;
    }

    public void validateWalletBalanceAndDeduct(Long userId, BigDecimal amount) {
        if (userFacade.getWalletBalance(userId).compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance, please deposit money first");
        }

        userFacade.deductWalletBalance(userId, amount);
    }

    //---------------------------------------- CART AND ITEM MANAGEMENT ------------------------------------------------


    private void finalizeCartAfterOrder(Cart cart) {
        cart.getItems().clear();
        cart.setRestaurantId(null);
        cartRepository.save(cart);
    }

    private String refreshCartAndGetMessage(Cart cart) {
        if (!pricingService.refreshExpiredPrices(cart)) return "Happy meal :)";

        pricingService.recalculateCartTotals(cart);
        return "Some prices of cart has been updated according to current prices in restaurant";
    }

    public List<OrderItem> createOrderItems(Cart cart,  Order order) {
        return cart.getItems().stream()
                .map(item -> createOrderItem(item, order))
                .toList();
    }

    public OrderItem createOrderItem(CartItem cartItem, Order order) {

        validateCartItems(cartItem);

        OrderItem orderItem = new OrderItem();

        orderItem.setFoodId(cartItem.getFoodId());
        orderItem.setQuantity(cartItem.getQuantity());
        orderItem.setPriceAtOrder(cartItem.getPriceAtAddition());
        orderItem.setItemTotal(cartItem.getItemTotal());

        orderItemRepository.save(orderItem);

        orderItem.setAddonIds(cartItem.getAddonIds());
        orderItem.setOrder(order);

        orderItemRepository.save(orderItem);

        return orderItem;
    }


    //-------------------------------------------- OTHER HELPER METHODS ------------------------------------------------


    private Order fetchOrder(Long orderId) {

        return orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order with id " + orderId + " not found"));
    }

    private void updateKitchenLoadIndicator(Long restaurantId) {
        catalogFacade.setKitchenLoadIndicator(restaurantId,
                kitchenLoadService.getKitchenStatus(restaurantId));
    }

    private Order createBaseOrder(Cart cart, AddressSummary address,
                                  PlaceOrderRequest request) {
        Order order = new Order();

        order.setUserId(cart.getUserId());
        order.setRestaurantId(cart.getRestaurantId());
        order.setTotalPrice(cart.getTotalPrice());
        order.setOrderStatus(OrderStatus.CREATED);
        order.setCreatedAt(LocalDateTime.now());
        order.setDeliveryAddressId(address.getAddressId());
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setTotalQuantity(cart.getTotalQuantity());

        if(request.isSpecial()) order.setSpecial(true);

        return order;
    }

}
