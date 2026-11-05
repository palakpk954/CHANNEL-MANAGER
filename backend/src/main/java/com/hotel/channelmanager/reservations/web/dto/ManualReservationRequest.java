package com.hotel.channelmanager.reservations.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ManualReservationRequest(
        @NotBlank(message = "Guest name is required")
        String guestName,

        String guestEmail,
        String guestPhone,

        @NotBlank(message = "Room type is required")
        String roomTypeId,

        String ratePlanId,

        @NotNull(message = "Check-in date is required")
        LocalDate checkInDate,

        @NotNull(message = "Check-out date is required")
        LocalDate checkOutDate,

        int unitsBooked,
        long totalAmountCents,
        String currency,
        String specialRequests
) {}

