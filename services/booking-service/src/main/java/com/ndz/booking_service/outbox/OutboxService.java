package com.ndz.booking_service.outbox;

import com.ndz.booking_service.entity.Booking;
import com.ndz.booking_service.entity.OutboxEvent;
import com.ndz.booking_service.entity.OutboxStatus;
import com.ndz.booking_service.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class OutboxService {

    public static final String BOOKING_CREATED = "BOOKING_CREATED";
    public static final String BOOKING_CONFIRMED = "BOOKING_CONFIRMED";
    public static final String BOOKING_CANCELLED = "BOOKING_CANCELLED";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void enqueue(String eventType, Booking booking) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", eventType);
        payload.put("bookingId", booking.getId().toString());
        payload.put("userId", booking.getUserId().toString());
        payload.put("shopId", booking.getShopId().toString());
        payload.put("slotId", booking.getSlotId().toString());
        payload.put("status", booking.getStatus().name());
        payload.put("amount", booking.getAmount());
        if (booking.getUserEmail() != null && !booking.getUserEmail().isBlank()) {
            payload.put("userEmail", booking.getUserEmail());
        }

        OutboxEvent event = new OutboxEvent();
        event.setAggregateId(booking.getId());
        event.setEventType(eventType);
        event.setPayload(objectMapper.writeValueAsString(payload));
        event.setStatus(OutboxStatus.PENDING);
        outboxEventRepository.save(event);
    }
}
