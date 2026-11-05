package com.hotel.channelmanager.inventory.web.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record UpdateCalendarDayRequest(
        @NotNull(message = "Date is required")
        LocalDate date,

        String roomTypeId,
        Integer availableUnits,

        String ratePlanId,
        Long amountCents,
        Boolean stopSell,
        Integer minStay,
        Integer maxStay,
        Boolean cta,
        Boolean ctd
) {}

