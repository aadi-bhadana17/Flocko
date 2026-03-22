package com.kilgore.fooddeliveryapp.service;

import com.kilgore.fooddeliveryapp.model.*;
import com.kilgore.fooddeliveryapp.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class GroupDealOrderService {

    private final GroupDealParticipationRepository groupDealParticipationRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final GroupDealRepository groupDealRepository;

    public GroupDealOrderService(GroupDealParticipationRepository groupDealParticipationRepository, UserRepository userRepository, OrderRepository orderRepository, OrderItemRepository orderItemRepository, GroupDealRepository groupDealRepository) {
        this.groupDealParticipationRepository = groupDealParticipationRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.groupDealRepository = groupDealRepository;
    }

    @Transactional
    public void processGroupDeal(Long dealId, Integer currentParticipation) {
        /*
        This method will process the complete deal - refund, place order, fetch participants - and one deal at a time

         So, at first it will fetch users who didn't withdraw at CONFIRMATION_WINDOW phase and then place order for them
         and later refund the discounted amount in their wallet
        */

        GroupDeal deal = groupDealRepository.findById(dealId).orElseThrow(() -> new RuntimeException("Deal not found"));

        List<GroupDealParticipation> participationList = groupDealParticipationRepository.findActiveParticipantsByDeal(deal.getDealId());
        BigDecimal currentPrice = calculateCurrentPrice(deal, currentParticipation);

        participationList.forEach(par -> {
            placeOrder(deal, par, currentPrice); // place order for user at his place
        });

        refundToUser(deal, participationList, currentParticipation); // refund the discounted amount to user
    }

    public void refundToUser(GroupDeal deal, List<GroupDealParticipation> participationList, Integer currentParticipation) {
        BigDecimal currentPrice = calculateCurrentPrice(deal, currentParticipation);

        List<User> users = new ArrayList<>();
        participationList.forEach(par -> {
            User userToRefund = par.getUser();
            BigDecimal refund;

            if(deal.getStatus() == GroupDealStatus.EXPIRED) {
                refund = par.getAmountPaid(); // full refund if deal expired
            } else {
                refund = (par.getAmountPaid().subtract(currentPrice)).multiply(BigDecimal.valueOf(par.getQuantity())); // refund only discounted amount
            }

            par.getUser().setWalletBalance(par.getUser().getWalletBalance().add(refund));
            users.add(userToRefund);
        });

        userRepository.saveAll(users);
    }

    private void placeOrder(GroupDeal deal, GroupDealParticipation par, BigDecimal currentPrice) {
        Order order = new Order();
        order.setUser(par.getUser());
        order.setRestaurant(deal.getRestaurant());
        order.setTotalPrice(currentPrice.multiply(BigDecimal.valueOf(par.getQuantity())));
        order.setOrderStatus(OrderStatus.CREATED);
        order.setCreatedAt(LocalDateTime.now());
        order.setDeliveryAddress(par.getAddressToDeliver());
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setTotalQuantity(par.getQuantity());
        order.setOrderType(OrderType.GROUP_DEAL);

        orderRepository.save(order);
        order.setOrderItems(createOrderItems(deal, order));
        orderRepository.save(order);
    }

    private List<OrderItem> createOrderItems(GroupDeal deal, Order order) {
        List<OrderItem> orderItems = new ArrayList<>();
        deal.getFoodList().forEach(food -> {
            OrderItem item = new OrderItem();
            item.setFood(food);
            item.setQuantity(1);
            item.setPriceAtOrder(food.getFoodPrice());
            item.setItemTotal(food.getFoodPrice());
            item.setOrder(order);

            orderItems.add(item);
        });
        orderItemRepository.saveAll(orderItems);
        return orderItems;
    }

    private BigDecimal calculateCurrentPrice(GroupDeal deal, int currentParticipation) {
        // Note: This duplicates calculateCurrentPrice from GroupDealService.
        // Extracted here to avoid circular dependency between GroupDealService and GroupDealOrderService.

        int achievedPercent = (currentParticipation * 100) / deal.getTargetParticipation();

        GroupDealTier achievedTier = deal.getDiscountList().stream()
                .filter(tier -> tier.getThresholdPercent() <= achievedPercent)
                .max(Comparator.comparingInt(GroupDealTier::getThresholdPercent))
                .orElse(null);

        if(achievedTier == null) return deal.getOriginalPrice();

        BigDecimal discount = deal.getOriginalPrice()
                .multiply(BigDecimal.valueOf(achievedTier.getDiscountPercent()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return deal.getOriginalPrice().subtract(discount);
    }
}
