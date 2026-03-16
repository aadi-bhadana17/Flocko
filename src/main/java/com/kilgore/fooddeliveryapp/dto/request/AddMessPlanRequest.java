package com.kilgore.fooddeliveryapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddMessPlanRequest {
    @NotBlank
    private String messPlanName;
    @NotBlank
    private String messPlanDescription;
    @NotNull
    @Positive
    private BigDecimal price;
    @NotNull
    @NotEmpty
    private List<AddMessPlanSlotRequest> slots;
}
