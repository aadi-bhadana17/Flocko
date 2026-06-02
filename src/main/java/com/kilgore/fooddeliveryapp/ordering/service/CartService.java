package com.kilgore.fooddeliveryapp.ordering.service;

import com.kilgore.fooddeliveryapp.catalog.api.CatalogFacade;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.FoodSummary;
import com.kilgore.fooddeliveryapp.common.util.UserAuthorization;
import com.kilgore.fooddeliveryapp.identity.api.UserFacade;
import com.kilgore.fooddeliveryapp.ordering.dto.request.AddToCartRequest;
import com.kilgore.fooddeliveryapp.ordering.dto.request.UpdateCartItemRequest;
import com.kilgore.fooddeliveryapp.ordering.dto.response.CartResponse;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.AddonSummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.identity.dto.summary.UserSummary;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityMisMatchAssociationException;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityUnavailableException;
import com.kilgore.fooddeliveryapp.ordering.mapper.OrderingMapper;
import com.kilgore.fooddeliveryapp.ordering.model.Cart;
import com.kilgore.fooddeliveryapp.ordering.model.CartItem;
import com.kilgore.fooddeliveryapp.ordering.model.SharedCartMember;
import com.kilgore.fooddeliveryapp.ordering.repository.CartItemRepository;
import com.kilgore.fooddeliveryapp.ordering.repository.CartRepository;
import com.kilgore.fooddeliveryapp.ordering.repository.SharedCartMemberRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final PricingService pricingService;
    private final UserAuthorization userAuthorization;
    private final SharedCartMemberRepository sharedCartMemberRepository;
    private final CatalogFacade catalogFacade;
    private final UserFacade userFacade;
    private final OrderingMapper orderingMapper;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                       PricingService pricingService, UserAuthorization userAuthorization,
                       SharedCartMemberRepository sharedCartMemberRepository, CatalogFacade catalogFacade,
                       UserFacade userFacade, OrderingMapper orderingMapper) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.pricingService = pricingService;
        this.userAuthorization = userAuthorization;
        this.sharedCartMemberRepository = sharedCartMemberRepository;
        this.catalogFacade = catalogFacade;
        this.userFacade = userFacade;
        this.orderingMapper = orderingMapper;
    }


    // -------------------------------------------------------- CART OPERATIONS ------------------------------------------------------------------------


    @Transactional
    public CartResponse addToCart(AddToCartRequest request) {

        Long userId = userAuthorization.authorizeUserId();
        Cart cart = getOrCreateCart(userId);

        FoodSummary food = catalogFacade.getFood(request.getFoodId());
        RestaurantSummary restaurant = catalogFacade.getRestaurant(food.getRestaurantId());

        assignOrVerifyRestaurant(cart, restaurant, food);
        addOrMergeCartItem(cart, request, food);

        return getCartResponse(cart, false);
    }

    public CartResponse getCart() {

        Long userId = userAuthorization.authorizeUserId();
        Cart cart = getUserCart(userId);

        boolean priceUpdated = synchronizeCartPrices(cart);

        return getCartResponse(cart, priceUpdated);
    }

    @Transactional
    public CartResponse updateCartItemQuantity(Long cartItemId, UpdateCartItemRequest request) {
        Long userId = userAuthorization.authorizeUserId();
        Cart cart = getUserCart(userId);

        CartItem item = verifyCartItem(cartItemId, cart);

        item.setQuantity(request.getQuantity());
        item.setItemTotal(pricingService.calculateItemTotal(item));

        pricingService.recalculateCartTotals(cart);

        return getCartResponse(cart, true);
    }

    @Transactional
    public CartResponse removeCartItem(Long cartItemId) {
        Long userId = userAuthorization.authorizeUserId();
        Cart cart = getUserCart(userId);

        CartItem item = verifyCartItem(cartItemId, cart);

        cart.getItems().remove(item);
        pricingService.recalculateCartTotals(cart);

        return getCartResponse(cart, true);
    }

    @Transactional
    public String clearCart() {
        Long userId = userAuthorization.authorizeUserId();
        Cart cart = getUserCart(userId);

        cart.getItems().clear();
        pricingService.recalculateCartTotals(cart);

        return "Cart has been cleared";
    }


    // ------------------------------------------------------------- CART ACCESS -------------------------------------------------------------------------


    protected CartItem  verifyCartItem(Long cartItemId, Cart cart) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new EntityNotFoundException("No item found with id " + cartItemId));

        if(!item.getCart().equals(cart)){
            throw new AccessDeniedException("You are not allowed to update this cart item, " +
                    "because this cart doesn't belongs to you");
        }

        return item;
    }

    private Cart getUserCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId);

        if(cart == null)
            throw new EntityNotFoundException("No user cart found with id " + userId);
        if(cart.getItems().isEmpty())
            throw new EntityUnavailableException("Your cart is empty, add items now");


        return cart;
    }

    private Cart getWorkingCartForUser(Long userId) {
        SharedCartMember activeMembership = sharedCartMemberRepository.findActiveMemberByUserId(userId);
        if (activeMembership != null && activeMembership.getCart() != null) {
            return activeMembership.getCart();
        }
        return cartRepository.findByUserId(userId);
    }


    // ----------------------------------------------------------- PRICE SYNC ------------------------------------------------------------------------------

    private boolean synchronizeCartPrices(Cart cart) {

        boolean priceUpdated = pricingService.refreshExpiredPrices(cart);

        if(priceUpdated) pricingService.recalculateCartTotals(cart);

        return priceUpdated;
    }

    // -------------------------------------------------------------- MAPPING ------------------------------------------------------------------------------


    private CartResponse getCartResponse(Cart cart, boolean priceUpdated) {
        UserSummary user = userFacade.getUserById(cart.getUserId());
        RestaurantSummary restaurant = catalogFacade.getRestaurant(cart.getRestaurantId());

        return orderingMapper.toCartResponse(cart, user, restaurant, getMessage(priceUpdated));
    }

    private String getMessage(boolean priceUpdated) {
        return  priceUpdated ?
                "Some prices have been updated based on current price " +
                        "in restaurant or due to change in quantity"
                : null;
    }


    // -------------------------------------------------------------- ITEM LOGIC ---------------------------------------------------------------------------------


    private void mergeItem(CartItem cartItem, AddToCartRequest request) {
        cartItem.setQuantity(request.getQuantity() + cartItem.getQuantity());
        cartItem.setItemTotal(pricingService.calculateItemTotal(cartItem));
    }

    private boolean haveSameAddons(List<Long> list1,  List<Long> list2) {
        if(list1.size() != list2.size()) return false;

        return new HashSet<>(list1).equals(new HashSet<>(list2));
    }

    protected void addOrMergeCartItem(Cart cart, AddToCartRequest request, FoodSummary food) {

        List<AddonSummary> addonSummaries = catalogFacade.getAddons(request.getAddonIds());
        BigDecimal currentPrice = pricingService.calculateCurrentPrice(food, addonSummaries);

        Optional<CartItem> existingItem = findMatchingCartItem(cart, food, request, currentPrice);

        if (existingItem.isPresent())
            mergeItem(existingItem.get(), request);
        else
            cart.getItems().add(createCartItem(cart, request));


        pricingService.recalculateCartTotals(cart);
    }

    private Optional<CartItem> findMatchingCartItem(Cart cart, FoodSummary food,
                                                    AddToCartRequest request, BigDecimal currentPrice) {
        return cart.getItems().stream()
                .filter(item -> item.getFoodId().equals(food.getFoodId()))
                .filter(item -> item.getPriceAtAddition().equals(currentPrice))
                .filter(item -> haveSameAddons(item.getAddonIds(), request.getAddonIds()))
                .findFirst();
    }

    private CartItem createCartItem(Cart cart, AddToCartRequest request) {
        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setFoodId(request.getFoodId());
        cartItem.setQuantity(request.getQuantity());
        cartItem.setAddonIds(request.getAddonIds());
        cartItem.setPriceAtAddition(pricingService.calculatePriceAtAddition(cartItem));
        cartItem.setItemTotal(pricingService.calculateItemTotal(cartItem));
        cartItem.setAddedTime(LocalDateTime.now());

        return cartItem;
    }


    // ----------------------------------------------------------- RESTAURANT RULE -----------------------------------------------------------------------------------


    private void assignOrVerifyRestaurant(Cart cart, RestaurantSummary restaurant, FoodSummary food) {
        if(cart.getRestaurantId() == null)
            cart.setRestaurantId(food.getRestaurantId());
        else
            ensureSameRestaurant(cart, restaurant, food);
    }

    private Cart getOrCreateCart(Long userId) {
        Cart cart = getWorkingCartForUser(userId);

        if (cart == null) {
            cart = new Cart();
            cart.setUserId(userId);
        }
        return cart;
    }

    private void ensureSameRestaurant(Cart cart, RestaurantSummary restaurant, FoodSummary food) {
        if(!cart.getRestaurantId().equals(restaurant.getRestaurantId())){

            String message = ("Food [id = %d, name = %s] doesn't belongs to the restaurant [id = %d, name = %s] " +
                    "Please empty your cart before adding this item.")
                    .formatted(
                            food.getFoodId(),
                            food.getFoodName(),
                            cart.getRestaurantId(),
                            restaurant.getRestaurantName()
                    );

            throw new EntityMisMatchAssociationException(message);
        }
    }
}
