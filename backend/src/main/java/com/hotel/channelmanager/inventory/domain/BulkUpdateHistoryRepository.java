package com.hotel.channelmanager.inventory.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BulkUpdateHistoryRepository extends JpaRepository<BulkUpdateHistory, String> {
    List<BulkUpdateHistory> findAllByPropertyIdAndOrganizationIdOrderByCreatedAtDesc(String propertyId, String organizationId);
    Optional<BulkUpdateHistory> findByIdAndPropertyIdAndOrganizationId(String id, String propertyId, String organizationId);
}

