package com.kilgore.fooddeliveryapp.service;

import com.kilgore.fooddeliveryapp.authorization.UserAuthorization;
import com.kilgore.fooddeliveryapp.dto.request.*;
import com.kilgore.fooddeliveryapp.dto.response.OwnerResponse;
import com.kilgore.fooddeliveryapp.dto.response.RestaurantResponse;
import com.kilgore.fooddeliveryapp.dto.response.StaffCreationResponse;
import com.kilgore.fooddeliveryapp.dto.summary.UserSummary;
import com.kilgore.fooddeliveryapp.exceptions.RestaurantAlreadyExistsException;
import com.kilgore.fooddeliveryapp.exceptions.RestaurantNotFoundException;
import com.kilgore.fooddeliveryapp.model.*;
import com.kilgore.fooddeliveryapp.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final UserAuthorization userAuthorization;
    private final PasswordEncoder passwordEncoder;

    public RestaurantService(RestaurantRepository restaurantRepository, UserRepository userRepository, UserAuthorization userAuthorization, PasswordEncoder passwordEncoder) {
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
        this.userAuthorization = userAuthorization;
        this.passwordEncoder = passwordEncoder;
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
                mapToOwnerDto(restaurant.getOwner()),
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
        if (request.getImages() != null) {
            restaurant.setImages(request.getImages());
        }
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


    public StaffCreationResponse addStaffToRestaurant(Long id, AddStaffRequest request) {
        User user = userAuthorization.authorizeUser();

        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RestaurantNotFoundException(id));

        if(!restaurant.getOwner().getEmail().equals(user.getEmail())) {
            throw new AccessDeniedException("You are not authorized to add staff to this restaurant.");
        }

        String password = request.getFirstName() + "@" + request.getPhone().substring(7) + "***";

        User staff = new User();
        staff.setFirstName(request.getFirstName());
        staff.setLastName(request.getLastName());
        staff.setEmail(request.getEmail());
        staff.setPhone(request.getPhone());
        staff.setRole(UserRole.RESTAURANT_STAFF);
        staff.setEmployedAt(id);
        staff.setPassword(passwordEncoder.encode(password));
        staff.setTempPassword(true);

        userRepository.save(staff);

        return toDto(staff, password);
    }

    private StaffCreationResponse toDto(User user, String password) {
        return new StaffCreationResponse(
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                password
        );
    }

    public List<UserSummary> getAllStaff(Long id) {
        User user = userAuthorization.authorizeUser();

        Restaurant restaurant =  restaurantRepository.findById(id)
                .orElseThrow(() -> new RestaurantNotFoundException(id));

        if(user.getRole() == UserRole.RESTAURANT_OWNER && !restaurant.getOwner().getEmail().equals(user.getEmail())) {
            throw new AccessDeniedException("You are not authorized to view the staff of this restaurant.");
        }

        return userRepository.findByRestaurantId(id)
                .stream()
                .filter(u -> u.getRole() == UserRole.RESTAURANT_STAFF)
                .map(this::toUserSummary)
                .toList();
    }

    private UserSummary toUserSummary(User user) {
        return new UserSummary(
                user.getUserId(),
                user.getFirstName() + " " + user.getLastName()
        );
    }
}
