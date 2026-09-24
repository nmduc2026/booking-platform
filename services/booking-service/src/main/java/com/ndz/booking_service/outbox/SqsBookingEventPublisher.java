package com.ndz.booking_service.outbox;

import com.ndz.booking_service.config.SqsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SqsBookingEventPublisher implements BookingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SqsBookingEventPublisher.class);

    private final SqsClient sqsClient;
    private final SqsProperties properties;
    private final ConcurrentHashMap<String, String> queueUrls = new ConcurrentHashMap<>();

    public SqsBookingEventPublisher(SqsClient sqsClient, SqsProperties properties) {
        this.sqsClient = sqsClient;
        this.properties = properties;
    }

    @Override
    public void publish(String eventType, String payload) {
        send(properties.bookingEventsQueue(), eventType, payload);
        send(properties.bookingEventsRealtimeQueue(), eventType, payload);
    }

    private void send(String queueName, String eventType, String payload) {
        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(resolveQueueUrl(queueName))
                .messageBody(payload)
                .messageAttributes(Map.of(
                        "eventType",
                        MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(eventType)
                                .build()
                ))
                .build());
        log.info("Published {} to {}", eventType, queueName);
    }

    private String resolveQueueUrl(String queueName) {
        return queueUrls.computeIfAbsent(queueName, name ->
                sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(name).build()).queueUrl()
        );
    }
}
