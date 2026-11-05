package com.hotel.channelmanager.inventory.web.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record CalendarGridResponse(
        String propertyId,
        String propertyName,
        String currency,
        String timezone,
        LocalDate startDate,
        LocalDate endDate,
        List<LocalDate> dates,
        List<RoomRow> rooms,
        List<RatePlanRow> ratePlans
) {
    public record RoomRow(
            String id,
            String code,
            String name,
            int totalUnits,
            Map<String, DayInventoryDto> days
    ) {}

    public record DayInventoryDto(
            int total,
            int sold,
            int blocked,
            int available,
            String statusToken // "available", "low", "soldout", "closed"
    ) {}

    public record RatePlanRow(
            String id,
            String roomTypeId,
            String code,
            String name,
            Map<String, DayRateDto> days
    ) {}

    public record DayRateDto(
            long amountCents,
            double amountFormatted,
            String currency,
            boolean stopSell,
            boolean cta,
            boolean ctd,
            int minStay,
            Integer maxStay,
            boolean hasRestrictions
    ) {}
}

