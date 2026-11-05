package com.hotel.channelmanager.channels.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "channel_rate_mappings")
public class ChannelRateMapping {

    @Id
    private String id;

    @Column(name = "organization_id", nullable = false)
    private String organizationId;

    @Column(name = "channel_account_id", nullable = false)
    private String channelAccountId;

    @Column(name = "rate_plan_id", nullable = false)
    private String ratePlanId;

    @Column(name = "channel_rate_id", nullable = false)
    private String channelRateId;

    @Column(name = "channel_rate_name", nullable = false)
    private String channelRateName;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ChannelRateMapping() {
        this.id = UUID.randomUUID().toString();
    }

    public ChannelRateMapping(String organizationId, String channelAccountId, String ratePlanId, String channelRateId, String channelRateName) {
        this.id = UUID.randomUUID().toString();
        this.organizationId = organizationId;
        this.channelAccountId = channelAccountId;
        this.ratePlanId = ratePlanId;
        this.channelRateId = channelRateId;
        this.channelRateName = channelRateName;
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

    public String getRatePlanId() {
        return ratePlanId;
    }

    public void setRatePlanId(String ratePlanId) {
        this.ratePlanId = ratePlanId;
    }

    public String getChannelRateId() {
        return channelRateId;
    }

    public void setChannelRateId(String channelRateId) {
        this.channelRateId = channelRateId;
    }

    public String getChannelRateName() {
        return channelRateName;
    }

    public void setChannelRateName(String channelRateName) {
        this.channelRateName = channelRateName;
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

