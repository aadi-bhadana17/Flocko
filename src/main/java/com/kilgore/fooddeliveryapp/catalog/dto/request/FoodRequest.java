package com.kilgore.fooddeliveryapp.catalog.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FoodRequest {

    @NotBlank
    private String foodName;
    @NotBlank
    private String foodDescription;
    @DecimalMin("1.0")
    private BigDecimal foodPrice;
    @NotNull
    private Long categoryId;

    private boolean vegetarian;
    private boolean available;

    @NotEmpty
    private List<@NotBlank String> images;
}
