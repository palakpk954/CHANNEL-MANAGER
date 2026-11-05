package com.hotel.channelmanager.channels.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sync_jobs")
public class SyncJob {

    @Id
    private String id;

    @Column(name = "organization_id", nullable = false)
    private String organizationId;

    @Column(name = "property_id", nullable = false)
    private String propertyId;

    @Column(name = "channel_account_id", nullable = false)
    private String channelAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 50)
    private SyncJobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private SyncJobStatus status = SyncJobStatus.PENDING;

    @Column(name = "payload_summary", columnDefinition = "TEXT")
    private String payloadSummary;

    @Column(name = "response_summary", columnDefinition = "TEXT")
    private String responseSummary;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 5;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    public SyncJob() {
        this.id = UUID.randomUUID().toString();
    }

    public SyncJob(String organizationId, String propertyId, String channelAccountId, SyncJobType jobType, String payloadSummary) {
        this.id = UUID.randomUUID().toString();
        this.organizationId = organizationId;
        this.propertyId = propertyId;
        this.channelAccountId = channelAccountId;
        this.jobType = jobType;
        this.payloadSummary = payloadSummary;
        this.status = SyncJobStatus.PENDING;
        this.attemptCount = 0;
        this.maxAttempts = 5;
        this.createdAt = Instant.now();
    }

    public void markSuccess(String responseSummary) {
        this.status = SyncJobStatus.SUCCESS;
        this.responseSummary = responseSummary;
        this.errorMessage = null;
        this.completedAt = Instant.now();
    }

    public void markFailed(String error, boolean canRetry, Instant nextRetry) {
        this.attemptCount++;
        this.errorMessage = error;
        if (canRetry && this.attemptCount < this.maxAttempts) {
            this.status = SyncJobStatus.PENDING;
            this.nextRetryAt = nextRetry;
        } else {
            this.status = SyncJobStatus.FAILED;
            this.completedAt = Instant.now();
        }
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

    public String getChannelAccountId() {
        return channelAccountId;
    }

    public void setChannelAccountId(String channelAccountId) {
        this.channelAccountId = channelAccountId;
    }

    public SyncJobType getJobType() {
        return jobType;
    }

    public void setJobType(SyncJobType jobType) {
        this.jobType = jobType;
    }

    public SyncJobStatus getStatus() {
        return status;
    }

    public void setStatus(SyncJobStatus status) {
        this.status = status;
    }

    public String getPayloadSummary() {
        return payloadSummary;
    }

    public void setPayloadSummary(String payloadSummary) {
        this.payloadSummary = payloadSummary;
    }

    public String getResponseSummary() {
        return responseSummary;
    }

    public void setResponseSummary(String responseSummary) {
        this.responseSummary = responseSummary;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(Instant nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}

