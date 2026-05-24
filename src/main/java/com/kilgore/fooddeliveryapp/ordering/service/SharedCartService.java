package com.kilgore.fooddeliveryapp.ordering.service;

import com.kilgore.fooddeliveryapp.catalog.model.Addon;
import com.kilgore.fooddeliveryapp.catalog.model.Food;
import com.kilgore.fooddeliveryapp.common.util.UserAuthorization;
import com.kilgore.fooddeliveryapp.identity.model.Address;
import com.kilgore.fooddeliveryapp.ordering.model.PaymentStatus;
import com.kilgore.fooddeliveryapp.identity.repository.AddressRepository;
import com.kilgore.fooddeliveryapp.ordering.dto.request.AddToCartRequest;
import com.kilgore.fooddeliveryapp.ordering.dto.request.PlaceOrderRequest;
import com.kilgore.fooddeliveryapp.ordering.dto.request.SharedCartRequest;
import com.kilgore.fooddeliveryapp.wallet.dto.request.WalletContributionRequest;
import com.kilgore.fooddeliveryapp.ordering.dto.response.SharedCartResponse;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityMisMatchAssociationException;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.common.exceptions.UserStatusException;
import com.kilgore.fooddeliveryapp.identity.model.AccountStatus;
import com.kilgore.fooddeliveryapp.identity.model.User;
import com.kilgore.fooddeliveryapp.ordering.model.*;
import com.kilgore.fooddeliveryapp.ordering.repository.CartRepository;
import com.kilgore.fooddeliveryapp.ordering.repository.OrderRepository;
import com.kilgore.fooddeliveryapp.ordering.repository.SharedCartMemberRepository;
import com.kilgore.fooddeliveryapp.ordering.repository.SharedCartRepository;
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
    private final MappingService mappingService;
    private final SharedCartMemberRepository sharedCartMemberRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final PricingService pricingService;
    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final OrderService orderService;


    public SharedCartService(SharedCartRepository sharedCartRepository, UserAuthorization userAuthorization,
                             MappingService mappingService, SharedCartMemberRepository sharedCartMemberRepository,
                             CartRepository cartRepository, CartService cartService, PricingService pricingService, OrderRepository orderRepository, AddressRepository addressRepository, OrderService orderService) {
        this.sharedCartRepository = sharedCartRepository;
        this.userAuthorization = userAuthorization;
        this.mappingService = mappingService;
        this.sharedCartMemberRepository = sharedCartMemberRepository;
        this.cartRepository = cartRepository;

        this.cartService = cartService;
        this.pricingService = pricingService;
        this.orderRepository = orderRepository;
        this.addressRepository = addressRepository;
        this.orderService = orderService;
    }

    @Transactional
    public SharedCartResponse createSharedCart(SharedCartRequest request) {
        User user = userAuthorization.authorizeUser();

        if(user.getAccountStatus().equals(AccountStatus.RESTRICTED)) {
            throw new UserStatusException("Your account is currently restricted, you can't place orders until " + user.getRestrictedUntil()
                    + ". Please contact support for more info.");
        }

        SharedCartMember existingMembership = sharedCartMemberRepository.findActiveMemberByUserId(user.getUserId());
        if(existingMembership != null)
            return mapToSharedCartResponse(existingMembership.getSharedCart(), user);

        SharedCart sharedCart = sharedCartRepository.findByHost(user);
        if(sharedCart != null && sharedCart.isActive())
            return mapToSharedCartResponse(sharedCart, user);

        sharedCart = new SharedCart();
        sharedCart.setHost(user);
        sharedCart.setRestaurant(request.getRestaurant());
        sharedCart.setHostPaysAll(request.isHostPaysAll());
        sharedCart.setJoinCode(generateUniqueJoinCode());

        sharedCartRepository.save(sharedCart);

        Cart cart = cartRepository.findByUser(user);
        if (cart != null) {
            if (cart.getRestaurant() != null && !cart.getRestaurant().getRestaurantId().equals(request.getRestaurant().getRestaurantId()) && !cart.getItems().isEmpty()) {
                throw new EntityMisMatchAssociationException("Your current cart belongs to another restaurant. Please clear it before creating a shared cart.");
            }
        } else {
            cart = new Cart();
            cart.setUser(user);
            cart.setRestaurant(request.getRestaurant());
        }
        if (cart.getRestaurant() == null) {
            cart.setRestaurant(request.getRestaurant());
        }
        cart.setSharedCart(sharedCart);
        cartRepository.save(cart);

        SharedCartMember member = new SharedCartMember();
        member.setUser(user);
        member.setSharedCart(sharedCart);
        member.setCart(cart);
        sharedCartMemberRepository.save(member);

        return mapToSharedCartResponse(sharedCart, user);
    }

    public SharedCartResponse getCurrentSharedCart() {
        User user =  userAuthorization.authorizeUser();

        SharedCartMember activeMembership = sharedCartMemberRepository.findActiveMemberByUserId(user.getUserId());
        if(activeMembership != null)
            return mapToSharedCartResponse(activeMembership.getSharedCart(), user);

        SharedCart sharedCart = sharedCartRepository.findByHost(user);
        if(sharedCart != null && sharedCart.isActive())
            return mapToSharedCartResponse(sharedCart, user);

        throw new EntityNotFoundException("You are not a member of any active shared cart right now.");
    }

    public SharedCartResponse getSharedCartByCode(String code) {
        User user  = userAuthorization.authorizeUser();

        SharedCart sharedCart = sharedCartRepository.findByJoinCode(code);
        if(sharedCart == null)
            throw new EntityNotFoundException("No shared cart found with the provided code.");

        return mapToSharedCartResponse(sharedCart, user);
    }

    @Transactional
    public String joinSharedCart(String code) {
        User user = userAuthorization.authorizeUser();

        if(user.getAccountStatus().equals(AccountStatus.RESTRICTED)) {
            throw new UserStatusException("Your account is currently restricted, you can't place orders until " + user.getRestrictedUntil()
                    + ". Please contact support for more info.");
        }

        SharedCartMember existingMembership = sharedCartMemberRepository.findActiveMemberByUserId(user.getUserId());
        if(existingMembership != null)
            throw new AccessDeniedException("You are already a member of another shared cart.");

        SharedCart sharedCart = sharedCartRepository.findByJoinCode(code);
        if(sharedCart == null)
            throw new EntityNotFoundException("Shared cart not found with the provided code.");

        if(sharedCart.getHost().equals(user))
            throw new AccessDeniedException("You cannot join your own shared cart.");

        Cart cart = cartRepository.findByUser(user);
        if (cart != null) {
            if (cart.getRestaurant() != null && !cart.getRestaurant().equals(sharedCart.getRestaurant()) && !cart.getItems().isEmpty()) {
                throw new EntityMisMatchAssociationException("Your current cart belongs to another restaurant. Please clear it before joining this shared cart.");
            }
        } else {
            cart = new Cart();
            cart.setUser(user);
            cart.setRestaurant(sharedCart.getRestaurant());
        }
        if (cart.getRestaurant() == null) {
            cart.setRestaurant(sharedCart.getRestaurant());
        }
        cart.setSharedCart(sharedCart);
        cartRepository.save(cart);

        SharedCartMember member = new SharedCartMember();
        member.setSharedCart(sharedCart);
        member.setUser(user);
        member.setCart(cart);

        sharedCartMemberRepository.save(member);

        return "You are now a member of this shared cart.";
    }

    @Transactional
    public SharedCartResponse addItemToSharedCart(String code, AddToCartRequest request) {
        User user = userAuthorization.authorizeUser();

        SharedCartMember member = sharedCartMemberRepository.findByUserAndJoinCode(code, user);
        if(member == null)
            throw new EntityNotFoundException("You are not a member of this shared cart yet.");

        Cart cart = member.getCart();

        Food food = cartService.extractFoodFromRequest(request);
        List<Addon> addonList = cartService.extractAddonsFromRequest(request, food);

        if(!food.getRestaurant().equals(cart.getRestaurant()))
            throw new EntityMisMatchAssociationException("The food you are trying to add does not belong to the restaurant of this shared cart.");

        cartService.addOrMergeCartItem(cart, food, addonList, request);

        cartRepository.save(cart);

        updateSharedCartTotalPrice(cart.getSharedCart());

        return mapToSharedCartResponse(cart.getSharedCart(), user);
    }

    @Transactional
    public SharedCartResponse removeItemFromSharedCart(String code, Long cartItemId) {
        User user = userAuthorization.authorizeUser();

        SharedCartMember member = sharedCartMemberRepository.findByUserAndJoinCode(code, user);
        if(member == null)
            throw new EntityNotFoundException("You are not a member of this shared cart yet.");

        Cart cart = member.getCart();
        CartItem item = cartService.verifyCartItem(cartItemId, cart);

        cart.getItems().remove(item);
        pricingService.updateCartTotal(cart);
        cartRepository.save(cart);

        updateSharedCartTotalPrice(cart.getSharedCart());

        return mapToSharedCartResponse(cart.getSharedCart(), user);
    }

    @Transactional
    public SharedCartResponse contributeToSharedCart(String code, WalletContributionRequest request) {
        User user = userAuthorization.authorizeUser();

        if(user.getWalletBalance().compareTo(request.getAmount()) < 0)
            throw new IllegalArgumentException("You don't have enough balance in your wallet to contribute this amount.");

        SharedCart sharedCart = sharedCartRepository.findByJoinCode(code);
        if(sharedCart == null)
            throw new EntityNotFoundException("You are not a member of this shared cart.");

        if(sharedCart.isHostPaysAll())
            throw new IllegalArgumentException("The host of this shared cart has chosen to pay all, so no contribution is needed.");

        SharedCartMember member = sharedCartMemberRepository.findByUserAndJoinCode(code, user);
        if(member == null)
            throw new EntityNotFoundException("You are not a member of this shared cart.");

        // Re-sync total from active member carts so contribution validation uses current totals.
        updateSharedCartTotalPrice(sharedCart);

        BigDecimal remainingAmount = sharedCart.getTotalPrice().subtract(sharedCart.getAmountPaid());
        if(request.getAmount().compareTo(remainingAmount) > 0)
            throw new IllegalArgumentException("You are contributing more than remaining amount of this shared cart.");

        member.setWalletContribution(member.getWalletContribution().add(request.getAmount()));
        sharedCart.setAmountPaid(sharedCart.getAmountPaid().add(request.getAmount()));

        user.setWalletBalance(user.getWalletBalance().subtract(request.getAmount()));

        return mapToSharedCartResponse(sharedCartRepository.save(sharedCart), user);
    }

    @Transactional
    public String checkoutSharedCart(String code, PlaceOrderRequest request) {
        User user = userAuthorization.authorizeUser();
        SharedCart sharedCart = sharedCartRepository.findByJoinCode(code);

        if(sharedCart == null)
            throw new EntityNotFoundException("You are not a member of this shared cart.");

        if(!sharedCart.getHost().equals(user))
            throw new AccessDeniedException("Only the host of the shared cart can perform checkout.");

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new EntityNotFoundException("Address not found with the provided id."));

        BigDecimal remainingAmount = sharedCart.getTotalPrice().subtract(sharedCart.getAmountPaid());

        if(user.getWalletBalance().compareTo(remainingAmount) < 0)
            throw new IllegalArgumentException("You don't have enough balance in your wallet.");

        user.setWalletBalance(user.getWalletBalance().subtract(remainingAmount));

        Order order = new Order();
        order.setUser(user);
        order.setRestaurant(sharedCart.getRestaurant());
        order.setTotalPrice(sharedCart.getTotalPrice());
        order.setOrderStatus(OrderStatus.CREATED);
        order.setCreatedAt(LocalDateTime.now());
        order.setDeliveryAddress(address);
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


    private SharedCartResponse mapToSharedCartResponse(SharedCart sharedCart, User viewer) {
        boolean viewerIsHost = sharedCart.getHost().getUserId().equals(viewer.getUserId());

        List<SharedCartMember> activeMembers = sharedCart.getMemberList().stream()
                .filter(SharedCartMember::isActive)
                .toList();

        BigDecimal recalculatedTotal = activeMembers.stream()
                .map(member -> member.getCart() != null && member.getCart().getTotalPrice() != null
                        ? member.getCart().getTotalPrice()
                        : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SharedCartResponse(
                sharedCart.getSharedCartId(),
                mappingService.toUserSummary(sharedCart.getHost()),
                mappingService.toRestaurantSummary(sharedCart.getRestaurant()),
                mappingService.createSharedCartMemberSummaries(activeMembers),
                sharedCart.getJoinCode(),
                recalculatedTotal,
                sharedCart.getAmountPaid(),
                sharedCart.isHostPaysAll(),
                sharedCart.isActive(),
                !viewerIsHost,
                viewerIsHost
        );
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

}
