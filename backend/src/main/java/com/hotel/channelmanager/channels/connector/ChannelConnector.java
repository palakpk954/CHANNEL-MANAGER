package com.hotel.channelmanager.channels.connector;

import com.hotel.channelmanager.channels.domain.ChannelAccount;
import com.hotel.channelmanager.channels.domain.ChannelType;

import java.time.Instant;
import java.util.List;

public interface ChannelConnector {

    ChannelType getChannelType();

    ChannelCapabilities getCapabilities();

    ConnectionResult testConnection(ChannelCredentials credentials);

    MappingOptions fetchMappingOptions(ChannelAccount account);

    SyncResult pushAvailability(ChannelAccount account, List<AvailabilityUpdate> updates);

    SyncResult pushRates(ChannelAccount account, List<RateUpdate> updates);

    SyncResult pushRestrictions(ChannelAccount account, List<RestrictionUpdate> updates);

    List<InboundReservation> pullReservations(ChannelAccount account, Instant since);

    void acknowledge(ChannelAccount account, String channelReservationId);
}

