package com.ndz.booking_service.messaging;

import com.ndz.booking_service.config.SqsProperties;
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
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final SqsClient sqsClient;
    private final SqsProperties properties;
    private final PaymentEventHandler paymentEventHandler;
    private volatile String queueUrl;

    public PaymentEventConsumer(
            SqsClient sqsClient,
            SqsProperties properties,
            PaymentEventHandler paymentEventHandler
    ) {
        this.sqsClient = sqsClient;
        this.properties = properties;
        this.paymentEventHandler = paymentEventHandler;
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
            log.warn("Failed to poll payment-events: {}", ex.getMessage());
            return;
        }

        if (messages.isEmpty()) {
            return;
        }

        for (Message message : messages) {
            try {
                paymentEventHandler.handle(message.body());
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(resolveQueueUrl())
                        .receiptHandle(message.receiptHandle())
                        .build());
            } catch (Exception ex) {
                log.warn("Failed to process payment event {}: {}", message.messageId(), ex.getMessage());
                // Leave message for retry after visibility timeout (at-least-once).
            }
        }
    }

    private String resolveQueueUrl() {
        if (queueUrl == null) {
            synchronized (this) {
                if (queueUrl == null) {
                    queueUrl = sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                                    .queueName(properties.paymentEventsQueue())
                                    .build())
                            .queueUrl();
                }
            }
        }
        return queueUrl;
    }
}
