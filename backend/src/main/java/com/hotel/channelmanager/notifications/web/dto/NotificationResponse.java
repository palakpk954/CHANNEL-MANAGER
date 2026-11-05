package com.hotel.channelmanager.notifications.web.dto;

import com.hotel.channelmanager.notifications.domain.Notification;

import java.time.Instant;

public record NotificationResponse(
        String id,
        String propertyId,
        String type,
        String title,
        String message,
        String actionUrl,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getPropertyId(),
                n.getType().name(),
                n.getTitle(),
                n.getMessage(),
                n.getActionUrl(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}

