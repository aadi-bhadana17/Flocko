package com.kilgore.fooddeliveryapp.service;

import com.kilgore.fooddeliveryapp.authorization.UserAuthorization;
import com.kilgore.fooddeliveryapp.dto.request.PlaceOrderRequest;
import com.kilgore.fooddeliveryapp.dto.request.UpdateOrderStatusRequest;
import com.kilgore.fooddeliveryapp.dto.response.OrderResponse;
import com.kilgore.fooddeliveryapp.dto.summary.*;
import com.kilgore.fooddeliveryapp.exceptions.*;
import com.kilgore.fooddeliveryapp.model.*;
import com.kilgore.fooddeliveryapp.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final PricingService pricingService;
    private final UserAuthorization userAuthorization;
    private final KitchenLoadService kitchenLoadService;

    public OrderService(OrderRepository orderRepository, CartRepository cartRepository,
                        AddressRepository addressRepository, OrderItemRepository orderItemRepository,
                        UserRepository userRepository, RestaurantRepository restaurantRepository, PricingService pricingService, UserAuthorization userAuthorization, KitchenLoadService kitchenLoadService) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.addressRepository = addressRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.pricingService = pricingService;
        this.userAuthorization = userAuthorization;
        this.kitchenLoadService = kitchenLoadService;
    }

    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        User user = userAuthorization.authorizeUser();

        if(user.getAccountStatus().equals(AccountStatus.RESTRICTED)) {
            throw new UserStatusException("Your account is currently restricted, you can't place orders until " + user.getRestrictedUntil()
                    + ". Please contact support for more info.");
        }

        Cart cart = fetchCart();

        if(cart.getItems().isEmpty())
            throw new EntityUnavailableException("Cart is empty");

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new EntityNotFoundException("Address not found"));

        String message = updateCartPrice(cart) ? "Some prices of cart has been updated according to current prices in restaurant"
                : "Happy meal :)";

        Order order = new Order();

        order.setUser(cart.getUser());
        order.setRestaurant(cart.getRestaurant());
        order.setTotalPrice(cart.getTotalPrice());
        order.setOrderStatus(OrderStatus.CREATED);
        order.setCreatedAt(LocalDateTime.now());
        order.setDeliveryAddress(address);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setTotalQuantity(cart.getTotalQuantity());

        if(request.isSpecial()) order.setSpecial(true);

        if (user.getWalletBalance().compareTo(cart.getTotalPrice()) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance, please deposit money first");
        }

        user.setWalletBalance(user.getWalletBalance().subtract(cart.getTotalPrice()));
        userRepository.save(user);

        orderRepository.save(order);

        List<OrderItem> orderItems = createOrderItems(cart, order);

        order.setOrderItems(orderItems);

        cart.getItems().clear();
        cart.setRestaurant(null);
        cartRepository.save(cart);

        if(request.getScheduledAt() != null) {
            order.setOrderType(OrderType.PRE_ORDER);
            order.setScheduledAt(request.getScheduledAt());
        }

        setRestaurantKitchenStatus(order.getRestaurant());

        return createOrderResponse(order, message);
    }

    private void setRestaurantKitchenStatus(Restaurant restaurant) {
        restaurant.setKitchenStatus(kitchenLoadService.getKitchenStatus(restaurant));
        restaurantRepository.save(restaurant);
    }

    private List<OrderItem> createOrderItems(Cart cart,  Order order) {
        return cart.getItems().stream()
                .map(item -> createOrderItem(item, order))
                .toList();
    }

    protected OrderItem createOrderItem(CartItem cartItem,  Order order) {

        validateCartItems(cartItem);

        OrderItem orderItem = new OrderItem();

        orderItem.setFood(cartItem.getFood());
        orderItem.setQuantity(cartItem.getQuantity());
        orderItem.setPriceAtOrder(cartItem.getPriceAtAddition());
        orderItem.setItemTotal(cartItem.getItemTotal());

        orderItemRepository.save(orderItem);

        orderItem.setAddons(cartItem.getAddons());
        orderItem.setOrder(order);

        orderItemRepository.save(orderItem);

        return orderItem;
    }

    private void validateCartItems(CartItem cartItem) {
        if(!cartItem.getFood().isAvailable()) {
            throw new EntityUnavailableException("One or more Foods from cart is not available at this time");
        }

        if(cartItem.getAddons().stream()
                .anyMatch(addon -> !addon.isAvailable()))
            throw new EntityUnavailableException("One or more Addons from cart is not available at this time");

    }

    private Cart fetchCart() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(authentication.getName());

        Cart cart = cartRepository.findByUser(user);
        if (cart == null) {
            throw new EntityUnavailableException("Cart not found");
        }

        return cart;
    }

    private boolean updateCartPrice(Cart cart) {
        if (!pricingService.refreshExpiredPrices(cart)) return false;

        pricingService.updateCartTotal(cart);
        return true;
    }

    private OrderResponse createOrderResponse(Order order, String message) {
        UserSummary user = new UserSummary(
                order.getUser().getUserId(),
                order.getUser().getFirstName() + " " + order.getUser().getLastName()
        );

        RestaurantSummary restaurant = new RestaurantSummary(
                order.getRestaurant().getRestaurantId(),
                order.getRestaurant().getRestaurantName(),
                order.getRestaurant().getCuisineType(),
                order.getRestaurant().getAvgRating()
        );

        AddressSummary address = new AddressSummary(
                order.getDeliveryAddress().getAddressId(),
                order.getDeliveryAddress().getBuildingNo(),
                order.getDeliveryAddress().getStreet(),
                order.getDeliveryAddress().getCity(),
                order.getDeliveryAddress().getPincode(),
                order.getDeliveryAddress().getLandmark(),
                user
        );

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

    private List<OrderItemSummary> createOrderItemsSummaries(Order order) {
        return order.getOrderItems().stream()
                .map(this::createOrderItemSummary)
                .toList();
    }

    private OrderItemSummary createOrderItemSummary(OrderItem orderItem) {

        List<AddonSummary> addons = createListOfAddonsSummary(orderItem.getAddons());

        return new OrderItemSummary(
                orderItem.getOrderItemId(),
                orderItem.getFood().getFoodId(),
                orderItem.getFood().getFoodName(),
                orderItem.getQuantity(),
                orderItem.getPriceAtOrder(),
                addons,
                orderItem.getItemTotal()
        );
    }

    private List<AddonSummary> createListOfAddonsSummary(List<Addon> addons) {
        return addons.stream()
                .map(this::createAddonSummary)
                .toList();
    }

    private AddonSummary  createAddonSummary(Addon addon) {
        return new AddonSummary(
                addon.getAddonId(),
                addon.getAddonName()
        );
    }

    public List<OrderResponse> getMyOrders() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(authentication.getName());

        return user.getOrders().stream()
                .map(order -> createOrderResponse(order, "Your order"))
                .toList();
    }

    public List<OrderResponse> getRestaurantOrders() {
        User user = userAuthorization.authorizeUser();

        List<Restaurant> restaurants;

        if (user.getRole() == UserRole.RESTAURANT_STAFF) {
            Restaurant restaurant = restaurantRepository.findById(user.getEmployedAt())
                    .orElseThrow(() -> new RestaurantNotFoundException(user.getEmployedAt()));
            restaurants = List.of(restaurant);
        } else {
            restaurants = user.getOwnedRestaurants();
        }

        return restaurants.stream()
                .flatMap(restaurant -> restaurant.getOrders().stream())
                .map(order -> createOrderResponse(order, "Your Restaurant Order"))
                .toList();
    }

    public OrderResponse getOrder(Long orderId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(authentication.getName());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order with id " + orderId + " not found"));

        if(authentication.getAuthorities().contains(new SimpleGrantedAuthority("CUSTOMER"))) {
            if(!order.getUser().equals(user)) {
                throw new AccessDeniedException("Access denied, this order doesn't belongs to you");
            }
        }

        if(authentication.getAuthorities().contains(new SimpleGrantedAuthority("RESTAURANT_OWNER"))) {
            if(!user.getOwnedRestaurants().contains(order.getRestaurant())) {
                throw new AccessDeniedException("Access denied, this order doesn't belongs to you and your restaurant too");
            }
        }

        return createOrderResponse(order, "");
    }

    public String cancelOrder(Long orderId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(authentication.getName());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order with id " + orderId + " not found"));

        if(!order.getUser().equals(user))
            throw new AccessDeniedException("Access denied, this order doesn't belongs to you");

        if(canUserCancelOrder(order)) {
            order.setOrderStatus(OrderStatus.CANCELLED);
            String message = "Order has  been cancelled";

            if(order.getOrderType() == OrderType.PRE_ORDER) {
                BigDecimal refund = order.getTotalPrice().multiply(BigDecimal.valueOf(0.75));
                order.setRefundAmount(refund);
                message = message + " and " + refund + " amount has been refunded to your wallet";
            }
            orderRepository.save(order);
            return message;
        }

        throw new EntityUnavailableException("The order is " + order.getOrderStatus() + " now, So it can't be cancelled");
    }

    private boolean canUserCancelOrder(Order order) {
        return order.getOrderStatus() == OrderStatus.CREATED ||
                order.getOrderStatus() == OrderStatus.CONFIRMED;
    }

    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(authentication.getName());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order with id " + orderId + " not found"));


        if(!user.getOwnedRestaurants().contains(order.getRestaurant()))
            throw new AccessDeniedException("Access denied, this order doesn't belongs to your Restaurant");

        OrderStatus currentStatus = order.getOrderStatus();
        OrderStatus newStatus = request.getOrderStatus();

        if (!currentStatus.canTransitionTo(newStatus))
            throw new InvalidOrderStateException(
                    "Cannot transition order from " + currentStatus + " to " + newStatus);

        order.setOrderStatus(newStatus);
        orderRepository.save(order);

        return createOrderResponse(order, "Order status has been updated");
    }

    public List<OrderResponse> getAdminRestaurantOrders(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant with id " + restaurantId + " not found"));

        return restaurant.getOrders().stream()
                .map(order -> createOrderResponse(order, "Order from restaurant - " + restaurant.getRestaurantName()))
                .toList();
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(order -> createOrderResponse(order, "All Orders"))
                .toList();
    }
}
