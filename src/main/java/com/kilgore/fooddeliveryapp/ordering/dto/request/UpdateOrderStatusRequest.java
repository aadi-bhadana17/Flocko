package com.kilgore.fooddeliveryapp.ordering.dto.request;

import com.kilgore.fooddeliveryapp.ordering.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateOrderStatusRequest {
    private OrderStatus orderStatus;
}
