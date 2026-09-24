package com.ndz.payment_service.outbox;

import com.ndz.payment_service.entity.OutboxEvent;
import com.ndz.payment_service.entity.OutboxStatus;
import com.ndz.payment_service.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OutboxRelayService {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final PaymentEventPublisher publisher;

    public OutboxRelayService(OutboxEventRepository outboxEventRepository, PaymentEventPublisher publisher) {
        this.outboxEventRepository = outboxEventRepository;
        this.publisher = publisher;
    }

    @Transactional
    public void publishPendingEvent(UUID eventId) {
        OutboxEvent event = outboxEventRepository.findById(eventId).orElse(null);
        if (event == null || event.getStatus() != OutboxStatus.PENDING) {
            return;
        }

        publisher.publish(event.getEventType(), event.getPayload());
        event.setStatus(OutboxStatus.SENT);
        event.setSentAt(Instant.now());
        log.info("Outbox event {} marked SENT ({})", event.getId(), event.getEventType());
    }
}
