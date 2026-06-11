package com.kilgore.fooddeliveryapp.service;

import com.kilgore.fooddeliveryapp.catalog.dto.request.AddonAvailableStatusRequest;
import com.kilgore.fooddeliveryapp.catalog.dto.request.CreateAddonRequest;
import com.kilgore.fooddeliveryapp.catalog.dto.response.AddonResponse;
import com.kilgore.fooddeliveryapp.catalog.model.Addon;
import com.kilgore.fooddeliveryapp.catalog.model.Category;
import com.kilgore.fooddeliveryapp.catalog.model.Restaurant;
import com.kilgore.fooddeliveryapp.catalog.repository.AddonRepository;
import com.kilgore.fooddeliveryapp.catalog.repository.CategoryRepository;
import com.kilgore.fooddeliveryapp.catalog.repository.RestaurantRepository;
import com.kilgore.fooddeliveryapp.catalog.service.AddonService;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.common.exceptions.RestaurantNotFoundException;
import com.kilgore.fooddeliveryapp.common.util.UserAuthorization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddonServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long RESTAURANT_ID = 10L;

    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private AddonRepository addonRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserAuthorization userAuthorization;

    @InjectMocks
    private AddonService addonService;

    @BeforeEach
    void setUp() {
        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
    }

    @Test
    void createAddon_createsAddonWithoutCategoriesWhenCategoryIdsMissing() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        CreateAddonRequest request = createAddonRequest("Extra Cheese", true, null, new BigDecimal("49.00"));

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(addonRepository.save(any(Addon.class))).thenAnswer(invocation -> {
            Addon saved = invocation.getArgument(0);
            saved.setAddonId(100L);
            return saved;
        });

        AddonResponse response = addonService.createAddon(RESTAURANT_ID, request);

        assertEquals(100L, response.getAddonId());
        assertEquals("Extra Cheese", response.getAddonName());
        assertTrue(response.isAvailable());
        assertEquals(new BigDecimal("49.00"), response.getPrice());
        assertTrue(response.getCategories().isEmpty());

        ArgumentCaptor<Addon> captor = ArgumentCaptor.forClass(Addon.class);
        verify(addonRepository).save(captor.capture());
        assertEquals("Extra Cheese", captor.getValue().getAddonName());
        assertEquals(restaurant, captor.getValue().getRestaurant());
        verify(categoryRepository, never()).findAllById(any());
    }

    @Test
    void createAddon_linksCategoriesWhenIdsProvided() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        Category category = createCategory(50L, "Sides", restaurant);
        CreateAddonRequest request = createAddonRequest("Dip", true, List.of(50L), new BigDecimal("19.00"));

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(addonRepository.save(any(Addon.class))).thenAnswer(invocation -> {
            Addon saved = invocation.getArgument(0);
            saved.setAddonId(101L);
            return saved;
        });
        when(categoryRepository.findAllById(List.of(50L))).thenReturn(List.of(category));

        AddonResponse response = addonService.createAddon(RESTAURANT_ID, request);

        assertEquals(1, response.getCategories().size());
        assertEquals(50L, response.getCategories().get(0).getCategoryId());
        assertEquals("Sides", response.getCategories().get(0).getCategoryName());
        assertTrue(category.getAvailableAddons().stream().anyMatch(addon -> addon.getAddonId().equals(101L)));
        verify(categoryRepository).findAllById(List.of(50L));
    }

    @Test
    void createAddon_throwsWhenRestaurantNotFound() {
        CreateAddonRequest request = createAddonRequest("Sauce", true, List.of(1L), new BigDecimal("9.00"));

        when(restaurantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RestaurantNotFoundException.class, () -> addonService.createAddon(99L, request));
        verify(addonRepository, never()).save(any(Addon.class));
    }

    @Test
    void createAddon_throwsWhenCallerIsNotOwner() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        CreateAddonRequest request = createAddonRequest("Sauce", true, List.of(1L), new BigDecimal("9.00"));

        when(userAuthorization.authorizeUserId()).thenReturn(OTHER_USER_ID);
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));

        assertThrows(AccessDeniedException.class, () -> addonService.createAddon(RESTAURANT_ID, request));
        verify(addonRepository, never()).save(any(Addon.class));
    }

    @Test
    void createAddon_throwsWhenAnyCategoryBelongsToAnotherRestaurant() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        Restaurant anotherRestaurant = createRestaurant(11L, OWNER_ID);
        Category foreignCategory = createCategory(77L, "Foreign", anotherRestaurant);
        CreateAddonRequest request = createAddonRequest("Sauce", true, List.of(77L), new BigDecimal("9.00"));

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findAllById(List.of(77L))).thenReturn(List.of(foreignCategory));

        assertThrows(AccessDeniedException.class, () -> addonService.createAddon(RESTAURANT_ID, request));
        verify(addonRepository).save(any(Addon.class));
    }

    @Test
    void getAddons_returnsMappedResponses() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        Addon addon = createAddon(200L, "Mayo", restaurant, true, new BigDecimal("10.00"));
        Category category = createCategory(70L, "Sauces", restaurant);
        category.addAddon(addon);

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(addonRepository.findAllByRestaurant_RestaurantId(RESTAURANT_ID)).thenReturn(List.of(addon));

        List<AddonResponse> responses = addonService.getAddons(RESTAURANT_ID);

        assertEquals(1, responses.size());
        assertEquals(200L, responses.get(0).getAddonId());
        assertEquals("Mayo", responses.get(0).getAddonName());
        assertEquals(1, responses.get(0).getCategories().size());
        verify(addonRepository).findAllByRestaurant_RestaurantId(RESTAURANT_ID);
    }

    @Test
    void getAddons_throwsWhenCallerIsNotOwner() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);

        when(userAuthorization.authorizeUserId()).thenReturn(OTHER_USER_ID);
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));

        assertThrows(AccessDeniedException.class, () -> addonService.getAddons(RESTAURANT_ID));
        verify(addonRepository, never()).findAllByRestaurant_RestaurantId(any(Long.class));
    }

    @Test
    void updateAddon_updatesFieldsAndReplacesCategories() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        Addon addon = createAddon(300L, "Old Addon", restaurant, true, new BigDecimal("20.00"));
        Category oldCategory = createCategory(1L, "Old Category", restaurant);
        Category newCategory = createCategory(2L, "New Category", restaurant);
        oldCategory.addAddon(addon);

        CreateAddonRequest request = createAddonRequest("Updated Addon", false, List.of(2L), new BigDecimal("35.00"));

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(addonRepository.findById(300L)).thenReturn(Optional.of(addon));
        when(categoryRepository.findAllById(List.of(2L))).thenReturn(List.of(newCategory));

        AddonResponse response = addonService.updateAddon(RESTAURANT_ID, 300L, request);

        assertEquals("Updated Addon", response.getAddonName());
        assertFalse(response.isAvailable());
        assertEquals(new BigDecimal("35.00"), response.getPrice());
        assertFalse(oldCategory.getAvailableAddons().contains(addon));
        assertTrue(newCategory.getAvailableAddons().contains(addon));

        ArgumentCaptor<Addon> captor = ArgumentCaptor.forClass(Addon.class);
        verify(addonRepository).save(captor.capture());
        assertEquals("Updated Addon", captor.getValue().getAddonName());
        verify(categoryRepository).findAllById(List.of(2L));
    }

    @Test
    void updateAddon_throwsWhenAddonNotFound() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        CreateAddonRequest request = createAddonRequest("Addon", true, List.of(1L), new BigDecimal("20.00"));

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(addonRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> addonService.updateAddon(RESTAURANT_ID, 404L, request));
        verify(addonRepository, never()).save(any(Addon.class));
    }

    @Test
    void updateAddon_throwsWhenAddonBelongsToAnotherRestaurant() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        Restaurant anotherRestaurant = createRestaurant(11L, OWNER_ID);
        Addon addon = createAddon(301L, "Addon", anotherRestaurant, true, new BigDecimal("20.00"));
        CreateAddonRequest request = createAddonRequest("Addon", true, List.of(), new BigDecimal("20.00"));

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(addonRepository.findById(301L)).thenReturn(Optional.of(addon));

        assertThrows(AccessDeniedException.class, () -> addonService.updateAddon(RESTAURANT_ID, 301L, request));
        verify(addonRepository, never()).save(any(Addon.class));
    }

    @Test
    void updateAvailability_updatesOnlyAvailabilityFlag() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        Addon addon = createAddon(400L, "Addon", restaurant, true, new BigDecimal("15.00"));
        AddonAvailableStatusRequest request = new AddonAvailableStatusRequest(false);

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(addonRepository.findById(400L)).thenReturn(Optional.of(addon));

        AddonResponse response = addonService.updateAvailability(RESTAURANT_ID, 400L, request);

        assertFalse(response.isAvailable());
        assertFalse(addon.isAvailable());
        verify(addonRepository, never()).save(any(Addon.class));
    }

    @Test
    void deleteAddon_deletesAndReturnsDeleted() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        Addon addon = createAddon(500L, "Addon", restaurant, true, new BigDecimal("25.00"));

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(addonRepository.findById(500L)).thenReturn(Optional.of(addon));

        String result = addonService.deleteAddon(RESTAURANT_ID, 500L);

        assertEquals("deleted", result);
        verify(addonRepository).delete(addon);
    }

    @Test
    void deleteAddon_throwsWhenAddonNotFound() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(addonRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> addonService.deleteAddon(RESTAURANT_ID, 999L));
        verify(addonRepository, never()).delete(any(Addon.class));
    }

    @Test
    void deleteAddon_throwsWhenAddonBelongsToAnotherRestaurant() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        Restaurant anotherRestaurant = createRestaurant(11L, OWNER_ID);
        Addon addon = createAddon(501L, "Addon", anotherRestaurant, true, new BigDecimal("25.00"));

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(addonRepository.findById(501L)).thenReturn(Optional.of(addon));

        assertThrows(AccessDeniedException.class, () -> addonService.deleteAddon(RESTAURANT_ID, 501L));
        verify(addonRepository, never()).delete(any(Addon.class));
    }

    private Restaurant createRestaurant(Long restaurantId, Long ownerUserId) {
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantId(restaurantId);
        restaurant.setRestaurantName("Kitchen Hub");
        restaurant.setOwnerUserId(ownerUserId);
        restaurant.setCategories(new ArrayList<>());
        return restaurant;
    }

    private Category createCategory(Long categoryId, String name, Restaurant restaurant) {
        Category category = new Category();
        category.setCategoryId(categoryId);
        category.setCategoryName(name);
        category.setRestaurant(restaurant);
        return category;
    }

    private Addon createAddon(Long addonId, String name, Restaurant restaurant, boolean available, BigDecimal price) {
        Addon addon = new Addon();
        addon.setAddonId(addonId);
        addon.setAddonName(name);
        addon.setRestaurant(restaurant);
        addon.setAvailable(available);
        addon.setPrice(price);
        return addon;
    }

    private CreateAddonRequest createAddonRequest(String name, boolean available, List<Long> categoryIds, BigDecimal price) {
        return new CreateAddonRequest(name, null, available, categoryIds, price);
    }
}
