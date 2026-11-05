package com.hotel.channelmanager.channels.sync;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.channelmanager.channels.connector.*;
import com.hotel.channelmanager.channels.domain.*;
import com.hotel.channelmanager.channels.outbox.OutboxEvent;
import com.hotel.channelmanager.channels.outbox.OutboxEventRepository;
import com.hotel.channelmanager.channels.outbox.OutboxStatus;
import com.hotel.channelmanager.channels.sync.payload.RatePlanDateRate;
import com.hotel.channelmanager.channels.sync.payload.RatePlanDateRestriction;
import com.hotel.channelmanager.channels.sync.payload.RoomDateInventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class SyncWorkerService {

    private static final Logger log = LoggerFactory.getLogger(SyncWorkerService.class);

    private final ChannelAccountRepository channelAccountRepository;
    private final ChannelRoomMappingRepository roomMappingRepository;
    private final ChannelRateMappingRepository rateMappingRepository;
    private final SyncJobRepository syncJobRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ChannelConnectorRegistry connectorRegistry;
    private final ObjectMapper objectMapper;

    // Exponential backoff durations for sync retries: 30s, 2m, 10m, 1h, 6h
    private static final Duration[] BACKOFF_DURATIONS = {
            Duration.ofSeconds(30),
            Duration.ofMinutes(2),
            Duration.ofMinutes(10),
            Duration.ofHours(1),
            Duration.ofHours(6)
    };

    public SyncWorkerService(
            ChannelAccountRepository channelAccountRepository,
            ChannelRoomMappingRepository roomMappingRepository,
            ChannelRateMappingRepository rateMappingRepository,
            SyncJobRepository syncJobRepository,
            OutboxEventRepository outboxEventRepository,
            ChannelConnectorRegistry connectorRegistry,
            ObjectMapper objectMapper
    ) {
        this.channelAccountRepository = channelAccountRepository;
        this.roomMappingRepository = roomMappingRepository;
        this.rateMappingRepository = rateMappingRepository;
        this.syncJobRepository = syncJobRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.connectorRegistry = connectorRegistry;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void processEvent(OutboxEvent event) {
        event.setStatus(OutboxStatus.PROCESSING);
        outboxEventRepository.save(event);

        try {
            String propertyId = event.getPropertyId();
            String orgId = event.getOrganizationId();
            List<ChannelAccount> accounts = channelAccountRepository.findAllByPropertyIdAndOrganizationIdAndSyncEnabledTrue(propertyId, orgId);

            if (accounts.isEmpty()) {
                log.info("No active channels configured for property {}. Outbox event marked published.", propertyId);
                event.setStatus(OutboxStatus.PUBLISHED);
                event.setProcessedAt(Instant.now());
                outboxEventRepository.save(event);
                return;
            }

            for (ChannelAccount account : accounts) {
                dispatchToChannel(account, event);
            }

            event.setStatus(OutboxStatus.PUBLISHED);
            event.setProcessedAt(Instant.now());
            outboxEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to process outbox event {}", event.getId(), e);
            event.setRetryCount(event.getRetryCount() + 1);
            event.setLastError(e.getMessage());
            if (event.getRetryCount() >= 5) {
                event.setStatus(OutboxStatus.FAILED);
            } else {
                event.setStatus(OutboxStatus.PENDING);
            }
            outboxEventRepository.save(event);
        }
    }

    private void dispatchToChannel(ChannelAccount account, OutboxEvent event) {
        Optional<ChannelConnector> connectorOpt = connectorRegistry.getConnector(account.getChannelType());
        if (connectorOpt.isEmpty()) {
            log.warn("No connector found for channel type {}", account.getChannelType());
            return;
        }

        ChannelConnector connector = connectorOpt.get();

        switch (event.getAggregateType()) {
            case "INVENTORY" -> syncInventory(account, connector, event);
            case "RATE" -> syncRates(account, connector, event);
            case "RESTRICTION" -> syncRestrictions(account, connector, event);
            default -> log.warn("Unrecognized aggregate type {} in event {}", event.getAggregateType(), event.getId());
        }
    }

    private void syncInventory(ChannelAccount account, ChannelConnector connector, OutboxEvent event) {
        try {
            List<RoomDateInventory> items = objectMapper.readValue(event.getPayload(), new TypeReference<>() {});
            List<ChannelRoomMapping> mappings = roomMappingRepository.findAllByChannelAccountIdAndOrganizationId(
                    account.getId(), account.getOrganizationId());

            Map<String, String> roomToChannelMap = new HashMap<>();
            for (ChannelRoomMapping m : mappings) {
                if (m.isActive()) {
                    roomToChannelMap.put(m.getRoomTypeId(), m.getChannelRoomId());
                }
            }

            List<AvailabilityUpdate> updates = new ArrayList<>();
            for (RoomDateInventory item : items) {
                String chRoomId = roomToChannelMap.get(item.roomTypeId());
                if (chRoomId != null) {
                    updates.add(new AvailabilityUpdate(item.roomTypeId(), chRoomId, item.date(), item.availableUnits()));
                }
            }

            if (updates.isEmpty()) {
                log.debug("No mapped rooms for channel {} in event {}. Skipping sync push.", account.getId(), event.getId());
                return;
            }

            SyncJob job = new SyncJob(
                    account.getOrganizationId(),
                    account.getPropertyId(),
                    account.getId(),
                    SyncJobType.AVAILABILITY_PUSH,
                    "Pushing availability for " + updates.size() + " date entries."
            );
            syncJobRepository.save(job);

            executePush(account, job, () -> connector.pushAvailability(account, updates));
        } catch (Exception e) {
            log.error("Failed to parse or push inventory sync for account {}", account.getId(), e);
        }
    }

    private void syncRates(ChannelAccount account, ChannelConnector connector, OutboxEvent event) {
        try {
            List<RatePlanDateRate> items = objectMapper.readValue(event.getPayload(), new TypeReference<>() {});
            List<ChannelRateMapping> mappings = rateMappingRepository.findAllByChannelAccountIdAndOrganizationId(
                    account.getId(), account.getOrganizationId());

            Map<String, String> planToChannelMap = new HashMap<>();
            for (ChannelRateMapping m : mappings) {
                if (m.isActive()) {
                    planToChannelMap.put(m.getRatePlanId(), m.getChannelRateId());
                }
            }

            List<RateUpdate> updates = new ArrayList<>();
            for (RatePlanDateRate item : items) {
                String chRateId = planToChannelMap.get(item.ratePlanId());
                if (chRateId != null) {
                    updates.add(new RateUpdate(item.ratePlanId(), chRateId, item.date(), item.amountCents(), item.currency()));
                }
            }

            if (updates.isEmpty()) {
                return;
            }

            SyncJob job = new SyncJob(
                    account.getOrganizationId(),
                    account.getPropertyId(),
                    account.getId(),
                    SyncJobType.RATE_PUSH,
                    "Pushing rates for " + updates.size() + " date entries."
            );
            syncJobRepository.save(job);

            executePush(account, job, () -> connector.pushRates(account, updates));
        } catch (Exception e) {
            log.error("Failed to parse or push rates sync for account {}", account.getId(), e);
        }
    }

    private void syncRestrictions(ChannelAccount account, ChannelConnector connector, OutboxEvent event) {
        try {
            List<RatePlanDateRestriction> items = objectMapper.readValue(event.getPayload(), new TypeReference<>() {});
            List<ChannelRateMapping> mappings = rateMappingRepository.findAllByChannelAccountIdAndOrganizationId(
                    account.getId(), account.getOrganizationId());

            Map<String, String> planToChannelMap = new HashMap<>();
            for (ChannelRateMapping m : mappings) {
                if (m.isActive()) {
                    planToChannelMap.put(m.getRatePlanId(), m.getChannelRateId());
                }
            }

            List<RestrictionUpdate> updates = new ArrayList<>();
            for (RatePlanDateRestriction item : items) {
                String chRateId = planToChannelMap.get(item.ratePlanId());
                if (chRateId != null) {
                    updates.add(new RestrictionUpdate(
                            item.ratePlanId(),
                            chRateId,
                            item.date(),
                            item.stopSell(),
                            item.minStay(),
                            item.maxStay(),
                            item.cta(),
                            item.ctd()
                    ));
                }
            }

            if (updates.isEmpty()) {
                return;
            }

            SyncJob job = new SyncJob(
                    account.getOrganizationId(),
                    account.getPropertyId(),
                    account.getId(),
                    SyncJobType.RESTRICTION_PUSH,
                    "Pushing restrictions for " + updates.size() + " date entries."
            );
            syncJobRepository.save(job);

            executePush(account, job, () -> connector.pushRestrictions(account, updates));
        } catch (Exception e) {
            log.error("Failed to parse or push restrictions sync for account {}", account.getId(), e);
        }
    }

    private void executePush(ChannelAccount account, SyncJob job, PushSupplier supplier) {
        try {
            SyncResult result = supplier.push();
            if (result.success()) {
                job.markSuccess(result.message() + " (Ref: " + result.externalReference() + ")");
                account.setLastSyncAt(Instant.now());
                account.setStatus(ChannelStatus.CONNECTED);
                account.setErrorMessage(null);
            } else {
                int attempt = job.getAttemptCount();
                Duration delay = attempt < BACKOFF_DURATIONS.length ? BACKOFF_DURATIONS[attempt] : Duration.ofHours(6);
                Instant nextRetry = Instant.now().plus(delay);

                job.markFailed(result.message(), result.retryable(), nextRetry);
                account.setStatus(ChannelStatus.ERROR);
                account.setErrorMessage(result.message());
            }
        } catch (Exception e) {
            log.error("Error pushing sync to channel account {}", account.getId(), e);
            int attempt = job.getAttemptCount();
            Duration delay = attempt < BACKOFF_DURATIONS.length ? BACKOFF_DURATIONS[attempt] : Duration.ofHours(6);
            Instant nextRetry = Instant.now().plus(delay);

            job.markFailed("Unexpected error: " + e.getMessage(), true, nextRetry);
            account.setStatus(ChannelStatus.ERROR);
            account.setErrorMessage(e.getMessage());
        } finally {
            syncJobRepository.save(job);
            channelAccountRepository.save(account);
        }
    }

    @FunctionalInterface
    private interface PushSupplier {
        SyncResult push() throws Exception;
    }
}

