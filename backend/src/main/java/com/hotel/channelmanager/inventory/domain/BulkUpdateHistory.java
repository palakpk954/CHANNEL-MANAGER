package com.hotel.channelmanager.inventory.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bulk_update_history")
public class BulkUpdateHistory {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "organization_id", nullable = false, length = 36)
    private String organizationId;

    @Column(name = "property_id", nullable = false, length = 36)
    private String propertyId;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "snapshot_before", nullable = false, columnDefinition = "TEXT")
    private String snapshotBefore; // JSON array of previous rates and inventory states

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "undone_at")
    private Instant undoneAt;

    public BulkUpdateHistory() {
        this.id = UUID.randomUUID().toString();
    }

    public BulkUpdateHistory(String organizationId, String propertyId, String description, String snapshotBefore, String createdBy) {
        this.id = UUID.randomUUID().toString();
        this.organizationId = organizationId;
        this.propertyId = propertyId;
        this.description = description;
        this.snapshotBefore = snapshotBefore;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public boolean isEligibleForUndo(long maxHours) {
        if (undoneAt != null) {
            return false;
        }
        Instant cutoff = Instant.now().minusSeconds(maxHours * 3600);
        return createdAt.isAfter(cutoff);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSnapshotBefore() {
        return snapshotBefore;
    }

    public void setSnapshotBefore(String snapshotBefore) {
        this.snapshotBefore = snapshotBefore;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUndoneAt() {
        return undoneAt;
    }

    public void setUndoneAt(Instant undoneAt) {
        this.undoneAt = undoneAt;
    }
}

