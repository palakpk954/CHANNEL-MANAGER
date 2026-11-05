package com.hotel.channelmanager.channels.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SyncJobRepository extends JpaRepository<SyncJob, String> {
    List<SyncJob> findAllByChannelAccountIdAndOrganizationIdOrderByCreatedAtDesc(String channelAccountId, String organizationId);
    Page<SyncJob> findAllByPropertyIdAndOrganizationIdOrderByCreatedAtDesc(String propertyId, String organizationId, Pageable pageable);
    Optional<SyncJob> findByIdAndOrganizationId(String id, String organizationId);
    List<SyncJob> findAllByStatusAndNextRetryAtLessThanEqual(SyncJobStatus status, Instant time);
}

