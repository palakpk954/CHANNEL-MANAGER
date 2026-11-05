package com.hotel.channelmanager.reservations.web.dto;

import com.hotel.channelmanager.reservations.domain.ConflictResolutionAction;
import com.hotel.channelmanager.reservations.domain.ConflictStatus;

import java.time.Instant;
import java.time.LocalDate;

public record ConflictResponse(
        String id,
        String propertyId,
        String reservationId,
        String guestName,
        String channelType,
        String channelReservationId,
        String roomTypeId,
        String roomTypeName,
        LocalDate conflictDate,
        String reason,
        ConflictStatus status,
        ConflictResolutionAction resolutionAction,
        String resolutionNotes,
        String resolvedBy,
        Instant resolvedAt,
        Instant createdAt
) {}

