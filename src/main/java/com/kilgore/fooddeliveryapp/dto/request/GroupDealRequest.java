package com.kilgore.fooddeliveryapp.dto.request;

import com.kilgore.fooddeliveryapp.annotations.GroupDealTimeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GroupDealRequest {
    @NotBlank
    private String dealName;
    @Future
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Positive
    private BigDecimal originalPrice;
    @Positive
    private int maxDiscount;
    @NotNull
    private List<Long> foodIds;
    @Positive
    private int targetParticipation;
    @NotNull
    private List<GroupDealTierRequest> discountList;

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = GroupDealTimeValidator.class)
    public @interface ValidGroupDealTime {
        String message() default "Start time must be before end time";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }

}
