package com.ndz.booking_service.messaging;

import com.ndz.booking_service.entity.ProcessedEvent;
import com.ndz.booking_service.repository.ProcessedEventRepository;
import com.ndz.booking_service.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
public class PaymentEventHandler {

    public static final String PAYMENT_SUCCEEDED = "PAYMENT_SUCCEEDED";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";

    private static final Logger log = LoggerFactory.getLogger(PaymentEventHandler.class);

    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;
    private final BookingService bookingService;

    public PaymentEventHandler(
            ObjectMapper objectMapper,
            ProcessedEventRepository processedEventRepository,
            BookingService bookingService
    ) {
        this.objectMapper = objectMapper;
        this.processedEventRepository = processedEventRepository;
        this.bookingService = bookingService;
    }

    @Transactional
    public void handle(String messageBody) {
        JsonNode root = objectMapper.readTree(messageBody);
        String eventType = text(root, "eventType");
        String paymentId = text(root, "paymentId");
        String bookingIdRaw = text(root, "bookingId");

        if (eventType == null || paymentId == null || bookingIdRaw == null) {
            throw new IllegalArgumentException("payment event missing eventType/paymentId/bookingId");
        }

        String eventId = eventType + ":" + paymentId;
        if (processedEventRepository.existsById(eventId)) {
            log.info("Skipping already processed event {}", eventId);
            return;
        }

        UUID bookingId = UUID.fromString(bookingIdRaw);
        switch (eventType) {
            case PAYMENT_SUCCEEDED -> bookingService.confirmFromPayment(bookingId);
            case PAYMENT_FAILED -> bookingService.cancelFromPayment(bookingId);
            default -> {
                log.debug("Ignoring unsupported payment event type {}", eventType);
                return;
            }
        }

        processedEventRepository.save(new ProcessedEvent(eventId));
        log.info("Processed payment event {}", eventId);
    }

    private static String text(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asString();
    }
}
