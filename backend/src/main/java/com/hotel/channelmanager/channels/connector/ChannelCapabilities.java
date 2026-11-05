package com.hotel.channelmanager.channels.connector;

import java.util.Set;

public record ChannelCapabilities(
        boolean supportsWebhooks,
        int maxBatchSize,
        int rateLimitPerMinute,
        Set<String> supportedRestrictions
) {}

