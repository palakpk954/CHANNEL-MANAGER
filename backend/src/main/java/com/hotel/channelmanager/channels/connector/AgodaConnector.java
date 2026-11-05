package com.hotel.channelmanager.channels.connector;

import com.hotel.channelmanager.channels.domain.ChannelAccount;
import com.hotel.channelmanager.channels.domain.ChannelType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Agoda YCS Connectivity API Connector Stub.
 * TODO: Integrate Agoda YCS Channel Manager API XML/JSON endpoints.
 */
@Component
public class AgodaConnector implements ChannelConnector {

    @Override
    public ChannelType getChannelType() {
        return ChannelType.AGODA;
    }

    @Override
    public ChannelCapabilities getCapabilities() {
        return new ChannelCapabilities(
                false,
                100,
                30,
                Set.of("STOP_SELL", "MIN_STAY", "MAX_STAY", "CTA", "CTD")
        );
    }

    @Override
    public ConnectionResult testConnection(ChannelCredentials credentials) {
        if (credentials == null || credentials.getHotelId() == null || credentials.getHotelId().isBlank()) {
            return ConnectionResult.error("Agoda Hotel ID is required.");
        }
        // TODO: Call Agoda authentication endpoint
        return ConnectionResult.ok("Agoda connection test passed (Stub mode).");
    }

    @Override
    public MappingOptions fetchMappingOptions(ChannelAccount account) {
        // TODO: Call Agoda room/rate plan query
        return new MappingOptions(
                List.of(new MappingOptions.ChannelRoomOption("agoda_rm_1", "Agoda Superior Double")),
                List.of(new MappingOptions.ChannelRateOption("agoda_rt_1", "Room with Breakfast", "agoda_rm_1"))
        );
    }

    @Override
    public SyncResult pushAvailability(ChannelAccount account, List<AvailabilityUpdate> updates) {
        // TODO: Post Agoda inventory XML payload
        return SyncResult.ok("agoda-stub-ack", "Agoda availability push acknowledged (Stub mode).");
    }

    @Override
    public SyncResult pushRates(ChannelAccount account, List<RateUpdate> updates) {
        // TODO: Post Agoda rate XML payload
        return SyncResult.ok("agoda-stub-ack", "Agoda rate push acknowledged (Stub mode).");
    }

    @Override
    public SyncResult pushRestrictions(ChannelAccount account, List<RestrictionUpdate> updates) {
        // TODO: Post Agoda restriction XML payload
        return SyncResult.ok("agoda-stub-ack", "Agoda restrictions push acknowledged (Stub mode).");
    }

    @Override
    public List<InboundReservation> pullReservations(ChannelAccount account, Instant since) {
        // TODO: Poll Agoda booking queue
        return Collections.emptyList();
    }

    @Override
    public void acknowledge(ChannelAccount account, String channelReservationId) {
        // TODO: Post Agoda booking ACK
    }
}

