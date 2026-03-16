package com.kilgore.fooddeliveryapp.service;

import com.kilgore.fooddeliveryapp.authorization.UserAuthorization;
import com.kilgore.fooddeliveryapp.dto.request.*;
import com.kilgore.fooddeliveryapp.dto.response.MessPlanResponse;
import com.kilgore.fooddeliveryapp.dto.response.MessPlanSlotResponse;
import com.kilgore.fooddeliveryapp.dto.response.OwnerResponse;
import com.kilgore.fooddeliveryapp.dto.response.RestaurantResponse;
import com.kilgore.fooddeliveryapp.dto.summary.FoodSummary;
import com.kilgore.fooddeliveryapp.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.exceptions.RestaurantAlreadyExistsException;
import com.kilgore.fooddeliveryapp.exceptions.RestaurantNotFoundException;
import com.kilgore.fooddeliveryapp.model.*;
import com.kilgore.fooddeliveryapp.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final UserAuthorization userAuthorization;
    private final MessPlanRepository messPlanRepository;
    private final FoodRepository foodRepository;
    private final MessPlanSlotRepository messPlanSlotRepository;

    public RestaurantService(RestaurantRepository restaurantRepository, UserRepository userRepository, UserAuthorization userAuthorization, MessPlanRepository messPlanRepository, FoodRepository foodRepository, MessPlanSlotRepository messPlanSlotRepository) {
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
        this.userAuthorization = userAuthorization;
        this.messPlanRepository = messPlanRepository;
        this.foodRepository = foodRepository;
        this.messPlanSlotRepository = messPlanSlotRepository;
    }

    public RestaurantResponse createRestaurant(RestaurantRequest request) {
        Restaurant restaurant = restaurantRepository
                .findRestaurantByRestaurantNameAndAddress_City(
                        request.getRestaurantName(),
                        request.getAddress().getCity()
                );
        if (restaurant != null) {
            throw new RestaurantAlreadyExistsException();
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User owner = userRepository.findByEmail(email);

        restaurant = new Restaurant();

        restaurant.setOwner(owner);
        restaurant.setRestaurantName(request.getRestaurantName());
        restaurant.setRestaurantDescription(request.getRestaurantDescription());
        restaurant.setCuisineType(request.getCuisineType());
        restaurant.setAddress(mapToAddress(request.getAddress()));
        restaurant.setContactInformation(mapToContactInformation(request.getContactInformation()));
        restaurant.setOpeningTime(request.getOpeningTime());
        restaurant.setClosingTime(request.getClosingTime());
        restaurant.setRegistrationDate(LocalDate.now());
        restaurant.setOpen(isOpen(request));
        restaurant.setRestaurantStatus(RestaurantStatus.ACTIVE);

        restaurant = restaurantRepository.save(restaurant);

        return toDto(restaurant);
    }

    public List<RestaurantResponse> getAllRestaurants() {
        return restaurantRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    private RestaurantResponse toDto(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getRestaurantId(),
                restaurant.getRestaurantName(),
                mapToOwnerDto(restaurant.getOwner()),
                restaurant.getCuisineType(),
                mapToAddressDto(restaurant.getAddress()),
                mapToContactInformationDto(restaurant.getContactInformation()),
                restaurant.getOpeningTime(),
                restaurant.getClosingTime(),
                restaurant.isOpen(),
                restaurant.getRestaurantStatus(),
                restaurant.getRegistrationDate()
        );
    }

    private OwnerResponse mapToOwnerDto(User owner) {
        return new OwnerResponse(
                owner.getUserId(),
                owner.getFirstName() + " " + owner.getLastName(),
                owner.getEmail()
        );
    }


    private RestaurantAddress mapToAddress(RestaurantAddressDto dto) {
        return new RestaurantAddress(
                dto.getBuildingNo(),
                dto.getStreet(),
                dto.getCity(),
                dto.getState(),
                dto.getPincode(),
                dto.getLandmark()
        );
    }

    private ContactInformation mapToContactInformation(ContactInformationDto dto) {
        return new ContactInformation(
                dto.getEmail(),
                dto.getMobile(),
                dto.getInstagram(),
                dto.getFacebook(),
                dto.getTwitter()
        );
    }

    private RestaurantAddressDto mapToAddressDto(RestaurantAddress address) {
        return new RestaurantAddressDto(
                address.getBuildingNo(),
                address.getStreet(),
                address.getCity(),
                address.getState(),
                address.getPincode(),
                address.getLandmark()
        );
    }

    private ContactInformationDto mapToContactInformationDto(ContactInformation contactInformation) {
        return new ContactInformationDto(
                contactInformation.getEmail(),
                contactInformation.getMobile(),
                contactInformation.getInstagram(),
                contactInformation.getFacebook(),
                contactInformation.getTwitter()
        );
    }

    private boolean isOpen(RestaurantRequest request) {
        LocalTime now = LocalTime.now();
        LocalTime open = request.getOpeningTime();
        LocalTime close = request.getClosingTime();

        if (open.equals(close)) {
            return true; // 24-hour restaurant
        }

        if (open.isBefore(close)) {
            // same-day (10:00 → 22:00)
            return !now.isBefore(open) && !now.isAfter(close);
        } else {
            // overnight (18:00 → 02:00)
            return !now.isBefore(open) || !now.isAfter(close);
        }
    }


    public RestaurantResponse getRestaurant(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RestaurantNotFoundException(id));

        return toDto(restaurant);
    }

    public RestaurantResponse updateRestaurant(RestaurantRequest request, Long id) {
        Restaurant restaurant  = restaurantRepository.findById(id)
                .orElseThrow(() -> new RestaurantNotFoundException(id));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        if(!restaurant.getOwner().getEmail().equals(email) && !authentication.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN"))) {
            throw new AccessDeniedException("You are not authorized to modify this restaurant.");
        }

        restaurant.setRestaurantName(request.getRestaurantName());
        restaurant.setRestaurantDescription(request.getRestaurantDescription());
        restaurant.setAddress(mapToAddress(request.getAddress()));
        restaurant.setCuisineType(request.getCuisineType());
        restaurant.setOpeningTime(request.getOpeningTime());
        restaurant.setClosingTime(request.getClosingTime());
        restaurant.setContactInformation(mapToContactInformation(request.getContactInformation()));

        restaurantRepository.save(restaurant);
        return toDto(restaurant);
    }

    public RestaurantResponse updateRestaurantStatus(Long id, RestaurantStatusRequest request) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RestaurantNotFoundException(id));

        restaurant.setOpen(request.isOpen());
        restaurantRepository.save(restaurant);
        return toDto(restaurant);
    }

    public List<RestaurantResponse> getMyRestaurants() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        return restaurantRepository.findAll()
                .stream()
                .filter(r -> r.getOwner().getEmail().equals(email))
                .map(this::toDto)
                .toList();
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
        messPlan.setMessPlanDescription(request.getMessPlanDescription());
        messPlan.setPrice(request.getPrice());
        messPlan.setRestaurant(restaurant);

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

        for(AddMessPlanSlotRequest slot : request.getSlots()) {
            MessPlanSlot messPlanSlot = new MessPlanSlot();
            messPlanSlot.setMessPlan(messPlan);
            messPlanSlot.setDayOfWeek(slot.getDayOfWeek());
            messPlanSlot.setMealType(slot.getMealType());

            List<Food> foods = foodRepository.findAllById(slot.getFoodIds());

            if(foods.size() != slot.getFoodIds().size()) {
                throw new EntityNotFoundException("One or more food items not found");
            }
            // checking if there is any corrupt foodId - which doesn't exist in DB

            messPlanSlot.setFoodItems(foods);

            messPlanSlotRepository.save(messPlanSlot);
            messPlan.getSlots().add(messPlanSlot);
        }

        return createMessPlanResponse(messPlan);
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
                messPlan.getMessPlanDescription(),
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

}
