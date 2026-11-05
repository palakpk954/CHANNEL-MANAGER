package com.hotel.channelmanager.reservations.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConflictRepository extends JpaRepository<Conflict, String> {
    List<Conflict> findAllByPropertyIdAndOrganizationIdOrderByCreatedAtDesc(String propertyId, String organizationId);
    List<Conflict> findAllByPropertyIdAndOrganizationIdAndStatus(String propertyId, String organizationId, ConflictStatus status);
    Optional<Conflict> findByIdAndOrganizationId(String id, String organizationId);
    long countByPropertyIdAndOrganizationIdAndStatus(String propertyId, String organizationId, ConflictStatus status);
}

