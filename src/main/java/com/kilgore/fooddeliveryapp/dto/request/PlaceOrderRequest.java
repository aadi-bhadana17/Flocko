package com.kilgore.fooddeliveryapp.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlaceOrderRequest {
    @NotNull
    private Long addressId;

    private LocalDateTime scheduledAt; // Optional , null for normal orders

    @JsonProperty("isSpecial")
    @JsonAlias("special")
    private boolean isSpecial; // Optional
}
