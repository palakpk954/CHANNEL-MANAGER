package com.hotel.channelmanager.channels.web.dto;

import java.util.List;

public record ChannelMappingsResponse(
        String channelAccountId,
        List<RoomMappingView> roomMappings,
        List<RateMappingView> rateMappings
) {
    public record RoomMappingView(
            String id,
            String roomTypeId,
            String channelRoomId,
            String channelRoomName,
            boolean active
    ) {}

    public record RateMappingView(
            String id,
            String ratePlanId,
            String channelRateId,
            String channelRateName,
            boolean active
    ) {}
}

