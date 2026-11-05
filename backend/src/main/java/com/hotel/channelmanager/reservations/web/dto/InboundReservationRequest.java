package com.hotel.channelmanager.reservations.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record InboundReservationRequest(
        @NotBlank(message = "Channel type is required")
        String channelType,

        @NotBlank(message = "Channel reservation ID is required")
        String channelReservationId,

        @NotBlank(message = "Guest name is required")
        String guestName,

        String guestEmail,
        String guestPhone,

        @NotBlank(message = "Channel room ID is required")
        String channelRoomId,

        String channelRateId,

        @NotNull(message = "Check-in date is required")
        LocalDate checkInDate,

        @NotNull(message = "Check-out date is required")
        LocalDate checkOutDate,

        int unitsBooked,
        long totalAmountCents,
        String currency,
        String specialRequests
) {}

