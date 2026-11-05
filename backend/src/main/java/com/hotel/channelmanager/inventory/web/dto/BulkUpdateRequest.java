package com.hotel.channelmanager.inventory.web.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record BulkUpdateRequest(
        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        List<Integer> daysOfWeek, // 1=Mon, 2=Tue, ..., 7=Sun (ISO-8601)
        List<String> roomTypeIds,
        List<String> ratePlanIds,

        Integer availableUnits,

        String priceAdjustmentType, // "SET_EXACT", "INCREASE_PERCENT", "DECREASE_PERCENT", "INCREASE_AMOUNT"
        Double priceValue,
        Boolean confirmLargePriceChange, // BR-2.10: confirmation required if change > 50%

        Integer minStay,
        Integer maxStay,
        Boolean cta,
        Boolean ctd,
        Boolean stopSell
) {}

