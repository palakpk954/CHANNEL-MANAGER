package com.hotel.channelmanager.reservations.web.dto;

import com.hotel.channelmanager.reservations.domain.ReservationStatus;

import java.time.Instant;
import java.time.LocalDate;

public record ReservationResponse(
        String id,
        String propertyId,
        String channelAccountId,
        String channelType,
        String channelReservationId,
        ReservationStatus status,
        String guestName,
        String guestEmail,
        String guestPhone,
        String roomTypeId,
        String roomTypeName,
        String roomTypeCode,
        String ratePlanId,
        String ratePlanName,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int unitsBooked,
        long totalAmountCents,
        double totalAmountFormatted,
        String currency,
        String specialRequests,
        String cancellationReason,
        Instant createdAt
) {}

