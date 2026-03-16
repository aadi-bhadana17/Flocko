package com.kilgore.fooddeliveryapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MessPlanSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long slotId;
    @ManyToOne
    private MessPlan messPlan;
    private DayOfWeek dayOfWeek;
    private MealType mealType;
    @ManyToMany
    List<Food> foodItems;
}
