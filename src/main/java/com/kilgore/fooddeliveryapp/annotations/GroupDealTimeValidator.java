package com.kilgore.fooddeliveryapp.annotations;

import com.kilgore.fooddeliveryapp.dto.request.GroupDealRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class GroupDealTimeValidator implements ConstraintValidator<GroupDealRequest.ValidGroupDealTime, GroupDealRequest> {

    @Override
    public boolean isValid(GroupDealRequest req, ConstraintValidatorContext ctx) {
        if (req.getStartTime() == null || req.getEndTime() == null) {
            return true; // let @NotNull handle it
        }
        if(req.getStartTime().equals(req.getEndTime())) {
            return true; // deal is active 24 hours, so we don't need to check the validation
        }
        return req.getStartTime().isBefore(req.getEndTime());
    }
}
