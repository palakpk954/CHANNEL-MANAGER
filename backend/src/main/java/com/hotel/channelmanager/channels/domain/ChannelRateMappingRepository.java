package com.hotel.channelmanager.channels.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelRateMappingRepository extends JpaRepository<ChannelRateMapping, String> {
    List<ChannelRateMapping> findAllByChannelAccountIdAndOrganizationId(String channelAccountId, String organizationId);
    List<ChannelRateMapping> findAllByRatePlanIdAndOrganizationId(String ratePlanId, String organizationId);
    Optional<ChannelRateMapping> findByChannelAccountIdAndRatePlanIdAndOrganizationId(
            String channelAccountId, String ratePlanId, String organizationId);
    Optional<ChannelRateMapping> findByChannelAccountIdAndChannelRateIdAndOrganizationId(
            String channelAccountId, String channelRateId, String organizationId);
}

