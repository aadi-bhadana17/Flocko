package com.kilgore.fooddeliveryapp.service;

import com.kilgore.fooddeliveryapp.authorization.UserAuthorization;
import com.kilgore.fooddeliveryapp.dto.request.GroupDealParticipationRequest;
import com.kilgore.fooddeliveryapp.dto.request.GroupDealRequest;
import com.kilgore.fooddeliveryapp.dto.request.GroupDealTierRequest;
import com.kilgore.fooddeliveryapp.dto.response.GroupDealParticipationResponse;
import com.kilgore.fooddeliveryapp.dto.response.GroupDealResponse;
import com.kilgore.fooddeliveryapp.dto.response.GroupDealTierResponse;
import com.kilgore.fooddeliveryapp.dto.summary.FoodSummary;
import com.kilgore.fooddeliveryapp.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.exceptions.*;
import com.kilgore.fooddeliveryapp.model.*;
import com.kilgore.fooddeliveryapp.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class GroupDealService {

    private final GroupDealRepository groupDealRepository;
    private final GroupDealParticipationRepository groupDealParticipationRepository;
    private final UserAuthorization userAuthorization;
    private final RestaurantRepository restaurantRepository;
    private final FoodRepository foodRepository;
    private final GroupDealTierRepository groupDealTierRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    public GroupDealService(GroupDealRepository groupDealRepository, GroupDealParticipationRepository groupDealParticipationRepository, UserAuthorization userAuthorization, RestaurantRepository restaurantRepository, FoodRepository foodRepository, GroupDealTierRepository groupDealTierRepository, UserRepository userRepository, AddressRepository addressRepository) {
        this.groupDealRepository = groupDealRepository;
        this.groupDealParticipationRepository = groupDealParticipationRepository;
        this.userAuthorization = userAuthorization;
        this.restaurantRepository = restaurantRepository;
        this.foodRepository = foodRepository;
        this.groupDealTierRepository = groupDealTierRepository;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
    }

    public List<GroupDealResponse> getDealsForRestaurant(Long restaurantId) {
        User user = userAuthorization.authorizeUser();

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(restaurantId));

        if(!restaurant.getOwner().equals(user)) {
            throw new AccessDeniedException("You are not the owner of this restaurant");
        }

        return groupDealRepository.getAllDealsForRestaurant(restaurantId).stream()
                .map(this::mapToGroupDealResponse)
                .toList();
    }

    public List<GroupDealResponse> getActiveDealsForRestaurant(Long restaurantId) {
        List<GroupDealStatus> statuses = List.of(GroupDealStatus.VOTING, GroupDealStatus.CONFIRMATION_WINDOW);

        return groupDealRepository.getActiveDealsForRestaurant(restaurantId, statuses).stream()
                .map(this::mapToGroupDealResponse)
                .toList();
    }

    @Transactional
    public GroupDealResponse createGroupDeal(Long restaurantId, GroupDealRequest request) {
        User user = userAuthorization.authorizeUser();

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(restaurantId));

        if(!restaurant.getOwner().equals(user)) {
            throw new AccessDeniedException("You are not the owner of this restaurant");
        }

        List<GroupDealStatus> statuses = List.of(GroupDealStatus.VOTING, GroupDealStatus.CONFIRMATION_WINDOW);


        if(groupDealRepository.getActiveDealForRestaurantAndByName(restaurantId, request.getDealName(), statuses) != null) {
            throw new EntityAlreadyExistsException("An active Group deal with the same name - " + request.getDealName()
                    + " already exists for this restaurant");
        }

        GroupDeal deal = new GroupDeal();
        deal.setDealName(request.getDealName());
        deal.setRestaurant(restaurant);
        deal.setStartTime(request.getStartTime());
        deal.setEndTime(request.getEndTime());
        deal.setOriginalPrice(request.getOriginalPrice());
        deal.setMaxDiscount(request.getMaxDiscount());
        deal.setFoodList(fetchFoodList(request.getFoodIds(), restaurantId));
        deal.setTargetParticipation(request.getTargetParticipation());
        deal.setStatus(GroupDealStatus.VOTING);

        groupDealRepository.save(deal);

        List<GroupDealTier> discountList = createDicountList(request.getDiscountList(), deal);
        deal.setDiscountList(discountList);
        groupDealRepository.save(deal);

        return mapToGroupDealResponse(deal);
    }

    @Transactional
    public String deleteGroupDeal(Long restaurantId, Long dealId) {
        User user = userAuthorization.authorizeUser();

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(restaurantId));

        if(!restaurant.getOwner().equals(user)) {
            throw new AccessDeniedException("You are not the owner of this restaurant");
        }

        GroupDeal deal = groupDealRepository.findById(dealId)
                .orElseThrow(() -> new EntityNotFoundException("Group deal with id " + dealId + " not found"));

        if(!deal.getRestaurant().equals(restaurant))
            throw new AccessDeniedException("This deal does not belong to your restaurant");

        List<GroupDealParticipation> participants = groupDealParticipationRepository.findByGroupDeal(deal);

        List<User> refundedUsers = new ArrayList<>();

        participants.forEach(pr -> {
            User usr =  pr.getUser();
            usr.setWalletBalance(usr.getWalletBalance().add(pr.getAmountPaid()));

            refundedUsers.add(usr);
        });
        userRepository.saveAll(refundedUsers);

        deal.setStatus(GroupDealStatus.DELETED);
        groupDealRepository.delete(deal);

        return "Group deal with id " + dealId + " has been deleted";
    }

    public GroupDealResponse getDeal(Long restaurantId, Long dealId) {
        User user = userAuthorization.authorizeUser();

        GroupDeal deal = groupDealRepository.findById(dealId)
                .orElseThrow(() -> new EntityNotFoundException("Group deal with id " + dealId + " not found"));

        return mapToGroupDealResponse(deal);
    }

    @Transactional
    public GroupDealParticipationResponse participateInGroupDeal(Long restaurantId, Long dealId,
                                                                 GroupDealParticipationRequest request) {
        User user = userAuthorization.authorizeUser();

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(restaurantId));

        GroupDeal deal = groupDealRepository.findById(dealId)
                .orElseThrow(() -> new EntityNotFoundException("Group deal with id " + dealId + " not found"));

        if(!deal.getRestaurant().equals(restaurant))
            throw new AccessDeniedException("This deal does not belong to your restaurant");

        if(deal.getStatus() !=  GroupDealStatus.VOTING)
            throw new IllegalStateException("Cannot participate in this deal as it is not in voting stage");

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new EntityNotFoundException("Address with id " + request.getAddressId() + " not found"));

        GroupDealParticipation participation = new GroupDealParticipation();
        participation.setGroupDeal(deal);
        participation.setUser(user);
        participation.setQuantity(request.getQuantity());
        participation.setPaymentStatus(PaymentStatus.SUCCESS);
        participation.setAmountPaid(deal.getOriginalPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
        participation.setAddressToDeliver(address);

        groupDealParticipationRepository.save(participation);

        return mapToGroupDealParticipationResponse(participation);
    }

    @Transactional
    public String withdrawFromGroupDeal(Long restaurantId, Long dealId) {
        User user = userAuthorization.authorizeUser();

        GroupDeal deal = groupDealRepository.findById(dealId)
                .orElseThrow(() -> new EntityNotFoundException("Group deal with id " + dealId + " not found"));

        if(deal.getStatus() == GroupDealStatus.FULFILLED || deal.getStatus() == GroupDealStatus.EXPIRED || deal.getStatus() == GroupDealStatus.DELETED)
            throw new IllegalStateException("Cannot withdraw from this deal as it is already " + deal.getStatus());

        GroupDealParticipation participation = groupDealParticipationRepository.findByUserAndGroupDeal(user, deal);

        participation.setConfirmed(false);
        groupDealParticipationRepository.save(participation);

        participation.getUser().setWalletBalance(participation.getUser().getWalletBalance().add(participation.getAmountPaid()));
        userRepository.save(participation.getUser());

        return "You have successfully withdrawn from the group deal with id " + dealId;
    }

    //------------------------------------------------HELPER METHODS------------------------------------------------


    private GroupDealResponse mapToGroupDealResponse(GroupDeal deal) {
        // This method would convert a GroupDeal entity to a GroupDealResponse DTO.
        // It would involve mapping all the relevant fields and possibly fetching related data like food items and discount tiers.

        List<FoodSummary> foodList = deal.getFoodList().stream()
                .map(food -> new FoodSummary(food.getFoodId(), food.getFoodName(),
                        food.getFoodDescription(), food.getFoodPrice(), food.isVegetarian()))
                .toList();

        List<GroupDealTierResponse> discountList = deal.getDiscountList().stream()
                .map(tier -> new GroupDealTierResponse(tier.getThresholdPercent(), tier.getDiscountPercent()))
                .toList();

        RestaurantSummary restaurant = new  RestaurantSummary(
                deal.getRestaurant().getRestaurantId(),
                deal.getRestaurant().getRestaurantName(),
                deal.getRestaurant().getCuisineType(),
                deal.getRestaurant().getAvgRating()
        );

        Integer currentParticipation = groupDealParticipationRepository.getTotalParticipantsByDeal(deal.getDealId());
        if(currentParticipation == null) currentParticipation = 0;

        return new GroupDealResponse(
                deal.getDealId(),
                deal.getDealName(),
                restaurant,
                deal.getStartTime(),
                deal.getEndTime(),
                foodList,
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

    private List<Food> fetchFoodList(List<Long> foodIds, Long restaurantId) {
        List<Food> foodList = foodRepository.findAllById(foodIds);

        if(foodList.size() != foodIds.size()) {
            throw new EntityNotFoundException("Some Food didn't exist in this restaurant");
        }

        foodList.forEach(food -> {
            if(!food.getRestaurant().getRestaurantId().equals(restaurantId))
                throw new EntityMisMatchAssociationException("Food " + food.getFoodName() + " does not belong to this restaurant");
        });


        return foodList;
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

    private GroupDealParticipationResponse mapToGroupDealParticipationResponse(GroupDealParticipation participation) {
        return new GroupDealParticipationResponse(
                participation.getParticipantId(),
                participation.getUser().getUserId(),
                participation.getUser().getFirstName() + " " + participation.getUser().getLastName(),
                participation.getUser().getEmail(),
                participation.getGroupDeal().getDealName(),
                participation.getQuantity(),
                participation.getPaymentStatus(),
                participation.getAmountPaid(),
                participation.isConfirmed()
        );
    }

    public List<GroupDealParticipationResponse> getParticipationsByDeal(Long restaurantId, Long dealId) {
        User user = userAuthorization.authorizeUser();
        
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(restaurantId));
        
        GroupDeal deal = groupDealRepository.findById(dealId)
                .orElseThrow(() -> new EntityNotFoundException("Group deal with id " + dealId + " not found"));

        if(!restaurant.getOwner().equals(user)) {
            throw new AccessDeniedException("You are not the owner of this restaurant");
        }

        if(!deal.getRestaurant().equals(restaurant)) {
            throw new AccessDeniedException("This deal does not belong to your restaurant");
        }
        
        return groupDealParticipationRepository.findGroupDealParticipationsByGroupDeal(deal).stream()
                .map(this::mapToGroupDealParticipationResponse)
                .toList();
    }
}
