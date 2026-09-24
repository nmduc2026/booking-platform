package com.ndz.notification_service.messaging;

import com.ndz.notification_service.config.SqsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "app.sqs", name = "consumer-enabled", havingValue = "true", matchIfMissing = true)
public class BookingEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(BookingEventConsumer.class);

    private final SqsClient sqsClient;
    private final SqsProperties properties;
    private final BookingEventHandler bookingEventHandler;
    private volatile String queueUrl;

    public BookingEventConsumer(
            SqsClient sqsClient,
            SqsProperties properties,
            BookingEventHandler bookingEventHandler
    ) {
        this.sqsClient = sqsClient;
        this.properties = properties;
        this.bookingEventHandler = bookingEventHandler;
    }

    @Scheduled(fixedDelayString = "${app.sqs.consumer-poll-interval-ms:2000}")
    public void poll() {
        List<Message> messages;
        try {
            messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                            .queueUrl(resolveQueueUrl())
                            .maxNumberOfMessages(Math.min(Math.max(properties.consumerBatchSize(), 1), 10))
                            .waitTimeSeconds(Math.min(Math.max(properties.consumerWaitTimeSeconds(), 0), 20))
                            .messageAttributeNames("All")
                            .build())
                    .messages();
        } catch (Exception ex) {
            log.warn("Failed to poll booking-events: {}", ex.getMessage());
            return;
        }

        if (messages.isEmpty()) {
            return;
        }

        for (Message message : messages) {
            try {
                bookingEventHandler.handle(message.body());
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(resolveQueueUrl())
                        .receiptHandle(message.receiptHandle())
                        .build());
            } catch (Exception ex) {
                log.warn("Failed to process booking event {}: {}", message.messageId(), ex.getMessage());
            }
        }
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
