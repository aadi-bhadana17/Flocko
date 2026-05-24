package com.kilgore.fooddeliveryapp.ordering.service;

import com.kilgore.fooddeliveryapp.catalog.model.Addon;
import com.kilgore.fooddeliveryapp.catalog.model.Food;
import com.kilgore.fooddeliveryapp.catalog.model.Restaurant;
import com.kilgore.fooddeliveryapp.catalog.repository.AddonRepository;
import com.kilgore.fooddeliveryapp.catalog.repository.FoodRepository;
import com.kilgore.fooddeliveryapp.common.util.UserAuthorization;
import com.kilgore.fooddeliveryapp.ordering.dto.request.AddToCartRequest;
import com.kilgore.fooddeliveryapp.ordering.dto.request.UpdateCartItemRequest;
import com.kilgore.fooddeliveryapp.ordering.dto.response.CartResponse;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.AddonSummary;
import com.kilgore.fooddeliveryapp.ordering.dto.summary.CartItemSummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.identity.dto.summary.UserSummary;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityMisMatchAssociationException;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityUnavailableException;
import com.kilgore.fooddeliveryapp.identity.model.User;
import com.kilgore.fooddeliveryapp.identity.repository.UserRepository;
import com.kilgore.fooddeliveryapp.ordering.model.Cart;
import com.kilgore.fooddeliveryapp.ordering.model.CartItem;
import com.kilgore.fooddeliveryapp.ordering.model.SharedCartMember;
import com.kilgore.fooddeliveryapp.ordering.repository.CartItemRepository;
import com.kilgore.fooddeliveryapp.ordering.repository.CartRepository;
import com.kilgore.fooddeliveryapp.ordering.repository.SharedCartMemberRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final FoodRepository foodRepository;
    private final AddonRepository addonRepository;
    private final CartItemRepository cartItemRepository;
    private final PricingService pricingService;
    private final UserAuthorization userAuthorization;
    private final SharedCartMemberRepository sharedCartMemberRepository;

    public CartService(CartRepository cartRepository, UserRepository userRepository,
                       FoodRepository foodRepository, AddonRepository addonRepository,
                       CartItemRepository cartItemRepository, PricingService pricingService, UserAuthorization userAuthorization, SharedCartMemberRepository sharedCartMemberRepository) {
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.foodRepository = foodRepository;
        this.addonRepository = addonRepository;
        this.cartItemRepository = cartItemRepository;
        this.pricingService = pricingService;
        this.userAuthorization = userAuthorization;
        this.sharedCartMemberRepository = sharedCartMemberRepository;
    }


    // -------------------------------------------------------- CART OPERATIONS ------------------------------------------------------------------------


    @Transactional
    public CartResponse addToCart(AddToCartRequest request) {

        User user = userAuthorization.authorizeUser();
        Cart cart = getWorkingCartForUser(user);

        if (cart == null) {
            cart = new Cart();
            cart.setUser(user);
        }

        Food food = extractFoodFromRequest(request);
        List<Addon> addonList = extractAddonsFromRequest(request, food);

        Restaurant restaurant = food.getRestaurant();

        if(cart.getRestaurant() != null && !cart.getRestaurant().equals(restaurant)){

            String message = ("Food [id = %d, name = %s] doesn't belongs to the restaurant [id = %d, name = %s] " +
                                "Please empty your cart before adding this item.")
                    .formatted(
                            food.getFoodId(),
                            food.getFoodName(),
                            cart.getRestaurant().getRestaurantId(),
                            cart.getRestaurant().getRestaurantName()
                    );

            throw new EntityMisMatchAssociationException(message);
        }

        if(cart.getRestaurant() == null){
            cart.setRestaurant(food.getRestaurant());
        }

        addOrMergeCartItem(cart, food, addonList,  request);

        return createCartResponse(cart, false);
    }

    public CartResponse getCart() {

        User user = userAuthorization.authorizeUser();

        Cart cart = cartRepository.findByUser(user);

        boolean priceUpdated = false;
        if (cart != null) {
            priceUpdated = pricingService.refreshExpiredPrices(cart);
            pricingService.updateCartTotal(cart);
        }

        return createCartResponse(cart, priceUpdated);
    }

    @Transactional
    public CartResponse updateCart(Long cartItemId, UpdateCartItemRequest request) {

        Cart cart = verifyCart();
        CartItem item = verifyCartItem(cartItemId, cart);

        item.setQuantity(request.getQuantity());
        item.setItemTotal(pricingService.calculateItemTotal(item));

        pricingService.updateCartTotal(cart);
        cartItemRepository.save(item);

        return createCartResponse(cart, true);
    }

    @Transactional
    public CartResponse removeCartItem(Long cartItemId) {
        Cart cart = verifyCart();
        CartItem item = verifyCartItem(cartItemId, cart);

        cart.getItems().remove(item);
        pricingService.updateCartTotal(cart);

        return createCartResponse(cart, true);
    }

    @Transactional
    public String clearCart() {
        Cart cart = verifyCart();

        cart.getItems().clear();
        pricingService.updateCartTotal(cart);

        return "Cart has been cleared";
    }


    // ------------------------------------------------------------- VALIDATION -------------------------------------------------------------------------


    private String authenticateUser() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        return authentication.getName();
    }

    public CartItem  verifyCartItem(Long cartItemId, Cart cart) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new EntityNotFoundException("No item found with id " + cartItemId));

        if(!item.getCart().equals(cart)){
            throw new AccessDeniedException("You are not allowed to update this cart item, " +
                    "because this cart doesn't belongs to you");
        }

        return item;
    }

    private Cart verifyCart() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByEmail(username);

        Cart cart = cartRepository.findByUser(user);
        if (cart == null) {
            throw new EntityNotFoundException("Cart not found");
        }
        if(cart.getItems().isEmpty()){
            throw new EntityUnavailableException("Your cart is empty, add items now");
        }
        return cart;
    }


    // -------------------------------------------------------------- MAPPING ------------------------------------------------------------------------------


    private CartResponse createCartResponse(Cart cart, boolean priceUpdated) {

        if(cart == null) return new CartResponse(null, null, null,
                0, BigDecimal.ZERO, null, "Your cart is empty");

        UserSummary user = new UserSummary(
                cart.getUser().getUserId(),
                cart.getUser().getFirstName() + " " +cart.getUser().getLastName()
        );

        RestaurantSummary restaurant = cart.getRestaurant() != null ?
                new RestaurantSummary(
                        cart.getRestaurant().getRestaurantId(),
                        cart.getRestaurant().getRestaurantName(),
                        cart.getRestaurant().getCuisineType(),
                        cart.getRestaurant().getAvgRating()
                ) : new RestaurantSummary();

        List<CartItemSummary> cartItems = cart.getItems().stream()
                .map(item -> createCartItemSummary(item, item.getAddons()))
                .toList();

        String message = priceUpdated ? "Some prices have been updated based on current price in restaurant or due to change in quantity" : null;

        return new CartResponse(
                cart.getCartId(),
                user,
                cartItems,
                cart.getTotalQuantity(),
                cart.getTotalPrice(),
                restaurant,
                message
        );
    }

    private CartItemSummary createCartItemSummary(CartItem item, List<Addon> addons) {

        List<AddonSummary> addonSummaries = createListOfAddonsSummary(addons);

        return new CartItemSummary(
                item.getCartItemId(),
                item.getFood().getFoodId(),
                item.getFood().getFoodName(),
                item.getQuantity(),
                addonSummaries,
                item.getItemTotal()
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


    // ------------------------------------------------------------- HELPERS ------------------------------------------------------------------------------


    public boolean haveSameAddons(List<Addon> list1,  List<Addon> list2) {
        if(list1.size() != list2.size()) return false;

        Set<Long> set1 = list1.stream().map(Addon::getAddonId).collect(Collectors.toSet());
        Set<Long> set2 = list2.stream().map(Addon::getAddonId).collect(Collectors.toSet());

        return set1.equals(set2);
    }

    public Food extractFoodFromRequest(AddToCartRequest request) {

        return foodRepository.findById(request.getFoodId())
                .orElseThrow(() -> new EntityNotFoundException("Food with id " + request.getFoodId() + " not found"));
    }

    public List<Addon> extractAddonsFromRequest(AddToCartRequest request, Food food) {
        return request.getAddonIds().stream()
                .map((id) -> extractAddon(id, food))
                .toList();
    }

    public Addon extractAddon(Long addonId, Food food) {
        Addon addon = addonRepository.findById(addonId)
                .orElseThrow(() -> new EntityNotFoundException("Addon with id " + addonId + " not found"));

        if(addon.getCategories().stream()
                .noneMatch(category -> category.equals(food.getFoodCategory()))) {

            String message = String.format(
                    "Invalid association : Addon [id= %d, name= %s] is not available for Food [id= %d, name= %s]"
                            .formatted(
                                    addonId,
                                    addon.getAddonName(),
                                    food.getFoodId(),
                                    food.getFoodName()
                            )
            );
            throw new EntityUnavailableException(message);
        }

        if(!addon.isAvailable()) {
            String message = String.format(
                    "Addon [id=%d, name=%s] is not available at this moment",
                    addon.getAddonId(),
                    addon.getAddonName()
            );

            throw new EntityUnavailableException(message);
        }

        return addon;
    }

    public void addOrMergeCartItem(Cart cart, Food food, List<Addon> addonList, AddToCartRequest request) {

        BigDecimal currentPrice = pricingService.calculateCurrentPrice(food, addonList);

        Optional<CartItem> existingCartItem = cart.getItems().stream()
                .filter(item -> item.getFood().equals(food))
                .filter(item -> item.getPriceAtAddition().equals(currentPrice))
                .filter(item -> haveSameAddons(item.getAddons(), addonList))
                .findFirst();

        if (existingCartItem.isPresent()) {
            CartItem cartItem = existingCartItem.get();

            cartItem.setQuantity(request.getQuantity() + cartItem.getQuantity());
            cartItem.setItemTotal(pricingService.calculateItemTotal(cartItem));

        }
        else {
            CartItem cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setFood(food);
            cartItem.setQuantity(request.getQuantity());
            cartItem.setAddons(addonList);
            cartItem.setPriceAtAddition(pricingService.calculatePriceAtAddition(cartItem));
            cartItem.setItemTotal(pricingService.calculateItemTotal(cartItem));
            cartItem.setAddedTime(LocalDateTime.now());

            cart.getItems().add(cartItem);
        }

        cart.setTotalPrice(pricingService.calculateCartTotal(cart));
        cart.setTotalQuantity(calculateTotalQuantity(cart));
        cartRepository.save(cart);
    }

    public int calculateTotalQuantity(Cart  cart) {
        return cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    private Cart getWorkingCartForUser(User user) {
        SharedCartMember activeMembership = sharedCartMemberRepository.findActiveMemberByUserId(user.getUserId());
        if (activeMembership != null && activeMembership.getCart() != null) {
            return activeMembership.getCart();
        }
        return cartRepository.findByUser(user);
    }
}
