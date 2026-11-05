package com.hotel.channelmanager.channels.web.dto;

import java.util.List;

public record MappingOptionsResponse(
        List<InternalRoomOption> internalRooms,
        List<InternalRateOption> internalRates,
        List<ChannelRoomOption> channelRooms,
        List<ChannelRateOption> channelRates
) {
    public record InternalRoomOption(
            String id,
            String name,
            String code,
            int totalUnits
    ) {}

    public record InternalRateOption(
            String id,
            String name,
            String code,
            String roomTypeId
    ) {}

    public record ChannelRoomOption(
            String channelRoomId,
            String channelRoomName
    ) {}

    public record ChannelRateOption(
            String channelRateId,
            String channelRateName,
            String channelRoomId
    ) {}
}

