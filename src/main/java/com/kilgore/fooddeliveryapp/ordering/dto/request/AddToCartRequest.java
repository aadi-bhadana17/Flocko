package com.kilgore.fooddeliveryapp.ordering.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddToCartRequest {

    private Long foodId;
    private int quantity;
    private List<Long> addonIds;
}
