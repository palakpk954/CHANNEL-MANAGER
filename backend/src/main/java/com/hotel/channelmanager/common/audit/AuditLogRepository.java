package com.hotel.channelmanager.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
    Page<AuditLog> findAllByOrganizationIdOrderByCreatedAtDesc(String organizationId, Pageable pageable);
    List<AuditLog> findTop50ByOrganizationIdOrderByCreatedAtDesc(String organizationId);
    List<AuditLog> findByOrganizationIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(String organizationId, String entityType, String entityId);
}

