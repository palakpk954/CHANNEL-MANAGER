package com.hotel.channelmanager.channels.web.dto;

import com.hotel.channelmanager.channels.domain.ChannelStatus;
import com.hotel.channelmanager.channels.domain.ChannelType;

import java.time.Instant;

public record ChannelAccountResponse(
        String id,
        String propertyId,
        ChannelType channelType,
        String accountName,
        ChannelStatus status,
        boolean syncEnabled,
        Instant lastSyncAt,
        String errorMessage,
        int roomMappingsCount,
        int rateMappingsCount,
        Instant createdAt
) {}

