package com.hotel.channelmanager.channels.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelAccountRepository extends JpaRepository<ChannelAccount, String> {
    List<ChannelAccount> findAllByPropertyIdAndOrganizationId(String propertyId, String organizationId);
    List<ChannelAccount> findAllByPropertyIdAndOrganizationIdAndSyncEnabledTrue(String propertyId, String organizationId);
    Optional<ChannelAccount> findByIdAndOrganizationId(String id, String organizationId);
    Optional<ChannelAccount> findByPropertyIdAndChannelTypeAndAccountNameAndOrganizationId(
            String propertyId, ChannelType channelType, String accountName, String organizationId);
    boolean existsByPropertyIdAndChannelTypeAndAccountNameAndOrganizationId(
            String propertyId, ChannelType channelType, String accountName, String organizationId);
    List<ChannelAccount> findAllByChannelTypeAndSyncEnabledTrue(ChannelType channelType);
    List<ChannelAccount> findAllByPropertyIdAndChannelTypeAndSyncEnabledTrue(String propertyId, ChannelType channelType);
}

