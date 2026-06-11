package com.kilgore.fooddeliveryapp.deals.service;

import com.kilgore.fooddeliveryapp.catalog.api.CatalogFacade;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantExtendedSummary;
import com.kilgore.fooddeliveryapp.common.enums.PaymentStatus;
import com.kilgore.fooddeliveryapp.common.util.UserAuthorization;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityAlreadyExistsException;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityMisMatchAssociationException;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.common.exceptions.RestaurantNotFoundException;
import com.kilgore.fooddeliveryapp.deals.model.GroupDeal;
import com.kilgore.fooddeliveryapp.deals.model.GroupDealParticipation;
import com.kilgore.fooddeliveryapp.deals.model.GroupDealStatus;
import com.kilgore.fooddeliveryapp.deals.model.GroupDealTier;
import com.kilgore.fooddeliveryapp.deals.repository.GroupDealParticipationRepository;
import com.kilgore.fooddeliveryapp.deals.repository.GroupDealRepository;
import com.kilgore.fooddeliveryapp.deals.repository.GroupDealTierRepository;
import com.kilgore.fooddeliveryapp.deals.dto.request.GroupDealParticipationRequest;
import com.kilgore.fooddeliveryapp.deals.dto.request.GroupDealRequest;
import com.kilgore.fooddeliveryapp.deals.dto.request.GroupDealTierRequest;
import com.kilgore.fooddeliveryapp.deals.dto.response.GroupDealParticipationResponse;
import com.kilgore.fooddeliveryapp.deals.dto.response.GroupDealResponse;
import com.kilgore.fooddeliveryapp.deals.dto.response.GroupDealTierResponse;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.FoodSummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.identity.api.UserFacade;
import com.kilgore.fooddeliveryapp.identity.dto.summary.AddressSummary;
import com.kilgore.fooddeliveryapp.identity.dto.summary.UserExtendedSummary;
import com.kilgore.fooddeliveryapp.identity.dto.summary.UserSummary;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class GroupDealService {

    private final GroupDealRepository groupDealRepository;
    private final GroupDealParticipationRepository groupDealParticipationRepository;
    private final UserAuthorization userAuthorization;
    private final GroupDealTierRepository groupDealTierRepository;
    private final CatalogFacade catalogFacade;
    private final UserFacade userFacade;

    public GroupDealService(GroupDealRepository groupDealRepository, GroupDealParticipationRepository groupDealParticipationRepository,
                            UserAuthorization userAuthorization, GroupDealTierRepository groupDealTierRepository, CatalogFacade catalogFacade, UserFacade userFacade) {
        this.groupDealRepository = groupDealRepository;
        this.groupDealParticipationRepository = groupDealParticipationRepository;
        this.userAuthorization = userAuthorization;
        this.groupDealTierRepository = groupDealTierRepository;
        this.catalogFacade = catalogFacade;
        this.userFacade = userFacade;
    }

    public List<GroupDealResponse> getDealsForRestaurant(Long restaurantId) {
        Long userId = userAuthorization.authorizeUserId();

        RestaurantExtendedSummary restaurant = catalogFacade.getRestaurantExtendedById(restaurantId);

        if(!restaurant.getOwnerUserId().equals(userId)) {
            throw new AccessDeniedException("You are not the owner of this restaurant");
        }

        return groupDealRepository.getAllDealsForRestaurant(restaurantId).stream()
                .map(this::mapToGroupDealResponse)
                .toList();
    }

    public List<GroupDealResponse> getActiveDealsForRestaurant(Long restaurantId) {
        userAuthorization.authorizeUser();
        List<GroupDealStatus> statuses = List.of(GroupDealStatus.VOTING, GroupDealStatus.CONFIRMATION_WINDOW);

        return groupDealRepository.getActiveDealsForRestaurant(restaurantId, statuses).stream()
                .map(this::mapToGroupDealResponse)
                .toList();
    }

    @Transactional
    public GroupDealResponse createGroupDeal(Long restaurantId, GroupDealRequest request) {
        Long userId= userAuthorization.authorizeUserId();

        if(!userFacade.isOwnerOfRestaurant(userId, restaurantId)) {
            throw new AccessDeniedException("You are not the owner of this restaurant");
        }

        List<GroupDealStatus> statuses = List.of(GroupDealStatus.CREATED, GroupDealStatus.VOTING, GroupDealStatus.CONFIRMATION_WINDOW);
        LocalDateTime now = LocalDateTime.now();

        if(groupDealRepository.getActiveDealForRestaurantAndByName(restaurantId, request.getDealName(), statuses, now) != null) {
            throw new EntityAlreadyExistsException("An active Group deal with the same name - " + request.getDealName()
                    + " already exists for this restaurant");
        }

        GroupDeal deal = buildGroupDeal(request, restaurantId);

        return mapToGroupDealResponse(deal);
    }

    @Transactional
    public String deleteGroupDeal(Long restaurantId, Long dealId) {
        Long userId = userAuthorization.authorizeUserId();

        if(!userFacade.isOwnerOfRestaurant(userId, restaurantId)) {
            throw new AccessDeniedException("You are not the owner of this restaurant");
        }

        GroupDeal deal = groupDealRepository.findById(dealId)
                .orElseThrow(() -> new EntityNotFoundException("Group deal with id " + dealId + " not found"));

        if(!deal.getRestaurantId().equals(restaurantId))
            throw new AccessDeniedException("This deal does not belong to your restaurant");

        if(deal.getStatus() != GroupDealStatus.CREATED)
            throw new IllegalStateException("Cannot delete this deal as it is in " + deal.getStatus() + " phase. Only deals in CREATED phase can be deleted.");

        deal.setStatus(GroupDealStatus.DELETED);
        groupDealRepository.save(deal);

        return "Group deal with id " + dealId + " has been deleted";
    }

    public GroupDealResponse getDeal(Long restaurantId, Long dealId) {
        Long userId = userAuthorization.authorizeUserId();

        GroupDeal deal = groupDealRepository.findById(dealId)
                .orElseThrow(() -> new EntityNotFoundException("Group deal with id " + dealId + " not found"));

        if(!deal.getRestaurantId().equals(restaurantId))
            throw new AccessDeniedException("This deal does not belong to this restaurant");

        return mapToGroupDealResponse(deal);
    }

    @Transactional
    public GroupDealParticipationResponse participateInGroupDeal(Long restaurantId, Long dealId,
                                                                 GroupDealParticipationRequest request) {
        Long userId = userAuthorization.authorizeUserId();

        GroupDeal deal = groupDealRepository.findById(dealId)
                .orElseThrow(() -> new EntityNotFoundException("Group deal with id " + dealId + " not found"));

        if(!deal.getRestaurantId().equals(restaurantId))
            throw new AccessDeniedException("This deal does not belong to your restaurant");

        if(deal.getStatus() !=  GroupDealStatus.VOTING)
            throw new IllegalStateException("Cannot participate in this deal as it is not in voting stage");

        GroupDealParticipation participation = buildGroupDealParticipation(request, userId, deal);

        return mapToGroupDealParticipationResponse(participation, userId);
    }

    @Transactional
    public String withdrawFromGroupDeal(Long restaurantId, Long dealId, Long  participationId) {
        userAuthorization.authorizeUser();

        GroupDeal deal = groupDealRepository.findById(dealId)
                .orElseThrow(() -> new EntityNotFoundException("Group deal with id " + dealId + " not found"));

        if(deal.getStatus() == GroupDealStatus.CREATED || deal.getStatus() == GroupDealStatus.FULFILLED || deal.getStatus() == GroupDealStatus.EXPIRED || deal.getStatus() == GroupDealStatus.DELETED)
            throw new IllegalStateException("Cannot withdraw from this deal as it is in " + deal.getStatus() + " phase");

        GroupDealParticipation participation = groupDealParticipationRepository.findById(participationId)
                .orElseThrow(() -> new EntityNotFoundException("Participation with id " + participationId + " not found"));

        participation.setConfirmed(false);
        groupDealParticipationRepository.save(participation);

        userFacade.addWalletBalance(participation.getUserId(), participation.getAmountPaid());

        return "You have successfully withdrawn from the group deal with id " + dealId + ". and refund has been processed";
    }

    //------------------------------------------------HELPER METHODS------------------------------------------------


    private GroupDealResponse mapToGroupDealResponse(GroupDeal deal) {

        List<GroupDealTierResponse> discountList = deal.getDiscountList().stream()
                .map(tier -> new GroupDealTierResponse(tier.getThresholdPercent(), tier.getDiscountPercent()))
                .toList();


        Integer currentParticipation = groupDealParticipationRepository.getTotalParticipantsByDeal(deal.getDealId());
        if(currentParticipation == null) currentParticipation = 0;

        return new GroupDealResponse(
                deal.getDealId(),
                deal.getDealName(),
                catalogFacade.getRestaurantById(deal.getRestaurantId()),
                deal.getStartTime(),
                deal.getEndTime(),
                catalogFacade.getFoodListByIds(deal.getFoodIds()),
                deal.getOriginalPrice(),
                deal.getMaxDiscount(),
                deal.getTargetParticipation(),
                currentParticipation,
                calculateCurrentPrice(deal, currentParticipation),
                deal.getStatus(),
                deal.getConfirmationWindowEndTime(),
                discountList
        );
    }

    private BigDecimal calculateCurrentPrice(GroupDeal deal, int currentParticipation) {
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

    private List<GroupDealTier> createDicountList(List<GroupDealTierRequest> requests, GroupDeal groupDeal) {
        List<GroupDealTier>  dicountList = new ArrayList<>();

        requests.forEach(request -> {
            GroupDealTier dealTier = new GroupDealTier();
            dealTier.setGroupDeal(groupDeal);
            dealTier.setThresholdPercent(request.getThresholdPercent());
            dealTier.setDiscountPercent(request.getDiscountPercent());

            groupDealTierRepository.save(dealTier);

            dicountList.add(dealTier);
        });

        return dicountList;
    }

    private GroupDealParticipationResponse mapToGroupDealParticipationResponse(GroupDealParticipation participation, Long userId) {
        UserExtendedSummary participantUser = userFacade.getUserExtendedById(participation.getUserId());

        return new GroupDealParticipationResponse(
                participation.getParticipantId(),
                participation.getUserId(),
                participantUser.getName(),
                participantUser.getEmail(),
                participation.getGroupDeal().getDealName(),
                participation.getQuantity(),
                participation.getPaymentStatus(),
                participation.getAmountPaid(),
                participation.isConfirmed()
        );
    }

    public List<GroupDealParticipationResponse> getParticipationsByDeal(Long restaurantId, Long dealId) {
        Long userId = userAuthorization.authorizeUserId();
        
        GroupDeal deal = groupDealRepository.findById(dealId)
                .orElseThrow(() -> new EntityNotFoundException("Group deal with id " + dealId + " not found"));

        if(!userFacade.isOwnerOfRestaurant(userId, restaurantId)) {
            throw new AccessDeniedException("You are not the owner of this restaurant");
        }

        if(!deal.getRestaurantId().equals(restaurantId)) {
            throw new AccessDeniedException("This deal does not belong to your restaurant");
        }

        return groupDealParticipationRepository.findGroupDealParticipationsByGroupDeal(deal).stream()
                .map(participation ->mapToGroupDealParticipationResponse(participation, userId))
                .toList();
    }

    public List<GroupDealParticipationResponse> getParticipationsByUser(Long restaurantId, Long dealId) {
        Long userId = userAuthorization.authorizeUserId();


        GroupDeal deal = groupDealRepository.findById(dealId)
                .orElseThrow(() -> new EntityNotFoundException("Group deal with id " + dealId + " not found"));

        if(!deal.getRestaurantId().equals(restaurantId)) {
            throw new EntityMisMatchAssociationException("This deal does not belong to this restaurant");
        }

        List<GroupDealParticipation> participationList = groupDealParticipationRepository.findByUserIdAndGroupDeal(userId, deal);
        if(participationList == null || participationList.isEmpty()) {
            throw new EntityNotFoundException("You didn't participate in this deal");
        }
        return participationList.stream()
                .map(par -> mapToGroupDealParticipationResponse(par, userId))
                .toList();
    }

    private GroupDeal buildGroupDeal(GroupDealRequest request, Long restaurantId) {
        GroupDeal deal = new GroupDeal();
        deal.setDealName(request.getDealName());
        deal.setRestaurantId(restaurantId);
        deal.setStartTime(request.getStartTime());
        deal.setEndTime(request.getEndTime());
        deal.setOriginalPrice(request.getOriginalPrice());
        deal.setMaxDiscount(request.getMaxDiscount());
        deal.setFoodIds(request.getFoodIds());
        deal.setTargetParticipation(request.getTargetParticipation());
        deal.setStatus(GroupDealStatus.CREATED);

        groupDealRepository.save(deal);

        List<GroupDealTier> discountList = createDicountList(request.getDiscountList(), deal);
        deal.setDiscountList(discountList);
        groupDealRepository.save(deal);

        return deal;
    }

    private GroupDealParticipation buildGroupDealParticipation(GroupDealParticipationRequest request, Long userId, GroupDeal deal) {
        GroupDealParticipation participation = new GroupDealParticipation();
        participation.setGroupDeal(deal);
        participation.setUserId(userId);
        participation.setQuantity(request.getQuantity());
        participation.setPaymentStatus(PaymentStatus.SUCCESS);
        participation.setAmountPaid(deal.getOriginalPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
        participation.setAddressIdToDeliver(request.getAddressId());

        groupDealParticipationRepository.save(participation);

        return participation;
    }
}
