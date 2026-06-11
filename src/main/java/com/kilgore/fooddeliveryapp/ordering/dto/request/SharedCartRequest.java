package com.kilgore.fooddeliveryapp.ordering.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SharedCartRequest {
    @NotNull
    private Long restaurantId;
    @NotNull
    private boolean hostPaysAll;
}