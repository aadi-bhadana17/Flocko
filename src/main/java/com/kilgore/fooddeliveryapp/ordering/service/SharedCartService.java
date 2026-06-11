package com.kilgore.fooddeliveryapp.ordering.service;


import com.kilgore.fooddeliveryapp.catalog.api.CatalogFacade;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.FoodSummary;
import com.kilgore.fooddeliveryapp.common.enums.PaymentStatus;
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
import com.kilgore.fooddeliveryapp.ordering.model.*;
import com.kilgore.fooddeliveryapp.ordering.repository.CartRepository;
import com.kilgore.fooddeliveryapp.ordering.repository.OrderRepository;
import com.kilgore.fooddeliveryapp.ordering.repository.SharedCartMemberRepository;
import com.kilgore.fooddeliveryapp.ordering.repository.SharedCartRepository;
import com.kilgore.fooddeliveryapp.wallet.dto.request.WalletContributionRequest;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class SharedCartService {

    private final SharedCartRepository sharedCartRepository;
    private final UserAuthorization userAuthorization;
    private final SharedCartMemberRepository sharedCartMemberRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final PricingService pricingService;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final UserFacade userFacade;
    private final CatalogFacade catalogFacade;
    private final OrderingMapper orderingMapper;


    public SharedCartService(SharedCartRepository sharedCartRepository, UserAuthorization userAuthorization,
                             SharedCartMemberRepository sharedCartMemberRepository, CartRepository cartRepository,
                             CartService cartService, PricingService pricingService, OrderRepository orderRepository,
                             OrderService orderService, UserFacade userFacade, CatalogFacade catalogFacade, OrderingMapper orderingMapper) {
        this.sharedCartRepository = sharedCartRepository;
        this.userAuthorization = userAuthorization;
        this.sharedCartMemberRepository = sharedCartMemberRepository;
        this.cartRepository = cartRepository;

        this.cartService = cartService;
        this.pricingService = pricingService;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.userFacade = userFacade;
        this.catalogFacade = catalogFacade;
        this.orderingMapper = orderingMapper;
    }

    @Transactional
    public SharedCartResponse createSharedCart(SharedCartRequest request) {
        Long userId = userAuthorization.authorizeUserId();

        verifyUserAccountStatus(userId);

        SharedCartMember existingMembership = sharedCartMemberRepository.findActiveMemberByUserId(userId);
        if(existingMembership != null)
            return orderingMapper.toSharedCartResponse(existingMembership.getSharedCart(), userId);

        SharedCart sharedCart = sharedCartRepository.findByHostUserId(userId);
        if(sharedCart != null && sharedCart.isActive())
            return orderingMapper.toSharedCartResponse(sharedCart, userId);

        sharedCart = createNewSharedCart(userId, request);

        Cart cart = prepareCartForSharedCart(userId, request, sharedCart);

        createNewSharedCartMember(sharedCart, userId, cart);

        return orderingMapper.toSharedCartResponse(sharedCart, userId);
    }

    public SharedCartResponse getCurrentSharedCart() {
        Long userId =  userAuthorization.authorizeUserId();

        SharedCartMember activeMembership = sharedCartMemberRepository.findActiveMemberByUserId(userId);
        if(activeMembership != null)
            return orderingMapper.toSharedCartResponse(activeMembership.getSharedCart(), userId);

        SharedCart sharedCart = sharedCartRepository.findByHostUserId(userId);
        if(sharedCart != null && sharedCart.isActive())
            return orderingMapper.toSharedCartResponse(sharedCart, userId);

        throw new EntityNotFoundException("You are not a member of any active shared cart right now.");
    }

    public SharedCartResponse getSharedCartByCode(String code) {
        Long userId  = userAuthorization.authorizeUserId();

        SharedCart sharedCart = sharedCartRepository.findByJoinCode(code);
        if(sharedCart == null)
            throw new EntityNotFoundException("No shared cart found with the provided code.");

        return orderingMapper.toSharedCartResponse(sharedCart, userId);
    }

    @Transactional
    public String joinSharedCart(String code) {
        Long userId = userAuthorization.authorizeUserId();

        verifyUserAccountStatus(userId);

        SharedCartMember existingMembership = sharedCartMemberRepository.findActiveMemberByUserId(userId);
        if(existingMembership != null)
            throw new AccessDeniedException("You are already a member of another shared cart.");

        SharedCart sharedCart = sharedCartRepository.findByJoinCode(code);
        if(sharedCart == null)
            throw new EntityNotFoundException("Shared cart not found with the provided code.");

        if(sharedCart.getHostUserId().equals(userId))
            throw new AccessDeniedException("You cannot join your own shared cart.");

        Cart cart = cartRepository.findByUserId(userId);
        if (cart != null) {
            if (cart.getRestaurantId() != null && !cart.getRestaurantId().equals(sharedCart.getRestaurantId()) && !cart.getItems().isEmpty()) {
                throw new EntityMisMatchAssociationException("Your current cart belongs to another restaurant. Please clear it before joining this shared cart.");
            }
        } else {
            cart = new Cart();
            cart.setUserId(userId);
            cart.setRestaurantId(sharedCart.getRestaurantId());
        }
        if (cart.getRestaurantId() == null) {
            cart.setRestaurantId(sharedCart.getRestaurantId());
        }
        cart.setSharedCart(sharedCart);
        cartRepository.save(cart);



        return "You are now a member of this shared cart.";
    }

    @Transactional
    public SharedCartResponse addItemToSharedCart(String code, AddToCartRequest request) {
        Long userId = userAuthorization.authorizeUserId();

        SharedCartMember member = sharedCartMemberRepository.findByUserIdAndJoinCode(code, userId);
        if(member == null)
            throw new EntityNotFoundException("You are not a member of this shared cart yet.");

        Cart cart = member.getCart();

        FoodSummary food = catalogFacade.getFoodById(request.getFoodId());


        if(!food.getRestaurantId().equals(cart.getRestaurantId()))
            throw new EntityMisMatchAssociationException("The food you are trying to add does not belong to the restaurant of this shared cart.");

        cartService.addOrMergeCartItem(cart, request, food);

        cartRepository.save(cart);

        updateSharedCartTotalPrice(cart.getSharedCart());

        return orderingMapper.toSharedCartResponse(cart.getSharedCart(), userId);
    }

    @Transactional
    public SharedCartResponse removeItemFromSharedCart(String code, Long cartItemId) {
        Long userId = userAuthorization.authorizeUserId();

        SharedCartMember member = sharedCartMemberRepository.findByUserIdAndJoinCode(code, userId);
        if(member == null)
            throw new EntityNotFoundException("You are not a member of this shared cart yet.");

        Cart cart = member.getCart();
        CartItem item = cartService.verifyCartItem(cartItemId, cart);

        cart.getItems().remove(item);
        pricingService.recalculateCartTotals(cart);
        cartRepository.save(cart);

        updateSharedCartTotalPrice(cart.getSharedCart());

        return orderingMapper.toSharedCartResponse(cart.getSharedCart(), userId);
    }

    @Transactional
    public SharedCartResponse contributeToSharedCart(String code, WalletContributionRequest request) {
        Long userId = userAuthorization.authorizeUserId();

        if(userFacade.getWalletBalance(userId).compareTo(request.getAmount()) < 0)
            throw new IllegalArgumentException("You don't have enough balance in your wallet to contribute this amount.");

        SharedCart sharedCart = sharedCartRepository.findByJoinCode(code);
        if(sharedCart == null)
            throw new EntityNotFoundException("You are not a member of this shared cart.");

        if(sharedCart.isHostPaysAll())
            throw new IllegalArgumentException("The host of this shared cart has chosen to pay all, so no contribution is needed.");

        SharedCartMember member = sharedCartMemberRepository.findByUserIdAndJoinCode(code, userId);
        if(member == null)
            throw new EntityNotFoundException("You are not a member of this shared cart.");

        // Re-sync total from active member carts so contribution validation uses current totals.
        updateSharedCartTotalPrice(sharedCart);

        BigDecimal remainingAmount = sharedCart.getTotalPrice().subtract(sharedCart.getAmountPaid());
        if(request.getAmount().compareTo(remainingAmount) > 0)
            throw new IllegalArgumentException("You are contributing more than remaining amount of this shared cart.");

        member.setWalletContribution(member.getWalletContribution().add(request.getAmount()));
        sharedCart.setAmountPaid(sharedCart.getAmountPaid().add(request.getAmount()));

        userFacade.deductWalletBalance(userId, request.getAmount());

        return orderingMapper.toSharedCartResponse(sharedCartRepository.save(sharedCart), userId);
    }

    @Transactional
    public String checkoutSharedCart(String code, PlaceOrderRequest request) {
        Long userId = userAuthorization.authorizeUserId();
        SharedCart sharedCart = sharedCartRepository.findByJoinCode(code);

        if(sharedCart == null)
            throw new EntityNotFoundException("You are not a member of this shared cart.");

        if(!sharedCart.getHostUserId().equals(userId))
            throw new AccessDeniedException("Only the host of the shared cart can perform checkout.");

        if (!userFacade.isAddressOwnedByUser(userId, request.getAddressId())) {
            throw new EntityNotFoundException("Address not found with the provided id.");
        }

        BigDecimal remainingAmount = sharedCart.getTotalPrice().subtract(sharedCart.getAmountPaid());

        if(userFacade.getWalletBalance(userId).compareTo(remainingAmount) < 0)
            throw new IllegalArgumentException("You don't have enough balance in your wallet.");

        userFacade.deductWalletBalance(userId, remainingAmount);

        Order order = new Order();
        order.setUserId(userId);
        order.setRestaurantId(sharedCart.getRestaurantId());
        order.setTotalPrice(sharedCart.getTotalPrice());
        order.setOrderStatus(OrderStatus.CREATED);
        order.setCreatedAt(LocalDateTime.now());
        order.setDeliveryAddressId(request.getAddressId());
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setTotalQuantity(sharedCart.getMemberList().stream()
                .flatMap(member -> member.getCart().getItems().stream())
                .mapToInt(CartItem::getQuantity)
                .sum());
        if(request.getScheduledAt() != null)
            order.setScheduledAt(request.getScheduledAt());
        order.setSpecial(request.isSpecial());
        orderRepository.save(order);

        List<OrderItem> orderItems = sharedCart.getMemberList().stream()
                .flatMap(member -> member.getCart().getItems().stream()
                        .map(item -> orderService.createOrderItem(item, order)))
                .toList();

        order.setOrderItems(new ArrayList<>(orderItems));
        orderRepository.save(order);

        // removing members from shared cart after placing the order
        sharedCart.getMemberList().forEach(member -> {
            member.getCart().getItems().clear();
            member.setActive(false);
            cartRepository.save(member.getCart());
        });
        sharedCartMemberRepository.saveAll(sharedCart.getMemberList());
        sharedCart.setActive(false);
        sharedCartRepository.save(sharedCart);

        return "Your shared cart order has been placed successfully!, " +
                "you can see oder details in Orders tab";
    }

    private String generateUniqueJoinCode() {
        String joinCode;
        do {
            joinCode = generateJoinCode();
        } while (sharedCartRepository.findByJoinCode(joinCode) != null);
        return joinCode;
    }

    private String generateJoinCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random rnd = new Random();
        StringBuilder code =  new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return code.toString();
    }

    private void updateSharedCartTotalPrice(SharedCart sharedCart) {
        sharedCart.setTotalPrice(
                sharedCart.getMemberList().stream()
                        .filter(SharedCartMember::isActive)
                        .map(m -> m.getCart() != null && m.getCart().getTotalPrice() != null
                                ? m.getCart().getTotalPrice()
                                : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
        sharedCartRepository.save(sharedCart);
    }

    private void verifyUserAccountStatus(Long userId) {
        if(userFacade.isUserRestricted(userId))
            throw new UserStatusException("Your account is currently restricted, you can't place join or create share cart until "
                    + userFacade.userRestrictedUntil(userId)
                    + ". Please contact support for more info.");

    }

    private SharedCart createNewSharedCart(Long userId, SharedCartRequest request) {
        SharedCart sharedCart = new SharedCart();
        sharedCart.setHostUserId(userId);
        sharedCart.setRestaurantId(request.getRestaurantId());
        sharedCart.setHostPaysAll(request.isHostPaysAll());
        sharedCart.setJoinCode(generateUniqueJoinCode());

        sharedCartRepository.save(sharedCart);

        return sharedCart;
    }

    private void createNewSharedCartMember(SharedCart sharedCart, Long userId, Cart cart) {
        SharedCartMember member = new SharedCartMember();
        member.setSharedCart(sharedCart);
        member.setUserId(userId);
        member.setCart(cart);

        sharedCartMemberRepository.save(member);
    }

    private Cart prepareCartForSharedCart(Long userId, SharedCartRequest request, SharedCart sharedCart) {
        Cart cart = cartRepository.findByUserId(userId);
        if (cart != null)
            verifyRestaurantCompatibility(request, cart);
        else {
            cart = new Cart();
            cart.setUserId(userId);
            cart.setRestaurantId(request.getRestaurantId());
        }
        if (cart.getRestaurantId() == null) {
            cart.setRestaurantId(request.getRestaurantId());
        }
        cart.setSharedCart(sharedCart);
        cartRepository.save(cart);

        return cart;
    }

    private void verifyRestaurantCompatibility(SharedCartRequest request, Cart cart) {
        if (cart.getRestaurantId() != null
                && !cart.getRestaurantId().equals(request.getRestaurantId())
                && !cart.getItems().isEmpty()) {
            throw new EntityMisMatchAssociationException
                    ("Your current cart belongs to another restaurant. Please clear it before creating a shared cart.");
        }
    }

}
