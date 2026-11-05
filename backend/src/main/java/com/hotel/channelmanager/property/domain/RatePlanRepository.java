package com.hotel.channelmanager.property.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatePlanRepository extends JpaRepository<RatePlan, String> {
    List<RatePlan> findAllByPropertyIdAndOrganizationId(String propertyId, String organizationId);
    List<RatePlan> findAllByRoomTypeIdAndOrganizationId(String roomTypeId, String organizationId);
    Optional<RatePlan> findByIdAndOrganizationId(String id, String organizationId);
    Optional<RatePlan> findByIdAndPropertyIdAndOrganizationId(String id, String propertyId, String organizationId);
    Optional<RatePlan> findByPropertyIdAndCodeAndOrganizationId(String propertyId, String code, String organizationId);
    List<RatePlan> findAllByParentRatePlanId(String parentRatePlanId);
    boolean existsByPropertyIdAndCodeAndOrganizationId(String propertyId, String code, String organizationId);
}

