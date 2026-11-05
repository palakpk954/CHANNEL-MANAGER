package com.hotel.channelmanager.notifications.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findAllByOrganizationIdAndPropertyIdOrderByCreatedAtDesc(String organizationId, String propertyId);

    List<Notification> findAllByOrganizationIdAndPropertyIdAndReadFalseOrderByCreatedAtDesc(String organizationId, String propertyId);

    long countByOrganizationIdAndPropertyIdAndReadFalse(String organizationId, String propertyId);

    Optional<Notification> findByIdAndOrganizationId(String id, String organizationId);

    List<Notification> findAllByOrganizationIdAndPropertyId(String organizationId, String propertyId);
}

