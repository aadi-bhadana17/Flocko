package com.kilgore.fooddeliveryapp.ordering.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateReviewRequest {
    @Min(1)
    @Max(5)
    private int rating;
    @NotBlank
    private String comment;
}
