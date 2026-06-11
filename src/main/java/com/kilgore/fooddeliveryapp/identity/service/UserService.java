package com.kilgore.fooddeliveryapp.identity.service;

import com.kilgore.fooddeliveryapp.catalog.api.CatalogFacade;
import com.kilgore.fooddeliveryapp.common.util.UserAuthorization;
import com.kilgore.fooddeliveryapp.identity.model.Address;
import com.kilgore.fooddeliveryapp.common.enums.PaymentStatus;
import com.kilgore.fooddeliveryapp.identity.repository.AddressRepository;
import com.kilgore.fooddeliveryapp.identity.dto.request.AddressRequest;
import com.kilgore.fooddeliveryapp.identity.dto.request.ChangePasswordRequest;
import com.kilgore.fooddeliveryapp.identity.dto.request.RoleChangeRequestDto;
import com.kilgore.fooddeliveryapp.identity.dto.request.UpdateProfileRequest;
import com.kilgore.fooddeliveryapp.identity.dto.response.AddressResponse;
import com.kilgore.fooddeliveryapp.catalog.dto.response.MessSubscriptionResponse;
import com.kilgore.fooddeliveryapp.identity.dto.response.RoleChangeRequestResponse;
import com.kilgore.fooddeliveryapp.identity.dto.response.UserProfileResponse;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.MessPlanSummary;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.common.exceptions.CredentialsNotMatchException;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.common.exceptions.UserAlreadyExistsException;
import com.kilgore.fooddeliveryapp.identity.model.RoleChangeRequest;
import com.kilgore.fooddeliveryapp.identity.model.User;
import com.kilgore.fooddeliveryapp.identity.repository.RoleChangeRequestRepository;
import com.kilgore.fooddeliveryapp.identity.repository.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
public class UserService {


    private final UserRepository userRepository;
    private final RoleChangeRequestRepository roleChangeRequestRepository;
    private final UserAuthorization userAuthorization;
    private final AddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;
    private final CatalogFacade catalogFacade;

    public UserService(UserRepository userRepository, RoleChangeRequestRepository roleChangeRequestRepository,
                       UserAuthorization userAuthorization, AddressRepository addressRepository,
                       PasswordEncoder passwordEncoder, CatalogFacade catalogFacade) {
        this.userRepository = userRepository;
        this.roleChangeRequestRepository = roleChangeRequestRepository;
        this.userAuthorization = userAuthorization;
        this.addressRepository = addressRepository;
        this.passwordEncoder = passwordEncoder;
        this.catalogFacade = catalogFacade;
    }

    // -----------------------------------------------------Profile Management------------------------------------------------------


    public UserProfileResponse getUserProfile() {
        User user = userAuthorization.authorizeUser();
        return createResponse(user);
    }

    @Transactional
    public UserProfileResponse updateUserProfile(UpdateProfileRequest request) {
        User user = userAuthorization.authorizeUser();
        User existingUser = userRepository.findByEmail(request.getEmail());

        if(existingUser != null && !user.equals(existingUser))
            throw new UserAlreadyExistsException();

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());

        userRepository.save(user);

        return createResponse(user);
    }


    public String changePassword(ChangePasswordRequest request) {
        User user = userAuthorization.authorizeUser();

        String oldPassword = request.getOldPassword();

        if(!passwordEncoder.matches(oldPassword, user.getPassword()))  // as BCrypt generated random hash everytime, so we can just compare old password from request by hashing it
            throw new CredentialsNotMatchException("Old password is incorrect"); // and when we call this method - .matches(), BCrypt store salt(that random thing) from always in the hash so if they get compared easily - by applying the same salt to both the old password and the stored password hash.

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setTempPassword(false);
        userRepository.save(user);

        return "Password changed successfully";
    }

    // ------------------------------------------------------Address Management----------------------------------------------------


    @Transactional
    public List<AddressResponse> getAddresses() {
        User user = userAuthorization.authorizeUser();
        return user.getAddresses().stream()
                .map(this::createAddressResponse)
                .collect(toList());
    }

    @Transactional
    public AddressResponse addAddress(AddressRequest request) {
        User user = userAuthorization.authorizeUser();

        Address address = new Address();

        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        address.setLandmark(request.getLandmark());
        address.setBuildingNo(request.getBuildingNo());
        address.setUser(user);

        user.getAddresses().add(address);
        userRepository.save(user);

        return createAddressResponse(address);
    }

    @Transactional
    public AddressResponse updateAddress(Long addressId, @Valid AddressRequest request) {
        User user = userAuthorization.authorizeUser();

        Address address = validateAddressOwnership(addressId, user);

        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        address.setLandmark(request.getLandmark());
        address.setBuildingNo(request.getBuildingNo());

        addressRepository.save(address);

        return createAddressResponse(address);
    }

    @Transactional
    public AddressResponse setDefaultAddress(Long addressId) {
        User user = userAuthorization.authorizeUser();

        Address address = validateAddressOwnership(addressId, user);

        // Unset previous default address
        user.getAddresses().forEach(addr -> {
            if (addr.isDefault) {
                addr.isDefault = false;
                addressRepository.save(addr);
            }});

        address.setDefault(true);
        addressRepository.save(address);

        return createAddressResponse(address);
    }

    @Transactional
    public String deleteAddress(Long addressId) {
        User user = userAuthorization.authorizeUser();

        Address address = validateAddressOwnership(addressId, user);
        addressRepository.delete(address);
        return "Address deleted successfully";
    }


    // ------------------------------------------------------Favourite Restaurants----------------------------------------------------

    @Transactional
    public List<RestaurantSummary> getFavouriteRestaurants() {
        User user = userAuthorization.authorizeUser();

        return user.getFavouriteRestaurantIds().stream()
                .map(catalogFacade::getRestaurantById)
                .toList();
    }

    @Transactional
    public String addFavouriteRestaurant(Long restaurantId) {
        User user = userAuthorization.authorizeUser();

        user.getFavouriteRestaurantIds().add(restaurantId);
        userRepository.save(user);

        return "Restaurant added to favourites";
    }

    @Transactional
    public String removeFavouriteRestaurant(Long restaurantId) {
        User user = userAuthorization.authorizeUser();

        user.getFavouriteRestaurantIds().remove(restaurantId);
        userRepository.save(user);

        return "Restaurant removed from favourites";
    }


    // ------------------------------------------------------Role Change Request----------------------------------------------------

    public RoleChangeRequestResponse createRoleChangeRequest(RoleChangeRequestDto request) {
        User user = userAuthorization.authorizeUser();

        RoleChangeRequest roleChangeRequest = new RoleChangeRequest();
        roleChangeRequest.setUser(user);
        roleChangeRequest.setRequestedRole(request.getRequestedRole());
        roleChangeRequest.setRequestReason(request.getRequestReason());

        roleChangeRequestRepository.save(roleChangeRequest);

        return new RoleChangeRequestResponse(
                roleChangeRequest.getRequestId(),
                user.getFirstName() + " " + user.getLastName(),
                user.getEmail(),
                roleChangeRequest.getRequestedRole(),
                roleChangeRequest.getRequestStatus(),
                roleChangeRequest.getRequestReason(),
                roleChangeRequest.getRequestedAt(),
                null,
                null,
                null
        );
    }


    // ------------------------------------------------------Helper Methods---------------------------------------------------------

    private UserProfileResponse createResponse(User user) {
        return new UserProfileResponse(
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getWalletBalance()
        );
    }

    private AddressResponse createAddressResponse(Address address) {
        return new AddressResponse(
                address.getAddressId(),
                address.getBuildingNo(),
                address.getStreet(),
                address.getCity(),
                address.getState(),
                address.getPincode(),
                address.getLandmark(),
                address.isDefault()
        );
    }

    private Address validateAddressOwnership(Long addressId, User user) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new EntityNotFoundException("Address with this id not found"));

        if (!address.getUser().equals(user))
            throw new AccessDeniedException("This Address does not belongs to you");

        return address;
    }
}
