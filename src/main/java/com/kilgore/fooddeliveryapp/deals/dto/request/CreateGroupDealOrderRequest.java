package com.kilgore.fooddeliveryapp.deals.dto.request;

import com.kilgore.fooddeliveryapp.ordering.model.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateGroupDealOrderRequest {
    private Long userId;
    private Long restaurantId;
    private Long deliveryAddressId;
    private BigDecimal price;
    private  int quantity;
    private List<Long> foodIds;
}
