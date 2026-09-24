package com.ndz.payment_service.outbox;

import com.ndz.payment_service.entity.OutboxEvent;
import com.ndz.payment_service.entity.OutboxStatus;
import com.ndz.payment_service.entity.Payment;
import com.ndz.payment_service.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class OutboxService {

    public static final String PAYMENT_SUCCEEDED = "PAYMENT_SUCCEEDED";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void enqueue(String eventType, Payment payment) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", eventType);
        payload.put("paymentId", payment.getId().toString());
        payload.put("bookingId", payment.getBookingId().toString());
        payload.put("stripePaymentIntentId", payment.getStripePaymentIntentId());
        payload.put("amount", payment.getAmount());
        payload.put("currency", payment.getCurrency());
        payload.put("status", payment.getStatus().name());

        OutboxEvent event = new OutboxEvent();
        event.setAggregateId(payment.getId());
        event.setEventType(eventType);
        event.setPayload(objectMapper.writeValueAsString(payload));
        event.setStatus(OutboxStatus.PENDING);
        outboxEventRepository.save(event);
    }
}
