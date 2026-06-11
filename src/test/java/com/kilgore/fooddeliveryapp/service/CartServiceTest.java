package com.kilgore.fooddeliveryapp.service;

import com.kilgore.fooddeliveryapp.catalog.api.CatalogFacade;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.AddonSummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.FoodSummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.catalog.model.CuisineType;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityMisMatchAssociationException;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityUnavailableException;
import com.kilgore.fooddeliveryapp.common.util.UserAuthorization;
import com.kilgore.fooddeliveryapp.identity.api.UserFacade;
import com.kilgore.fooddeliveryapp.identity.dto.summary.UserSummary;
import com.kilgore.fooddeliveryapp.ordering.dto.request.AddToCartRequest;
import com.kilgore.fooddeliveryapp.ordering.dto.request.UpdateCartItemRequest;
import com.kilgore.fooddeliveryapp.ordering.dto.response.CartResponse;
import com.kilgore.fooddeliveryapp.ordering.mapper.OrderingMapper;
import com.kilgore.fooddeliveryapp.ordering.model.Cart;
import com.kilgore.fooddeliveryapp.ordering.model.CartItem;
import com.kilgore.fooddeliveryapp.ordering.model.SharedCartMember;
import com.kilgore.fooddeliveryapp.ordering.repository.CartItemRepository;
import com.kilgore.fooddeliveryapp.ordering.repository.CartRepository;
import com.kilgore.fooddeliveryapp.ordering.repository.SharedCartMemberRepository;
import com.kilgore.fooddeliveryapp.ordering.service.CartService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long RESTAURANT_ID = 10L;
    private static final Long OTHER_RESTAURANT_ID = 11L;
    private static final Long FOOD_ID = 100L;
    private static final Long ADDON_ID = 9L;

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private PricingService pricingService;
    @Mock
    private UserAuthorization userAuthorization;
    @Mock
    private SharedCartMemberRepository sharedCartMemberRepository;
    @Mock
    private CatalogFacade catalogFacade;
    @Mock
    private UserFacade userFacade;
    @Mock
    private OrderingMapper orderingMapper;

    @InjectMocks
    private CartService cartService;

    private RestaurantSummary restaurantSummary;

    @BeforeEach
    void setup() {
        restaurantSummary = new RestaurantSummary(RESTAURANT_ID, "Tasty Hub", CuisineType.INDIAN, new BigDecimal("4.2"));

        lenient().when(orderingMapper.toCartResponse(any(Cart.class), any(UserSummary.class), any(RestaurantSummary.class), any()))
                .thenAnswer(invocation -> {
                    Cart cart = invocation.getArgument(0);
                    RestaurantSummary restaurant = invocation.getArgument(2);
                    String message = invocation.getArgument(3);
                    CartResponse response = new CartResponse();
                    response.setTotalQuantity(cart.getTotalQuantity());
                    response.setTotalPrice(cart.getTotalPrice());
                    response.setRestaurant(restaurant);
                    response.setMessage(message);
                    return response;
                });

        lenient().when(userFacade.getUserById(USER_ID)).thenReturn(new UserSummary(USER_ID, "Test User"));
        lenient().when(catalogFacade.getRestaurantById(RESTAURANT_ID)).thenReturn(restaurantSummary);
        lenient().when(catalogFacade.getRestaurantById(OTHER_RESTAURANT_ID)).thenReturn(
                new RestaurantSummary(OTHER_RESTAURANT_ID, "Other Hub", CuisineType.INDIAN, new BigDecimal("4.0")));
    }

    @Test
    void addToCart_createsNewCartWhenNoneExists() {
        FoodSummary food = createFoodSummary(FOOD_ID, RESTAURANT_ID);
        AddToCartRequest request = new AddToCartRequest(FOOD_ID, 2, List.of());

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(catalogFacade.getFoodById(FOOD_ID)).thenReturn(food);
        when(catalogFacade.getAddonsByIds(List.of())).thenReturn(List.of());
        when(sharedCartMemberRepository.findActiveMemberByUserId(USER_ID)).thenReturn(null);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(null);
        when(pricingService.calculateCurrentPrice(eq(food), eq(List.of()))).thenReturn(new BigDecimal("120.00"));
        when(pricingService.calculatePriceAtAddition(any(CartItem.class))).thenReturn(new BigDecimal("120.00"));
        when(pricingService.calculateItemTotal(any(CartItem.class))).thenReturn(new BigDecimal("240.00"));
        doAnswer(invocation -> {
            Cart cart = invocation.getArgument(0);
            cart.setTotalQuantity(cart.getItems().stream().mapToInt(CartItem::getQuantity).sum());
            cart.setTotalPrice(new BigDecimal("240.00"));
            return null;
        }).when(pricingService).recalculateCartTotals(any(Cart.class));

        CartResponse response = cartService.addToCart(request);

        assertNotNull(response.getRestaurant());
        assertEquals(2, response.getTotalQuantity());
        assertEquals(new BigDecimal("240.00"), response.getTotalPrice());
    }

    @Test
    void addToCart_mergesWhenSameFoodAddonsAndPrice() {
        FoodSummary food = createFoodSummary(FOOD_ID, RESTAURANT_ID);
        AddonSummary addon = new AddonSummary(ADDON_ID, "Cheese", new BigDecimal("20.00"));

        Cart cart = createCart(USER_ID, RESTAURANT_ID);
        CartItem existing = createCartItem(cart, FOOD_ID, List.of(ADDON_ID), 1, new BigDecimal("120.00"));
        cart.getItems().add(existing);

        AddToCartRequest request = new AddToCartRequest(FOOD_ID, 2, List.of(ADDON_ID));

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartMemberRepository.findActiveMemberByUserId(USER_ID)).thenReturn(null);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(cart);
        when(catalogFacade.getFoodById(FOOD_ID)).thenReturn(food);
        when(catalogFacade.getAddonsByIds(List.of(ADDON_ID))).thenReturn(List.of(addon));
        when(pricingService.calculateCurrentPrice(eq(food), eq(List.of(addon)))).thenReturn(new BigDecimal("120.00"));
        when(pricingService.calculateItemTotal(existing)).thenReturn(new BigDecimal("360.00"));
        doAnswer(invocation -> {
            Cart target = invocation.getArgument(0);
            target.setTotalQuantity(target.getItems().stream().mapToInt(CartItem::getQuantity).sum());
            target.setTotalPrice(new BigDecimal("360.00"));
            return null;
        }).when(pricingService).recalculateCartTotals(cart);

        CartResponse response = cartService.addToCart(request);

        assertEquals(3, existing.getQuantity());
        assertEquals(new BigDecimal("360.00"), existing.getItemTotal());
        assertEquals(3, response.getTotalQuantity());
    }

    @Test
    void addToCart_throwsWhenRestaurantMismatch() {
        FoodSummary food = createFoodSummary(FOOD_ID, OTHER_RESTAURANT_ID);
        Cart cart = createCart(USER_ID, RESTAURANT_ID);
        AddToCartRequest request = new AddToCartRequest(FOOD_ID, 1, List.of());

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartMemberRepository.findActiveMemberByUserId(USER_ID)).thenReturn(null);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(cart);
        when(catalogFacade.getFoodById(FOOD_ID)).thenReturn(food);

        assertThrows(EntityMisMatchAssociationException.class, () -> cartService.addToCart(request));
    }

    @Test
    void addToCart_usesSharedCartWhenMemberActive() {
        FoodSummary food = createFoodSummary(FOOD_ID, RESTAURANT_ID);
        AddToCartRequest request = new AddToCartRequest(FOOD_ID, 1, List.of());

        Cart sharedCart = createCart(USER_ID, RESTAURANT_ID);
        SharedCartMember member = new SharedCartMember();
        member.setUserId(USER_ID);
        member.setCart(sharedCart);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartMemberRepository.findActiveMemberByUserId(USER_ID)).thenReturn(member);
        when(catalogFacade.getFoodById(FOOD_ID)).thenReturn(food);
        when(catalogFacade.getAddonsByIds(List.of())).thenReturn(List.of());
        when(pricingService.calculateCurrentPrice(eq(food), eq(List.of()))).thenReturn(new BigDecimal("120.00"));
        when(pricingService.calculatePriceAtAddition(any(CartItem.class))).thenReturn(new BigDecimal("120.00"));
        when(pricingService.calculateItemTotal(any(CartItem.class))).thenReturn(new BigDecimal("120.00"));
        doAnswer(invocation -> {
            Cart target = invocation.getArgument(0);
            target.setTotalQuantity(1);
            target.setTotalPrice(new BigDecimal("120.00"));
            return null;
        }).when(pricingService).recalculateCartTotals(sharedCart);

        CartResponse response = cartService.addToCart(request);

        assertEquals(1, response.getTotalQuantity());
        assertEquals(1, sharedCart.getItems().size());
    }

    @Test
    void addToCart_throwsWhenFoodNotFound() {
        AddToCartRequest request = new AddToCartRequest(FOOD_ID, 1, List.of());

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartMemberRepository.findActiveMemberByUserId(USER_ID)).thenReturn(null);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(null);
        when(catalogFacade.getFoodById(FOOD_ID)).thenThrow(new EntityNotFoundException("Food with id 100 not found"));

        assertThrows(EntityNotFoundException.class, () -> cartService.addToCart(request));
    }

    @Test
    void addToCart_throwsWhenAddonNotFound() {
        FoodSummary food = createFoodSummary(FOOD_ID, RESTAURANT_ID);
        AddToCartRequest request = new AddToCartRequest(FOOD_ID, 1, List.of(ADDON_ID));

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartMemberRepository.findActiveMemberByUserId(USER_ID)).thenReturn(null);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(null);
        when(catalogFacade.getFoodById(FOOD_ID)).thenReturn(food);
        when(catalogFacade.getAddonsByIds(List.of(ADDON_ID)))
                .thenThrow(new EntityNotFoundException("Addon not found"));

        assertThrows(EntityNotFoundException.class, () -> cartService.addToCart(request));
    }

    @Test
    void addToCart_throwsWhenAddonNotAssociatedWithFoodCategory() {
        FoodSummary food = createFoodSummary(FOOD_ID, RESTAURANT_ID);
        AddToCartRequest request = new AddToCartRequest(FOOD_ID, 1, List.of(ADDON_ID));

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartMemberRepository.findActiveMemberByUserId(USER_ID)).thenReturn(null);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(null);
        when(catalogFacade.getFoodById(FOOD_ID)).thenReturn(food);
        when(catalogFacade.getAddonsByIds(List.of(ADDON_ID)))
                .thenThrow(new EntityUnavailableException("Addon is not associated with this food category"));

        assertThrows(EntityUnavailableException.class, () -> cartService.addToCart(request));
    }

    @Test
    void addToCart_throwsWhenAddonUnavailable() {
        FoodSummary food = createFoodSummary(FOOD_ID, RESTAURANT_ID);
        AddToCartRequest request = new AddToCartRequest(FOOD_ID, 1, List.of(ADDON_ID));

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartMemberRepository.findActiveMemberByUserId(USER_ID)).thenReturn(null);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(null);
        when(catalogFacade.getFoodById(FOOD_ID)).thenReturn(food);
        when(catalogFacade.getAddonsByIds(List.of(ADDON_ID)))
                .thenThrow(new EntityUnavailableException("Addon is unavailable"));

        assertThrows(EntityUnavailableException.class, () -> cartService.addToCart(request));
    }

    @Test
    void getCart_throwsWhenCartMissing() {
        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(null);

        assertThrows(EntityNotFoundException.class, () -> cartService.getCart());
    }

    @Test
    void getCart_updatesPricingWhenCartPresent() {
        Cart cart = createCart(USER_ID, RESTAURANT_ID);
        cart.getItems().add(createCartItem(cart, FOOD_ID, List.of(), 1, new BigDecimal("120.00")));

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(cart);
        when(pricingService.refreshExpiredPrices(cart)).thenReturn(true);

        CartResponse response = cartService.getCart();

        assertNotNull(response.getRestaurant());
        verify(pricingService).recalculateCartTotals(cart);
    }

    @Test
    void updateCart_ItemQuantity_updatesQuantityAndTotal() {
        Cart cart = createCart(USER_ID, RESTAURANT_ID);
        CartItem item = createCartItem(cart, FOOD_ID, List.of(), 1, new BigDecimal("120.00"));
        item.setCartItemId(1L);
        cart.getItems().add(item);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(cart);
        when(cartItemRepository.findById(1L)).thenReturn(java.util.Optional.of(item));
        when(pricingService.calculateItemTotal(item)).thenReturn(new BigDecimal("240.00"));
        doAnswer(invocation -> {
            Cart target = invocation.getArgument(0);
            target.setTotalQuantity(target.getItems().stream().mapToInt(CartItem::getQuantity).sum());
            target.setTotalPrice(target.getItems().stream()
                    .map(CartItem::getItemTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            return null;
        }).when(pricingService).recalculateCartTotals(cart);

        CartResponse response = cartService.updateCartItemQuantity(1L, new UpdateCartItemRequest(2));

        assertEquals(2, item.getQuantity());
        assertEquals(new BigDecimal("240.00"), item.getItemTotal());
        assertEquals(2, response.getTotalQuantity());
    }

    @Test
    void updateCart_throwsWhenCartItemQuantityNotFound() {
        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(null);

        assertThrows(EntityNotFoundException.class,
                () -> cartService.updateCartItemQuantity(1L, new UpdateCartItemRequest(1)));
    }

    @Test
    void updateCart_throwsWhenCartItemQuantityEmpty() {
        Cart cart = createCart(USER_ID, RESTAURANT_ID);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(cart);

        assertThrows(EntityUnavailableException.class,
                () -> cartService.updateCartItemQuantity(1L, new UpdateCartItemRequest(1)));
    }

    @Test
    void updateCart_ItemQuantity_throwsWhenItemNotFound() {
        Cart cart = createCart(USER_ID, RESTAURANT_ID);
        cart.getItems().add(new CartItem());

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(cart);
        when(cartItemRepository.findById(9L)).thenReturn(java.util.Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> cartService.updateCartItemQuantity(9L, new UpdateCartItemRequest(1)));
    }

    @Test
    void updateCart_throwsWhenItemNotInCartItemQuantity() {
        Cart cart = createCart(USER_ID, RESTAURANT_ID);
        CartItem item = createCartItem(cart, FOOD_ID, List.of(), 1, new BigDecimal("120.00"));
        item.setCartItemId(1L);
        Cart otherCart = createCart(USER_ID, RESTAURANT_ID);
        item.setCart(otherCart);
        cart.getItems().add(item);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(cart);
        when(cartItemRepository.findById(1L)).thenReturn(java.util.Optional.of(item));

        assertThrows(AccessDeniedException.class,
                () -> cartService.updateCartItemQuantity(1L, new UpdateCartItemRequest(1)));
    }

    @Test
    void removeCartItem_removesAndRecalculatesTotal() {
        Cart cart = createCart(USER_ID, RESTAURANT_ID);
        CartItem item = createCartItem(cart, FOOD_ID, List.of(), 1, new BigDecimal("120.00"));
        item.setCartItemId(1L);
        cart.getItems().add(item);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(cart);
        when(cartItemRepository.findById(1L)).thenReturn(java.util.Optional.of(item));
        doAnswer(invocation -> {
            Cart target = invocation.getArgument(0);
            target.setTotalQuantity(0);
            target.setTotalPrice(BigDecimal.ZERO);
            return null;
        }).when(pricingService).recalculateCartTotals(cart);

        CartResponse response = cartService.removeCartItem(1L);

        assertTrue(cart.getItems().isEmpty());
        assertEquals(0, response.getTotalQuantity());
        verify(pricingService).recalculateCartTotals(cart);
    }

    @Test
    void clearCart_clearsItemsAndUpdatesTotal() {
        Cart cart = createCart(USER_ID, RESTAURANT_ID);
        cart.getItems().add(new CartItem());

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(cart);

        String message = cartService.clearCart();

        assertEquals("Cart has been cleared", message);
        verify(pricingService).recalculateCartTotals(cart);
    }

    @Test
    void getCart_throwsWhenAuthenticationMissing_expectedSecurityBehavior() {
        when(userAuthorization.authorizeUserId()).thenThrow(new AccessDeniedException("No valid authority"));

        assertThrows(AccessDeniedException.class, () -> cartService.getCart());
        verify(cartRepository, never()).findByUserId(any());
    }

    private FoodSummary createFoodSummary(Long foodId, Long restaurantId) {
        return new FoodSummary(foodId, "Pasta", "Creamy pasta", new BigDecimal("100.00"), true, restaurantId);
    }

    private Cart createCart(Long userId, Long restaurantId) {
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setRestaurantId(restaurantId);
        cart.setItems(new ArrayList<>());
        cart.setTotalPrice(BigDecimal.ZERO);
        cart.setTotalQuantity(0);
        return cart;
    }

    private CartItem createCartItem(Cart cart, Long foodId, List<Long> addonIds, int quantity, BigDecimal priceAtAddition) {
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setFoodId(foodId);
        item.setAddonIds(addonIds);
        item.setQuantity(quantity);
        item.setPriceAtAddition(priceAtAddition);
        item.setItemTotal(priceAtAddition.multiply(BigDecimal.valueOf(quantity)));
        item.setAddedTime(LocalDateTime.now().minusMinutes(5));
        return item;
    }
}
