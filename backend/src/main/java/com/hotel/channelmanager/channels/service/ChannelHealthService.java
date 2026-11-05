package com.hotel.channelmanager.channels.service;

import com.hotel.channelmanager.common.exception.ApiException;
import com.hotel.channelmanager.common.tenant.TenantContext;
import com.hotel.channelmanager.channels.domain.*;
import com.hotel.channelmanager.channels.sync.SyncWorkerService;
import com.hotel.channelmanager.channels.web.dto.SyncJobResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChannelHealthService {

    private final SyncJobRepository syncJobRepository;
    private final ChannelAccountRepository channelAccountRepository;

    public ChannelHealthService(
            SyncJobRepository syncJobRepository,
            ChannelAccountRepository channelAccountRepository
    ) {
        this.syncJobRepository = syncJobRepository;
        this.channelAccountRepository = channelAccountRepository;
    }

    @Transactional(readOnly = true)
    public List<SyncJobResponse> getSyncJobsForAccount(String accountId) {
        String orgId = TenantContext.getOrganizationId();
        channelAccountRepository.findByIdAndOrganizationId(accountId, orgId)
                .orElseThrow(() -> ApiException.notFound("Channel account not found."));

        return syncJobRepository.findAllByChannelAccountIdAndOrganizationIdOrderByCreatedAtDesc(accountId, orgId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<SyncJobResponse> getSyncJobsForProperty(String propertyId, int page, int size) {
        String orgId = TenantContext.getOrganizationId();
        return syncJobRepository.findAllByPropertyIdAndOrganizationIdOrderByCreatedAtDesc(
                propertyId, orgId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Transactional
    public SyncJobResponse retrySyncJob(String propertyId, String accountId, String jobId) {
        String orgId = TenantContext.getOrganizationId();
        SyncJob job = syncJobRepository.findByIdAndOrganizationId(jobId, orgId)
                .orElseThrow(() -> ApiException.notFound("Sync job not found."));

        if (!job.getChannelAccountId().equals(accountId)) {
            throw ApiException.badRequest("Sync job does not belong to the specified channel account.", "Verify the channel account ID.");
        }

        job.setStatus(SyncJobStatus.PENDING);
        job.setErrorMessage(null);
        syncJobRepository.save(job);

        return toResponse(job);
    }

    private SyncJobResponse toResponse(SyncJob j) {
        return new SyncJobResponse(
                j.getId(),
                j.getChannelAccountId(),
                j.getJobType(),
                j.getStatus(),
                j.getPayloadSummary(),
                j.getResponseSummary(),
                j.getAttemptCount(),
                j.getMaxAttempts(),
                j.getErrorMessage(),
                j.getCreatedAt(),
                j.getCompletedAt()
        );
    }
}
