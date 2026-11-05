package com.hotel.channelmanager.reservations.web.dto;

import com.hotel.channelmanager.reservations.domain.ConflictResolutionAction;
import jakarta.validation.constraints.NotNull;

public record ResolveConflictRequest(
        @NotNull(message = "Resolution action is required")
        ConflictResolutionAction action,

        String targetRoomTypeId, // For MOVE_GUEST
        String notes
) {}

