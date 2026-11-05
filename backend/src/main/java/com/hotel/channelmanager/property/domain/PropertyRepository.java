package com.hotel.channelmanager.property.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyRepository extends JpaRepository<Property, String> {
    List<Property> findAllByOrganizationId(String organizationId);
    Optional<Property> findByIdAndOrganizationId(String id, String organizationId);
    boolean existsByNameAndOrganizationId(String name, String organizationId);
}

