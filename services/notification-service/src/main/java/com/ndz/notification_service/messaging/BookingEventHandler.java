package com.ndz.notification_service.messaging;

import com.ndz.notification_service.config.MailProperties;
import com.ndz.notification_service.mail.EmailService;
import com.ndz.notification_service.mail.EmailTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

@Service
public class BookingEventHandler {

    public static final String BOOKING_CONFIRMED = "BOOKING_CONFIRMED";
    public static final String BOOKING_CANCELLED = "BOOKING_CANCELLED";

    private static final Logger log = LoggerFactory.getLogger(BookingEventHandler.class);

    private final ObjectMapper objectMapper;
    private final EmailService emailService;
    private final EmailTemplateService templates;
    private final MailProperties mailProperties;

    public BookingEventHandler(
            ObjectMapper objectMapper,
            EmailService emailService,
            EmailTemplateService templates,
            MailProperties mailProperties
    ) {
        this.objectMapper = objectMapper;
        this.emailService = emailService;
        this.templates = templates;
        this.mailProperties = mailProperties;
    }

    public void handle(String messageBody) {
        JsonNode root = objectMapper.readTree(messageBody);
        String eventType = text(root, "eventType");
        if (eventType == null) {
            throw new IllegalArgumentException("booking event missing eventType");
        }

        String bookingId = text(root, "bookingId");
        String shopId = text(root, "shopId");
        String amount = amountText(root.get("amount"));
        String to = resolveRecipient(root);

        switch (eventType) {
            case BOOKING_CONFIRMED -> emailService.sendHtml(
                    to,
                    templates.confirmSubject(bookingId),
                    templates.confirmBody(bookingId, shopId, amount)
            );
            case BOOKING_CANCELLED -> emailService.sendHtml(
                    to,
                    templates.cancelSubject(bookingId),
                    templates.cancelBody(bookingId, shopId, amount)
            );
            default -> log.debug("Ignoring booking event type {}", eventType);
        }
    }

    private String resolveRecipient(JsonNode root) {
        String userEmail = text(root, "userEmail");
        if (userEmail != null && !userEmail.isBlank()) {
            return userEmail;
        }
        return mailProperties.fallbackTo();
    }

    private static String text(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asString();
    }

    private static String amountText(JsonNode amountNode) {
        if (amountNode == null || amountNode.isNull()) {
            return "n/a";
        }
        if (amountNode.isNumber()) {
            return EmailTemplateService.formatAmount(amountNode.decimalValue());
        }
        try {
            return EmailTemplateService.formatAmount(new BigDecimal(amountNode.asString()));
        } catch (Exception ex) {
            return amountNode.asString();
        }
    }
}
