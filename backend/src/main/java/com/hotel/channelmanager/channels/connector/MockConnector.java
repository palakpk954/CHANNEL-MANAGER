package com.hotel.channelmanager.channels.connector;

import com.hotel.channelmanager.channels.domain.ChannelAccount;
import com.hotel.channelmanager.channels.domain.ChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Component
public class MockConnector implements ChannelConnector {

    private static final Logger log = LoggerFactory.getLogger(MockConnector.class);

    private final List<InboundReservation> simulatedQueue = Collections.synchronizedList(new ArrayList<>());

    @Override
    public ChannelType getChannelType() {
        return ChannelType.MOCK;
    }

    @Override
    public ChannelCapabilities getCapabilities() {
        return new ChannelCapabilities(
                true,
                500,
                120,
                Set.of("STOP_SELL", "MIN_STAY", "MAX_STAY", "CTA", "CTD")
        );
    }

    @Override
    public ConnectionResult testConnection(ChannelCredentials credentials) {
        if (credentials == null) {
            return ConnectionResult.error("No credentials provided.");
        }
        if (credentials.getApiKey() != null && credentials.getApiKey().contains("invalid")) {
            return ConnectionResult.error("Invalid API credentials provided for Mock Channel.");
        }
        return ConnectionResult.ok("Mock Channel connection verified successfully.");
    }

    @Override
    public MappingOptions fetchMappingOptions(ChannelAccount account) {
        List<MappingOptions.ChannelRoomOption> rooms = List.of(
                new MappingOptions.ChannelRoomOption("mock_deluxe", "Deluxe King Room"),
                new MappingOptions.ChannelRoomOption("mock_standard", "Standard Queen Room"),
                new MappingOptions.ChannelRoomOption("mock_suite", "Executive Suite")
        );

        List<MappingOptions.ChannelRateOption> rates = List.of(
                new MappingOptions.ChannelRateOption("mock_bar", "Best Available Rate (Flexible)", "mock_deluxe"),
                new MappingOptions.ChannelRateOption("mock_nref", "Non-Refundable Promo", "mock_deluxe"),
                new MappingOptions.ChannelRateOption("mock_bar", "Best Available Rate (Flexible)", "mock_standard"),
                new MappingOptions.ChannelRateOption("mock_nref", "Non-Refundable Promo", "mock_standard"),
                new MappingOptions.ChannelRateOption("mock_bar", "Best Available Rate (Flexible)", "mock_suite"),
                new MappingOptions.ChannelRateOption("mock_nref", "Non-Refundable Promo", "mock_suite")
        );

        return new MappingOptions(rooms, rates);
    }

    @Override
    public SyncResult pushAvailability(ChannelAccount account, List<AvailabilityUpdate> updates) {
        if (account.getAccountName() != null && account.getAccountName().contains("fail_sync")) {
            log.warn("MockConnector: Simulating upstream failure for account {}", account.getId());
            return SyncResult.failure(500, "Simulated OTA upstream server timeout", true);
        }
        log.info("MockConnector: Successfully pushed {} availability updates to {}", updates.size(), account.getAccountName());
        return SyncResult.ok("mock-ack-avail-" + UUID.randomUUID().toString().substring(0, 8),
                "Pushed " + updates.size() + " availability updates successfully");
    }

    @Override
    public SyncResult pushRates(ChannelAccount account, List<RateUpdate> updates) {
        if (account.getAccountName() != null && account.getAccountName().contains("fail_sync")) {
            log.warn("MockConnector: Simulating upstream rate push failure for account {}", account.getId());
            return SyncResult.failure(500, "Simulated OTA upstream server timeout", true);
        }
        log.info("MockConnector: Successfully pushed {} rate updates to {}", updates.size(), account.getAccountName());
        return SyncResult.ok("mock-ack-rate-" + UUID.randomUUID().toString().substring(0, 8),
                "Pushed " + updates.size() + " rate updates successfully");
    }

    @Override
    public SyncResult pushRestrictions(ChannelAccount account, List<RestrictionUpdate> updates) {
        if (account.getAccountName() != null && account.getAccountName().contains("fail_sync")) {
            log.warn("MockConnector: Simulating upstream restriction push failure for account {}", account.getId());
            return SyncResult.failure(500, "Simulated OTA upstream server timeout", true);
        }
        log.info("MockConnector: Successfully pushed {} restriction updates to {}", updates.size(), account.getAccountName());
        return SyncResult.ok("mock-ack-restr-" + UUID.randomUUID().toString().substring(0, 8),
                "Pushed " + updates.size() + " restriction updates successfully");
    }

    @Override
    public List<InboundReservation> pullReservations(ChannelAccount account, Instant since) {
        List<InboundReservation> available = new ArrayList<>(simulatedQueue);
        simulatedQueue.clear();
        return available;
    }

    @Override
    public void acknowledge(ChannelAccount account, String channelReservationId) {
        log.info("MockConnector: Acknowledged reservation {} on channel {}", channelReservationId, account.getAccountName());
    }

    // Test helper to enqueue a simulated booking
    public void enqueueSimulatedBooking(InboundReservation reservation) {
        simulatedQueue.add(reservation);
    }
}

