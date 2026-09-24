package com.ndz.booking_service.outbox;

import com.ndz.booking_service.config.OutboxProperties;
import com.ndz.booking_service.entity.OutboxEvent;
import com.ndz.booking_service.entity.OutboxStatus;
import com.ndz.booking_service.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "app.outbox", name = "relay-enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayJob.class);

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxRelayService outboxRelayService;
    private final OutboxProperties properties;

    public OutboxRelayJob(
            OutboxEventRepository outboxEventRepository,
            OutboxRelayService outboxRelayService,
            OutboxProperties properties
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxRelayService = outboxRelayService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:2000}")
    public void relay() {
        List<OutboxEvent> batch = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        if (batch.isEmpty()) {
            return;
        }

        int limit = Math.min(properties.batchSize(), batch.size());
        for (int i = 0; i < limit; i++) {
            OutboxEvent event = batch.get(i);
            try {
                outboxRelayService.publishPendingEvent(event.getId());
            } catch (Exception ex) {
                log.warn("Failed to relay outbox event {}: {}", event.getId(), ex.getMessage());
                // Leave as PENDING for retry on next poll (at-least-once).
            }
        }
    }
}
