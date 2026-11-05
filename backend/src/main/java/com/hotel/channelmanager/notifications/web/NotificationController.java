package com.hotel.channelmanager.notifications.web;

import com.hotel.channelmanager.notifications.service.NotificationService;
import com.hotel.channelmanager.notifications.web.dto.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/properties/{propertyId}/notifications")
@Tag(name = "Notifications", description = "In-app notifications for staff alerts, double bookings, and channel sync events")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Get list of notifications for property")
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @PathVariable String propertyId,
            @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        List<NotificationResponse> list = notificationService.getNotifications(propertyId, unreadOnly);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notifications count for header badge")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable String propertyId) {
        long count = notificationService.getUnreadCount(propertyId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PostMapping("/{notificationId}/read")
    @Operation(summary = "Mark single notification as read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable String propertyId,
            @PathVariable String notificationId
    ) {
        notificationService.markAsRead(propertyId, notificationId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mark-all-read")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Void> markAllAsRead(@PathVariable String propertyId) {
        notificationService.markAllAsRead(propertyId);
        return ResponseEntity.ok().build();
    }
}

