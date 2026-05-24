package com.kilgore.fooddeliveryapp.catalog.dto.request;

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
    private String description;
    @NotNull
    @Positive
    private BigDecimal price;
    @NotNull
    @NotEmpty
    private List<AddMessPlanSlotRequest> slots;
}
