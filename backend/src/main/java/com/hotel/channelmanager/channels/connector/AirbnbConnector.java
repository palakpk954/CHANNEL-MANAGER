package com.hotel.channelmanager.channels.connector;

import com.hotel.channelmanager.channels.domain.ChannelAccount;
import com.hotel.channelmanager.channels.domain.ChannelType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Airbnb Partner API Connector Stub.
 * TODO: Integrate Airbnb Open API for Listings, Pricing and Availability.
 */
@Component
public class AirbnbConnector implements ChannelConnector {

    @Override
    public ChannelType getChannelType() {
        return ChannelType.AIRBNB;
    }

    @Override
    public ChannelCapabilities getCapabilities() {
        return new ChannelCapabilities(
                true,
                50,
                30,
                Set.of("STOP_SELL", "MIN_STAY", "MAX_STAY")
        );
    }

    @Override
    public ConnectionResult testConnection(ChannelCredentials credentials) {
        if (credentials == null || credentials.getApiKey() == null || credentials.getApiKey().isBlank()) {
            return ConnectionResult.error("Airbnb OAuth Access Token is required.");
        }
        // TODO: Call Airbnb GET /v2/listings
        return ConnectionResult.ok("Airbnb connection test passed (Stub mode).");
    }

    @Override
    public MappingOptions fetchMappingOptions(ChannelAccount account) {
        // TODO: Fetch Airbnb listing IDs
        return new MappingOptions(
                List.of(new MappingOptions.ChannelRoomOption("abnb_listing_1", "Airbnb Entire Apartment")),
                List.of(new MappingOptions.ChannelRateOption("abnb_rate_std", "Standard Base Price", "abnb_listing_1"))
        );
    }

    @Override
    public SyncResult pushAvailability(ChannelAccount account, List<AvailabilityUpdate> updates) {
        // TODO: Call Airbnb PUT /v2/calendars
        return SyncResult.ok("abnb-stub-ack", "Airbnb calendar sync acknowledged (Stub mode).");
    }

    @Override
    public SyncResult pushRates(ChannelAccount account, List<RateUpdate> updates) {
        // TODO: Call Airbnb pricing updates
        return SyncResult.ok("abnb-stub-ack", "Airbnb pricing sync acknowledged (Stub mode).");
    }

    @Override
    public SyncResult pushRestrictions(ChannelAccount account, List<RestrictionUpdate> updates) {
        // TODO: Call Airbnb min_nights restrictions
        return SyncResult.ok("abnb-stub-ack", "Airbnb restrictions sync acknowledged (Stub mode).");
    }

    @Override
    public List<InboundReservation> pullReservations(ChannelAccount account, Instant since) {
        // TODO: Call Airbnb GET /v2/reservations
        return Collections.emptyList();
    }

    @Override
    public void acknowledge(ChannelAccount account, String channelReservationId) {
        // TODO: Airbnb webhook / confirmation
    }
}

