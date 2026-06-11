package com.kilgore.fooddeliveryapp.service;

import com.kilgore.fooddeliveryapp.catalog.dto.request.CreateCategoryRequest;
import com.kilgore.fooddeliveryapp.catalog.dto.response.CreateCategoryResponse;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.AddonSummary;
import com.kilgore.fooddeliveryapp.catalog.model.Addon;
import com.kilgore.fooddeliveryapp.catalog.model.Category;
import com.kilgore.fooddeliveryapp.catalog.model.CuisineType;
import com.kilgore.fooddeliveryapp.catalog.model.Restaurant;
import com.kilgore.fooddeliveryapp.catalog.repository.CategoryRepository;
import com.kilgore.fooddeliveryapp.catalog.repository.RestaurantRepository;
import com.kilgore.fooddeliveryapp.catalog.service.CategoryService;
import com.kilgore.fooddeliveryapp.catalog.util.CatalogMapper;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityAlreadyExistsException;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.common.exceptions.RestaurantNotFoundException;
import com.kilgore.fooddeliveryapp.common.util.UserAuthorization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long RESTAURANT_ID = 10L;

    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CatalogMapper catalogMapper;
    @Mock
    private UserAuthorization userAuthorization;

    @InjectMocks
    private CategoryService categoryService;

    @BeforeEach
    void setup() {
        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
    }

    @Test
    void createCategory_createsWhenOwnerAndUnique() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        CreateCategoryRequest request = new CreateCategoryRequest("Main Course", "Meal", 1);

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findCategoryByCategoryNameAndRestaurant_RestaurantId("Main Course", RESTAURANT_ID))
                .thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category saved = invocation.getArgument(0);
            saved.setCategoryId(55L);
            return saved;
        });

        CreateCategoryResponse response = categoryService.createCategory(RESTAURANT_ID, request);

        assertEquals(55L, response.getCategoryId());
        assertEquals("Main Course", response.getCategoryName());
        assertEquals(RESTAURANT_ID, response.getRestaurant().getRestaurantId());
        assertEquals(1, response.getDisplayOrder());
    }

    @Test
    void createCategory_throwsWhenDuplicateCategory() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        CreateCategoryRequest request = new CreateCategoryRequest("Main Course", "Meal", 1);
        Category existing = createCategory(5L, restaurant, "Main Course");

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findCategoryByCategoryNameAndRestaurant_RestaurantId("Main Course", RESTAURANT_ID))
                .thenReturn(Optional.of(existing));

        assertThrows(EntityAlreadyExistsException.class, () -> categoryService.createCategory(RESTAURANT_ID, request));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void createCategory_throwsWhenRestaurantMissing() {
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.empty());

        assertThrows(RestaurantNotFoundException.class,
                () -> categoryService.createCategory(RESTAURANT_ID, new CreateCategoryRequest("Main", "Meal", 1)));
    }

    @Test
    void createCategory_throwsWhenNotOwner() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);

        when(userAuthorization.authorizeUserId()).thenReturn(OTHER_USER_ID);
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));

        assertThrows(AccessDeniedException.class,
                () -> categoryService.createCategory(RESTAURANT_ID, new CreateCategoryRequest("Main", "Meal", 1)));
        verify(categoryRepository, never()).findCategoryByCategoryNameAndRestaurant_RestaurantId(any(String.class), any(Long.class));
    }

    @Test
    void getAllCategories_returnsMappedResponses() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        Category category = createCategory(1L, restaurant, "Main Course");
        Addon addon = new Addon();
        addon.setAddonId(9L);
        addon.setAddonName("Extra Cheese");
        category.getAvailableAddons().add(addon);
        restaurant.setCategories(List.of(category));

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(catalogMapper.toAddonSummary(addon)).thenReturn(new AddonSummary(9L, "Extra Cheese", new BigDecimal("10.00")));

        List<CreateCategoryResponse> responses = categoryService.getAllCategories(RESTAURANT_ID);

        assertEquals(1, responses.size());
        assertEquals("Main Course", responses.get(0).getCategoryName());
        assertEquals(1, responses.get(0).getAddons().size());
    }

    @Test
    void getAllCategories_returnsEmptyWhenNoCategories() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        restaurant.setCategories(List.of());

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));

        List<CreateCategoryResponse> responses = categoryService.getAllCategories(RESTAURANT_ID);

        assertTrue(responses.isEmpty());
    }

    @Test
    void updateCategory_updatesFieldsWhenValid() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        Category category = createCategory(1L, restaurant, "Main Course");
        CreateCategoryRequest request = new CreateCategoryRequest("Desserts", "Sweet", 2);

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        CreateCategoryResponse response = categoryService.updateCategory(RESTAURANT_ID, 1L, request);

        assertEquals("Desserts", response.getCategoryName());
        assertEquals("Sweet", response.getDescription());
        assertEquals(2, response.getDisplayOrder());
        verify(categoryRepository).save(category);
    }

    @Test
    void updateCategory_throwsWhenCategoryMissing() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> categoryService.updateCategory(RESTAURANT_ID, 1L, new CreateCategoryRequest("Desserts", "Sweet", 2)));
    }

    @Test
    void updateCategory_throwsWhenCategoryBelongsToOtherRestaurant() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        Restaurant otherRestaurant = createRestaurant(20L, OWNER_ID);
        Category category = createCategory(1L, otherRestaurant, "Main Course");

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertThrows(AccessDeniedException.class,
                () -> categoryService.updateCategory(RESTAURANT_ID, 1L, new CreateCategoryRequest("Desserts", "Sweet", 2)));
    }

    @Test
    void deleteCategory_deletesWhenValid() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        Category category = createCategory(1L, restaurant, "Main Course");

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        categoryService.deleteCategory(RESTAURANT_ID, 1L);

        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteCategory_throwsWhenCategoryMissing() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> categoryService.deleteCategory(RESTAURANT_ID, 1L));
    }

    @Test
    void deleteCategory_throwsWhenCategoryBelongsToOtherRestaurant() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        Restaurant otherRestaurant = createRestaurant(20L, OWNER_ID);
        Category category = createCategory(1L, otherRestaurant, "Main Course");

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertThrows(AccessDeniedException.class, () -> categoryService.deleteCategory(RESTAURANT_ID, 1L));
    }

    private Restaurant createRestaurant(Long id, Long ownerUserId) {
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantId(id);
        restaurant.setRestaurantName("Tasty Hub");
        restaurant.setCuisineType(CuisineType.INDIAN);
        restaurant.setOwnerUserId(ownerUserId);
        return restaurant;
    }

    private Category createCategory(Long id, Restaurant restaurant, String name) {
        Category category = new Category();
        category.setCategoryId(id);
        category.setCategoryName(name);
        category.setDescription("Desc");
        category.setDisplayOrder(1);
        category.setRestaurant(restaurant);
        return category;
    }
}
