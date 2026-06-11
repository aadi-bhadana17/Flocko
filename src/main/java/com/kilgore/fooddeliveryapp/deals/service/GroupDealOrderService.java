package com.kilgore.fooddeliveryapp.deals.service;

import com.kilgore.fooddeliveryapp.common.enums.RefundReason;
import com.kilgore.fooddeliveryapp.common.enums.RefundStatus;
import com.kilgore.fooddeliveryapp.deals.dto.request.CreateGroupDealOrderRequest;
import com.kilgore.fooddeliveryapp.deals.model.GroupDeal;
import com.kilgore.fooddeliveryapp.deals.model.GroupDealParticipation;
import com.kilgore.fooddeliveryapp.deals.model.GroupDealStatus;
import com.kilgore.fooddeliveryapp.deals.model.GroupDealTier;
import com.kilgore.fooddeliveryapp.deals.repository.GroupDealParticipationRepository;
import com.kilgore.fooddeliveryapp.deals.repository.GroupDealRepository;
import com.kilgore.fooddeliveryapp.common.enums.PaymentStatus;
import com.kilgore.fooddeliveryapp.identity.api.UserFacade;
import com.kilgore.fooddeliveryapp.ordering.api.OrderingFacade;
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
    private final GroupDealRepository groupDealRepository;
    private final UserFacade userFacade;
    private final OrderingFacade orderingFacade;

    public GroupDealOrderService(GroupDealParticipationRepository groupDealParticipationRepository, GroupDealRepository groupDealRepository, UserFacade userFacade, OrderingFacade orderingFacade) {
        this.groupDealParticipationRepository = groupDealParticipationRepository;
        this.groupDealRepository = groupDealRepository;
        this.userFacade = userFacade;
        this.orderingFacade = orderingFacade;
    }

    @Transactional
    public void processGroupDeal(Long dealId, Integer currentParticipation) {
        GroupDeal deal = groupDealRepository.findById(dealId)
                .orElseThrow(() -> new RuntimeException("Deal not found"));

        // status update + processing in same transaction
        deal.setStatus(GroupDealStatus.FULFILLED);
        groupDealRepository.save(deal);

        List<GroupDealParticipation> participationList =
                groupDealParticipationRepository.findActiveParticipantsByDeal(deal.getDealId());
        BigDecimal currentPrice = calculateCurrentPrice(deal, currentParticipation);

        participationList.forEach(par -> placeOrder(deal, par, currentPrice));
        refundToUser(deal, participationList, currentParticipation);
    }

    @Transactional
    public void expireDeal(Long dealId, Integer currentParticipation) {
        GroupDeal deal = groupDealRepository.findById(dealId)
                .orElseThrow(() -> new RuntimeException("Deal not found"));

        // status update + refund in same transaction
        deal.setStatus(GroupDealStatus.EXPIRED);
        groupDealRepository.save(deal);

        List<GroupDealParticipation> participantList =
                groupDealParticipationRepository.findActiveParticipantsByDeal(dealId);

        if (participantList != null && !participantList.isEmpty())
            refundToUser(deal, participantList, currentParticipation);
    }

    public void refundToUser(GroupDeal deal, List<GroupDealParticipation> participationList, Integer currentParticipation) {
        BigDecimal currentPrice = calculateCurrentPrice(deal, currentParticipation);

        participationList.forEach(par -> {
            Long userIdToRefund = par.getUserId();
            BigDecimal refund;

            if(deal.getStatus() == GroupDealStatus.EXPIRED) {
                refund = par.getAmountPaid(); // full refund if deal expired
                par.setRefundStatus(RefundStatus.PENDING); // mark as pending until we confirm refund is successful
            } else {
                refund = (par.getAmountPaid().subtract(currentPrice)).multiply(BigDecimal.valueOf(par.getQuantity())); // refund only discounted amount
                par.setRefundStatus(RefundStatus.PENDING);
            }

            userFacade.addWalletBalance(userIdToRefund, refund);
            par.setRefundStatus(RefundStatus.COMPLETED);
            par.setRefundReason(deal.getStatus() == GroupDealStatus.EXPIRED ?
                    RefundReason.DEAL_EXPIRED : RefundReason.GROUP_DEAL_DISCOUNT);
            par.setRefundAt(LocalDateTime.now());
            par.setRefundAmount(refund);

            groupDealParticipationRepository.save(par);
        });
    }

    private void placeOrder(GroupDeal deal, GroupDealParticipation par, BigDecimal currentPrice) {
        Long orderId = orderingFacade.placeGroupDealOrder(mapToCreateGroupDealOrderRequest(deal, par, currentPrice));
    }

    private CreateGroupDealOrderRequest mapToCreateGroupDealOrderRequest(GroupDeal deal, GroupDealParticipation par,
                                                                         BigDecimal currentPrice) {
        return new CreateGroupDealOrderRequest(
                par.getUserId(),
                deal.getRestaurantId(),
                par.getAddressIdToDeliver(),
                currentPrice.multiply(BigDecimal.valueOf(par.getQuantity())),
                par.getQuantity(),
                deal.getFoodIds()
        );
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
