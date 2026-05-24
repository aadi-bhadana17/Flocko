package com.kilgore.fooddeliveryapp.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCategoryRequest {

    @NotBlank
    private String categoryName;
    @NotBlank
    private String description;
    @Positive
    @NotNull
    private Integer displayOrder;

}
