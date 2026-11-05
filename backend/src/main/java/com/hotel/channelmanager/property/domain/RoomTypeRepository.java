package com.hotel.channelmanager.property.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, String> {
    List<RoomType> findAllByPropertyIdAndOrganizationId(String propertyId, String organizationId);
    Optional<RoomType> findByIdAndOrganizationId(String id, String organizationId);
    Optional<RoomType> findByIdAndPropertyIdAndOrganizationId(String id, String propertyId, String organizationId);
    Optional<RoomType> findByPropertyIdAndCodeAndOrganizationId(String propertyId, String code, String organizationId);
    boolean existsByPropertyIdAndCodeAndOrganizationId(String propertyId, String code, String organizationId);
}

