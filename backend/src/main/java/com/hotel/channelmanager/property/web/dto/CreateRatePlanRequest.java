package com.hotel.channelmanager.property.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateRatePlanRequest(
        @NotBlank(message = "Rate plan code is required")
        @Size(max = 50, message = "Code must not exceed 50 characters")
        String code,

        @NotBlank(message = "Rate plan name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        @NotBlank(message = "Linked room type is required")
        String roomTypeId,

        @NotBlank(message = "Meal plan is required")
        String mealPlan,

        @NotBlank(message = "Cancellation policy is required")
        String cancellationPolicy,

        String parentRatePlanId,
        String derivationType,
        BigDecimal derivationValue
) {}

