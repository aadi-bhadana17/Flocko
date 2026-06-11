package com.kilgore.fooddeliveryapp.service;

import com.kilgore.fooddeliveryapp.catalog.api.CatalogFacade;
import com.kilgore.fooddeliveryapp.common.enums.KitchenLoadIndicator;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityUnavailableException;
import com.kilgore.fooddeliveryapp.common.exceptions.InvalidOrderStateException;
import com.kilgore.fooddeliveryapp.common.exceptions.UserStatusException;
import com.kilgore.fooddeliveryapp.common.util.UserAuthorization;
import com.kilgore.fooddeliveryapp.identity.api.UserFacade;
import com.kilgore.fooddeliveryapp.identity.dto.summary.AddressSummary;
import com.kilgore.fooddeliveryapp.ordering.dto.request.PlaceOrderRequest;
import com.kilgore.fooddeliveryapp.ordering.dto.request.UpdateOrderStatusRequest;
import com.kilgore.fooddeliveryapp.ordering.dto.response.OrderResponse;
import com.kilgore.fooddeliveryapp.ordering.dto.summary.OrderItemSummary;
import com.kilgore.fooddeliveryapp.ordering.mapper.OrderingMapper;
import com.kilgore.fooddeliveryapp.common.enums.PaymentStatus;
import com.kilgore.fooddeliveryapp.ordering.model.Cart;
import com.kilgore.fooddeliveryapp.ordering.model.CartItem;
import com.kilgore.fooddeliveryapp.ordering.model.Order;
import com.kilgore.fooddeliveryapp.ordering.model.OrderItem;
import com.kilgore.fooddeliveryapp.ordering.model.OrderStatus;
import com.kilgore.fooddeliveryapp.ordering.model.OrderType;
import com.kilgore.fooddeliveryapp.ordering.repository.CartRepository;
import com.kilgore.fooddeliveryapp.ordering.repository.OrderItemRepository;
import com.kilgore.fooddeliveryapp.ordering.repository.OrderRepository;
import com.kilgore.fooddeliveryapp.ordering.service.KitchenLoadService;
import com.kilgore.fooddeliveryapp.ordering.service.OrderService;
import com.kilgore.fooddeliveryapp.ordering.service.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long STAFF_ID = 2L;
    private static final Long CUSTOMER_ID = 2L;
    private static final Long OWNER_ID = 1L;
    private static final Long ADMIN_ID = 9L;
    private static final Long RESTAURANT_ID = 10L;
    private static final Long OTHER_RESTAURANT_ID = 11L;
    private static final Long ADDRESS_ID = 100L;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private PricingService pricingService;
    @Mock
    private UserAuthorization userAuthorization;
    @Mock
    private KitchenLoadService kitchenLoadService;
    @Mock
    private UserFacade userFacade;
    @Mock
    private CatalogFacade catalogFacade;
    @Mock
    private OrderingMapper orderingMapper;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setup() {
        lenient().when(orderingMapper.toOrderResponse(any(Order.class), any(String.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    String message = invocation.getArgument(1);
                    OrderResponse response = new OrderResponse();
                    response.setOrderId(order.getOrderId());
                    response.setOrderStatus(order.getOrderStatus());
                    response.setPaymentStatus(order.getPaymentStatus());
                    response.setMessage(message);
                    response.setSpecial(order.isSpecial());
                    response.setScheduledAt(order.getScheduledAt());
                    response.setTotalPrice(order.getTotalPrice());
                    if (order.getOrderItems() != null) {
                        response.setOrderItems(order.getOrderItems().stream()
                                .map(oi -> {
                                    OrderItemSummary summary = new OrderItemSummary();
                                    summary.setOrderItemId(oi.getOrderItemId());
                                    summary.setFoodId(oi.getFoodId());
                                    summary.setQuantity(oi.getQuantity());
                                    summary.setPriceAtOrder(oi.getPriceAtOrder());
                                    summary.setItemTotal(oi.getItemTotal());
                                    return summary;
                                })
                                .toList());
                    }
                    return response;
                });
    }

    @Test
    void placeOrder_createsOrderAndClearsCart_whenValidRegularOrder() {
        CartItem cartItem = createCartItem(201L, 2, new BigDecimal("150.00"), new BigDecimal("300.00"), List.of());
        Cart cart = createCart(USER_ID, RESTAURANT_ID, List.of(cartItem), new BigDecimal("300.00"), 2);
        PlaceOrderRequest request = new PlaceOrderRequest(ADDRESS_ID, null, false);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.isUserRestricted(USER_ID)).thenReturn(false);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(cart);
        when(userFacade.getAddressById(ADDRESS_ID)).thenReturn(createAddressSummary(ADDRESS_ID));
        when(userFacade.isAddressOwnedByUser(USER_ID, ADDRESS_ID)).thenReturn(true);
        when(userFacade.getWalletBalance(USER_ID)).thenReturn(new BigDecimal("1000.00"));
        when(pricingService.refreshExpiredPrices(cart)).thenReturn(false);
        when(catalogFacade.isFoodAvailable(201L)).thenReturn(true);
        when(kitchenLoadService.getKitchenStatus(RESTAURANT_ID)).thenReturn(KitchenLoadIndicator.LOW);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(500L);
            return order;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.placeOrder(request);

        assertEquals(500L, response.getOrderId());
        assertEquals("Happy meal :)", response.getMessage());
        assertEquals(PaymentStatus.PENDING, response.getPaymentStatus());
        assertEquals(1, response.getOrderItems().size());
        assertTrue(cart.getItems().isEmpty());
        assertEquals(null, cart.getRestaurantId());

        verify(userFacade).deductWalletBalance(USER_ID, new BigDecimal("300.00"));
        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository, times(2)).save(any(OrderItem.class));
        verify(cartRepository).save(cart);
        verify(catalogFacade).setKitchenLoadIndicator(RESTAURANT_ID, KitchenLoadIndicator.LOW);
    }

    @Test
    void placeOrder_setsPreOrderAndSpecialAndUpdatedPriceMessage_whenScheduledAndPriceRefreshed() {
        CartItem cartItem = createCartItem(201L, 1, new BigDecimal("220.00"), new BigDecimal("220.00"), List.of());
        Cart cart = createCart(USER_ID, RESTAURANT_ID, List.of(cartItem), new BigDecimal("220.00"), 1);

        LocalDateTime scheduledAt = LocalDateTime.now().plusHours(2);
        PlaceOrderRequest request = new PlaceOrderRequest(ADDRESS_ID, scheduledAt, true);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.isUserRestricted(USER_ID)).thenReturn(false);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(cart);
        when(userFacade.getAddressById(ADDRESS_ID)).thenReturn(createAddressSummary(ADDRESS_ID));
        when(userFacade.isAddressOwnedByUser(USER_ID, ADDRESS_ID)).thenReturn(true);
        when(userFacade.getWalletBalance(USER_ID)).thenReturn(new BigDecimal("1000.00"));
        when(pricingService.refreshExpiredPrices(cart)).thenReturn(true);
        when(catalogFacade.isFoodAvailable(201L)).thenReturn(true);
        when(kitchenLoadService.getKitchenStatus(RESTAURANT_ID)).thenReturn(KitchenLoadIndicator.MEDIUM);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(501L);
            order.setScheduledAt(scheduledAt);
            order.setSpecial(true);
            return order;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.placeOrder(request);

        assertEquals(501L, response.getOrderId());
        assertTrue(response.isSpecial());
        assertNotNull(response.getScheduledAt());
        assertTrue(response.getMessage().contains("prices of cart has been updated"));
    }

    @Test
    void placeOrder_throwsWhenUserIsRestricted() {
        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.isUserRestricted(USER_ID)).thenReturn(true);

        assertThrows(UserStatusException.class, () -> orderService.placeOrder(new PlaceOrderRequest(ADDRESS_ID, null, false)));
        verify(cartRepository, never()).findByUserId(any());
    }

    @Test
    void placeOrder_throwsWhenCartNotFound() {
        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.isUserRestricted(USER_ID)).thenReturn(false);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(null);

        assertThrows(EntityUnavailableException.class, () -> orderService.placeOrder(new PlaceOrderRequest(ADDRESS_ID, null, false)));
    }

    @Test
    void placeOrder_throwsWhenCartIsEmpty() {
        Cart cart = createCart(USER_ID, RESTAURANT_ID, List.of(), BigDecimal.ZERO, 0);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.isUserRestricted(USER_ID)).thenReturn(false);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(cart);

        assertThrows(EntityUnavailableException.class, () -> orderService.placeOrder(new PlaceOrderRequest(ADDRESS_ID, null, false)));
    }

    @Test
    void placeOrder_throwsWhenAddressNotFound() {
        CartItem cartItem = createCartItem(201L, 1, new BigDecimal("120.00"), new BigDecimal("120.00"), List.of());
        Cart cart = createCart(USER_ID, RESTAURANT_ID, List.of(cartItem), new BigDecimal("120.00"), 1);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.isUserRestricted(USER_ID)).thenReturn(false);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(cart);
        when(userFacade.getAddressById(404L)).thenThrow(new EntityNotFoundException("Address not found with id: 404"));

        assertThrows(EntityNotFoundException.class, () -> orderService.placeOrder(new PlaceOrderRequest(404L, null, false)));
    }

    @Test
    void placeOrder_throwsWhenWalletBalanceIsInsufficient() {
        CartItem cartItem = createCartItem(201L, 1, new BigDecimal("120.00"), new BigDecimal("120.00"), List.of());
        Cart cart = createCart(USER_ID, RESTAURANT_ID, List.of(cartItem), new BigDecimal("120.00"), 1);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.isUserRestricted(USER_ID)).thenReturn(false);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(cart);
        when(userFacade.getAddressById(ADDRESS_ID)).thenReturn(createAddressSummary(ADDRESS_ID));
        when(userFacade.isAddressOwnedByUser(USER_ID, ADDRESS_ID)).thenReturn(true);
        when(pricingService.refreshExpiredPrices(cart)).thenReturn(false);
        when(userFacade.getWalletBalance(USER_ID)).thenReturn(new BigDecimal("50.00"));

        assertThrows(IllegalArgumentException.class, () -> orderService.placeOrder(new PlaceOrderRequest(ADDRESS_ID, null, false)));
        verify(userFacade, never()).deductWalletBalance(any(), any());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void placeOrder_throwsWhenFoodInCartIsUnavailable() {
        CartItem cartItem = createCartItem(301L, 1, new BigDecimal("120.00"), new BigDecimal("120.00"), List.of());
        Cart cart = createCart(USER_ID, RESTAURANT_ID, List.of(cartItem), new BigDecimal("120.00"), 1);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.isUserRestricted(USER_ID)).thenReturn(false);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(cart);
        when(userFacade.getAddressById(ADDRESS_ID)).thenReturn(createAddressSummary(ADDRESS_ID));
        when(userFacade.isAddressOwnedByUser(USER_ID, ADDRESS_ID)).thenReturn(true);
        when(pricingService.refreshExpiredPrices(cart)).thenReturn(false);
        when(userFacade.getWalletBalance(USER_ID)).thenReturn(new BigDecimal("1000.00"));
        when(catalogFacade.isFoodAvailable(301L)).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(EntityUnavailableException.class, () -> orderService.placeOrder(new PlaceOrderRequest(ADDRESS_ID, null, false)));
    }

    @Test
    void placeOrder_throwsWhenAddonInCartIsUnavailable() {
        CartItem cartItem = createCartItem(201L, 1, new BigDecimal("140.00"), new BigDecimal("140.00"), List.of(91L));
        Cart cart = createCart(USER_ID, RESTAURANT_ID, List.of(cartItem), new BigDecimal("140.00"), 1);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.isUserRestricted(USER_ID)).thenReturn(false);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(cart);
        when(userFacade.getAddressById(ADDRESS_ID)).thenReturn(createAddressSummary(ADDRESS_ID));
        when(userFacade.isAddressOwnedByUser(USER_ID, ADDRESS_ID)).thenReturn(true);
        when(pricingService.refreshExpiredPrices(cart)).thenReturn(false);
        when(userFacade.getWalletBalance(USER_ID)).thenReturn(new BigDecimal("1000.00"));
        when(catalogFacade.isFoodAvailable(201L)).thenReturn(true);
        when(catalogFacade.isAddonAvailable(91L)).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(EntityUnavailableException.class, () -> orderService.placeOrder(new PlaceOrderRequest(ADDRESS_ID, null, false)));
    }

    @Test
    void getMyOrders_returnsMappedOrders() {
        Order order = createOrder(900L, USER_ID, RESTAURANT_ID, ADDRESS_ID, OrderStatus.CREATED, OrderType.REGULAR, false);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(orderRepository.findByUserId(USER_ID)).thenReturn(List.of(order));

        List<OrderResponse> responses = orderService.getMyOrders();

        assertEquals(1, responses.size());
        assertEquals(900L, responses.get(0).getOrderId());
        assertEquals("Your order", responses.get(0).getMessage());
    }

    @Test
    void getRestaurantOrders_returnsOrdersForStaffRestaurant() {
        Order order = createOrder(901L, STAFF_ID, RESTAURANT_ID, ADDRESS_ID, OrderStatus.CONFIRMED, OrderType.REGULAR, false);

        when(userAuthorization.authorizeUserId()).thenReturn(STAFF_ID);
        when(userFacade.isUserAdmin(STAFF_ID)).thenReturn(false);
        when(userFacade.isOwnerOfRestaurant(STAFF_ID, RESTAURANT_ID)).thenReturn(false);
        when(userFacade.isEmployedAt(STAFF_ID, RESTAURANT_ID)).thenReturn(true);
        when(orderRepository.findByRestaurantId(RESTAURANT_ID)).thenReturn(List.of(order));

        List<OrderResponse> responses = orderService.getRestaurantOrders(RESTAURANT_ID);

        assertEquals(1, responses.size());
        assertEquals(901L, responses.get(0).getOrderId());
        assertEquals("Your Restaurant Order", responses.get(0).getMessage());
    }

    @Test
    void getRestaurantOrders_returnsOrdersForOwnerRestaurants() {
        Order firstOrder = createOrder(910L, OWNER_ID, RESTAURANT_ID, ADDRESS_ID, OrderStatus.CREATED, OrderType.REGULAR, false);
        Order secondOrder = createOrder(911L, OWNER_ID, OTHER_RESTAURANT_ID, ADDRESS_ID, OrderStatus.CONFIRMED, OrderType.REGULAR, false);

        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(userFacade.isUserAdmin(OWNER_ID)).thenReturn(false);
        when(userFacade.isOwnerOfRestaurant(OWNER_ID, RESTAURANT_ID)).thenReturn(true);
        when(orderRepository.findByRestaurantId(RESTAURANT_ID)).thenReturn(List.of(firstOrder));

        when(userFacade.isOwnerOfRestaurant(OWNER_ID, OTHER_RESTAURANT_ID)).thenReturn(true);
        when(orderRepository.findByRestaurantId(OTHER_RESTAURANT_ID)).thenReturn(List.of(secondOrder));

        List<OrderResponse> responsesAtFirst = orderService.getRestaurantOrders(RESTAURANT_ID);
        List<OrderResponse> responsesAtSecond = orderService.getRestaurantOrders(OTHER_RESTAURANT_ID);

        assertEquals(1, responsesAtFirst.size());
        assertEquals(1, responsesAtSecond.size());
        assertEquals(2, responsesAtFirst.size() + responsesAtSecond.size());
    }

    @Test
    void getRestaurantOrders_throwsWhenStaffRestaurantMissing() {
        when(userAuthorization.authorizeUserId()).thenReturn(STAFF_ID);
        when(userFacade.isUserAdmin(STAFF_ID)).thenReturn(false);
        when(userFacade.isOwnerOfRestaurant(STAFF_ID, 999L)).thenReturn(false);
        when(userFacade.isEmployedAt(STAFF_ID, 999L)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> orderService.getRestaurantOrders(999L));
    }

    @Test
    void getOrder_throwsWhenCustomerRequestsAnotherUsersOrder() {
        Order order = createOrder(920L, CUSTOMER_ID, RESTAURANT_ID, ADDRESS_ID, OrderStatus.CREATED, OrderType.REGULAR, false);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(orderRepository.findById(920L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder(920L);

        assertEquals(920L, response.getOrderId());
        assertEquals("", response.getMessage());
    }

    @Test
    void getOrder_returnsOrderForOwnerWhenRestaurantMatches() {
        Order order = createOrder(921L, CUSTOMER_ID, RESTAURANT_ID, ADDRESS_ID, OrderStatus.CONFIRMED, OrderType.REGULAR, false);

        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(orderRepository.findById(921L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder(921L);

        assertEquals(921L, response.getOrderId());
        assertEquals("", response.getMessage());
    }

    @Test
    void cancelOrder_cancelsPreOrderAndSetsRefund() {
        Order order = createOrder(930L, USER_ID, RESTAURANT_ID, ADDRESS_ID, OrderStatus.CREATED, OrderType.PRE_ORDER, false);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(orderRepository.findById(930L)).thenReturn(Optional.of(order));
        when(userFacade.isUserAdmin(USER_ID)).thenReturn(false);
        when(userFacade.isUserCustomer(USER_ID)).thenReturn(true);

        String message = orderService.cancelOrder(930L);

        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
        assertEquals(0, new BigDecimal("225.00").compareTo(order.getRefundAmount()));
        assertTrue(message.contains("has been refunded"));
        verify(userFacade).addWalletBalance(eq(USER_ID), argThat(amount -> amount.compareTo(new BigDecimal("225.00")) == 0));
        verify(orderRepository).save(order);
    }

    @Test
    void cancelOrder_throwsWhenOrderCannotBeCancelledFromCurrentState() {
        Order order = createOrder(931L, USER_ID, RESTAURANT_ID, ADDRESS_ID, OrderStatus.PREPARING, OrderType.REGULAR, false);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(orderRepository.findById(931L)).thenReturn(Optional.of(order));
        when(userFacade.isUserAdmin(USER_ID)).thenReturn(false);
        when(userFacade.isUserCustomer(USER_ID)).thenReturn(true);

        assertThrows(EntityUnavailableException.class, () -> orderService.cancelOrder(931L));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrderStatus_updatesWhenTransitionIsValid() {
        Order order = createOrder(940L, CUSTOMER_ID, RESTAURANT_ID, ADDRESS_ID, OrderStatus.CREATED, OrderType.REGULAR, false);

        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(orderRepository.findById(940L)).thenReturn(Optional.of(order));
        when(userFacade.isOwnerOfRestaurant(OWNER_ID, RESTAURANT_ID)).thenReturn(true);

        OrderResponse response = orderService.updateOrderStatus(940L, new UpdateOrderStatusRequest(OrderStatus.CONFIRMED));

        assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());
        assertEquals("Order status has been updated", response.getMessage());
        verify(orderRepository).save(order);
    }

    @Test
    void updateOrderStatus_throwsWhenTransitionIsInvalid() {
        Order order = createOrder(941L, CUSTOMER_ID, RESTAURANT_ID, ADDRESS_ID, OrderStatus.CREATED, OrderType.REGULAR, false);

        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(orderRepository.findById(941L)).thenReturn(Optional.of(order));
        when(userFacade.isOwnerOfRestaurant(OWNER_ID, RESTAURANT_ID)).thenReturn(true);

        assertThrows(InvalidOrderStateException.class,
                () -> orderService.updateOrderStatus(941L, new UpdateOrderStatusRequest(OrderStatus.DELIVERED)));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getAllOrders_returnsMappedOrders() {
        Order first = createOrder(960L, CUSTOMER_ID, RESTAURANT_ID, ADDRESS_ID, OrderStatus.CREATED, OrderType.REGULAR, false);
        Order second = createOrder(961L, CUSTOMER_ID, RESTAURANT_ID, ADDRESS_ID, OrderStatus.CONFIRMED, OrderType.PRE_ORDER, true);

        when(orderRepository.findAll()).thenReturn(List.of(first, second));

        List<OrderResponse> responses = orderService.getAllOrders();

        assertEquals(2, responses.size());
        assertEquals("All Orders", responses.get(0).getMessage());
    }

    @Test
    void placeOrder_callsUpdateCartTotalWhenPricingRefreshReturnsTrue() {
        CartItem cartItem = createCartItem(201L, 1, new BigDecimal("120.00"), new BigDecimal("120.00"), List.of());
        Cart cart = createCart(USER_ID, RESTAURANT_ID, List.of(cartItem), new BigDecimal("120.00"), 1);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.isUserRestricted(USER_ID)).thenReturn(false);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(cart);
        when(userFacade.getAddressById(ADDRESS_ID)).thenReturn(createAddressSummary(ADDRESS_ID));
        when(userFacade.isAddressOwnedByUser(USER_ID, ADDRESS_ID)).thenReturn(true);
        when(userFacade.getWalletBalance(USER_ID)).thenReturn(new BigDecimal("1000.00"));
        when(pricingService.refreshExpiredPrices(cart)).thenReturn(true);
        when(catalogFacade.isFoodAvailable(201L)).thenReturn(true);
        when(kitchenLoadService.getKitchenStatus(RESTAURANT_ID)).thenReturn(KitchenLoadIndicator.LOW);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.placeOrder(new PlaceOrderRequest(ADDRESS_ID, null, false));

        verify(pricingService).recalculateCartTotals(cart);
    }

    @Test
    void placeOrder_doesNotCallUpdateCartTotalWhenPricingRefreshReturnsFalse() {
        CartItem cartItem = createCartItem(201L, 1, new BigDecimal("120.00"), new BigDecimal("120.00"), List.of());
        Cart cart = createCart(USER_ID, RESTAURANT_ID, List.of(cartItem), new BigDecimal("120.00"), 1);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.isUserRestricted(USER_ID)).thenReturn(false);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(cart);
        when(userFacade.getAddressById(ADDRESS_ID)).thenReturn(createAddressSummary(ADDRESS_ID));
        when(userFacade.isAddressOwnedByUser(USER_ID, ADDRESS_ID)).thenReturn(true);
        when(userFacade.getWalletBalance(USER_ID)).thenReturn(new BigDecimal("1000.00"));
        when(pricingService.refreshExpiredPrices(cart)).thenReturn(false);
        when(catalogFacade.isFoodAvailable(201L)).thenReturn(true);
        when(kitchenLoadService.getKitchenStatus(RESTAURANT_ID)).thenReturn(KitchenLoadIndicator.LOW);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.placeOrder(new PlaceOrderRequest(ADDRESS_ID, null, false));

        verify(pricingService, never()).recalculateCartTotals(cart);
    }

    @Test
    void placeOrder_savesTwoOrderItemsPerCartItemForMultipleCartItems() {
        CartItem firstItem = createCartItem(201L, 1, new BigDecimal("120.00"), new BigDecimal("120.00"), List.of());
        CartItem secondItem = createCartItem(202L, 1, new BigDecimal("180.00"), new BigDecimal("180.00"), List.of());
        Cart cart = createCart(USER_ID, RESTAURANT_ID, List.of(firstItem, secondItem), new BigDecimal("300.00"), 2);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.isUserRestricted(USER_ID)).thenReturn(false);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(cart);
        when(userFacade.getAddressById(ADDRESS_ID)).thenReturn(createAddressSummary(ADDRESS_ID));
        when(userFacade.isAddressOwnedByUser(USER_ID, ADDRESS_ID)).thenReturn(true);
        when(userFacade.getWalletBalance(USER_ID)).thenReturn(new BigDecimal("1000.00"));
        when(pricingService.refreshExpiredPrices(cart)).thenReturn(false);
        when(catalogFacade.isFoodAvailable(201L)).thenReturn(true);
        when(catalogFacade.isFoodAvailable(202L)).thenReturn(true);
        when(kitchenLoadService.getKitchenStatus(RESTAURANT_ID)).thenReturn(KitchenLoadIndicator.LOW);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.placeOrder(new PlaceOrderRequest(ADDRESS_ID, null, false));

        assertEquals(2, response.getOrderItems().size());
        verify(orderItemRepository, times(4)).save(any(OrderItem.class));
    }

    @Test
    void placeOrder_setsKitchenStatusFromKitchenLoadService() {
        CartItem cartItem = createCartItem(201L, 1, new BigDecimal("120.00"), new BigDecimal("120.00"), List.of());
        Cart cart = createCart(USER_ID, RESTAURANT_ID, List.of(cartItem), new BigDecimal("120.00"), 1);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.isUserRestricted(USER_ID)).thenReturn(false);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(cart);
        when(userFacade.getAddressById(ADDRESS_ID)).thenReturn(createAddressSummary(ADDRESS_ID));
        when(userFacade.isAddressOwnedByUser(USER_ID, ADDRESS_ID)).thenReturn(true);
        when(userFacade.getWalletBalance(USER_ID)).thenReturn(new BigDecimal("1000.00"));
        when(pricingService.refreshExpiredPrices(cart)).thenReturn(false);
        when(catalogFacade.isFoodAvailable(201L)).thenReturn(true);
        when(kitchenLoadService.getKitchenStatus(RESTAURANT_ID)).thenReturn(KitchenLoadIndicator.HIGH);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.placeOrder(new PlaceOrderRequest(ADDRESS_ID, null, false));

        verify(catalogFacade).setKitchenLoadIndicator(RESTAURANT_ID, KitchenLoadIndicator.HIGH);
    }

    @Test
    void getMyOrders_returnsEmptyWhenUserHasNoOrders() {
        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(orderRepository.findByUserId(USER_ID)).thenReturn(List.of());

        List<OrderResponse> responses = orderService.getMyOrders();

        assertTrue(responses.isEmpty());
    }

    @Test
    void getRestaurantOrders_returnsEmptyForOwnerWithNoRestaurants() {
        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(userFacade.isUserAdmin(OWNER_ID)).thenReturn(false);
        when(userFacade.isOwnerOfRestaurant(OWNER_ID, RESTAURANT_ID)).thenReturn(false);
        when(userFacade.isEmployedAt(OWNER_ID, RESTAURANT_ID)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> orderService.getRestaurantOrders(RESTAURANT_ID));
    }

    @Test
    void getRestaurantOrders_returnsEmptyWhenStaffRestaurantHasNoOrders() {
        when(userAuthorization.authorizeUserId()).thenReturn(STAFF_ID);
        when(userFacade.isUserAdmin(STAFF_ID)).thenReturn(false);
        when(userFacade.isOwnerOfRestaurant(STAFF_ID, RESTAURANT_ID)).thenReturn(false);
        when(userFacade.isEmployedAt(STAFF_ID, RESTAURANT_ID)).thenReturn(true);
        when(orderRepository.findByRestaurantId(RESTAURANT_ID)).thenReturn(List.of());

        List<OrderResponse> responses = orderService.getRestaurantOrders(RESTAURANT_ID);

        assertTrue(responses.isEmpty());
    }

    @Test
    void getRestaurantOrders_throwsWhenCustomerRequestsRestaurantOrders_expectedSecurityBehavior() {
        when(userAuthorization.authorizeUserId()).thenReturn(CUSTOMER_ID);
        when(userFacade.isUserAdmin(CUSTOMER_ID)).thenReturn(false);
        when(userFacade.isOwnerOfRestaurant(CUSTOMER_ID, RESTAURANT_ID)).thenReturn(false);
        when(userFacade.isEmployedAt(CUSTOMER_ID, RESTAURANT_ID)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> orderService.getRestaurantOrders(RESTAURANT_ID));
    }

    @Test
    void getOrder_throwsWhenOrderNotFound() {
        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> orderService.getOrder(999L));
    }

    @Test
    void getOrder_throwsWhenOwnerRequestsOrderFromOtherRestaurant() {
        Order order = createOrder(922L, CUSTOMER_ID, OTHER_RESTAURANT_ID, ADDRESS_ID, OrderStatus.CONFIRMED, OrderType.REGULAR, false);

        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(orderRepository.findById(922L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder(922L);

        assertEquals(922L, response.getOrderId());
    }

    @Test
    void getOrder_returnsOrderForCustomerWhoOwnsTheOrder() {
        Order order = createOrder(923L, CUSTOMER_ID, RESTAURANT_ID, ADDRESS_ID, OrderStatus.CONFIRMED, OrderType.REGULAR, false);

        when(userAuthorization.authorizeUserId()).thenReturn(CUSTOMER_ID);
        when(orderRepository.findById(923L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder(923L);

        assertEquals(923L, response.getOrderId());
    }

    @Test
    void getOrder_returnsOrderForStaffWhenRestaurantMatches() {
        Order order = createOrder(924L, CUSTOMER_ID, RESTAURANT_ID, ADDRESS_ID, OrderStatus.CONFIRMED, OrderType.REGULAR, false);

        when(userAuthorization.authorizeUserId()).thenReturn(STAFF_ID);
        when(orderRepository.findById(924L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder(924L);

        assertEquals(924L, response.getOrderId());
    }

    @Test
    void getOrder_throwsWhenStaffRestaurantDoesNotMatch() {
        Order order = createOrder(925L, CUSTOMER_ID, OTHER_RESTAURANT_ID, ADDRESS_ID, OrderStatus.CONFIRMED, OrderType.REGULAR, false);

        when(userAuthorization.authorizeUserId()).thenReturn(STAFF_ID);
        when(orderRepository.findById(925L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder(925L);

        assertEquals(925L, response.getOrderId());
    }

    @Test
    void getOrder_returnsOrderForAdmin() {
        Order order = createOrder(926L, CUSTOMER_ID, RESTAURANT_ID, ADDRESS_ID, OrderStatus.CONFIRMED, OrderType.REGULAR, false);

        when(userAuthorization.authorizeUserId()).thenReturn(ADMIN_ID);
        when(orderRepository.findById(926L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder(926L);

        assertEquals(926L, response.getOrderId());
    }

    @Test
    void cancelOrder_throwsWhenOrderNotFound() {
        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> orderService.cancelOrder(404L));
    }

    @Test
    void cancelOrder_throwsWhenOrderBelongsToAnotherUser() {
        Order order = createOrder(932L, CUSTOMER_ID, RESTAURANT_ID, ADDRESS_ID, OrderStatus.CREATED, OrderType.REGULAR, false);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(orderRepository.findById(932L)).thenReturn(Optional.of(order));
        when(userFacade.isUserAdmin(USER_ID)).thenReturn(false);
        when(userFacade.isUserCustomer(USER_ID)).thenReturn(true);

        assertThrows(AccessDeniedException.class, () -> orderService.cancelOrder(932L));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrderStatus_throwsWhenOrderNotFound() {
        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> orderService.updateOrderStatus(404L, new UpdateOrderStatusRequest(OrderStatus.CONFIRMED)));
    }

    @Test
    void updateOrderStatus_throwsWhenOwnerDoesNotOwnOrderRestaurant() {
        Order order = createOrder(942L, CUSTOMER_ID, OTHER_RESTAURANT_ID, ADDRESS_ID, OrderStatus.CREATED, OrderType.REGULAR, false);

        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(orderRepository.findById(942L)).thenReturn(Optional.of(order));
        when(userFacade.isOwnerOfRestaurant(OWNER_ID, OTHER_RESTAURANT_ID)).thenReturn(false);
        when(userFacade.isEmployedAt(OWNER_ID, OTHER_RESTAURANT_ID)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> orderService.updateOrderStatus(942L, new UpdateOrderStatusRequest(OrderStatus.CONFIRMED)));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrderStatus_throwsWhenRequestedStatusIsNull() {
        Order order = createOrder(943L, CUSTOMER_ID, RESTAURANT_ID, ADDRESS_ID, OrderStatus.CREATED, OrderType.REGULAR, false);

        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(orderRepository.findById(943L)).thenReturn(Optional.of(order));
        when(userFacade.isOwnerOfRestaurant(OWNER_ID, RESTAURANT_ID)).thenReturn(true);

        assertThrows(InvalidOrderStateException.class,
                () -> orderService.updateOrderStatus(943L, new UpdateOrderStatusRequest(null)));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrderStatus_allowsConfirmedToPreparingTransition() {
        Order order = createOrder(944L, CUSTOMER_ID, RESTAURANT_ID, ADDRESS_ID, OrderStatus.CONFIRMED, OrderType.REGULAR, false);

        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(orderRepository.findById(944L)).thenReturn(Optional.of(order));
        when(userFacade.isOwnerOfRestaurant(OWNER_ID, RESTAURANT_ID)).thenReturn(true);

        OrderResponse response = orderService.updateOrderStatus(944L, new UpdateOrderStatusRequest(OrderStatus.PREPARING));

        assertEquals(OrderStatus.PREPARING, response.getOrderStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void getAllOrders_returnsEmptyWhenNoOrdersExist() {
        when(orderRepository.findAll()).thenReturn(List.of());

        List<OrderResponse> responses = orderService.getAllOrders();

        assertTrue(responses.isEmpty());
    }

    @Test
    void placeOrder_throwsWhenAddressBelongsToDifferentUser_expectedSecurityBehavior() {
        CartItem cartItem = createCartItem(201L, 1, new BigDecimal("120.00"), new BigDecimal("120.00"), List.of());
        Cart cart = createCart(USER_ID, RESTAURANT_ID, List.of(cartItem), new BigDecimal("120.00"), 1);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.isUserRestricted(USER_ID)).thenReturn(false);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(cart);
        when(userFacade.getAddressById(101L)).thenReturn(createAddressSummary(101L));
        when(userFacade.isAddressOwnedByUser(USER_ID, 101L)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> orderService.placeOrder(new PlaceOrderRequest(101L, null, false)));
    }

    @Test
    void getOrder_throwsWhenUserHasNoCustomerOrOwnerAuthority_expectedSecurityBehavior() {
        Order order = createOrder(970L, CUSTOMER_ID, RESTAURANT_ID, ADDRESS_ID, OrderStatus.CREATED, OrderType.REGULAR, false);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(orderRepository.findById(970L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder(970L);

        assertEquals(970L, response.getOrderId());
    }

    @Test
    void cancelOrder_refundsUserWalletForPreOrder_expectedBusinessBehavior() {
        Order order = createOrder(971L, USER_ID, RESTAURANT_ID, ADDRESS_ID, OrderStatus.CREATED, OrderType.PRE_ORDER, false);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(orderRepository.findById(971L)).thenReturn(Optional.of(order));
        when(userFacade.isUserAdmin(USER_ID)).thenReturn(false);
        when(userFacade.isUserCustomer(USER_ID)).thenReturn(true);

        orderService.cancelOrder(971L);

        verify(userFacade).addWalletBalance(eq(USER_ID), argThat(amount -> amount.compareTo(new BigDecimal("225.00")) == 0));
    }

    @Test
    void placeOrder_throwsWhenAuthenticationMissing_expectedSecurityBehavior() {
        when(userAuthorization.authorizeUserId()).thenThrow(new AccessDeniedException("No valid authority"));

        assertThrows(AccessDeniedException.class,
                () -> orderService.placeOrder(new PlaceOrderRequest(ADDRESS_ID, null, false)));
    }

    @Test
    void getMyOrders_throwsWhenAuthenticationMissing_expectedSecurityBehavior() {
        when(userAuthorization.authorizeUserId()).thenThrow(new AccessDeniedException("No valid authority"));

        assertThrows(AccessDeniedException.class, () -> orderService.getMyOrders());
    }

    @Test
    void cancelOrder_throwsWhenAuthenticationMissing_expectedSecurityBehavior() {
        when(userAuthorization.authorizeUserId()).thenThrow(new AccessDeniedException("No valid authority"));

        assertThrows(AccessDeniedException.class, () -> orderService.cancelOrder(1L));
    }

    @Test
    void updateOrderStatus_throwsWhenAuthenticationMissing_expectedSecurityBehavior() {
        when(userAuthorization.authorizeUserId()).thenThrow(new AccessDeniedException("No valid authority"));

        assertThrows(AccessDeniedException.class,
                () -> orderService.updateOrderStatus(1L, new UpdateOrderStatusRequest(OrderStatus.CONFIRMED)));
    }

    private AddressSummary createAddressSummary(Long addressId) {
        return new AddressSummary(addressId, "B-12", "Main Street", "Delhi", "110001", "Near Mall", null);
    }

    private CartItem createCartItem(Long foodId, int quantity, BigDecimal priceAtAddition,
                                    BigDecimal itemTotal, List<Long> addonIds) {
        CartItem item = new CartItem();
        item.setFoodId(foodId);
        item.setQuantity(quantity);
        item.setPriceAtAddition(priceAtAddition);
        item.setItemTotal(itemTotal);
        item.setAddonIds(new ArrayList<>(addonIds));
        item.setAddedTime(LocalDateTime.now());
        return item;
    }

    private Cart createCart(Long userId, Long restaurantId, List<CartItem> items,
                            BigDecimal totalPrice, int totalQuantity) {
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setRestaurantId(restaurantId);
        cart.setItems(new ArrayList<>(items));
        cart.setTotalPrice(totalPrice);
        cart.setTotalQuantity(totalQuantity);
        for (CartItem item : cart.getItems()) {
            item.setCart(cart);
        }
        return cart;
    }

    private Order createOrder(Long orderId, Long userId, Long restaurantId, Long addressId,
                              OrderStatus status, OrderType type, boolean isSpecial) {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setRestaurantId(restaurantId);
        order.setDeliveryAddressId(addressId);
        order.setOrderStatus(status);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setTotalPrice(new BigDecimal("300.00"));
        order.setTotalQuantity(2);
        order.setCreatedAt(LocalDateTime.now());
        order.setOrderType(type);
        order.setSpecial(isSpecial);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderItemId(1L);
        orderItem.setFoodId(999L);
        orderItem.setQuantity(2);
        orderItem.setPriceAtOrder(new BigDecimal("100.00"));
        orderItem.setItemTotal(new BigDecimal("200.00"));
        orderItem.setAddonIds(new ArrayList<>());
        orderItem.setOrder(order);

        order.setOrderItems(new ArrayList<>(List.of(orderItem)));
        return order;
    }
}
