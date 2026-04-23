package com.kilgore.fooddeliveryapp.repository;

import com.kilgore.fooddeliveryapp.model.Food;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {

    Optional<Food> findByFoodNameAndRestaurant_RestaurantId(String foodName, Long restaurantId);

    List<Food> findByRestaurant_RestaurantId(Long restaurantId);

    @EntityGraph(attributePaths = {"foodCategory", "images"})
    List<Food> findAllByRestaurant_RestaurantId(@Param("restaurantId") Long restaurantId);
}
