package com.kilgore.fooddeliveryapp.catalog.repository;

import com.kilgore.fooddeliveryapp.catalog.model.MessPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessPlanRepository extends JpaRepository<MessPlan,Long> {

    List<MessPlan> findByRestaurant_RestaurantId(Long restaurantId);

    @Query("SELECT m FROM MessPlan m WHERE m.restaurant.restaurantId = :restaurantId AND m.isActive = true AND m.deletionScheduledAt IS NULL")
    List<MessPlan> findVisibleByRestaurantId(@Param("restaurantId") Long restaurantId);

    @Query("SELECT m FROM MessPlan m WHERE m.deletionScheduledAt IS NOT NULL AND m.deletionScheduledAt <= :now AND m.isActive = true")
    List<MessPlan> findPlansDueForDeletion(@Param("now") LocalDateTime now);
}
