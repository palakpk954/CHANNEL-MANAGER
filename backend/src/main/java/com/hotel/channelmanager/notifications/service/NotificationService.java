package com.hotel.channelmanager.notifications.service;

import com.hotel.channelmanager.common.exception.ApiException;
import com.hotel.channelmanager.common.tenant.TenantContext;
import com.hotel.channelmanager.notifications.domain.Notification;
import com.hotel.channelmanager.notifications.domain.NotificationRepository;
import com.hotel.channelmanager.notifications.domain.NotificationType;
import com.hotel.channelmanager.notifications.web.dto.NotificationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public NotificationResponse createNotification(
            String organizationId,
            String propertyId,
            NotificationType type,
            String title,
            String message,
            String actionUrl
    ) {
        Notification notification = new Notification(
                organizationId,
                propertyId,
                type,
                title,
                message,
                actionUrl
        );
        Notification saved = notificationRepository.save(notification);
        return NotificationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(String propertyId, boolean unreadOnly) {
        String orgId = TenantContext.getOrganizationId();
        List<Notification> list = unreadOnly
                ? notificationRepository.findAllByOrganizationIdAndPropertyIdAndReadFalseOrderByCreatedAtDesc(orgId, propertyId)
                : notificationRepository.findAllByOrganizationIdAndPropertyIdOrderByCreatedAtDesc(orgId, propertyId);

        return list.stream().map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String propertyId) {
        String orgId = TenantContext.getOrganizationId();
        return notificationRepository.countByOrganizationIdAndPropertyIdAndReadFalse(orgId, propertyId);
    }

    @Transactional
    public void markAsRead(String propertyId, String notificationId) {
        String orgId = TenantContext.getOrganizationId();
        Notification notification = notificationRepository.findByIdAndOrganizationId(notificationId, orgId)
                .orElseThrow(() -> ApiException.notFound("Notification not found."));

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(String propertyId) {
        String orgId = TenantContext.getOrganizationId();
        List<Notification> unread = notificationRepository.findAllByOrganizationIdAndPropertyIdAndReadFalseOrderByCreatedAtDesc(orgId, propertyId);
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }
}

