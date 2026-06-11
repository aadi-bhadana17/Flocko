package com.kilgore.fooddeliveryapp.catalog.service;

import com.kilgore.fooddeliveryapp.catalog.dto.request.ContactInformationDto;
import com.kilgore.fooddeliveryapp.catalog.dto.request.RestaurantAddressDto;
import com.kilgore.fooddeliveryapp.catalog.dto.request.RestaurantRequest;
import com.kilgore.fooddeliveryapp.catalog.dto.request.RestaurantStatusRequest;
import com.kilgore.fooddeliveryapp.identity.dto.response.StaffCreationResponse;
import com.kilgore.fooddeliveryapp.catalog.model.Restaurant;
import com.kilgore.fooddeliveryapp.catalog.model.RestaurantStatus;
import com.kilgore.fooddeliveryapp.catalog.repository.RestaurantRepository;
import com.kilgore.fooddeliveryapp.common.util.UserAuthorization;
import com.kilgore.fooddeliveryapp.catalog.model.ContactInformation;
import com.kilgore.fooddeliveryapp.catalog.model.RestaurantAddress;
import com.kilgore.fooddeliveryapp.catalog.dto.response.RestaurantResponse;
import com.kilgore.fooddeliveryapp.common.exceptions.RestaurantAlreadyExistsException;
import com.kilgore.fooddeliveryapp.common.exceptions.RestaurantNotFoundException;
import com.kilgore.fooddeliveryapp.identity.api.UserFacade;
import com.kilgore.fooddeliveryapp.catalog.dto.request.AddStaffRequest;
import com.kilgore.fooddeliveryapp.identity.dto.summary.StaffSummary;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolation;


import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final UserAuthorization userAuthorization;
    private final Validator validator;
    private final UserFacade userFacade;

    public RestaurantService(RestaurantRepository restaurantRepository, UserAuthorization userAuthorization,
                             Validator validator, UserFacade userFacade) {
        this.restaurantRepository = restaurantRepository;
        this.userAuthorization = userAuthorization;
        this.validator = validator;
        this.userFacade = userFacade;
    }

    private void validate(Object obj) {
        Set<ConstraintViolation<Object>> violations = validator.validate(obj);

        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(
                    violations.iterator().next().getMessage()
            );
        }
    }

    public RestaurantResponse createRestaurant(RestaurantRequest request) {
        validate(request);
        Restaurant restaurant = restaurantRepository
                .findRestaurantByRestaurantNameAndAddress_City(
                        request.getRestaurantName(),
                        request.getAddress().getCity()
                );
        if (restaurant != null) {
            throw new RestaurantAlreadyExistsException();
        }

       Long ownerId = userAuthorization.authorizeUserId();

        restaurant = new Restaurant();

        restaurant.setOwnerUserId(ownerId);
        restaurant.setRestaurantName(request.getRestaurantName());
        restaurant.setRestaurantDescription(request.getRestaurantDescription());
        restaurant.setCuisineType(request.getCuisineType());
        restaurant.setAddress(mapToAddress(request.getAddress()));
        restaurant.setContactInformation(mapToContactInformation(request.getContactInformation()));
        restaurant.setOpeningTime(request.getOpeningTime());
        restaurant.setClosingTime(request.getClosingTime());
        restaurant.setImages(request.getImages());
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
                restaurant.getRestaurantDescription(),
                userFacade.getUserById(restaurant.getOwnerUserId()),
                restaurant.getCuisineType(),
                mapToAddressDto(restaurant.getAddress()),
                mapToContactInformationDto(restaurant.getContactInformation()),
                restaurant.getOpeningTime(),
                restaurant.getClosingTime(),
                restaurant.getImages(),
                restaurant.isOpen(),
                restaurant.getRestaurantStatus(),
                restaurant.getRegistrationDate()
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

        if (request.getOpeningTime() == null || request.getClosingTime() == null) {
            throw new IllegalArgumentException("Opening/Closing time cannot be null");
        }

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

    @CacheEvict(value = "restaurantMenu", key = "#id")
    public RestaurantResponse updateRestaurant(RestaurantRequest request, Long id) {
        Restaurant restaurant  = restaurantRepository.findById(id)
                .orElseThrow(() -> new RestaurantNotFoundException(id));

        Long userId = userAuthorization.authorizeUserId();

        if(!restaurant.getOwnerUserId().equals(userId) && !userFacade.isUserAdmin(userId)) {
            throw new AccessDeniedException("You are not authorized to modify this restaurant.");
        }

        restaurant.setRestaurantName(request.getRestaurantName());
        restaurant.setRestaurantDescription(request.getRestaurantDescription());
        restaurant.setAddress(mapToAddress(request.getAddress()));
        restaurant.setCuisineType(request.getCuisineType());
        restaurant.setOpeningTime(request.getOpeningTime());
        restaurant.setClosingTime(request.getClosingTime());
        if (request.getImages() != null) {
            restaurant.setImages(request.getImages());
        }
        restaurant.setContactInformation(mapToContactInformation(request.getContactInformation()));

        restaurantRepository.save(restaurant);
        return toDto(restaurant);
    }

    @CacheEvict(value = "restaurantMenu", key = "#id")
    public RestaurantResponse updateRestaurantStatus(Long id, RestaurantStatusRequest request) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RestaurantNotFoundException(id));

        restaurant.setOpen(request.isOpen());
        restaurantRepository.save(restaurant);
        return toDto(restaurant);
    }

    public List<RestaurantResponse> getMyRestaurants() {
        Long userId = userAuthorization.authorizeUserId();

        return restaurantRepository.findAllByOwnerUserId(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }


    public StaffCreationResponse addStaffToRestaurant(Long restaurantId, AddStaffRequest request) {
        Long userId = userAuthorization.authorizeUserId();

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(restaurantId));

        if(!restaurant.getOwnerUserId().equals(userId)) {
            throw new AccessDeniedException("You are not authorized to add staff to this restaurant.");
        }

        return userFacade.createStaff(request, restaurantId);
    }

    public List<StaffSummary> getAllStaff(Long restaurantId) {
        Long userId = userAuthorization.authorizeUserId();

        Restaurant restaurant =  restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(restaurantId));

        if(!restaurant.getOwnerUserId().equals(userId) && !userFacade.isUserAdmin(userId)) {
            throw new AccessDeniedException("You are not authorized to view the staff of this restaurant.");
        }

        return userFacade.getRestaurantStaff(restaurantId);
    }
}
