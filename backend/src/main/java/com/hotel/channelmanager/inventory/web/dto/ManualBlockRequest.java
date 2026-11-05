package com.hotel.channelmanager.inventory.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ManualBlockRequest(
        @NotBlank(message = "Room type ID is required")
        String roomTypeId,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        @Min(value = 1, message = "At least 1 room must be held")
        int unitsBlocked,

        @NotBlank(message = "Reason for hold is required")
        String reason
) {}

