package com.kilgore.fooddeliveryapp.catalog.repository;

import com.kilgore.fooddeliveryapp.catalog.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findCategoryByCategoryNameAndRestaurant_RestaurantId(String categoryName, Long restaurantId);
}
