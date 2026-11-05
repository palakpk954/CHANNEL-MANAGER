package com.hotel.channelmanager.channels.connector;

import com.hotel.channelmanager.channels.domain.ChannelAccount;
import com.hotel.channelmanager.channels.domain.ChannelType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * MakeMyTrip (MMT) Extranet API Connector Stub.
 * TODO: Integrate MakeMyTrip Channel Manager Open API.
 */
@Component
public class MakeMyTripConnector implements ChannelConnector {

    @Override
    public ChannelType getChannelType() {
        return ChannelType.MAKEMYTRIP;
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
            return ConnectionResult.error("MakeMyTrip Hotel ID is required.");
        }
        // TODO: Call MMT Ping endpoint
        return ConnectionResult.ok("MakeMyTrip connection test passed (Stub mode).");
    }

    @Override
    public MappingOptions fetchMappingOptions(ChannelAccount account) {
        // TODO: Call MMT room type and rate plan mapping query
        return new MappingOptions(
                List.of(new MappingOptions.ChannelRoomOption("mmt_deluxe", "MMT Deluxe Room")),
                List.of(new MappingOptions.ChannelRateOption("mmt_ep", "European Plan (Room Only)", "mmt_deluxe"))
        );
    }

    @Override
    public SyncResult pushAvailability(ChannelAccount account, List<AvailabilityUpdate> updates) {
        // TODO: Post MMT inventory update payload
        return SyncResult.ok("mmt-stub-ack", "MakeMyTrip availability push acknowledged (Stub mode).");
    }

    @Override
    public SyncResult pushRates(ChannelAccount account, List<RateUpdate> updates) {
        // TODO: Post MMT rate update payload
        return SyncResult.ok("mmt-stub-ack", "MakeMyTrip rate push acknowledged (Stub mode).");
    }

    @Override
    public SyncResult pushRestrictions(ChannelAccount account, List<RestrictionUpdate> updates) {
        // TODO: Post MMT restriction update payload
        return SyncResult.ok("mmt-stub-ack", "MakeMyTrip restrictions push acknowledged (Stub mode).");
    }

    @Override
    public List<InboundReservation> pullReservations(ChannelAccount account, Instant since) {
        // TODO: Poll MMT booking feed
        return Collections.emptyList();
    }

    @Override
    public void acknowledge(ChannelAccount account, String channelReservationId) {
        // TODO: MMT booking acknowledgement
    }
}

