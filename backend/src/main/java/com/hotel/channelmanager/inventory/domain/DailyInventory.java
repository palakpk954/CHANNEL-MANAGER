package com.hotel.channelmanager.inventory.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_inventory", uniqueConstraints = {
        @UniqueConstraint(name = "uq_room_type_date", columnNames = {"room_type_id", "date"})
})
public class DailyInventory {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "organization_id", nullable = false, length = 36)
    private String organizationId;

    @Column(name = "property_id", nullable = false, length = 36)
    private String propertyId;

    @Column(name = "room_type_id", nullable = false, length = 36)
    private String roomTypeId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "total_units", nullable = false)
    private int totalUnits = 1;

    @Column(name = "sold_units", nullable = false)
    private int soldUnits = 0;

    @Column(name = "blocked_units", nullable = false)
    private int blockedUnits = 0;

    @Column(name = "available_units", nullable = false)
    private int availableUnits = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public DailyInventory() {
        this.id = UUID.randomUUID().toString();
    }

    public DailyInventory(String organizationId, String propertyId, String roomTypeId, LocalDate date, int totalUnits) {
        this.id = UUID.randomUUID().toString();
        this.organizationId = organizationId;
        this.propertyId = propertyId;
        this.roomTypeId = roomTypeId;
        this.date = date;
        this.totalUnits = totalUnits;
        this.soldUnits = 0;
        this.blockedUnits = 0;
        this.availableUnits = Math.max(0, totalUnits);
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void recalculateAvailability() {
        // BR-1.2: available = total - sold - blocked. Never negative.
        this.availableUnits = Math.max(0, this.totalUnits - this.soldUnits - this.blockedUnits);
        this.updatedAt = Instant.now();
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

    public String getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(String roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getTotalUnits() {
        return totalUnits;
    }

    public void setTotalUnits(int totalUnits) {
        this.totalUnits = totalUnits;
        recalculateAvailability();
    }

    public int getSoldUnits() {
        return soldUnits;
    }

    public void setSoldUnits(int soldUnits) {
        this.soldUnits = soldUnits;
        recalculateAvailability();
    }

    public int getBlockedUnits() {
        return blockedUnits;
    }

    public void setBlockedUnits(int blockedUnits) {
        this.blockedUnits = blockedUnits;
        recalculateAvailability();
    }

    public int getAvailableUnits() {
        return availableUnits;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

