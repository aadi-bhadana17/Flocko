package com.kilgore.fooddeliveryapp.catalog.repository;

import com.kilgore.fooddeliveryapp.catalog.model.Food;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {

    Optional<Food> findByFoodNameAndRestaurantId(String foodName, Long restaurantId);

    List<Food> findByRestaurantId(Long restaurantId);

    @EntityGraph(attributePaths = {"foodCategory", "images"})
    List<Food> findAllByRestaurantId(@Param("restaurantId") Long restaurantId);
}
