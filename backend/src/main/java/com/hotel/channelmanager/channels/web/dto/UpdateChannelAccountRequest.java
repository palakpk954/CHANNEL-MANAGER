package com.hotel.channelmanager.channels.web.dto;

import com.hotel.channelmanager.channels.connector.ChannelCredentials;

public record UpdateChannelAccountRequest(
        String accountName,
        ChannelCredentials credentials,
        Boolean syncEnabled
) {}

