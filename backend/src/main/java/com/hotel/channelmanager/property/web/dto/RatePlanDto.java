package com.hotel.channelmanager.property.web.dto;

import com.hotel.channelmanager.property.domain.RatePlan;

import java.math.BigDecimal;

public record RatePlanDto(
        String id,
        String organizationId,
        String propertyId,
        String roomTypeId,
        String code,
        String name,
        String mealPlan,
        String cancellationPolicy,
        String parentRatePlanId,
        String derivationType,
        BigDecimal derivationValue,
        String status
) {
    public static RatePlanDto fromEntity(RatePlan ratePlan) {
        return new RatePlanDto(
                ratePlan.getId(),
                ratePlan.getOrganizationId(),
                ratePlan.getPropertyId(),
                ratePlan.getRoomTypeId(),
                ratePlan.getCode(),
                ratePlan.getName(),
                ratePlan.getMealPlan(),
                ratePlan.getCancellationPolicy(),
                ratePlan.getParentRatePlanId(),
                ratePlan.getDerivationType(),
                ratePlan.getDerivationValue(),
                ratePlan.getStatus()
        );
    }
}

