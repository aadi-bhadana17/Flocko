package com.kilgore.fooddeliveryapp.ordering.service;

import com.kilgore.fooddeliveryapp.catalog.dto.summary.AddonSummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.catalog.model.Addon;
import com.kilgore.fooddeliveryapp.catalog.model.Restaurant;
import com.kilgore.fooddeliveryapp.catalog.repository.RestaurantRepository;
import com.kilgore.fooddeliveryapp.common.util.UserAuthorization;
import com.kilgore.fooddeliveryapp.common.exceptions.*;
import com.kilgore.fooddeliveryapp.identity.dto.summary.AddressSummary;
import com.kilgore.fooddeliveryapp.identity.model.Address;
import com.kilgore.fooddeliveryapp.ordering.dto.summary.OrderItemSummary;
import com.kilgore.fooddeliveryapp.ordering.model.PaymentStatus;
import com.kilgore.fooddeliveryapp.identity.repository.AddressRepository;
import com.kilgore.fooddeliveryapp.ordering.dto.request.PlaceOrderRequest;
import com.kilgore.fooddeliveryapp.ordering.dto.request.UpdateOrderStatusRequest;
import com.kilgore.fooddeliveryapp.ordering.dto.response.OrderResponse;
import com.kilgore.fooddeliveryapp.identity.dto.summary.UserSummary;
import com.kilgore.fooddeliveryapp.identity.model.AccountStatus;
import com.kilgore.fooddeliveryapp.identity.model.User;
import com.kilgore.fooddeliveryapp.identity.model.UserRole;
import com.kilgore.fooddeliveryapp.identity.repository.UserRepository;
import com.kilgore.fooddeliveryapp.ordering.model.*;
import com.kilgore.fooddeliveryapp.ordering.repository.CartRepository;
import com.kilgore.fooddeliveryapp.ordering.repository.OrderItemRepository;
import com.kilgore.fooddeliveryapp.ordering.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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

        if (!Objects.equals(address.getUser().getUserId(), user.getUserId()))
            throw new AccessDeniedException("This address doesn't belongs to you, choose another address");

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

    protected OrderItem createOrderItem(CartItem cartItem, Order order) {

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
        User user = userAuthorization.authorizeUser();

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
        User user = userAuthorization.authorizeUser();

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
        } else if (user.getRole() == UserRole.RESTAURANT_OWNER) {
            restaurants = user.getOwnedRestaurants();
        } else {
            throw new AccessDeniedException("Access denied...");
        }

        return restaurants.stream()
                .flatMap(restaurant -> restaurant.getOrders().stream())
                .map(order -> createOrderResponse(order, "Your Restaurant Order"))
                .toList();
    }

    public OrderResponse getOrder(Long orderId) {
        User user = userAuthorization.authorizeUser();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order with id " + orderId + " not found"));

        boolean isCustomer = authentication.getAuthorities().contains(new SimpleGrantedAuthority("CUSTOMER"));
        boolean isOwner = authentication.getAuthorities().contains(new SimpleGrantedAuthority("RESTAURANT_OWNER"));
        boolean isStaff = authentication.getAuthorities().contains(new SimpleGrantedAuthority("RESTAURANT_STAFF"));
        boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN"));

        if (isCustomer) {
            if (!Objects.equals(order.getUser().getUserId(), user.getUserId())) {
                throw new AccessDeniedException("Access denied...");
            }
        } else if (isOwner) {
            if (!user.getOwnedRestaurants().contains(order.getRestaurant())) {
                throw new AccessDeniedException("Access denied...");
            }
        } else if (isStaff) {
            if (!user.getEmployedAt().equals(order.getRestaurant().getRestaurantId())) {
                throw new AccessDeniedException("Access denied...");
            }
        } else if (!isAdmin) {
            throw new AccessDeniedException("No valid authority");
        }

        return createOrderResponse(order, "");
    }

    public String cancelOrder(Long orderId) {
        User user = userAuthorization.authorizeUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order with id " + orderId + " not found"));

        if (!Objects.equals(order.getUser().getUserId(), user.getUserId()))
            throw new AccessDeniedException("Access denied, this order doesn't belongs to you");

        if(canUserCancelOrder(order)) {
            order.setOrderStatus(OrderStatus.CANCELLED);
            String message = "Order has  been cancelled";

            BigDecimal refund;
            refund = order.getTotalPrice();

            if(order.getOrderType() == OrderType.PRE_ORDER) {
                refund = order.getTotalPrice().multiply(BigDecimal.valueOf(0.75));
            }
            order.setRefundAmount(refund);
            user.setWalletBalance(user.getWalletBalance().add(refund));
            userRepository.save(user);
            message = message + " and " + refund + " amount has been refunded to your wallet";

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
        User user = userAuthorization.authorizeUser();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order with id " + orderId + " not found"));


        OrderStatus newStatus = getOrderStatus(request, user, order);

        order.setOrderStatus(newStatus);
        orderRepository.save(order);

        return createOrderResponse(order, "Order status has been updated");
    }

    private OrderStatus getOrderStatus(UpdateOrderStatusRequest request, User user, Order order) {
        if(!user.getOwnedRestaurants().contains(order.getRestaurant()))
            throw new AccessDeniedException("Access denied, this order doesn't belongs to your Restaurant");

        OrderStatus currentStatus = order.getOrderStatus();
        OrderStatus newStatus = request.getOrderStatus();
        if (newStatus == null) {
            throw new InvalidOrderStateException("Cannot transition order from " + currentStatus + " to null");
        }

        if (!currentStatus.canTransitionTo(newStatus))
            throw new InvalidOrderStateException(
                    "Cannot transition order from " + currentStatus + " to " + newStatus);
        return newStatus;
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
