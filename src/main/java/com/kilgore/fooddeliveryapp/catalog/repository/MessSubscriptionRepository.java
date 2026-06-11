package com.kilgore.fooddeliveryapp.catalog.repository;

import com.kilgore.fooddeliveryapp.catalog.dto.response.MessSubscriptionResponse;
import com.kilgore.fooddeliveryapp.catalog.model.MealType;
import com.kilgore.fooddeliveryapp.catalog.model.MessPlan;
import com.kilgore.fooddeliveryapp.catalog.model.MessSubscription;
import com.kilgore.fooddeliveryapp.identity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public interface MessSubscriptionRepository extends JpaRepository<MessSubscription, Long> {

    @Query("SELECT ms FROM MessSubscription ms " +
            "WHERE ms.userId = : userId " +
            "AND ms.messPlan = :messPlan " +
            "AND ms.active = true")
    MessSubscription findActiveSubscriptionByUserIdAndMessPlan(Long userId, MessPlan messPlan);

    @Query("SELECT ms FROM MessSubscription ms " +
            "JOIN ms.messPlan mp " +
            "JOIN mp.slots slot  " +
            "WHERE ms.endDate >= :today " +
            "AND ms.active = true " +
            "AND slot.dayOfWeek = :dayOfWeek " +
            "AND slot.mealType = :mealType")
    List<MessSubscription> findActiveSubscriptionsByDayAndMealType(LocalDate today, DayOfWeek dayOfWeek, MealType mealType);

    @Query("SELECT ms FROM MessSubscription ms WHERE ms.active = true AND ms.endDate <= :currentDate")
    List<MessSubscription> findActiveSubscriptionsByCurrentDate(LocalDate currentDate);

    @Query("SELECT ms FROM MessSubscription ms WHERE ms.userId = :userId AND ms.active = true")
    List<MessSubscription> findActiveSubscriptionsByUserId(@Param("userId") Long userId);
}
