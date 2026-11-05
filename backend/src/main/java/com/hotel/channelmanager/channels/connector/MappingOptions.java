package com.hotel.channelmanager.channels.connector;

import java.util.List;

public record MappingOptions(
        List<ChannelRoomOption> roomOptions,
        List<ChannelRateOption> rateOptions
) {
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

