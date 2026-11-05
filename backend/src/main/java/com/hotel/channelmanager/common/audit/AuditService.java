package com.hotel.channelmanager.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.channelmanager.common.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    public void logChange(String actorId, String actorEmail, String entityType, String entityId,
                          String action, Object before, Object after, String ipAddress) {
        String orgId = TenantContext.getOrganizationId();
        if (orgId == null) {
            log.warn("Attempted to record audit log without tenant context: entityType={}, entityId={}", entityType, entityId);
            return;
        }

        try {
            String beforeJson = before != null ? (before instanceof String ? (String) before : objectMapper.writeValueAsString(before)) : null;
            String afterJson = after != null ? (after instanceof String ? (String) after : objectMapper.writeValueAsString(after)) : null;

            AuditLog entry = new AuditLog(orgId, actorId, actorEmail, entityType, entityId, action, beforeJson, afterJson, ipAddress);
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to write audit log for entity {}:{}", entityType, entityId, e);
        }
    }

    public void recordAction(String action, String entityType, String entityId, String description) {
        logChange(null, null, entityType, entityId, action, null, description, null);
    }

    public List<AuditLog> getRecentActivity(int limit) {
        String orgId = TenantContext.getOrganizationId();
        return auditLogRepository.findTop50ByOrganizationIdOrderByCreatedAtDesc(orgId);
    }

    public Page<AuditLog> getAuditLogsPaged(int page, int size) {
        String orgId = TenantContext.getOrganizationId();
        return auditLogRepository.findAllByOrganizationIdOrderByCreatedAtDesc(orgId, PageRequest.of(page, size));
    }
}

