package com.hotel.channelmanager.property.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoomTypeRequest(
        @NotBlank(message = "Room code is required")
        @Size(max = 50, message = "Room code must not exceed 50 characters")
        String code,

        @NotBlank(message = "Room name is required")
        @Size(max = 255, message = "Room name must not exceed 255 characters")
        String name,

        @Min(value = 1, message = "Maximum occupancy must be at least 1 guest")
        int maxOccupancy,

        @Min(value = 1, message = "Total room count must be at least 1")
        int totalUnits,

        String bedType,
        String amenities
) {}

