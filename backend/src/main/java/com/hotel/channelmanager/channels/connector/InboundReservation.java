package com.hotel.channelmanager.channels.connector;

import java.time.Instant;
import java.time.LocalDate;

public record InboundReservation(
        String channelReservationId,
        String channelType,
        String guestName,
        String guestEmail,
        String guestPhone,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int totalUnits,
        long totalAmountCents,
        String currency,
        String channelRoomId,
        String channelRateId,
        String status, // CONFIRMED, CANCELLED, MODIFIED
        Instant channelCreatedAt
) {}

