package com.ndz.booking_service.outbox;

import com.ndz.booking_service.config.SqsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Component
public class SqsBookingEventPublisher implements BookingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SqsBookingEventPublisher.class);

    private final SqsClient sqsClient;
    private final SqsProperties properties;
    private volatile String queueUrl;

    public SqsBookingEventPublisher(SqsClient sqsClient, SqsProperties properties) {
        this.sqsClient = sqsClient;
        this.properties = properties;
    }

    @Override
    public void publish(String eventType, String payload) {
        String url = resolveQueueUrl();
        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(url)
                .messageBody(payload)
                .messageAttributes(java.util.Map.of(
                        "eventType",
                        software.amazon.awssdk.services.sqs.model.MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(eventType)
                                .build()
                ))
                .build());
        log.info("Published {} to {}", eventType, properties.bookingEventsQueue());
    }

    private String resolveQueueUrl() {
        if (queueUrl == null) {
            synchronized (this) {
                if (queueUrl == null) {
                    queueUrl = sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                                    .queueName(properties.bookingEventsQueue())
                                    .build())
                            .queueUrl();
                }
            }
        }
        return queueUrl;
    }
}
