package com.kilgore.fooddeliveryapp.ordering.service;

import com.kilgore.fooddeliveryapp.catalog.api.CatalogFacade;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.FoodSummary;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityMisMatchAssociationException;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.common.exceptions.UserStatusException;
import com.kilgore.fooddeliveryapp.common.util.UserAuthorization;
import com.kilgore.fooddeliveryapp.identity.api.UserFacade;
import com.kilgore.fooddeliveryapp.ordering.dto.request.AddToCartRequest;
import com.kilgore.fooddeliveryapp.ordering.dto.request.PlaceOrderRequest;
import com.kilgore.fooddeliveryapp.ordering.dto.request.SharedCartRequest;
import com.kilgore.fooddeliveryapp.ordering.dto.response.SharedCartResponse;
import com.kilgore.fooddeliveryapp.ordering.mapper.OrderingMapper;
import com.kilgore.fooddeliveryapp.ordering.model.Cart;
import com.kilgore.fooddeliveryapp.ordering.model.CartItem;
import com.kilgore.fooddeliveryapp.ordering.model.Order;
import com.kilgore.fooddeliveryapp.ordering.model.OrderItem;
import com.kilgore.fooddeliveryapp.ordering.model.OrderStatus;
import com.kilgore.fooddeliveryapp.common.enums.PaymentStatus;
import com.kilgore.fooddeliveryapp.ordering.model.SharedCart;
import com.kilgore.fooddeliveryapp.ordering.model.SharedCartMember;
import com.kilgore.fooddeliveryapp.ordering.repository.CartRepository;
import com.kilgore.fooddeliveryapp.ordering.repository.OrderRepository;
import com.kilgore.fooddeliveryapp.ordering.repository.SharedCartMemberRepository;
import com.kilgore.fooddeliveryapp.ordering.repository.SharedCartRepository;
import com.kilgore.fooddeliveryapp.wallet.dto.request.WalletContributionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedCartServiceTest {

    private static final Long USER_ID = 10L;
    private static final Long HOST_ID = 11L;
    private static final Long RESTAURANT_ID = 22L;
    private static final String JOIN_CODE = "JOIN42";

    @Mock
    private SharedCartRepository sharedCartRepository;
    @Mock
    private UserAuthorization userAuthorization;
    @Mock
    private SharedCartMemberRepository sharedCartMemberRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartService cartService;
    @Mock
    private PricingService pricingService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderService orderService;
    @Mock
    private UserFacade userFacade;
    @Mock
    private CatalogFacade catalogFacade;
    @Mock
    private OrderingMapper orderingMapper;

    @InjectMocks
    private SharedCartService sharedCartService;

    @Test
    void createSharedCart_existingActiveMembership_returnsMappedResponse() {
        // Arrange
        SharedCartRequest request = new SharedCartRequest(RESTAURANT_ID, false);
        SharedCart sharedCart = newSharedCart(USER_ID, RESTAURANT_ID, JOIN_CODE);
        Cart cart = newCart(USER_ID, RESTAURANT_ID);
        SharedCartMember member = newMember(USER_ID, cart, sharedCart);
        SharedCartResponse response = new SharedCartResponse();

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartMemberRepository.findActiveMemberByUserId(USER_ID)).thenReturn(member);
        when(orderingMapper.toSharedCartResponse(sharedCart, USER_ID)).thenReturn(response);

        // Act
        SharedCartResponse result = sharedCartService.createSharedCart(request);

        // Assert
        assertSame(response, result);
        verify(sharedCartRepository, never()).findByHostUserId(any());
        verify(sharedCartRepository, never()).save(any());
    }

    @Test
    void createSharedCart_existingActiveSharedCart_returnsMappedResponse() {
        // Arrange
        SharedCartRequest request = new SharedCartRequest(RESTAURANT_ID, true);
        SharedCart sharedCart = newSharedCart(USER_ID, RESTAURANT_ID, JOIN_CODE);
        SharedCartResponse response = new SharedCartResponse();

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartMemberRepository.findActiveMemberByUserId(USER_ID)).thenReturn(null);
        when(sharedCartRepository.findByHostUserId(USER_ID)).thenReturn(sharedCart);
        when(orderingMapper.toSharedCartResponse(sharedCart, USER_ID)).thenReturn(response);

        // Act
        SharedCartResponse result = sharedCartService.createSharedCart(request);

        // Assert
        assertSame(response, result);
        verify(sharedCartRepository, never()).save(any());
    }

    @Test
    void createSharedCart_userRestricted_throwsUserStatusException() {
        // Arrange
        SharedCartRequest request = new SharedCartRequest(RESTAURANT_ID, false);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.isUserRestricted(USER_ID)).thenReturn(true);
        when(userFacade.userRestrictedUntil(USER_ID)).thenReturn(LocalDateTime.of(2026, 6, 4, 10, 0));

        // Act
        UserStatusException exception = assertThrows(UserStatusException.class,
                () -> sharedCartService.createSharedCart(request));

        // Assert
        assertTrue(exception.getMessage().contains("restricted"));
        verify(sharedCartRepository, never()).save(any());
    }

    @Test
    void createSharedCart_incompatibleRestaurantCartWithItems_throwsEntityMisMatchAssociationException() {
        // Arrange
        SharedCartRequest request = new SharedCartRequest(RESTAURANT_ID, false);
        Cart cart = newCart(USER_ID, 99L);
        cart.getItems().add(newCartItem(1L, 1));

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.isUserRestricted(USER_ID)).thenReturn(false);
        when(sharedCartMemberRepository.findActiveMemberByUserId(USER_ID)).thenReturn(null);
        when(sharedCartRepository.findByHostUserId(USER_ID)).thenReturn(null);
        when(sharedCartRepository.findByJoinCode(any())).thenReturn(null);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(cart);

        // Act
        assertThrows(EntityMisMatchAssociationException.class,
                () -> sharedCartService.createSharedCart(request));

        // Assert - shared cart is persisted before restaurant compatibility check fails
        verify(sharedCartRepository).save(any(SharedCart.class));
        verify(sharedCartMemberRepository, never()).save(any());
    }

    @Test
    void createSharedCart_noExistingCart_createsSharedCartAndMember() {
        // Arrange
        SharedCartRequest request = new SharedCartRequest(RESTAURANT_ID, false);
        SharedCartResponse response = new SharedCartResponse();

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.isUserRestricted(USER_ID)).thenReturn(false);
        when(sharedCartMemberRepository.findActiveMemberByUserId(USER_ID)).thenReturn(null);
        when(sharedCartRepository.findByHostUserId(USER_ID)).thenReturn(null);
        when(sharedCartRepository.findByJoinCode(any())).thenReturn(null);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(null);
        when(orderingMapper.toSharedCartResponse(any(SharedCart.class), eq(USER_ID))).thenReturn(response);

        // Act
        SharedCartResponse result = sharedCartService.createSharedCart(request);

        // Assert
        assertSame(response, result);
        verify(sharedCartRepository).save(any(SharedCart.class));
        verify(cartRepository).save(any(Cart.class));
        verify(sharedCartMemberRepository).save(any(SharedCartMember.class));
    }

    @Test
    void createSharedCart_unauthorized_throwsAccessDeniedException() {
        // Arrange
        SharedCartRequest request = new SharedCartRequest(RESTAURANT_ID, false);

        when(userAuthorization.authorizeUserId()).thenThrow(new AccessDeniedException("No valid authority"));

        // Act
        assertThrows(AccessDeniedException.class, () -> sharedCartService.createSharedCart(request));

        // Assert
        verify(sharedCartRepository, never()).save(any());
    }

    @Test
    void getCurrentSharedCart_activeMembership_returnsMappedResponse() {
        // Arrange
        SharedCart sharedCart = newSharedCart(USER_ID, RESTAURANT_ID, JOIN_CODE);
        SharedCartMember member = newMember(USER_ID, newCart(USER_ID, RESTAURANT_ID), sharedCart);
        SharedCartResponse response = new SharedCartResponse();

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartMemberRepository.findActiveMemberByUserId(USER_ID)).thenReturn(member);
        when(orderingMapper.toSharedCartResponse(sharedCart, USER_ID)).thenReturn(response);

        // Act
        SharedCartResponse result = sharedCartService.getCurrentSharedCart();

        // Assert
        assertSame(response, result);
        verify(sharedCartRepository, never()).findByHostUserId(any());
    }

    @Test
    void getCurrentSharedCart_activeHostSharedCart_returnsMappedResponse() {
        // Arrange
        SharedCart sharedCart = newSharedCart(USER_ID, RESTAURANT_ID, JOIN_CODE);
        SharedCartResponse response = new SharedCartResponse();

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartMemberRepository.findActiveMemberByUserId(USER_ID)).thenReturn(null);
        when(sharedCartRepository.findByHostUserId(USER_ID)).thenReturn(sharedCart);
        when(orderingMapper.toSharedCartResponse(sharedCart, USER_ID)).thenReturn(response);

        // Act
        SharedCartResponse result = sharedCartService.getCurrentSharedCart();

        // Assert
        assertSame(response, result);
    }

    @Test
    void getCurrentSharedCart_noActiveCart_throwsEntityNotFoundException() {
        // Arrange
        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartMemberRepository.findActiveMemberByUserId(USER_ID)).thenReturn(null);
        when(sharedCartRepository.findByHostUserId(USER_ID)).thenReturn(null);

        // Act
        assertThrows(EntityNotFoundException.class, sharedCartService::getCurrentSharedCart);

        // Assert
        verify(orderingMapper, never()).toSharedCartResponse(any(), any());
    }

    @Test
    void getCurrentSharedCart_unauthorized_throwsAccessDeniedException() {
        // Arrange
        when(userAuthorization.authorizeUserId()).thenThrow(new AccessDeniedException("No valid authority"));

        // Act
        assertThrows(AccessDeniedException.class, sharedCartService::getCurrentSharedCart);

        // Assert
        verify(sharedCartMemberRepository, never()).findActiveMemberByUserId(any());
    }

    @Test
    void getSharedCartByCode_found_returnsMappedResponse() {
        // Arrange
        SharedCart sharedCart = newSharedCart(HOST_ID, RESTAURANT_ID, JOIN_CODE);
        SharedCartResponse response = new SharedCartResponse();

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartRepository.findByJoinCode(JOIN_CODE)).thenReturn(sharedCart);
        when(orderingMapper.toSharedCartResponse(sharedCart, USER_ID)).thenReturn(response);

        // Act
        SharedCartResponse result = sharedCartService.getSharedCartByCode(JOIN_CODE);

        // Assert
        assertSame(response, result);
    }

    @Test
    void getSharedCartByCode_notFound_throwsEntityNotFoundException() {
        // Arrange
        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartRepository.findByJoinCode(JOIN_CODE)).thenReturn(null);

        // Act
        assertThrows(EntityNotFoundException.class,
                () -> sharedCartService.getSharedCartByCode(JOIN_CODE));

        // Assert
        verify(orderingMapper, never()).toSharedCartResponse(any(), any());
    }

    @Test
    void getSharedCartByCode_unauthorized_throwsAccessDeniedException() {
        // Arrange
        when(userAuthorization.authorizeUserId()).thenThrow(new AccessDeniedException("No valid authority"));

        // Act
        assertThrows(AccessDeniedException.class,
                () -> sharedCartService.getSharedCartByCode(JOIN_CODE));

        // Assert
        verify(sharedCartRepository, never()).findByJoinCode(any());
    }

    @Test
    void getSharedCartByCode_nullCode_throwsEntityNotFoundException() {
        // Arrange
        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartRepository.findByJoinCode(null)).thenReturn(null);

        // Act
        assertThrows(EntityNotFoundException.class,
                () -> sharedCartService.getSharedCartByCode(null));

        // Assert
        verify(sharedCartRepository).findByJoinCode(null);
    }

    @Test
    void joinSharedCart_existingCartSameRestaurant_linksAndSavesCart() {
        // Arrange
        SharedCart sharedCart = newSharedCart(HOST_ID, RESTAURANT_ID, JOIN_CODE);
        Cart cart = newCart(USER_ID, RESTAURANT_ID);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.isUserRestricted(USER_ID)).thenReturn(false);
        when(sharedCartMemberRepository.findActiveMemberByUserId(USER_ID)).thenReturn(null);
        when(sharedCartRepository.findByJoinCode(JOIN_CODE)).thenReturn(sharedCart);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(cart);

        // Act
        String result = sharedCartService.joinSharedCart(JOIN_CODE);

        // Assert
        assertTrue(result.contains("member"));
        assertSame(sharedCart, cart.getSharedCart());
        verify(cartRepository).save(cart);
    }

    @Test
    void joinSharedCart_userRestricted_throwsUserStatusException() {
        // Arrange
        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.isUserRestricted(USER_ID)).thenReturn(true);
        when(userFacade.userRestrictedUntil(USER_ID)).thenReturn(LocalDateTime.of(2026, 6, 4, 10, 0));

        // Act
        assertThrows(UserStatusException.class,
                () -> sharedCartService.joinSharedCart(JOIN_CODE));

        // Assert
        verify(sharedCartRepository, never()).findByJoinCode(any());
    }

    @Test
    void joinSharedCart_existingMembership_throwsAccessDeniedException() {
        // Arrange
        SharedCartMember member = newMember(USER_ID, newCart(USER_ID, RESTAURANT_ID), newSharedCart(HOST_ID, RESTAURANT_ID, JOIN_CODE));

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.isUserRestricted(USER_ID)).thenReturn(false);
        when(sharedCartMemberRepository.findActiveMemberByUserId(USER_ID)).thenReturn(member);

        // Act
        assertThrows(AccessDeniedException.class,
                () -> sharedCartService.joinSharedCart(JOIN_CODE));

        // Assert
        verify(sharedCartRepository, never()).findByJoinCode(any());
    }

    @Test
    void joinSharedCart_sharedCartNotFound_throwsEntityNotFoundException() {
        // Arrange
        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.isUserRestricted(USER_ID)).thenReturn(false);
        when(sharedCartMemberRepository.findActiveMemberByUserId(USER_ID)).thenReturn(null);
        when(sharedCartRepository.findByJoinCode(JOIN_CODE)).thenReturn(null);

        // Act
        assertThrows(EntityNotFoundException.class,
                () -> sharedCartService.joinSharedCart(JOIN_CODE));

        // Assert
        verify(cartRepository, never()).save(any());
    }

    @Test
    void joinSharedCart_userIsHost_throwsAccessDeniedException() {
        // Arrange
        SharedCart sharedCart = newSharedCart(USER_ID, RESTAURANT_ID, JOIN_CODE);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.isUserRestricted(USER_ID)).thenReturn(false);
        when(sharedCartMemberRepository.findActiveMemberByUserId(USER_ID)).thenReturn(null);
        when(sharedCartRepository.findByJoinCode(JOIN_CODE)).thenReturn(sharedCart);

        // Act
        assertThrows(AccessDeniedException.class,
                () -> sharedCartService.joinSharedCart(JOIN_CODE));

        // Assert
        verify(cartRepository, never()).save(any());
    }

    @Test
    void joinSharedCart_cartDifferentRestaurantWithItems_throwsEntityMisMatchAssociationException() {
        // Arrange
        SharedCart sharedCart = newSharedCart(HOST_ID, RESTAURANT_ID, JOIN_CODE);
        Cart cart = newCart(USER_ID, 99L);
        cart.getItems().add(newCartItem(1L, 1));

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.isUserRestricted(USER_ID)).thenReturn(false);
        when(sharedCartMemberRepository.findActiveMemberByUserId(USER_ID)).thenReturn(null);
        when(sharedCartRepository.findByJoinCode(JOIN_CODE)).thenReturn(sharedCart);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(cart);

        // Act
        assertThrows(EntityMisMatchAssociationException.class,
                () -> sharedCartService.joinSharedCart(JOIN_CODE));

        // Assert
        verify(cartRepository, never()).save(any());
    }

    @Test
    void joinSharedCart_cartNull_createsCartAndSaves() {
        // Arrange
        SharedCart sharedCart = newSharedCart(HOST_ID, RESTAURANT_ID, JOIN_CODE);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.isUserRestricted(USER_ID)).thenReturn(false);
        when(sharedCartMemberRepository.findActiveMemberByUserId(USER_ID)).thenReturn(null);
        when(sharedCartRepository.findByJoinCode(JOIN_CODE)).thenReturn(sharedCart);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(null);

        // Act
        sharedCartService.joinSharedCart(JOIN_CODE);

        // Assert
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void joinSharedCart_cartDifferentRestaurantButEmptyItems_allowsJoin() {
        // Arrange
        SharedCart sharedCart = newSharedCart(HOST_ID, RESTAURANT_ID, JOIN_CODE);
        Cart cart = newCart(USER_ID, 99L);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.isUserRestricted(USER_ID)).thenReturn(false);
        when(sharedCartMemberRepository.findActiveMemberByUserId(USER_ID)).thenReturn(null);
        when(sharedCartRepository.findByJoinCode(JOIN_CODE)).thenReturn(sharedCart);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(cart);

        // Act
        String result = sharedCartService.joinSharedCart(JOIN_CODE);

        // Assert
        assertTrue(result.contains("member"));
        assertSame(sharedCart, cart.getSharedCart());
        verify(cartRepository).save(cart);
    }

    @Test
    void joinSharedCart_unauthorized_throwsAccessDeniedException() {
        // Arrange
        when(userAuthorization.authorizeUserId()).thenThrow(new AccessDeniedException("No valid authority"));

        // Act
        assertThrows(AccessDeniedException.class,
                () -> sharedCartService.joinSharedCart(JOIN_CODE));

        // Assert
        verify(sharedCartRepository, never()).findByJoinCode(any());
    }

    @Test
    void addItemToSharedCart_happyPath_addsItemAndUpdatesTotal() {
        // Arrange
        AddToCartRequest request = new AddToCartRequest(5L, 2, List.of());
        SharedCart sharedCart = newSharedCart(HOST_ID, RESTAURANT_ID, JOIN_CODE);
        Cart cart = newCart(USER_ID, RESTAURANT_ID);
        cart.setTotalPrice(new BigDecimal("12.00"));
        SharedCartMember member = newMember(USER_ID, cart, sharedCart);
        sharedCart.getMemberList().add(member);
        FoodSummary food = new FoodSummary(5L, "Burger", "", new BigDecimal("6.00"), true, RESTAURANT_ID);
        SharedCartResponse response = new SharedCartResponse();

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartMemberRepository.findByUserIdAndJoinCode(JOIN_CODE, USER_ID)).thenReturn(member);
        when(catalogFacade.getFoodById(5L)).thenReturn(food);
        when(sharedCartRepository.save(sharedCart)).thenReturn(sharedCart);
        when(orderingMapper.toSharedCartResponse(sharedCart, USER_ID)).thenReturn(response);

        // Act
        SharedCartResponse result = sharedCartService.addItemToSharedCart(JOIN_CODE, request);

        // Assert
        assertSame(response, result);
        verify(cartService).addOrMergeCartItem(cart, request, food);
        verify(cartRepository).save(cart);
        verify(sharedCartRepository).save(sharedCart);
    }

    @Test
    void addItemToSharedCart_notMember_throwsEntityNotFoundException() {
        // Arrange
        AddToCartRequest request = new AddToCartRequest(5L, 2, List.of());

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartMemberRepository.findByUserIdAndJoinCode(JOIN_CODE, USER_ID)).thenReturn(null);

        // Act
        assertThrows(EntityNotFoundException.class,
                () -> sharedCartService.addItemToSharedCart(JOIN_CODE, request));

        // Assert
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addItemToSharedCart_foodRestaurantMismatch_throwsEntityMisMatchAssociationException() {
        // Arrange
        AddToCartRequest request = new AddToCartRequest(5L, 1, List.of());
        SharedCart sharedCart = newSharedCart(HOST_ID, RESTAURANT_ID, JOIN_CODE);
        Cart cart = newCart(USER_ID, 99L);
        SharedCartMember member = newMember(USER_ID, cart, sharedCart);
        FoodSummary food = new FoodSummary(5L, "Burger", "", new BigDecimal("6.00"), true, RESTAURANT_ID);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartMemberRepository.findByUserIdAndJoinCode(JOIN_CODE, USER_ID)).thenReturn(member);
        when(catalogFacade.getFoodById(5L)).thenReturn(food);

        // Act
        assertThrows(EntityMisMatchAssociationException.class,
                () -> sharedCartService.addItemToSharedCart(JOIN_CODE, request));

        // Assert
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addItemToSharedCart_memberCartNull_throwsNullPointerException() {
        // Arrange
        AddToCartRequest request = new AddToCartRequest(5L, 1, List.of());
        SharedCart sharedCart = newSharedCart(HOST_ID, RESTAURANT_ID, JOIN_CODE);
        SharedCartMember member = newMember(USER_ID, null, sharedCart);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartMemberRepository.findByUserIdAndJoinCode(JOIN_CODE, USER_ID)).thenReturn(member);

        // Act
        assertThrows(NullPointerException.class,
                () -> sharedCartService.addItemToSharedCart(JOIN_CODE, request));

        // Assert
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addItemToSharedCart_sharedCartMemberListNull_throwsNullPointerException() {
        // Arrange
        AddToCartRequest request = new AddToCartRequest(5L, 1, List.of());
        SharedCart sharedCart = newSharedCart(HOST_ID, RESTAURANT_ID, JOIN_CODE);
        sharedCart.setMemberList(null);
        Cart cart = newCart(USER_ID, RESTAURANT_ID);
        SharedCartMember member = newMember(USER_ID, cart, sharedCart);
        FoodSummary food = new FoodSummary(5L, "Burger", "", new BigDecimal("6.00"), true, RESTAURANT_ID);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartMemberRepository.findByUserIdAndJoinCode(JOIN_CODE, USER_ID)).thenReturn(member);
        when(catalogFacade.getFoodById(5L)).thenReturn(food);

        // Act
        assertThrows(NullPointerException.class,
                () -> sharedCartService.addItemToSharedCart(JOIN_CODE, request));

        // Assert
        verify(cartRepository).save(cart);
    }

    @Test
    void addItemToSharedCart_unauthorized_throwsAccessDeniedException() {
        // Arrange
        AddToCartRequest request = new AddToCartRequest(5L, 1, List.of());

        when(userAuthorization.authorizeUserId()).thenThrow(new AccessDeniedException("No valid authority"));

        // Act
        assertThrows(AccessDeniedException.class,
                () -> sharedCartService.addItemToSharedCart(JOIN_CODE, request));

        // Assert
        verify(sharedCartMemberRepository, never()).findByUserIdAndJoinCode(any(), any());
    }

    @Test
    void removeItemFromSharedCart_happyPath_removesItemAndUpdatesTotals() {
        // Arrange
        SharedCart sharedCart = newSharedCart(HOST_ID, RESTAURANT_ID, JOIN_CODE);
        Cart cart = newCart(USER_ID, RESTAURANT_ID);
        CartItem item = newCartItem(3L, 1);
        cart.getItems().add(item);
        cart.setTotalPrice(new BigDecimal("8.00"));
        SharedCartMember member = newMember(USER_ID, cart, sharedCart);
        sharedCart.getMemberList().add(member);
        SharedCartResponse response = new SharedCartResponse();

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartMemberRepository.findByUserIdAndJoinCode(JOIN_CODE, USER_ID)).thenReturn(member);
        when(cartService.verifyCartItem(3L, cart)).thenReturn(item);
        when(sharedCartRepository.save(sharedCart)).thenReturn(sharedCart);
        when(orderingMapper.toSharedCartResponse(sharedCart, USER_ID)).thenReturn(response);

        // Act
        SharedCartResponse result = sharedCartService.removeItemFromSharedCart(JOIN_CODE, 3L);

        // Assert
        assertSame(response, result);
        assertTrue(cart.getItems().isEmpty());
        verify(pricingService).recalculateCartTotals(cart);
        verify(cartRepository).save(cart);
        verify(sharedCartRepository).save(sharedCart);
    }

    @Test
    void removeItemFromSharedCart_notMember_throwsEntityNotFoundException() {
        // Arrange
        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartMemberRepository.findByUserIdAndJoinCode(JOIN_CODE, USER_ID)).thenReturn(null);

        // Act
        assertThrows(EntityNotFoundException.class,
                () -> sharedCartService.removeItemFromSharedCart(JOIN_CODE, 1L));

        // Assert
        verify(cartRepository, never()).save(any());
    }

    @Test
    void removeItemFromSharedCart_verifyCartItemThrows_propagatesException() {
        // Arrange
        SharedCart sharedCart = newSharedCart(HOST_ID, RESTAURANT_ID, JOIN_CODE);
        Cart cart = newCart(USER_ID, RESTAURANT_ID);
        SharedCartMember member = newMember(USER_ID, cart, sharedCart);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartMemberRepository.findByUserIdAndJoinCode(JOIN_CODE, USER_ID)).thenReturn(member);
        when(cartService.verifyCartItem(1L, cart)).thenThrow(new EntityNotFoundException("not found"));

        // Act
        assertThrows(EntityNotFoundException.class,
                () -> sharedCartService.removeItemFromSharedCart(JOIN_CODE, 1L));

        // Assert
        verify(cartRepository, never()).save(any());
    }

    @Test
    void removeItemFromSharedCart_memberCartNull_throwsNullPointerException() {
        // Arrange
        SharedCart sharedCart = newSharedCart(HOST_ID, RESTAURANT_ID, JOIN_CODE);
        SharedCartMember member = newMember(USER_ID, null, sharedCart);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartMemberRepository.findByUserIdAndJoinCode(JOIN_CODE, USER_ID)).thenReturn(member);

        // Act
        assertThrows(NullPointerException.class,
                () -> sharedCartService.removeItemFromSharedCart(JOIN_CODE, 1L));

        // Assert
        verify(cartRepository, never()).save(any());
    }

    @Test
    void removeItemFromSharedCart_cartItemsNull_throwsNullPointerException() {
        // Arrange
        SharedCart sharedCart = newSharedCart(HOST_ID, RESTAURANT_ID, JOIN_CODE);
        Cart cart = newCart(USER_ID, RESTAURANT_ID);
        cart.setItems(null);
        SharedCartMember member = newMember(USER_ID, cart, sharedCart);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartMemberRepository.findByUserIdAndJoinCode(JOIN_CODE, USER_ID)).thenReturn(member);
        when(cartService.verifyCartItem(1L, cart)).thenReturn(newCartItem(1L, 1));

        // Act
        assertThrows(NullPointerException.class,
                () -> sharedCartService.removeItemFromSharedCart(JOIN_CODE, 1L));

        // Assert
        verify(cartRepository, never()).save(any());
    }

    @Test
    void removeItemFromSharedCart_unauthorized_throwsAccessDeniedException() {
        // Arrange
        when(userAuthorization.authorizeUserId()).thenThrow(new AccessDeniedException("No valid authority"));

        // Act
        assertThrows(AccessDeniedException.class,
                () -> sharedCartService.removeItemFromSharedCart(JOIN_CODE, 1L));

        // Assert
        verify(sharedCartMemberRepository, never()).findByUserIdAndJoinCode(any(), any());
    }

    @Test
    void contributeToSharedCart_happyPath_updatesContributionAndDeductsWallet() {
        // Arrange
        SharedCart sharedCart = newSharedCart(HOST_ID, RESTAURANT_ID, JOIN_CODE);
        sharedCart.setHostPaysAll(false);
        sharedCart.setTotalPrice(new BigDecimal("30.00"));
        sharedCart.setAmountPaid(new BigDecimal("10.00"));
        Cart cart = newCart(USER_ID, RESTAURANT_ID);
        cart.setTotalPrice(new BigDecimal("20.00"));
        SharedCartMember member = newMember(USER_ID, cart, sharedCart);
        sharedCart.getMemberList().add(member);
        WalletContributionRequest request = new WalletContributionRequest(new BigDecimal("5.00"));
        SharedCartResponse response = new SharedCartResponse();

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.getWalletBalance(USER_ID)).thenReturn(new BigDecimal("50.00"));
        when(sharedCartRepository.findByJoinCode(JOIN_CODE)).thenReturn(sharedCart);
        when(sharedCartMemberRepository.findByUserIdAndJoinCode(JOIN_CODE, USER_ID)).thenReturn(member);
        when(sharedCartRepository.save(any(SharedCart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderingMapper.toSharedCartResponse(any(SharedCart.class), eq(USER_ID))).thenReturn(response);

        // Act
        SharedCartResponse result = sharedCartService.contributeToSharedCart(JOIN_CODE, request);

        // Assert
        assertSame(response, result);
        assertEquals(new BigDecimal("5.00"), member.getWalletContribution());
        assertEquals(new BigDecimal("15.00"), sharedCart.getAmountPaid());
        verify(userFacade).deductWalletBalance(USER_ID, request.getAmount());
        verify(sharedCartRepository, times(2)).save(sharedCart);
    }

    @Test
    void contributeToSharedCart_insufficientWallet_throwsIllegalArgumentException() {
        // Arrange
        WalletContributionRequest request = new WalletContributionRequest(new BigDecimal("5.00"));

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.getWalletBalance(USER_ID)).thenReturn(new BigDecimal("2.00"));

        // Act
        assertThrows(IllegalArgumentException.class,
                () -> sharedCartService.contributeToSharedCart(JOIN_CODE, request));

        // Assert
        verify(sharedCartRepository, never()).findByJoinCode(any());
    }

    @Test
    void contributeToSharedCart_sharedCartNotFound_throwsEntityNotFoundException() {
        // Arrange
        WalletContributionRequest request = new WalletContributionRequest(new BigDecimal("5.00"));

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.getWalletBalance(USER_ID)).thenReturn(new BigDecimal("10.00"));
        when(sharedCartRepository.findByJoinCode(JOIN_CODE)).thenReturn(null);

        // Act
        assertThrows(EntityNotFoundException.class,
                () -> sharedCartService.contributeToSharedCart(JOIN_CODE, request));

        // Assert
        verify(sharedCartMemberRepository, never()).findByUserIdAndJoinCode(any(), any());
    }

    @Test
    void contributeToSharedCart_hostPaysAll_throwsIllegalArgumentException() {
        // Arrange
        SharedCart sharedCart = newSharedCart(HOST_ID, RESTAURANT_ID, JOIN_CODE);
        sharedCart.setHostPaysAll(true);
        WalletContributionRequest request = new WalletContributionRequest(new BigDecimal("5.00"));

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.getWalletBalance(USER_ID)).thenReturn(new BigDecimal("10.00"));
        when(sharedCartRepository.findByJoinCode(JOIN_CODE)).thenReturn(sharedCart);

        // Act
        assertThrows(IllegalArgumentException.class,
                () -> sharedCartService.contributeToSharedCart(JOIN_CODE, request));

        // Assert
        verify(sharedCartMemberRepository, never()).findByUserIdAndJoinCode(any(), any());
    }

    @Test
    void contributeToSharedCart_notMember_throwsEntityNotFoundException() {
        // Arrange
        SharedCart sharedCart = newSharedCart(HOST_ID, RESTAURANT_ID, JOIN_CODE);
        sharedCart.setHostPaysAll(false);
        WalletContributionRequest request = new WalletContributionRequest(new BigDecimal("5.00"));

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.getWalletBalance(USER_ID)).thenReturn(new BigDecimal("10.00"));
        when(sharedCartRepository.findByJoinCode(JOIN_CODE)).thenReturn(sharedCart);
        when(sharedCartMemberRepository.findByUserIdAndJoinCode(JOIN_CODE, USER_ID)).thenReturn(null);

        // Act
        assertThrows(EntityNotFoundException.class,
                () -> sharedCartService.contributeToSharedCart(JOIN_CODE, request));

        // Assert
        verify(userFacade, never()).deductWalletBalance(any(), any());
    }

    @Test
    void contributeToSharedCart_overpayRemaining_throwsIllegalArgumentException() {
        // Arrange
        SharedCart sharedCart = newSharedCart(HOST_ID, RESTAURANT_ID, JOIN_CODE);
        sharedCart.setHostPaysAll(false);
        sharedCart.setTotalPrice(new BigDecimal("10.00"));
        sharedCart.setAmountPaid(new BigDecimal("9.00"));
        sharedCart.getMemberList().add(newMember(USER_ID, newCart(USER_ID, RESTAURANT_ID), sharedCart));
        WalletContributionRequest request = new WalletContributionRequest(new BigDecimal("2.00"));

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.getWalletBalance(USER_ID)).thenReturn(new BigDecimal("10.00"));
        when(sharedCartRepository.findByJoinCode(JOIN_CODE)).thenReturn(sharedCart);
        when(sharedCartMemberRepository.findByUserIdAndJoinCode(JOIN_CODE, USER_ID)).thenReturn(newMember(USER_ID, newCart(USER_ID, RESTAURANT_ID), sharedCart));

        // Act
        assertThrows(IllegalArgumentException.class,
                () -> sharedCartService.contributeToSharedCart(JOIN_CODE, request));

        // Assert
        verify(userFacade, never()).deductWalletBalance(any(), any());
    }

    @Test
    void contributeToSharedCart_memberCartNullInMemberList_handlesSafely() {
        // Arrange
        SharedCart sharedCart = newSharedCart(HOST_ID, RESTAURANT_ID, JOIN_CODE);
        sharedCart.setHostPaysAll(false);
        sharedCart.setTotalPrice(new BigDecimal("0.00"));
        SharedCartMember listMember = newMember(USER_ID, null, sharedCart);
        sharedCart.getMemberList().add(listMember);
        SharedCartMember contributor = newMember(USER_ID, newCart(USER_ID, RESTAURANT_ID), sharedCart);
        WalletContributionRequest request = new WalletContributionRequest(BigDecimal.ZERO);
        SharedCartResponse response = new SharedCartResponse();

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.getWalletBalance(USER_ID)).thenReturn(new BigDecimal("10.00"));
        when(sharedCartRepository.findByJoinCode(JOIN_CODE)).thenReturn(sharedCart);
        when(sharedCartMemberRepository.findByUserIdAndJoinCode(JOIN_CODE, USER_ID)).thenReturn(contributor);
        when(sharedCartRepository.save(any(SharedCart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderingMapper.toSharedCartResponse(any(SharedCart.class), eq(USER_ID))).thenReturn(response);

        // Act
        SharedCartResponse result = sharedCartService.contributeToSharedCart(JOIN_CODE, request);

        // Assert
        assertSame(response, result);
        assertEquals(BigDecimal.ZERO, sharedCart.getTotalPrice());
        verify(sharedCartRepository, times(2)).save(sharedCart);
    }

    @Test
    void contributeToSharedCart_sharedCartMemberListNull_throwsNullPointerException() {
        // Arrange
        SharedCart sharedCart = newSharedCart(HOST_ID, RESTAURANT_ID, JOIN_CODE);
        sharedCart.setHostPaysAll(false);
        sharedCart.setMemberList(null);
        WalletContributionRequest request = new WalletContributionRequest(BigDecimal.ZERO);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(userFacade.getWalletBalance(USER_ID)).thenReturn(new BigDecimal("10.00"));
        when(sharedCartRepository.findByJoinCode(JOIN_CODE)).thenReturn(sharedCart);
        when(sharedCartMemberRepository.findByUserIdAndJoinCode(JOIN_CODE, USER_ID)).thenReturn(newMember(USER_ID, newCart(USER_ID, RESTAURANT_ID), sharedCart));

        // Act
        assertThrows(NullPointerException.class,
                () -> sharedCartService.contributeToSharedCart(JOIN_CODE, request));

        // Assert
        verify(sharedCartRepository, never()).save(any());
    }

    @Test
    void contributeToSharedCart_unauthorized_throwsAccessDeniedException() {
        // Arrange
        WalletContributionRequest request = new WalletContributionRequest(new BigDecimal("1.00"));
        when(userAuthorization.authorizeUserId()).thenThrow(new AccessDeniedException("No valid authority"));

        // Act
        assertThrows(AccessDeniedException.class,
                () -> sharedCartService.contributeToSharedCart(JOIN_CODE, request));

        // Assert
        verify(sharedCartRepository, never()).findByJoinCode(any());
    }

    @Test
    void checkoutSharedCart_happyPath_createsOrderAndClosesSharedCart() {
        // Arrange
        PlaceOrderRequest request = new PlaceOrderRequest(77L, null, true);
        SharedCart sharedCart = newSharedCart(USER_ID, RESTAURANT_ID, JOIN_CODE);
        sharedCart.setTotalPrice(new BigDecimal("40.00"));
        sharedCart.setAmountPaid(new BigDecimal("10.00"));
        Cart cart1 = newCart(USER_ID, RESTAURANT_ID);
        cart1.getItems().add(newCartItem(1L, 2));
        Cart cart2 = newCart(99L, RESTAURANT_ID);
        cart2.getItems().add(newCartItem(2L, 3));
        SharedCartMember member1 = newMember(USER_ID, cart1, sharedCart);
        SharedCartMember member2 = newMember(99L, cart2, sharedCart);
        sharedCart.getMemberList().add(member1);
        sharedCart.getMemberList().add(member2);
        OrderItem orderItem1 = new OrderItem();
        OrderItem orderItem2 = new OrderItem();

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartRepository.findByJoinCode(JOIN_CODE)).thenReturn(sharedCart);
        when(userFacade.isAddressOwnedByUser(USER_ID, 77L)).thenReturn(true);
        when(userFacade.getWalletBalance(USER_ID)).thenReturn(new BigDecimal("50.00"));
        when(orderService.createOrderItem(any(CartItem.class), any(Order.class)))
                .thenReturn(orderItem1, orderItem2);

        // Act
        String result = sharedCartService.checkoutSharedCart(JOIN_CODE, request);

        // Assert
        assertTrue(result.contains("placed"));
        verify(userFacade).deductWalletBalance(USER_ID, new BigDecimal("30.00"));
        verify(orderRepository, times(2)).save(any(Order.class));
        verify(cartRepository, times(2)).save(any(Cart.class));
        verify(sharedCartMemberRepository).saveAll(sharedCart.getMemberList());
        verify(sharedCartRepository).save(sharedCart);
        assertFalse(member1.isActive());
        assertTrue(cart1.getItems().isEmpty());
        assertFalse(sharedCart.isActive());
    }

    @Test
    void checkoutSharedCart_sharedCartNotFound_throwsEntityNotFoundException() {
        // Arrange
        PlaceOrderRequest request = new PlaceOrderRequest(77L, null, false);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartRepository.findByJoinCode(JOIN_CODE)).thenReturn(null);

        // Act
        assertThrows(EntityNotFoundException.class,
                () -> sharedCartService.checkoutSharedCart(JOIN_CODE, request));

        // Assert
        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkoutSharedCart_userNotHost_throwsAccessDeniedException() {
        // Arrange
        PlaceOrderRequest request = new PlaceOrderRequest(77L, null, false);
        SharedCart sharedCart = newSharedCart(HOST_ID, RESTAURANT_ID, JOIN_CODE);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartRepository.findByJoinCode(JOIN_CODE)).thenReturn(sharedCart);

        // Act
        assertThrows(AccessDeniedException.class,
                () -> sharedCartService.checkoutSharedCart(JOIN_CODE, request));

        // Assert
        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkoutSharedCart_addressNotOwned_throwsEntityNotFoundException() {
        // Arrange
        PlaceOrderRequest request = new PlaceOrderRequest(77L, null, false);
        SharedCart sharedCart = newSharedCart(USER_ID, RESTAURANT_ID, JOIN_CODE);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartRepository.findByJoinCode(JOIN_CODE)).thenReturn(sharedCart);
        when(userFacade.isAddressOwnedByUser(USER_ID, 77L)).thenReturn(false);

        // Act
        assertThrows(EntityNotFoundException.class,
                () -> sharedCartService.checkoutSharedCart(JOIN_CODE, request));

        // Assert
        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkoutSharedCart_insufficientWallet_throwsIllegalArgumentException() {
        // Arrange
        PlaceOrderRequest request = new PlaceOrderRequest(77L, null, false);
        SharedCart sharedCart = newSharedCart(USER_ID, RESTAURANT_ID, JOIN_CODE);
        sharedCart.setTotalPrice(new BigDecimal("30.00"));
        sharedCart.setAmountPaid(new BigDecimal("5.00"));

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartRepository.findByJoinCode(JOIN_CODE)).thenReturn(sharedCart);
        when(userFacade.isAddressOwnedByUser(USER_ID, 77L)).thenReturn(true);
        when(userFacade.getWalletBalance(USER_ID)).thenReturn(new BigDecimal("10.00"));

        // Act
        assertThrows(IllegalArgumentException.class,
                () -> sharedCartService.checkoutSharedCart(JOIN_CODE, request));

        // Assert
        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkoutSharedCart_memberCartNull_throwsNullPointerException() {
        // Arrange
        PlaceOrderRequest request = new PlaceOrderRequest(77L, null, false);
        SharedCart sharedCart = newSharedCart(USER_ID, RESTAURANT_ID, JOIN_CODE);
        SharedCartMember member = newMember(USER_ID, null, sharedCart);
        sharedCart.getMemberList().add(member);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartRepository.findByJoinCode(JOIN_CODE)).thenReturn(sharedCart);
        when(userFacade.isAddressOwnedByUser(USER_ID, 77L)).thenReturn(true);
        when(userFacade.getWalletBalance(USER_ID)).thenReturn(new BigDecimal("10.00"));

        // Act
        assertThrows(NullPointerException.class,
                () -> sharedCartService.checkoutSharedCart(JOIN_CODE, request));

        // Assert
        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkoutSharedCart_cartItemsNull_throwsNullPointerException() {
        // Arrange
        PlaceOrderRequest request = new PlaceOrderRequest(77L, null, false);
        SharedCart sharedCart = newSharedCart(USER_ID, RESTAURANT_ID, JOIN_CODE);
        Cart cart = newCart(USER_ID, RESTAURANT_ID);
        cart.setItems(null);
        SharedCartMember member = newMember(USER_ID, cart, sharedCart);
        sharedCart.getMemberList().add(member);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartRepository.findByJoinCode(JOIN_CODE)).thenReturn(sharedCart);
        when(userFacade.isAddressOwnedByUser(USER_ID, 77L)).thenReturn(true);
        when(userFacade.getWalletBalance(USER_ID)).thenReturn(new BigDecimal("10.00"));

        // Act
        assertThrows(NullPointerException.class,
                () -> sharedCartService.checkoutSharedCart(JOIN_CODE, request));

        // Assert
        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkoutSharedCart_sharedCartMemberListNull_throwsNullPointerException() {
        // Arrange
        PlaceOrderRequest request = new PlaceOrderRequest(77L, null, false);
        SharedCart sharedCart = newSharedCart(USER_ID, RESTAURANT_ID, JOIN_CODE);
        sharedCart.setMemberList(null);

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartRepository.findByJoinCode(JOIN_CODE)).thenReturn(sharedCart);
        when(userFacade.isAddressOwnedByUser(USER_ID, 77L)).thenReturn(true);
        when(userFacade.getWalletBalance(USER_ID)).thenReturn(new BigDecimal("10.00"));

        // Act
        assertThrows(NullPointerException.class,
                () -> sharedCartService.checkoutSharedCart(JOIN_CODE, request));

        // Assert
        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkoutSharedCart_unauthorized_throwsAccessDeniedException() {
        // Arrange
        PlaceOrderRequest request = new PlaceOrderRequest(77L, null, false);
        when(userAuthorization.authorizeUserId()).thenThrow(new AccessDeniedException("No valid authority"));

        // Act
        assertThrows(AccessDeniedException.class,
                () -> sharedCartService.checkoutSharedCart(JOIN_CODE, request));

        // Assert
        verify(sharedCartRepository, never()).findByJoinCode(any());
    }

    @Test
    void checkoutSharedCart_orderFields_populatedCorrectly() {
        // Arrange
        PlaceOrderRequest request = new PlaceOrderRequest(77L, LocalDateTime.of(2026, 6, 4, 12, 0), true);
        SharedCart sharedCart = newSharedCart(USER_ID, RESTAURANT_ID, JOIN_CODE);
        sharedCart.setTotalPrice(new BigDecimal("30.00"));
        sharedCart.setAmountPaid(new BigDecimal("5.00"));
        Cart cart = newCart(USER_ID, RESTAURANT_ID);
        cart.getItems().add(newCartItem(1L, 2));
        SharedCartMember member = newMember(USER_ID, cart, sharedCart);
        sharedCart.getMemberList().add(member);
        OrderItem orderItem = new OrderItem();

        when(userAuthorization.authorizeUserId()).thenReturn(USER_ID);
        when(sharedCartRepository.findByJoinCode(JOIN_CODE)).thenReturn(sharedCart);
        when(userFacade.isAddressOwnedByUser(USER_ID, 77L)).thenReturn(true);
        when(userFacade.getWalletBalance(USER_ID)).thenReturn(new BigDecimal("50.00"));
        when(orderService.createOrderItem(any(CartItem.class), any(Order.class))).thenReturn(orderItem);

        // Act
        sharedCartService.checkoutSharedCart(JOIN_CODE, request);

        // Assert
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(2)).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertEquals(USER_ID, savedOrder.getUserId());
        assertEquals(RESTAURANT_ID, savedOrder.getRestaurantId());
        assertEquals(new BigDecimal("30.00"), savedOrder.getTotalPrice());
        assertEquals(OrderStatus.CREATED, savedOrder.getOrderStatus());
        assertEquals(PaymentStatus.SUCCESS, savedOrder.getPaymentStatus());
        assertEquals(2, savedOrder.getTotalQuantity());
        assertEquals(request.getAddressId(), savedOrder.getDeliveryAddressId());
        assertTrue(savedOrder.isSpecial());
    }

    private SharedCart newSharedCart(Long hostId, Long restaurantId, String joinCode) {
        SharedCart sharedCart = new SharedCart();
        sharedCart.setHostUserId(hostId);
        sharedCart.setRestaurantId(restaurantId);
        sharedCart.setJoinCode(joinCode);
        sharedCart.setActive(true);
        sharedCart.setHostPaysAll(false);
        sharedCart.setTotalPrice(BigDecimal.ZERO);
        sharedCart.setAmountPaid(BigDecimal.ZERO);
        sharedCart.setMemberList(new ArrayList<>());
        return sharedCart;
    }

    private Cart newCart(Long userId, Long restaurantId) {
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setRestaurantId(restaurantId);
        cart.setItems(new ArrayList<>());
        cart.setTotalPrice(BigDecimal.ZERO);
        return cart;
    }

    private SharedCartMember newMember(Long userId, Cart cart, SharedCart sharedCart) {
        SharedCartMember member = new SharedCartMember();
        member.setUserId(userId);
        member.setCart(cart);
        member.setSharedCart(sharedCart);
        member.setWalletContribution(BigDecimal.ZERO);
        member.setActive(true);
        if (cart != null) {
            cart.setSharedCart(sharedCart);
        }
        return member;
    }

    private CartItem newCartItem(Long id, int quantity) {
        CartItem item = new CartItem();
        item.setCartItemId(id);
        item.setQuantity(quantity);
        return item;
    }
}

