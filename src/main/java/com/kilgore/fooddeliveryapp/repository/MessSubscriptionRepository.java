package com.kilgore.fooddeliveryapp.repository;

import com.kilgore.fooddeliveryapp.model.MealType;
import com.kilgore.fooddeliveryapp.model.MessPlan;
import com.kilgore.fooddeliveryapp.model.MessSubscription;
import com.kilgore.fooddeliveryapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public interface MessSubscriptionRepository extends JpaRepository<MessSubscription, Long> {

    List<MessSubscription> findAllByUser(User user);

    @Query("SELECT ms FROM MessSubscription ms " +
            "WHERE ms.user = :currentUser " +
            "AND ms.messPlan = :messPlan " +
            "AND ms.active = true")
    MessSubscription findActiveSubscriptionByUserAndMessPlan(User currentUser, MessPlan messPlan);

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
}
