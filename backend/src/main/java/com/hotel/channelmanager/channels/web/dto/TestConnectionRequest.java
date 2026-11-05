package com.hotel.channelmanager.channels.web.dto;

import com.hotel.channelmanager.channels.connector.ChannelCredentials;
import com.hotel.channelmanager.channels.domain.ChannelType;
import jakarta.validation.constraints.NotNull;

public record TestConnectionRequest(
        @NotNull(message = "Channel type is required")
        ChannelType channelType,

        @NotNull(message = "Credentials are required")
        ChannelCredentials credentials
) {}

