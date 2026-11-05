package com.hotel.channelmanager.channels.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordEvent(
            String organizationId,
            String propertyId,
            String aggregateType,
            String aggregateId,
            String eventType,
            Object payload,
            String idempotencyKey
    ) {
        if (outboxEventRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.warn("Outbox event with idempotency key {} already exists. Skipping duplicate.", idempotencyKey);
            return;
        }

        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            OutboxEvent event = new OutboxEvent(
                    organizationId,
                    propertyId,
                    aggregateType,
                    aggregateId,
                    eventType,
                    jsonPayload,
                    idempotencyKey
            );
            outboxEventRepository.save(event);
            log.debug("Recorded outbox event {} for property {}", event.getId(), propertyId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload for idempotency key {}", idempotencyKey, e);
            throw new RuntimeException("Could not serialize outbox payload", e);
        }
    }
}

