package com.hotel.channelmanager.channels.web.dto;

import com.hotel.channelmanager.channels.connector.ChannelCredentials;
import com.hotel.channelmanager.channels.domain.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateChannelAccountRequest(
        @NotNull(message = "Channel type is required")
        ChannelType channelType,

        @NotBlank(message = "Account name is required")
        String accountName,

        @NotNull(message = "Credentials are required")
        ChannelCredentials credentials
) {}

