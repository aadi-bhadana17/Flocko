package com.kilgore.fooddeliveryapp.service;

import com.kilgore.fooddeliveryapp.common.util.UserAuthorization;
import com.kilgore.fooddeliveryapp.identity.dto.request.AddressRequest;
import com.kilgore.fooddeliveryapp.identity.dto.request.ChangePasswordRequest;
import com.kilgore.fooddeliveryapp.identity.dto.request.RoleChangeRequestDto;
import com.kilgore.fooddeliveryapp.identity.dto.request.UpdateProfileRequest;
import com.kilgore.fooddeliveryapp.identity.dto.response.AddressResponse;
import com.kilgore.fooddeliveryapp.catalog.api.CatalogFacade;
import com.kilgore.fooddeliveryapp.identity.dto.response.RoleChangeRequestResponse;
import com.kilgore.fooddeliveryapp.identity.dto.response.UserProfileResponse;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.common.exceptions.CredentialsNotMatchException;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.common.exceptions.UserAlreadyExistsException;
import com.kilgore.fooddeliveryapp.identity.service.UserService;
import com.kilgore.fooddeliveryapp.identity.model.Address;
import com.kilgore.fooddeliveryapp.catalog.model.CuisineType;
import com.kilgore.fooddeliveryapp.identity.model.RequestStatus;
import com.kilgore.fooddeliveryapp.identity.model.RoleChangeRequest;
import com.kilgore.fooddeliveryapp.identity.model.User;
import com.kilgore.fooddeliveryapp.identity.model.UserRole;
import com.kilgore.fooddeliveryapp.identity.repository.AddressRepository;
import com.kilgore.fooddeliveryapp.identity.repository.RoleChangeRequestRepository;
import com.kilgore.fooddeliveryapp.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleChangeRequestRepository roleChangeRequestRepository;
    @Mock
    private UserAuthorization userAuthorization;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private CatalogFacade catalogFacade;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserProfile_returnsMappedResponse() {
        User user = createUser(1L);
        when(userAuthorization.authorizeUser()).thenReturn(user);

        UserProfileResponse response = userService.getUserProfile();

        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertEquals("john@example.com", response.getEmail());
        assertEquals("9999999999", response.getPhone());
        assertEquals(UserRole.CUSTOMER, response.getRole());
        assertEquals(new BigDecimal("150.00"), response.getWalletBalance());
        verify(userAuthorization).authorizeUser();
    }

    @Test
    void updateUserProfile_updatesAndSavesWhenEmailIsUnique() {
        User user = createUser(1L);
        UpdateProfileRequest request = new UpdateProfileRequest("Jane", "Smith", "jane@example.com", "8888888888");

        when(userAuthorization.authorizeUser()).thenReturn(user);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(null);

        UserProfileResponse response = userService.updateUserProfile(request);

        assertEquals("Jane", user.getFirstName());
        assertEquals("Smith", user.getLastName());
        assertEquals("jane@example.com", user.getEmail());
        assertEquals("8888888888", user.getPhone());
        assertEquals("Jane", response.getFirstName());
        assertEquals("jane@example.com", response.getEmail());
        verify(userRepository).save(user);
    }

    @Test
    void updateUserProfile_allowsUsingOwnExistingEmail() {
        User user = createUser(1L);
        UpdateProfileRequest request = new UpdateProfileRequest("Jane", "Smith", "john@example.com", "8888888888");

        when(userAuthorization.authorizeUser()).thenReturn(user);
        when(userRepository.findByEmail("john@example.com")).thenReturn(user);

        UserProfileResponse response = userService.updateUserProfile(request);

        assertEquals("john@example.com", response.getEmail());
        verify(userRepository).save(user);
    }

    @Test
    void updateUserProfile_throwsWhenEmailBelongsToAnotherUser() {
        User currentUser = createUser(1L);
        User existingUser = createUser(2L);
        UpdateProfileRequest request = new UpdateProfileRequest("Jane", "Smith", "jane@example.com", "8888888888");

        when(userAuthorization.authorizeUser()).thenReturn(currentUser);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(existingUser);

        assertThrows(UserAlreadyExistsException.class, () -> userService.updateUserProfile(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_updatesPasswordWhenOldPasswordMatches() {
        User user = createUser(1L);
        ChangePasswordRequest request = new ChangePasswordRequest("old-pass", "new-pass-123", "new-pass-123");

        when(userAuthorization.authorizeUser()).thenReturn(user);
        when(passwordEncoder.matches("old-pass", user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("new-pass-123")).thenReturn("encoded-new-pass");

        String response = userService.changePassword(request);

        assertEquals("Password changed successfully", response);
        assertEquals("encoded-new-pass", user.getPassword());
        assertFalse(user.isTempPassword());
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_throwsWhenOldPasswordDoesNotMatch() {
        User user = createUser(1L);
        ChangePasswordRequest request = new ChangePasswordRequest("wrong-old", "new-pass-123", "new-pass-123");

        when(userAuthorization.authorizeUser()).thenReturn(user);
        when(passwordEncoder.matches("wrong-old", user.getPassword())).thenReturn(false);

        assertThrows(CredentialsNotMatchException.class, () -> userService.changePassword(request));
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any(String.class));
    }

    @Test
    void setDefaultAddress_switchesDefaultAddressAndReturnsUpdatedAddress() {
        User user = createUser(1L);
        Address oldDefault = createAddress(11L, user, true);
        Address newDefault = createAddress(22L, user, false);
        user.setAddresses(new ArrayList<>(List.of(oldDefault, newDefault)));

        when(userAuthorization.authorizeUser()).thenReturn(user);
        when(addressRepository.findById(22L)).thenReturn(Optional.of(newDefault));

        AddressResponse response = userService.setDefaultAddress(22L);

        assertFalse(oldDefault.isDefault());
        assertTrue(newDefault.isDefault());
        assertEquals(22L, response.getAddressId());

        ArgumentCaptor<Address> saveCaptor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository, times(2)).save(saveCaptor.capture());
        List<Address> savedAddresses = saveCaptor.getAllValues();
        assertTrue(savedAddresses.contains(oldDefault));
        assertTrue(savedAddresses.contains(newDefault));
    }

    @Test
    void setDefaultAddress_setsSelectedAddressWhenNoPreviousDefaultExists() {
        User user = createUser(1L);
        Address first = createAddress(11L, user, false);
        Address selected = createAddress(22L, user, false);
        user.setAddresses(new ArrayList<>(List.of(first, selected)));

        when(userAuthorization.authorizeUser()).thenReturn(user);
        when(addressRepository.findById(22L)).thenReturn(Optional.of(selected));

        AddressResponse response = userService.setDefaultAddress(22L);

        assertTrue(selected.isDefault());
        assertFalse(first.isDefault());
        assertEquals(22L, response.getAddressId());
        verify(addressRepository, times(1)).save(selected);
    }

    @Test
    void setDefaultAddress_throwsWhenAddressNotFound() {
        User user = createUser(1L);

        when(userAuthorization.authorizeUser()).thenReturn(user);
        when(addressRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.setDefaultAddress(404L));
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    void updateAddress_throwsWhenAddressBelongsToDifferentUser() {
        User currentUser = createUser(1L);
        User owner = createUser(2L);
        Address address = createAddress(15L, owner, false);
        AddressRequest request = new AddressRequest("A-1", "Street", "City", "123456", "State", "Near Park");

        when(userAuthorization.authorizeUser()).thenReturn(currentUser);
        when(addressRepository.findById(15L)).thenReturn(Optional.of(address));

        assertThrows(AccessDeniedException.class, () -> userService.updateAddress(15L, request));
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    void updateAddress_updatesOwnedAddress() {
        User user = createUser(1L);
        Address address = createAddress(15L, user, false);
        AddressRequest request = createAddressRequest("B-8", "MG Road", "Pune", "411001", "Maharashtra", "Near Metro");

        when(userAuthorization.authorizeUser()).thenReturn(user);
        when(addressRepository.findById(15L)).thenReturn(Optional.of(address));

        AddressResponse response = userService.updateAddress(15L, request);

        assertEquals("B-8", response.getBuildingNo());
        assertEquals("MG Road", response.getStreet());
        assertEquals("Pune", response.getCity());
        assertEquals("411001", response.getPincode());
        verify(addressRepository).save(address);
    }

    @Test
    void updateAddress_throwsWhenAddressNotFound() {
        User user = createUser(1L);

        when(userAuthorization.authorizeUser()).thenReturn(user);
        when(addressRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.updateAddress(999L, createAddressRequest("A", "S", "C", "1", "ST", null)));
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    void getAddresses_returnsEmptyListWhenUserHasNoAddresses() {
        User user = createUser(1L);
        user.setAddresses(new ArrayList<>());

        when(userAuthorization.authorizeUser()).thenReturn(user);

        List<AddressResponse> addresses = userService.getAddresses();

        assertTrue(addresses.isEmpty());
    }

    @Test
    void getAddresses_returnsMappedAddressList() {
        User user = createUser(1L);
        Address address = createAddress(10L, user, true);
        user.getAddresses().add(address);

        when(userAuthorization.authorizeUser()).thenReturn(user);

        List<AddressResponse> addresses = userService.getAddresses();

        assertEquals(1, addresses.size());
        assertEquals(10L, addresses.get(0).getAddressId());
        assertEquals("Park Street", addresses.get(0).getStreet());
        assertTrue(addresses.get(0).isDefault());
    }

    @Test
    void addAddress_addsAddressToUserAndSavesUser() {
        User user = createUser(1L);
        AddressRequest request = createAddressRequest("B-7", "Main St", "Delhi", "110001", "Delhi", "Near Mall");

        when(userAuthorization.authorizeUser()).thenReturn(user);

        AddressResponse response = userService.addAddress(request);

        assertEquals(1, user.getAddresses().size());
        assertEquals("Main St", response.getStreet());
        verify(userRepository).save(user);
    }

    @Test
    void deleteAddress_deletesOwnedAddress() {
        User user = createUser(1L);
        Address address = createAddress(99L, user, false);

        when(userAuthorization.authorizeUser()).thenReturn(user);
        when(addressRepository.findById(99L)).thenReturn(Optional.of(address));

        String message = userService.deleteAddress(99L);

        assertEquals("Address deleted successfully", message);
        verify(addressRepository).delete(address);
    }

    @Test
    void deleteAddress_throwsWhenAddressNotFound() {
        User user = createUser(1L);

        when(userAuthorization.authorizeUser()).thenReturn(user);
        when(addressRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.deleteAddress(99L));
        verify(addressRepository, never()).delete(any(Address.class));
    }

    @Test
    void deleteAddress_throwsWhenAddressBelongsToDifferentUser() {
        User currentUser = createUser(1L);
        User owner = createUser(2L);
        Address address = createAddress(99L, owner, false);

        when(userAuthorization.authorizeUser()).thenReturn(currentUser);
        when(addressRepository.findById(99L)).thenReturn(Optional.of(address));

        assertThrows(AccessDeniedException.class, () -> userService.deleteAddress(99L));
        verify(addressRepository, never()).delete(any(Address.class));
    }

    @Test
    void addFavouriteRestaurant_addsRestaurantToUserFavourites() {
        User user = createUser(1L);

        when(userAuthorization.authorizeUser()).thenReturn(user);

        String response = userService.addFavouriteRestaurant(50L);

        assertEquals("Restaurant added to favourites", response);
        assertTrue(user.getFavouriteRestaurantIds().contains(50L));
        verify(userRepository).save(user);
    }

    @Test
    void removeFavouriteRestaurant_removesRestaurantAndSaves() {
        User user = createUser(1L);
        user.getFavouriteRestaurantIds().add(70L);

        when(userAuthorization.authorizeUser()).thenReturn(user);

        String response = userService.removeFavouriteRestaurant(70L);

        assertEquals("Restaurant removed from favourites", response);
        assertTrue(user.getFavouriteRestaurantIds().isEmpty());
        verify(userRepository).save(user);
    }

    @Test
    void removeFavouriteRestaurant_returnsSuccessEvenWhenRestaurantNotInFavourites() {
        User user = createUser(1L);

        when(userAuthorization.authorizeUser()).thenReturn(user);

        String response = userService.removeFavouriteRestaurant(71L);

        assertEquals("Restaurant removed from favourites", response);
        assertTrue(user.getFavouriteRestaurantIds().isEmpty());
        verify(userRepository).save(user);
    }

    @Test
    void getFavouriteRestaurants_returnsMappedSummaries() {
        User user = createUser(1L);
        user.getFavouriteRestaurantIds().add(70L);
        RestaurantSummary summary = new RestaurantSummary(70L, "Spice Route", CuisineType.INDIAN, new BigDecimal("4.2"));

        when(userAuthorization.authorizeUser()).thenReturn(user);
        when(catalogFacade.getRestaurantById(70L)).thenReturn(summary);

        List<RestaurantSummary> summaries = userService.getFavouriteRestaurants();

        assertEquals(1, summaries.size());
        assertEquals(70L, summaries.get(0).getRestaurantId());
        assertEquals("Spice Route", summaries.get(0).getRestaurantName());
        assertEquals(CuisineType.INDIAN, summaries.get(0).getCuisineType());
        assertEquals(new BigDecimal("4.2"), summaries.get(0).getAvgRating());
    }

    @Test
    void createRoleChangeRequest_savesAndReturnsResponse() {
        User user = createUser(1L);
        RoleChangeRequestDto request = new RoleChangeRequestDto(UserRole.RESTAURANT_OWNER, "I run a kitchen");

        when(userAuthorization.authorizeUser()).thenReturn(user);
        when(roleChangeRequestRepository.save(any(RoleChangeRequest.class))).thenAnswer(invocation -> {
            RoleChangeRequest saved = invocation.getArgument(0);
            saved.setRequestId(44L);
            saved.setRequestStatus(RequestStatus.PENDING);
            saved.setRequestedAt(LocalDateTime.now());
            return saved;
        });

        RoleChangeRequestResponse response = userService.createRoleChangeRequest(request);

        assertEquals(44L, response.getRequestId());
        assertEquals("John Doe", response.getUserName());
        assertEquals("john@example.com", response.getUserEmail());
        assertEquals(UserRole.RESTAURANT_OWNER, response.getRequestedRole());
        assertEquals(RequestStatus.PENDING, response.getRequestStatus());
        assertEquals("I run a kitchen", response.getRequestReason());
        assertNull(response.getRespondedAt());
        assertNull(response.getAdminName());
        assertNull(response.getAdminEmail());

        ArgumentCaptor<RoleChangeRequest> captor = ArgumentCaptor.forClass(RoleChangeRequest.class);
        verify(roleChangeRequestRepository).save(captor.capture());
        assertEquals(user, captor.getValue().getUser());
        assertEquals(UserRole.RESTAURANT_OWNER, captor.getValue().getRequestedRole());
    }

    private User createUser(Long userId) {
        User user = new User();
        user.setUserId(userId);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@example.com");
        user.setPassword("stored-password");
        user.setPhone("9999999999");
        user.setRole(UserRole.CUSTOMER);
        user.setWalletBalance(new BigDecimal("150.00"));
        user.setTempPassword(true);
        user.setAddresses(new ArrayList<>());
        user.setFavouriteRestaurantIds(new ArrayList<>());
        return user;
    }

    private Address createAddress(Long addressId, User user, boolean isDefault) {
        Address address = new Address();
        address.setAddressId(addressId);
        address.setBuildingNo("B-12");
        address.setStreet("Park Street");
        address.setCity("Delhi");
        address.setState("Delhi");
        address.setPincode("110001");
        address.setLandmark("Near Mall");
        address.setUser(user);
        address.setDefault(isDefault);
        return address;
    }

    private AddressRequest createAddressRequest(String buildingNo, String street, String city, String pincode, String state, String landmark) {
        return new AddressRequest(buildingNo, street, city, pincode, state, landmark);
    }
}
