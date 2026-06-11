package com.kilgore.fooddeliveryapp.service;

import com.kilgore.fooddeliveryapp.catalog.dto.request.AddStaffRequest;
import com.kilgore.fooddeliveryapp.catalog.dto.request.ContactInformationDto;
import com.kilgore.fooddeliveryapp.catalog.dto.request.RestaurantAddressDto;
import com.kilgore.fooddeliveryapp.catalog.dto.request.RestaurantRequest;
import com.kilgore.fooddeliveryapp.catalog.dto.request.RestaurantStatusRequest;
import com.kilgore.fooddeliveryapp.catalog.dto.response.RestaurantResponse;
import com.kilgore.fooddeliveryapp.catalog.model.ContactInformation;
import com.kilgore.fooddeliveryapp.catalog.model.CuisineType;
import com.kilgore.fooddeliveryapp.catalog.model.Restaurant;
import com.kilgore.fooddeliveryapp.catalog.model.RestaurantAddress;
import com.kilgore.fooddeliveryapp.catalog.model.RestaurantStatus;
import com.kilgore.fooddeliveryapp.catalog.repository.RestaurantRepository;
import com.kilgore.fooddeliveryapp.catalog.service.RestaurantService;
import com.kilgore.fooddeliveryapp.common.exceptions.RestaurantAlreadyExistsException;
import com.kilgore.fooddeliveryapp.common.exceptions.RestaurantNotFoundException;
import com.kilgore.fooddeliveryapp.common.util.UserAuthorization;
import com.kilgore.fooddeliveryapp.identity.api.UserFacade;
import com.kilgore.fooddeliveryapp.identity.dto.response.StaffCreationResponse;
import com.kilgore.fooddeliveryapp.identity.dto.summary.StaffSummary;
import com.kilgore.fooddeliveryapp.identity.dto.summary.UserSummary;
import com.kilgore.fooddeliveryapp.identity.model.UserRole;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long ADMIN_ID = 99L;
    private static final String OWNER_EMAIL = "owner@example.com";

    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private UserAuthorization userAuthorization;
    @Mock
    private UserFacade userFacade;

    @InjectMocks
    private RestaurantService restaurantService;

    private Validator validator;

    @BeforeEach
    void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        restaurantService = new RestaurantService(restaurantRepository, userAuthorization, validator, userFacade);
    }

    @Test
    void createRestaurant_createsWhenUnique() {
        RestaurantRequest request = createRestaurantRequest("Spice Yard");

        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(restaurantRepository.findRestaurantByRestaurantNameAndAddress_City("Spice Yard", "Pune"))
                .thenReturn(null);
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> {
            Restaurant saved = invocation.getArgument(0);
            saved.setRestaurantId(55L);
            return saved;
        });
        when(userFacade.getUserById(OWNER_ID)).thenReturn(new UserSummary(OWNER_ID, OWNER_EMAIL));

        RestaurantResponse response = restaurantService.createRestaurant(request);

        assertEquals(55L, response.getRestaurantId());
        assertEquals("Spice Yard", response.getRestaurantName());
        assertTrue(response.isOpen());
        assertEquals(RestaurantStatus.ACTIVE, response.getRestaurantStatus());
    }

    @Test
    void createRestaurant_throwsWhenRestaurantAlreadyExists() {
        RestaurantRequest request = createRestaurantRequest("Spice Yard");
        when(restaurantRepository.findRestaurantByRestaurantNameAndAddress_City("Spice Yard", "Pune"))
                .thenReturn(createRestaurant(10L, OWNER_ID));

        assertThrows(RestaurantAlreadyExistsException.class, () -> restaurantService.createRestaurant(request));
        verify(restaurantRepository, never()).save(any(Restaurant.class));
    }

    @Test
    void getAllRestaurants_returnsMappedResponses() {
        Restaurant first = createRestaurant(1L, OWNER_ID);
        Restaurant second = createRestaurant(2L, OWNER_ID);

        when(restaurantRepository.findAll()).thenReturn(List.of(first, second));
        when(userFacade.getUserById(OWNER_ID)).thenReturn(new UserSummary(OWNER_ID, OWNER_EMAIL));

        List<RestaurantResponse> responses = restaurantService.getAllRestaurants();

        assertEquals(2, responses.size());
        assertEquals("Tasty Hub", responses.get(0).getRestaurantName());
        assertEquals("Tasty Hub", responses.get(1).getRestaurantName());
    }

    @Test
    void getAllRestaurants_returnsEmptyListWhenNoRestaurants() {
        when(restaurantRepository.findAll()).thenReturn(List.of());

        List<RestaurantResponse> responses = restaurantService.getAllRestaurants();

        assertTrue(responses.isEmpty());
    }

    @Test
    void getRestaurant_throwsWhenNotFound() {
        when(restaurantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RestaurantNotFoundException.class, () -> restaurantService.getRestaurant(99L));
    }

    @Test
    void getRestaurant_returnsMappedResponse() {
        Restaurant restaurant = createRestaurant(10L, OWNER_ID);

        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));
        when(userFacade.getUserById(OWNER_ID)).thenReturn(new UserSummary(OWNER_ID, OWNER_EMAIL));

        RestaurantResponse response = restaurantService.getRestaurant(10L);

        assertEquals(10L, response.getRestaurantId());
        assertEquals("Tasty Hub", response.getRestaurantName());
        assertEquals(OWNER_EMAIL, response.getOwner().getUserName());
    }

    @Test
    void updateRestaurant_updatesWhenOwner() {
        Restaurant restaurant = createRestaurant(10L, OWNER_ID);
        RestaurantRequest request = createRestaurantRequest("New Name");

        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));
        when(userFacade.getUserById(OWNER_ID)).thenReturn(new UserSummary(OWNER_ID, OWNER_EMAIL));

        RestaurantResponse response = restaurantService.updateRestaurant(request, 10L);

        assertEquals("New Name", response.getRestaurantName());
        assertEquals("Pune", response.getAddress().getCity());
        verify(restaurantRepository).save(restaurant);
    }

    @Test
    void updateRestaurant_allowsAdminRole() {
        Restaurant restaurant = createRestaurant(10L, OWNER_ID);
        RestaurantRequest request = createRestaurantRequest("Admin Update");

        when(userAuthorization.authorizeUserId()).thenReturn(ADMIN_ID);
        when(userFacade.isUserAdmin(ADMIN_ID)).thenReturn(true);
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));
        when(userFacade.getUserById(OWNER_ID)).thenReturn(new UserSummary(OWNER_ID, OWNER_EMAIL));

        RestaurantResponse response = restaurantService.updateRestaurant(request, 10L);

        assertEquals("Admin Update", response.getRestaurantName());
        verify(restaurantRepository).save(restaurant);
    }

    @Test
    void updateRestaurant_throwsWhenNotOwnerOrAdmin() {
        Restaurant restaurant = createRestaurant(10L, OWNER_ID);
        RestaurantRequest request = createRestaurantRequest("Blocked");

        when(userAuthorization.authorizeUserId()).thenReturn(2L);
        when(userFacade.isUserAdmin(2L)).thenReturn(false);
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));

        assertThrows(AccessDeniedException.class, () -> restaurantService.updateRestaurant(request, 10L));
        verify(restaurantRepository, never()).save(any(Restaurant.class));
    }

    @Test
    void updateRestaurant_throwsWhenRestaurantMissing() {
        RestaurantRequest request = createRestaurantRequest("Missing");

        when(restaurantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RestaurantNotFoundException.class, () -> restaurantService.updateRestaurant(request, 99L));
    }

    @Test
    void updateRestaurant_updatesImagesWhenProvided() {
        Restaurant restaurant = createRestaurant(10L, OWNER_ID);
        RestaurantRequest request = createRestaurantRequest("Updated Images");
        request.setImages(List.of("new.png"));

        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));
        when(userFacade.getUserById(OWNER_ID)).thenReturn(new UserSummary(OWNER_ID, OWNER_EMAIL));

        RestaurantResponse response = restaurantService.updateRestaurant(request, 10L);

        assertEquals(List.of("new.png"), response.getImages());
        verify(restaurantRepository).save(restaurant);
    }

    @Test
    void updateRestaurant_keepsExistingImagesWhenRequestImagesNull() {
        Restaurant restaurant = createRestaurant(10L, OWNER_ID);
        RestaurantRequest request = createRestaurantRequest("Keep Images");
        request.setImages(null);

        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));
        when(userFacade.getUserById(OWNER_ID)).thenReturn(new UserSummary(OWNER_ID, OWNER_EMAIL));

        RestaurantResponse response = restaurantService.updateRestaurant(request, 10L);

        assertEquals(List.of("img1.png"), response.getImages());
        verify(restaurantRepository).save(restaurant);
    }

    @Test
    void updateRestaurantStatus_updatesOpenFlag() {
        Restaurant restaurant = createRestaurant(10L, OWNER_ID);
        restaurant.setOpen(true);

        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));
        when(userFacade.getUserById(OWNER_ID)).thenReturn(new UserSummary(OWNER_ID, OWNER_EMAIL));

        RestaurantResponse response = restaurantService.updateRestaurantStatus(10L, new RestaurantStatusRequest(false));

        assertFalse(response.isOpen());
        verify(restaurantRepository).save(restaurant);
    }

    @Test
    void updateRestaurantStatus_throwsWhenRestaurantMissing() {
        when(restaurantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RestaurantNotFoundException.class,
                () -> restaurantService.updateRestaurantStatus(99L, new RestaurantStatusRequest(true)));
    }

    @Test
    void getMyRestaurants_filtersByOwnerUserId() {
        Restaurant owned = createRestaurant(1L, OWNER_ID);
        Restaurant other = createRestaurant(2L, 2L);

        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(restaurantRepository.findAllByOwnerUserId(OWNER_ID)).thenReturn(List.of(owned));
        when(userFacade.getUserById(OWNER_ID)).thenReturn(new UserSummary(OWNER_ID, OWNER_EMAIL));

        List<RestaurantResponse> responses = restaurantService.getMyRestaurants();

        assertEquals(1, responses.size());
        assertEquals(OWNER_EMAIL, responses.get(0).getOwner().getUserName());
        verify(restaurantRepository, never()).findAll();
    }

    @Test
    void getMyRestaurants_returnsEmptyWhenNoOwnedRestaurants() {
        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(restaurantRepository.findAllByOwnerUserId(OWNER_ID)).thenReturn(List.of());

        List<RestaurantResponse> responses = restaurantService.getMyRestaurants();

        assertTrue(responses.isEmpty());
    }

    @Test
    void addStaffToRestaurant_createsStaffForOwner() {
        Restaurant restaurant = createRestaurant(10L, OWNER_ID);
        AddStaffRequest request = new AddStaffRequest("Sam", "Cook", "sam@example.com", "9876543210");
        StaffCreationResponse staffResponse = new StaffCreationResponse("Sam Cook", "sam@example.com", "9876543210",
                UserRole.RESTAURANT_STAFF, "Sam@210***");

        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));
        when(userFacade.createStaff(request, 10L)).thenReturn(staffResponse);

        StaffCreationResponse response = restaurantService.addStaffToRestaurant(10L, request);

        assertEquals("Sam", response.getName().split(" ")[0]);
        assertTrue(response.getPassword().contains("Sam@210***"));
        verify(userFacade).createStaff(request, 10L);
    }

    @Test
    void addStaffToRestaurant_throwsWhenNotOwner() {
        Restaurant restaurant = createRestaurant(10L, OWNER_ID);
        AddStaffRequest request = new AddStaffRequest("Sam", "Cook", "sam@example.com", "9876543210");

        when(userAuthorization.authorizeUserId()).thenReturn(2L);
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));

        assertThrows(AccessDeniedException.class, () -> restaurantService.addStaffToRestaurant(10L, request));
        verify(userFacade, never()).createStaff(any(), any());
    }

    @Test
    void addStaffToRestaurant_throwsWhenRestaurantNotFound() {
        AddStaffRequest request = new AddStaffRequest("Sam", "Cook", "sam@example.com", "9876543210");

        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(restaurantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RestaurantNotFoundException.class, () -> restaurantService.addStaffToRestaurant(99L, request));
    }

    @Test
    void addStaffToRestaurant_throwsWhenUnauthenticated() {
        AddStaffRequest request = new AddStaffRequest("Sam", "Cook", "sam@example.com", "9876543210");

        when(userAuthorization.authorizeUserId()).thenThrow(new AccessDeniedException("No valid authority"));

        assertThrows(AccessDeniedException.class, () -> restaurantService.addStaffToRestaurant(10L, request));
        verify(restaurantRepository, never()).findById(any(Long.class));
    }

    @Test
    void getAllStaff_returnsOnlyStaffMembers() {
        Restaurant restaurant = createRestaurant(10L, OWNER_ID);
        StaffSummary staff = new StaffSummary(5L, "Mia Wong", "staff@example.com", "9876543210",
                UserRole.RESTAURANT_STAFF, false);

        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));
        when(userFacade.getRestaurantStaff(10L)).thenReturn(List.of(staff));

        List<StaffSummary> staffList = restaurantService.getAllStaff(10L);

        assertEquals(1, staffList.size());
        assertEquals("Mia Wong", staffList.get(0).getName());
    }

    @Test
    void getAllStaff_returnsEmptyWhenNoStaffMembers() {
        Restaurant restaurant = createRestaurant(10L, OWNER_ID);

        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));
        when(userFacade.getRestaurantStaff(10L)).thenReturn(List.of());

        List<StaffSummary> staffList = restaurantService.getAllStaff(10L);

        assertTrue(staffList.isEmpty());
    }

    @Test
    void getAllStaff_throwsWhenOwnerMismatch() {
        Restaurant restaurant = createRestaurant(10L, OWNER_ID);

        when(userAuthorization.authorizeUserId()).thenReturn(2L);
        when(userFacade.isUserAdmin(2L)).thenReturn(false);
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));

        assertThrows(AccessDeniedException.class, () -> restaurantService.getAllStaff(10L));
    }

    @Test
    void getAllStaff_allowsAdminRole() {
        Restaurant restaurant = createRestaurant(10L, OWNER_ID);
        StaffSummary staff = new StaffSummary(5L, "Staff User", "staff@example.com", "9876543210",
                UserRole.RESTAURANT_STAFF, false);

        when(userAuthorization.authorizeUserId()).thenReturn(ADMIN_ID);
        when(userFacade.isUserAdmin(ADMIN_ID)).thenReturn(true);
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));
        when(userFacade.getRestaurantStaff(10L)).thenReturn(List.of(staff));

        List<StaffSummary> staffList = restaurantService.getAllStaff(10L);

        assertEquals(1, staffList.size());
    }

    @Test
    void getAllStaff_throwsWhenUnauthenticated() {
        when(userAuthorization.authorizeUserId()).thenThrow(new AccessDeniedException("No valid authority"));

        assertThrows(AccessDeniedException.class, () -> restaurantService.getAllStaff(10L));
        verify(restaurantRepository, never()).findById(any(Long.class));
    }

    @Test
    void createRestaurant_setsOpenTrue_when24Hours() {
        RestaurantRequest request = createRestaurantRequest("24x7 Cafe");
        request.setOpeningTime(LocalTime.of(9, 0));
        request.setClosingTime(LocalTime.of(9, 0));

        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(restaurantRepository.findRestaurantByRestaurantNameAndAddress_City(any(), any())).thenReturn(null);
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userFacade.getUserById(OWNER_ID)).thenReturn(new UserSummary(OWNER_ID, OWNER_EMAIL));

        RestaurantResponse response = restaurantService.createRestaurant(request);

        assertTrue(response.isOpen());
    }

    @Test
    void createRestaurant_setsOpenFalse_whenOutsideSameDayHours() {
        RestaurantRequest request = createRestaurantRequest("Day Cafe");
        request.setOpeningTime(LocalTime.of(10, 0));
        request.setClosingTime(LocalTime.of(12, 0));

        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(restaurantRepository.findRestaurantByRestaurantNameAndAddress_City(any(), any())).thenReturn(null);
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userFacade.getUserById(OWNER_ID)).thenReturn(new UserSummary(OWNER_ID, OWNER_EMAIL));

        RestaurantResponse response = restaurantService.createRestaurant(request);

        assertFalse(response.isOpen());
    }

    @Test
    void createRestaurant_setsOpenTrue_forOvernightHours() {
        RestaurantRequest request = createRestaurantRequest("Night Hub");
        request.setOpeningTime(LocalTime.of(18, 0));
        request.setClosingTime(LocalTime.of(2, 0));

        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(restaurantRepository.findRestaurantByRestaurantNameAndAddress_City(any(), any())).thenReturn(null);
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userFacade.getUserById(OWNER_ID)).thenReturn(new UserSummary(OWNER_ID, OWNER_EMAIL));

        RestaurantResponse response = restaurantService.createRestaurant(request);

        assertTrue(response.isOpen());
    }

    @Test
    void addStaffToRestaurant_throwsWhenPhoneTooShort() {
        Restaurant restaurant = createRestaurant(10L, OWNER_ID);
        AddStaffRequest request = new AddStaffRequest("Sam", "Cook", "sam@example.com", "123");

        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));
        when(userFacade.createStaff(eq(request), eq(10L))).thenThrow(new StringIndexOutOfBoundsException());

        assertThrows(StringIndexOutOfBoundsException.class,
                () -> restaurantService.addStaffToRestaurant(10L, request));
    }

    @Test
    void addStaffToRestaurant_throwsWhenPhoneNull() {
        Restaurant restaurant = createRestaurant(10L, OWNER_ID);
        AddStaffRequest request = new AddStaffRequest("Sam", "Cook", "sam@example.com", null);

        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));
        when(userFacade.createStaff(eq(request), eq(10L))).thenThrow(new NullPointerException());

        assertThrows(NullPointerException.class,
                () -> restaurantService.addStaffToRestaurant(10L, request));
    }

    @Test
    void createRestaurant_throwsWhenUnauthorized() {
        RestaurantRequest request = createRestaurantRequest("Ghost Kitchen");

        when(userAuthorization.authorizeUserId()).thenThrow(new AccessDeniedException("No valid authority"));
        when(restaurantRepository.findRestaurantByRestaurantNameAndAddress_City(any(), any())).thenReturn(null);

        assertThrows(AccessDeniedException.class, () -> restaurantService.createRestaurant(request));
    }

    @Test
    void getRestaurant_mapsAllFieldsCorrectly() {
        Restaurant restaurant = createRestaurant(10L, OWNER_ID);

        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));
        when(userFacade.getUserById(OWNER_ID)).thenReturn(new UserSummary(OWNER_ID, OWNER_EMAIL));

        RestaurantResponse response = restaurantService.getRestaurant(10L);

        assertEquals(10L, response.getRestaurantId());
        assertEquals("Tasty Hub", response.getRestaurantName());
        assertEquals("Comfort food", response.getRestaurantDescription());
        assertEquals(CuisineType.INDIAN, response.getCuisineType());
        assertEquals("Pune", response.getAddress().getCity());
        assertEquals("MH", response.getAddress().getState());
        assertEquals("tasty@example.com", response.getContactInformation().getEmail());
        assertEquals(LocalTime.of(9, 0), response.getOpeningTime());
        assertEquals(LocalTime.of(21, 0), response.getClosingTime());
        assertEquals(List.of("img1.png"), response.getImages());
        assertTrue(response.isOpen());
        assertEquals(RestaurantStatus.ACTIVE, response.getRestaurantStatus());
    }

    @Test
    void createRestaurant_setsOpenTrue_whenWithinSameDayHours() {
        RestaurantRequest request = createRestaurantRequest("Lunch Spot");
        request.setOpeningTime(LocalTime.of(0, 0));
        request.setClosingTime(LocalTime.of(23, 59));

        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(restaurantRepository.findRestaurantByRestaurantNameAndAddress_City(any(), any())).thenReturn(null);
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userFacade.getUserById(OWNER_ID)).thenReturn(new UserSummary(OWNER_ID, OWNER_EMAIL));

        RestaurantResponse response = restaurantService.createRestaurant(request);

        assertTrue(response.isOpen());
    }

    @Test
    void createRestaurant_throwsWhenAddressNull() {
        RestaurantRequest request = createRestaurantRequest("Broken");
        request.setAddress(null);

        assertThrows(IllegalArgumentException.class, () -> restaurantService.createRestaurant(request));
    }

    @Test
    void createRestaurant_throwsWhenContactInfoNull() {
        RestaurantRequest request = createRestaurantRequest("Broken Contact");
        request.setContactInformation(null);

        assertThrows(IllegalArgumentException.class, () -> restaurantService.createRestaurant(request));
    }

    @Test
    void createRestaurant_throwsWhenOpeningTimeNull() {
        RestaurantRequest request = createRestaurantRequest("No Time");
        request.setOpeningTime(null);

        assertThrows(IllegalArgumentException.class, () -> restaurantService.createRestaurant(request));
    }

    @Test
    void createRestaurant_throwsWhenClosingTimeNull() {
        RestaurantRequest request = createRestaurantRequest("No Close");
        request.setClosingTime(null);

        assertThrows(IllegalArgumentException.class, () -> restaurantService.createRestaurant(request));
    }

    @Test
    void addStaffToRestaurant_handlesNullFirstName() {
        Restaurant restaurant = createRestaurant(10L, OWNER_ID);
        AddStaffRequest request = new AddStaffRequest(null, "Cook", "sam@example.com", "9876543210");
        StaffCreationResponse staffResponse = new StaffCreationResponse("Cook", "sam@example.com", "9876543210",
                UserRole.RESTAURANT_STAFF, "null@210***");

        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));
        when(userFacade.createStaff(request, 10L)).thenReturn(staffResponse);

        StaffCreationResponse response = restaurantService.addStaffToRestaurant(10L, request);

        assertTrue(response.getPassword().contains("null@"));
    }

    @Test
    void addStaffToRestaurant_allowsNullEmail() {
        Restaurant restaurant = createRestaurant(10L, OWNER_ID);
        AddStaffRequest request = new AddStaffRequest("Sam", "Cook", null, "9876543210");
        StaffCreationResponse staffResponse = new StaffCreationResponse("Sam Cook", null, "9876543210",
                UserRole.RESTAURANT_STAFF, "Sam@210***");

        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));
        when(userFacade.createStaff(request, 10L)).thenReturn(staffResponse);

        StaffCreationResponse response = restaurantService.addStaffToRestaurant(10L, request);

        assertEquals(null, response.getEmail());
    }

    private Restaurant createRestaurant(Long id, Long ownerUserId) {
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantId(id);
        restaurant.setRestaurantName("Tasty Hub");
        restaurant.setRestaurantDescription("Comfort food");
        restaurant.setCuisineType(CuisineType.INDIAN);
        restaurant.setAddress(new RestaurantAddress("12A", "Main St", "Pune", "MH", "411001", "Near Park"));
        restaurant.setContactInformation(new ContactInformation("tasty@example.com", "9876543210", null, null, null));
        restaurant.setOpeningTime(LocalTime.of(9, 0));
        restaurant.setClosingTime(LocalTime.of(21, 0));
        restaurant.setImages(List.of("img1.png"));
        restaurant.setOwnerUserId(ownerUserId);
        restaurant.setRegistrationDate(LocalDate.now());
        restaurant.setOpen(true);
        restaurant.setRestaurantStatus(RestaurantStatus.ACTIVE);
        return restaurant;
    }

    private RestaurantRequest createRestaurantRequest(String name) {
        RestaurantRequest request = new RestaurantRequest();
        request.setRestaurantName(name);
        request.setRestaurantDescription("Cozy place");
        request.setCuisineType(CuisineType.INDIAN);
        request.setAddress(new RestaurantAddressDto("12A", "Main St", "Pune", "MH", "411001", "Near Park"));
        request.setContactInformation(new ContactInformationDto("contact@example.com", "9876543210", null, null, null));
        request.setOpeningTime(LocalTime.of(9, 0));
        request.setClosingTime(LocalTime.of(9, 0));
        request.setImages(List.of("img1.png"));
        return request;
    }
}
