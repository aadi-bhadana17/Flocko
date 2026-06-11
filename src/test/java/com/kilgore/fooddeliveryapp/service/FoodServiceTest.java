package com.kilgore.fooddeliveryapp.service;

import com.kilgore.fooddeliveryapp.catalog.dto.request.FoodRequest;
import com.kilgore.fooddeliveryapp.catalog.dto.request.FoodStatusRequest;
import com.kilgore.fooddeliveryapp.catalog.dto.response.FoodResponse;
import com.kilgore.fooddeliveryapp.catalog.model.Category;
import com.kilgore.fooddeliveryapp.catalog.model.CuisineType;
import com.kilgore.fooddeliveryapp.catalog.model.Food;
import com.kilgore.fooddeliveryapp.catalog.model.Restaurant;
import com.kilgore.fooddeliveryapp.catalog.repository.CategoryRepository;
import com.kilgore.fooddeliveryapp.catalog.repository.FoodRepository;
import com.kilgore.fooddeliveryapp.catalog.repository.RestaurantRepository;
import com.kilgore.fooddeliveryapp.catalog.service.FoodService;
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
import java.time.LocalDateTime;
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
class FoodServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long RESTAURANT_ID = 10L;

    @Mock
    private FoodRepository foodRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private UserAuthorization userAuthorization;
    @Mock
    private CatalogMapper catalogMapper;

    @InjectMocks
    private FoodService foodService;

    @BeforeEach
    void setup() {
        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
    }

    @Test
    void createFood_createsFoodWhenOwnerAndCategoryValid() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        Category category = createCategory(5L, restaurant);
        FoodRequest request = createFoodRequest(5L);

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(foodRepository.findByFoodNameAndRestaurantId("Pasta", RESTAURANT_ID)).thenReturn(Optional.empty());
        when(foodRepository.save(any(Food.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubFoodLookupDependencies(restaurant, category);

        FoodResponse response = foodService.createFood(RESTAURANT_ID, request);

        assertEquals("Pasta", response.getName());
        assertEquals(new BigDecimal("199.00"), response.getPrice());
        assertTrue(response.isAvailable());
        verify(foodRepository).save(any(Food.class));
    }

    @Test
    void createFood_throwsWhenRestaurantNotFound() {
        FoodRequest request = createFoodRequest(5L);

        when(restaurantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RestaurantNotFoundException.class, () -> foodService.createFood(99L, request));
        verify(foodRepository, never()).save(any(Food.class));
    }

    @Test
    void createFood_throwsWhenCallerIsNotOwner() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        FoodRequest request = createFoodRequest(5L);

        when(userAuthorization.authorizeUserId()).thenReturn(OTHER_USER_ID);
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));

        assertThrows(AccessDeniedException.class, () -> foodService.createFood(RESTAURANT_ID, request));
        verify(foodRepository, never()).save(any(Food.class));
    }

    @Test
    void createFood_throwsWhenCategoryNotFound() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        FoodRequest request = createFoodRequest(5L);

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> foodService.createFood(RESTAURANT_ID, request));
    }

    @Test
    void createFood_throwsWhenCategoryBelongsToDifferentRestaurant() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        Restaurant otherRestaurant = createRestaurant(11L, OWNER_ID);
        Category category = createCategory(5L, otherRestaurant);
        FoodRequest request = createFoodRequest(5L);

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));

        assertThrows(AccessDeniedException.class, () -> foodService.createFood(RESTAURANT_ID, request));
    }

    @Test
    void createFood_throwsWhenFoodAlreadyExists() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        Category category = createCategory(5L, restaurant);
        FoodRequest request = createFoodRequest(5L);
        Food existingFood = createFood(88L, restaurant, category);

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(foodRepository.findByFoodNameAndRestaurantId("Pasta", RESTAURANT_ID))
                .thenReturn(Optional.of(existingFood));

        assertThrows(EntityAlreadyExistsException.class, () -> foodService.createFood(RESTAURANT_ID, request));
        verify(foodRepository, never()).save(any(Food.class));
    }

    @Test
    void findAllFoods_returnsMappedResponses() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        Category category = createCategory(5L, restaurant);
        Food first = createFood(1L, restaurant, category);
        Food second = createFood(2L, restaurant, category);

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(foodRepository.findByRestaurantId(RESTAURANT_ID)).thenReturn(List.of(first, second));
        stubFoodLookupDependencies(restaurant, category);

        List<FoodResponse> responses = foodService.findAllFoods(RESTAURANT_ID);

        assertEquals(2, responses.size());
        assertEquals("Pasta", responses.get(0).getName());
    }

    @Test
    void findAllFoods_throwsWhenAuthenticationMissing_expectedSecurityBehavior() {
        when(userAuthorization.authorizeUserId()).thenThrow(new AccessDeniedException("No valid authority"));

        assertThrows(AccessDeniedException.class, () -> foodService.findAllFoods(RESTAURANT_ID));
    }

    @Test
    void findFoodById_returnsMappedResponse() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        Category category = createCategory(5L, restaurant);
        Food food = createFood(1L, restaurant, category);

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(foodRepository.findById(1L)).thenReturn(Optional.of(food));
        stubFoodLookupDependencies(restaurant, category);

        FoodResponse response = foodService.findFoodById(RESTAURANT_ID, 1L);

        assertEquals(1L, response.getId());
        assertEquals("Pasta", response.getName());
    }

    @Test
    void findFoodById_throwsWhenFoodNotFound() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(foodRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> foodService.findFoodById(RESTAURANT_ID, 9L));
    }

    @Test
    void updateFood_updatesFieldsWhenValid() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        Category category = createCategory(5L, restaurant);
        FoodRequest request = createFoodRequest(5L);
        Food food = createFood(1L, restaurant, category);

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(foodRepository.findById(1L)).thenReturn(Optional.of(food));
        when(foodRepository.findByFoodNameAndRestaurantId("Pasta", RESTAURANT_ID)).thenReturn(Optional.of(food));
        stubFoodLookupDependencies(restaurant, category);

        FoodResponse response = foodService.updateFood(RESTAURANT_ID, 1L, request);

        assertEquals("Pasta", response.getName());
        assertEquals(new BigDecimal("199.00"), response.getPrice());
    }

    @Test
    void updateFood_throwsWhenFoodNotFound() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        Category category = createCategory(5L, restaurant);
        FoodRequest request = createFoodRequest(5L);

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(foodRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> foodService.updateFood(RESTAURANT_ID, 1L, request));
    }

    @Test
    void updateFood_throwsWhenDuplicateNameExists() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        Category category = createCategory(5L, restaurant);
        FoodRequest request = createFoodRequest(5L);
        Food food = createFood(1L, restaurant, category);
        Food otherFood = createFood(2L, restaurant, category);

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(foodRepository.findById(1L)).thenReturn(Optional.of(food));
        when(foodRepository.findByFoodNameAndRestaurantId("Pasta", RESTAURANT_ID)).thenReturn(Optional.of(otherFood));

        assertThrows(EntityAlreadyExistsException.class, () -> foodService.updateFood(RESTAURANT_ID, 1L, request));
    }

    @Test
    void updateFoodStatus_updatesAvailability() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        Category category = createCategory(5L, restaurant);
        Food food = createFood(1L, restaurant, category);

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(foodRepository.findById(1L)).thenReturn(Optional.of(food));
        stubFoodLookupDependencies(restaurant, category);

        FoodResponse response = foodService.updateFoodStatus(RESTAURANT_ID, 1L, new FoodStatusRequest(false));

        assertTrue(!response.isAvailable());
    }

    @Test
    void updateFoodStatus_throwsWhenFoodNotFound() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(foodRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> foodService.updateFoodStatus(RESTAURANT_ID, 1L, new FoodStatusRequest(true)));
    }

    @Test
    void deleteFood_deletesWhenValid() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);
        Category category = createCategory(5L, restaurant);
        Food food = createFood(1L, restaurant, category);

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(foodRepository.findById(1L)).thenReturn(Optional.of(food));

        String message = foodService.deleteFood(RESTAURANT_ID, 1L);

        assertTrue(message.contains("Food with id : 1"));
        verify(foodRepository).delete(food);
    }

    @Test
    void deleteFood_throwsWhenFoodNotFound() {
        Restaurant restaurant = createRestaurant(RESTAURANT_ID, OWNER_ID);

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(foodRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> foodService.deleteFood(RESTAURANT_ID, 1L));
    }

    @Test
    void deleteFood_throwsWhenAuthenticationMissing_expectedSecurityBehavior() {
        when(userAuthorization.authorizeUserId()).thenThrow(new AccessDeniedException("No valid authority"));

        assertThrows(AccessDeniedException.class, () -> foodService.deleteFood(RESTAURANT_ID, 1L));
    }

    private void stubFoodLookupDependencies(Restaurant restaurant, Category category) {
        when(restaurantRepository.findById(restaurant.getRestaurantId())).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findById(category.getCategoryId())).thenReturn(Optional.of(category));
        when(catalogMapper.toRestaurantSummary(restaurant)).thenReturn(
                new com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantSummary(
                        restaurant.getRestaurantId(), restaurant.getRestaurantName(),
                        restaurant.getCuisineType(), restaurant.getAvgRating()));
        when(catalogMapper.toCategorySummary(category)).thenReturn(
                new com.kilgore.fooddeliveryapp.catalog.dto.summary.CategorySummary(
                        category.getCategoryId(), category.getCategoryName()));
    }

    private Restaurant createRestaurant(Long id, Long ownerUserId) {
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantId(id);
        restaurant.setRestaurantName("Tasty Hub");
        restaurant.setCuisineType(CuisineType.INDIAN);
        restaurant.setAvgRating(new BigDecimal("4.2"));
        restaurant.setOwnerUserId(ownerUserId);
        return restaurant;
    }

    private Category createCategory(Long id, Restaurant restaurant) {
        Category category = new Category();
        category.setCategoryId(id);
        category.setCategoryName("Main Course");
        category.setRestaurant(restaurant);
        return category;
    }

    private Food createFood(Long id, Restaurant restaurant, Category category) {
        Food food = new Food();
        food.setFoodId(id);
        food.setFoodName("Pasta");
        food.setFoodDescription("Creamy pasta");
        food.setFoodPrice(new BigDecimal("199.00"));
        food.setImages(List.of("img1.png"));
        food.setCategoryId(category.getCategoryId());
        food.setRestaurantId(restaurant.getRestaurantId());
        food.setVegetarian(true);
        food.setAvailable(true);
        food.setCreatedAt(LocalDateTime.now());
        return food;
    }

    private FoodRequest createFoodRequest(Long categoryId) {
        FoodRequest request = new FoodRequest();
        request.setFoodName("Pasta");
        request.setFoodDescription("Creamy pasta");
        request.setFoodPrice(new BigDecimal("199.00"));
        request.setImages(List.of("img1.png"));
        request.setCategoryId(categoryId);
        request.setVegetarian(true);
        request.setAvailable(true);
        return request;
    }
}
