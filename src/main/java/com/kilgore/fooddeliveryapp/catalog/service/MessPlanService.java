package com.kilgore.fooddeliveryapp.catalog.service;

import com.kilgore.fooddeliveryapp.catalog.model.Food;
import com.kilgore.fooddeliveryapp.catalog.model.MessPlan;
import com.kilgore.fooddeliveryapp.catalog.model.MessPlanSlot;
import com.kilgore.fooddeliveryapp.catalog.model.Restaurant;
import com.kilgore.fooddeliveryapp.common.util.UserAuthorization;
import com.kilgore.fooddeliveryapp.catalog.dto.request.AddMessPlanRequest;
import com.kilgore.fooddeliveryapp.catalog.dto.request.AddMessPlanSlotRequest;
import com.kilgore.fooddeliveryapp.catalog.dto.response.MessPlanResponse;
import com.kilgore.fooddeliveryapp.catalog.dto.response.MessPlanSlotResponse;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.FoodSummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityMisMatchAssociationException;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityUnavailableException;
import com.kilgore.fooddeliveryapp.common.exceptions.RestaurantNotFoundException;
import com.kilgore.fooddeliveryapp.identity.model.User;
import com.kilgore.fooddeliveryapp.catalog.repository.FoodRepository;
import com.kilgore.fooddeliveryapp.catalog.repository.MessPlanRepository;
import com.kilgore.fooddeliveryapp.catalog.repository.RestaurantRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
public class MessPlanService {

    private final UserAuthorization userAuthorization;
    private final RestaurantRepository restaurantRepository;
    private final MessPlanRepository messPlanRepository;
    private final FoodRepository foodRepository;

    public MessPlanService(UserAuthorization userAuthorization, RestaurantRepository restaurantRepository, MessPlanRepository messPlanRepository, FoodRepository foodRepository) {
        this.userAuthorization = userAuthorization;
        this.restaurantRepository = restaurantRepository;
        this.messPlanRepository = messPlanRepository;
        this.foodRepository = foodRepository;
    }


    public List<MessPlanResponse> getMessPlansForRestaurant(Long restaurantId) {
        return messPlanRepository.findVisibleByRestaurantId(restaurantId).stream()
                .map(this::createMessPlanResponse)
                .toList();
    }

    public MessPlanResponse getMessPlanById(Long restaurantId, Long messPlanId) {
        MessPlan messPlan = messPlanRepository.findById(messPlanId)
                .orElseThrow(()->new EntityNotFoundException("MessPlan not found"));

        if(!messPlan.getRestaurant().getRestaurantId().equals(restaurantId)) {
            throw new EntityMisMatchAssociationException("MessPlan does not belong to the specified restaurant.");
        }

        if(!messPlan.isActive() || messPlan.getDeletionScheduledAt() != null)
            throw new EntityUnavailableException("MessPlan : "+ messPlan.getMessPlanName() +" is not active");

        return createMessPlanResponse(messPlan);
    }

    @Transactional
    public MessPlanResponse addMessPlanToRestaurant(Long id, AddMessPlanRequest request) {
        User user = userAuthorization.authorizeUser();
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RestaurantNotFoundException(id));

        if(!restaurant.getOwner().getEmail().equals(user.getEmail())) {
            throw new AccessDeniedException("You are not authorized to modify this restaurant.");
        }

        MessPlan messPlan = new MessPlan();
        messPlan.setMessPlanName(request.getMessPlanName());
        messPlan.setDescription(request.getDescription());
        messPlan.setPrice(request.getPrice());
        messPlan.setRestaurant(restaurant);
        messPlan.setActive(true);
        messPlan.setDeletionScheduledAt(null);

        checkDistinctSlots(request.getSlots());
        /*
         will throw an error if something duplicate found,
         code will run normally otherwise as return type is - void
         and, it is called before saving because if anything wrong - why touch DB then?
        */

        messPlanRepository.save(messPlan);

        /*
         bul adding slots required a messPlan, so we have no choice but to save this object - messPlan
         even tough there might be a case, where we get corrupt foodIds in request
         so we add @Transactional here
        */

        List<MessPlanSlot> messPlanSlots = extractSlotsFromRequest(request, messPlan);
        messPlan.getSlots().clear();
        messPlan.getSlots().addAll(messPlanSlots);
        messPlanRepository.save(messPlan);

        return createMessPlanResponse(messPlan);
    }

    @Transactional
    public MessPlanResponse updateMessPlan(Long restaurantId, Long messPlanId,
                                           AddMessPlanRequest request) {
        User user = userAuthorization.authorizeUser();

        MessPlan messPlan = messPlanRepository.findById(messPlanId)
                .orElseThrow(() -> new EntityNotFoundException("MessPlan not found"));

        if(!messPlan.getRestaurant().getOwner().getEmail().equals(user.getEmail())) {
            throw new AccessDeniedException("You are not authorized to modify the MessPlan of this restaurant.");
        }

        if(!messPlan.getRestaurant().getRestaurantId().equals(restaurantId)) {
            throw new EntityMisMatchAssociationException("MessPlan does not belong to the specified restaurant.");
        }

        messPlan.setMessPlanName(request.getMessPlanName());
        messPlan.setDescription(request.getDescription());
        messPlan.setPrice(request.getPrice());

        checkDistinctSlots(request.getSlots());

        List<MessPlanSlot> updatedSlots = extractSlotsFromRequest(request, messPlan);
        messPlan.getSlots().clear();
        messPlan.getSlots().addAll(updatedSlots);
        messPlanRepository.save(messPlan);

        return createMessPlanResponse(messPlan);
    }



    private List<MessPlanSlot> extractSlotsFromRequest(AddMessPlanRequest request, MessPlan messPlan) {
        return request.getSlots().stream()
                .map(slotReq -> {
                    MessPlanSlot slot = new MessPlanSlot();
                    slot.setMessPlan(messPlan);
                    slot.setDayOfWeek(slotReq.getDayOfWeek());
                    slot.setMealType(slotReq.getMealType());

                    List<Food> foods = foodRepository.findAllById(slotReq.getFoodIds());

                    if(foods.size() != slotReq.getFoodIds().size()) {
                        throw new EntityNotFoundException("One or more food items not found");
                    }
                    // checking if there is any corrupt foodId - which doesn't exist in DB

                    slot.setFoodItems(foods);
                    return slot;
                })
                .toList();
    }


    private MessPlanResponse createMessPlanResponse(MessPlan messPlan) {

        RestaurantSummary restaurantSummary = new RestaurantSummary(
                messPlan.getRestaurant().getRestaurantId(),
                messPlan.getRestaurant().getRestaurantName(),
                messPlan.getRestaurant().getCuisineType(),
                messPlan.getRestaurant().getAvgRating()
        );

        List<MessPlanSlotResponse> slots = messPlan.getSlots().stream()
                .map(this::createMessPlanSlotResponse)
                .toList();

        return new MessPlanResponse(
                messPlan.getMessPlanId(),
                messPlan.getMessPlanName(),
                messPlan.getDescription(),
                messPlan.getPrice(),
                restaurantSummary,
                slots,
                messPlan.isActive()
        );
    }

    private FoodSummary createFoodSummary(Food food) {
        return new FoodSummary(
                food.getFoodId(),
                food.getFoodName(),
                food.getFoodDescription(),
                food.getFoodPrice(),
                food.isVegetarian()
        );
    }

    private MessPlanSlotResponse createMessPlanSlotResponse(MessPlanSlot slot) {
        List<FoodSummary> foods = slot.getFoodItems().stream()
                .map(this::createFoodSummary)
                .toList();

        return new MessPlanSlotResponse(
                slot.getSlotId(),
                slot.getDayOfWeek(),
                slot.getMealType(),
                foods
        );
    }

    private void checkDistinctSlots(List<AddMessPlanSlotRequest> slots) {
        long distinctCount = slots.stream()
                .map(s -> s.getDayOfWeek() + "-" + s.getMealType())
                .distinct()
                .count();

        if(distinctCount != slots.size()) {
            throw new IllegalArgumentException("Duplicate day+meal combinations found");
        }
    }

    public String deleteMessPlan(Long restaurantId, Long messPlanId) {
        User user = userAuthorization.authorizeUser();

        MessPlan messPlan = messPlanRepository.findById(messPlanId)
                .orElseThrow(() -> new EntityNotFoundException("MessPlan not found"));

        if(!messPlan.getRestaurant().getOwner().getEmail().equals(user.getEmail())) {
            throw new AccessDeniedException("You are not authorized to delete the MessPlan of this restaurant.");
        }

        if(!messPlan.getRestaurant().getRestaurantId().equals(restaurantId)) {
            throw new EntityMisMatchAssociationException("MessPlan does not belong to the specified restaurant.");
        }

        // Instead of deleting the mess plan, we will mark it as inactive and schedule it for deletion for next Monday

        LocalDateTime nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atStartOfDay();

        messPlan.setDeletionScheduledAt(nextMonday);
        messPlanRepository.save(messPlan);

        return "Mess Plan : " + messPlan.getMessPlanName() + " has been scheduled for deletion on " +
                nextMonday.toLocalDate() +
                " and the customer will be notified about this, " +
                "till then it will be active for the already subscribed customers " +
                "but won't be visible for new subscriptions";
    }
}
