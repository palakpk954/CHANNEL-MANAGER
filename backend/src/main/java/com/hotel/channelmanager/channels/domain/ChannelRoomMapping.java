package com.hotel.channelmanager.channels.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "channel_room_mappings")
public class ChannelRoomMapping {

    @Id
    private String id;

    @Column(name = "organization_id", nullable = false)
    private String organizationId;

    @Column(name = "channel_account_id", nullable = false)
    private String channelAccountId;

    @Column(name = "room_type_id", nullable = false)
    private String roomTypeId;

    @Column(name = "channel_room_id", nullable = false)
    private String channelRoomId;

    @Column(name = "channel_room_name", nullable = false)
    private String channelRoomName;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ChannelRoomMapping() {
        this.id = UUID.randomUUID().toString();
    }

    public ChannelRoomMapping(String organizationId, String channelAccountId, String roomTypeId, String channelRoomId, String channelRoomName) {
        this.id = UUID.randomUUID().toString();
        this.organizationId = organizationId;
        this.channelAccountId = channelAccountId;
        this.roomTypeId = roomTypeId;
        this.channelRoomId = channelRoomId;
        this.channelRoomName = channelRoomName;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
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

    public String getChannelAccountId() {
        return channelAccountId;
    }

    public void setChannelAccountId(String channelAccountId) {
        this.channelAccountId = channelAccountId;
    }

    public String getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(String roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public String getChannelRoomId() {
        return channelRoomId;
    }

    public void setChannelRoomId(String channelRoomId) {
        this.channelRoomId = channelRoomId;
    }

    public String getChannelRoomName() {
        return channelRoomName;
    }

    public void setChannelRoomName(String channelRoomName) {
        this.channelRoomName = channelRoomName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

