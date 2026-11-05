package com.hotel.channelmanager.channels.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelRoomMappingRepository extends JpaRepository<ChannelRoomMapping, String> {
    List<ChannelRoomMapping> findAllByChannelAccountIdAndOrganizationId(String channelAccountId, String organizationId);
    List<ChannelRoomMapping> findAllByRoomTypeIdAndOrganizationId(String roomTypeId, String organizationId);
    Optional<ChannelRoomMapping> findByChannelAccountIdAndRoomTypeIdAndOrganizationId(
            String channelAccountId, String roomTypeId, String organizationId);
    Optional<ChannelRoomMapping> findByChannelAccountIdAndChannelRoomIdAndOrganizationId(
            String channelAccountId, String channelRoomId, String organizationId);
}

