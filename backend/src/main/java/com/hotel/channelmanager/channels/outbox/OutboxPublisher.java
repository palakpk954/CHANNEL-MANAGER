package com.hotel.channelmanager.channels.outbox;

import com.hotel.channelmanager.channels.sync.SyncWorkerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final SyncWorkerService syncWorkerService;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository, SyncWorkerService syncWorkerService) {
        this.outboxEventRepository = outboxEventRepository;
        this.syncWorkerService = syncWorkerService;
    }

    @Scheduled(fixedDelay = 2000)
    public void pollAndPublish() {
        publishPendingBatch(50);
    }

    public int publishPendingBatch(int batchSize) {
        List<OutboxEvent> pending = outboxEventRepository.findAllByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING, PageRequest.of(0, batchSize));

        if (pending.isEmpty()) {
            return 0;
        }

        log.debug("Found {} pending outbox events to publish", pending.size());
        for (OutboxEvent event : pending) {
            try {
                syncWorkerService.processEvent(event);
            } catch (Exception e) {
                log.error("Unhandled exception publishing outbox event {}", event.getId(), e);
            }
        }
        return pending.size();
    }
}

