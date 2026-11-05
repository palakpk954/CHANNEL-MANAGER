package com.hotel.channelmanager.reservations.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "conflicts")
public class Conflict {

    @Id
    private String id;

    @Column(name = "organization_id", nullable = false)
    private String organizationId;

    @Column(name = "property_id", nullable = false)
    private String propertyId;

    @Column(name = "reservation_id", nullable = false)
    private String reservationId;

    @Column(name = "room_type_id", nullable = false)
    private String roomTypeId;

    @Column(name = "conflict_date", nullable = false)
    private LocalDate conflictDate;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ConflictStatus status = ConflictStatus.UNRESOLVED;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_action", length = 50)
    private ConflictResolutionAction resolutionAction;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "resolved_by")
    private String resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Conflict() {
        this.id = UUID.randomUUID().toString();
    }

    public Conflict(
            String organizationId,
            String propertyId,
            String reservationId,
            String roomTypeId,
            LocalDate conflictDate,
            String reason
    ) {
        this.id = UUID.randomUUID().toString();
        this.organizationId = organizationId;
        this.propertyId = propertyId;
        this.reservationId = reservationId;
        this.roomTypeId = roomTypeId;
        this.conflictDate = conflictDate;
        this.reason = reason;
        this.status = ConflictStatus.UNRESOLVED;
        this.createdAt = Instant.now();
    }

    public void resolve(ConflictResolutionAction action, String notes, String resolvedBy) {
        this.status = ConflictStatus.RESOLVED;
        this.resolutionAction = action;
        this.resolutionNotes = notes;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = Instant.now();
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

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(String roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public LocalDate getConflictDate() {
        return conflictDate;
    }

    public void setConflictDate(LocalDate conflictDate) {
        this.conflictDate = conflictDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public ConflictStatus getStatus() {
        return status;
    }

    public void setStatus(ConflictStatus status) {
        this.status = status;
    }

    public ConflictResolutionAction getResolutionAction() {
        return resolutionAction;
    }

    public void setResolutionAction(ConflictResolutionAction resolutionAction) {
        this.resolutionAction = resolutionAction;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

