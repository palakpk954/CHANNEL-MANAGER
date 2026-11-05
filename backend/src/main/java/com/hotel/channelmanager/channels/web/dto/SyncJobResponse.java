package com.hotel.channelmanager.channels.web.dto;

import com.hotel.channelmanager.channels.domain.SyncJobStatus;
import com.hotel.channelmanager.channels.domain.SyncJobType;

import java.time.Instant;

public record SyncJobResponse(
        String id,
        String channelAccountId,
        SyncJobType jobType,
        SyncJobStatus status,
        String payloadSummary,
        String responseSummary,
        int attemptCount,
        int maxAttempts,
        String errorMessage,
        Instant createdAt,
        Instant completedAt
) {}

