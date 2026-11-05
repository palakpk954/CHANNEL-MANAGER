package com.hotel.channelmanager.channels.web.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SaveRoomMappingRequest(
        @NotEmpty(message = "At least one room mapping must be provided")
        List<RoomMappingItem> mappings
) {
    public record RoomMappingItem(
            String roomTypeId,
            String channelRoomId,
            String channelRoomName,
            boolean active
    ) {}
}

