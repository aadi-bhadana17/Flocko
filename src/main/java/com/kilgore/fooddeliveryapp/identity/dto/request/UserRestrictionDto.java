package com.kilgore.fooddeliveryapp.identity.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRestrictionDto {

    private Long userId;
    private String reason;
    private int durationInDays;
}
