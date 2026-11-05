package com.hotel.channelmanager.channels.connector;

import com.hotel.channelmanager.channels.domain.ChannelAccount;
import com.hotel.channelmanager.channels.domain.ChannelType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Goibibo IngoMMT Partner API Connector Stub.
 * TODO: Integrate IngoMMT Partner API.
 */
@Component
public class GoibiboConnector implements ChannelConnector {

    @Override
    public ChannelType getChannelType() {
        return ChannelType.GOIBIBO;
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
            return ConnectionResult.error("Goibibo Hotel ID is required.");
        }
        // TODO: Call IngoMMT ping endpoint
        return ConnectionResult.ok("Goibibo connection test passed (Stub mode).");
    }

    @Override
    public MappingOptions fetchMappingOptions(ChannelAccount account) {
        // TODO: Fetch Goibibo inventory catalog
        return new MappingOptions(
                List.of(new MappingOptions.ChannelRoomOption("goibibo_standard", "Goibibo Standard")),
                List.of(new MappingOptions.ChannelRateOption("goibibo_ep", "Room Only", "goibibo_standard"))
        );
    }

    @Override
    public SyncResult pushAvailability(ChannelAccount account, List<AvailabilityUpdate> updates) {
        // TODO: Post Goibibo inventory update
        return SyncResult.ok("goibibo-stub-ack", "Goibibo availability push acknowledged (Stub mode).");
    }

    @Override
    public SyncResult pushRates(ChannelAccount account, List<RateUpdate> updates) {
        // TODO: Post Goibibo rate update
        return SyncResult.ok("goibibo-stub-ack", "Goibibo rate push acknowledged (Stub mode).");
    }

    @Override
    public SyncResult pushRestrictions(ChannelAccount account, List<RestrictionUpdate> updates) {
        // TODO: Post Goibibo restrictions update
        return SyncResult.ok("goibibo-stub-ack", "Goibibo restrictions push acknowledged (Stub mode).");
    }

    @Override
    public List<InboundReservation> pullReservations(ChannelAccount account, Instant since) {
        // TODO: Poll IngoMMT bookings
        return Collections.emptyList();
    }

    @Override
    public void acknowledge(ChannelAccount account, String channelReservationId) {
        // TODO: IngoMMT booking ACK
    }
}

