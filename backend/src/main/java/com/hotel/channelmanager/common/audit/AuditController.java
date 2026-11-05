package com.hotel.channelmanager.common.audit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Activity History", description = "Immutable audit log of inventory, rates, mappings, and permissions")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent 50 activity history events")
    public ResponseEntity<List<AuditLog>> getRecentActivity() {
        return ResponseEntity.ok(auditService.getRecentActivity(50));
    }

    @GetMapping
    @Operation(summary = "Get paged activity history")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(auditService.getAuditLogsPaged(page, size));
    }
}

