package com.hotel.channelmanager.channels.connector;

import com.hotel.channelmanager.channels.domain.ChannelAccount;
import com.hotel.channelmanager.channels.domain.ChannelType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Expedia Partner Central (EPC) Connector Stub.
 * Implements Expedia QuickConnect JSON / Product API.
 * TODO: Integrate Expedia Partner Central API with OAuth 2.0 authentication.
 */
@Component
public class ExpediaConnector implements ChannelConnector {

    @Override
    public ChannelType getChannelType() {
        return ChannelType.EXPEDIA;
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
            return ConnectionResult.error("Expedia Property ID is required.");
        }
        // TODO: Call Expedia /products test endpoint
        return ConnectionResult.ok("Expedia connection test passed (Stub mode).");
    }

    @Override
    public MappingOptions fetchMappingOptions(ChannelAccount account) {
        // TODO: Call Expedia Product API to retrieve room types and rate plans
        return new MappingOptions(
                List.of(
                        new MappingOptions.ChannelRoomOption("exp_rm_1", "Expedia Standard Room"),
                        new MappingOptions.ChannelRoomOption("exp_rm_2", "Expedia Superior Room")
                ),
                List.of(
                        new MappingOptions.ChannelRateOption("exp_rt_1", "Expedia Room Only", "exp_rm_1"),
                        new MappingOptions.ChannelRateOption("exp_rt_2", "Expedia Room Only", "exp_rm_2")
                )
        );
    }

    @Override
    public SyncResult pushAvailability(ChannelAccount account, List<AvailabilityUpdate> updates) {
        // TODO: Call Expedia Availability and Rates API
        return SyncResult.ok("exp-stub-ack", "Expedia availability push acknowledged (Stub mode).");
    }

    @Override
    public SyncResult pushRates(ChannelAccount account, List<RateUpdate> updates) {
        // TODO: Call Expedia Rates API
        return SyncResult.ok("exp-stub-ack", "Expedia rate push acknowledged (Stub mode).");
    }

    @Override
    public SyncResult pushRestrictions(ChannelAccount account, List<RestrictionUpdate> updates) {
        // TODO: Call Expedia Restrictions API
        return SyncResult.ok("exp-stub-ack", "Expedia restrictions push acknowledged (Stub mode).");
    }

    @Override
    public List<InboundReservation> pullReservations(ChannelAccount account, Instant since) {
        // TODO: Call Expedia Booking Retrieval API
        return Collections.emptyList();
    }

    @Override
    public void acknowledge(ChannelAccount account, String channelReservationId) {
        // TODO: Call Expedia Booking Confirmation API
    }
}

