package com.kilgore.fooddeliveryapp.dto.response;

import com.kilgore.fooddeliveryapp.dto.summary.FoodSummary;
import com.kilgore.fooddeliveryapp.model.MealType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessPlanSlotResponse {
    private Long slotId;
    private DayOfWeek  dayOfWeek;
    private MealType mealType;
    private List<FoodSummary> foodItems;
}
