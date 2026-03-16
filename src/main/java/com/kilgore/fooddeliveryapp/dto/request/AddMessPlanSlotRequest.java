package com.kilgore.fooddeliveryapp.dto.request;

import com.kilgore.fooddeliveryapp.model.MealType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddMessPlanSlotRequest {
    @NotNull
    private DayOfWeek dayOfWeek;
    @NotNull
    private MealType mealType;
    @NotEmpty
    @NotNull
    private List<Long> foodIds;
}
