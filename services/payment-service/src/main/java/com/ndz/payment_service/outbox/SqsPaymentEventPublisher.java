package com.ndz.payment_service.outbox;

import com.ndz.payment_service.config.SqsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Map;

@Component
public class SqsPaymentEventPublisher implements PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SqsPaymentEventPublisher.class);

    private final SqsClient sqsClient;
    private final SqsProperties properties;
    private volatile String queueUrl;

    public SqsPaymentEventPublisher(SqsClient sqsClient, SqsProperties properties) {
        this.sqsClient = sqsClient;
        this.properties = properties;
    }

    @Override
    public void publish(String eventType, String payload) {
        String url = resolveQueueUrl();
        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(url)
                .messageBody(payload)
                .messageAttributes(Map.of(
                        "eventType",
                        MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(eventType)
                                .build()
                ))
                .build());
        log.info("Published {} to {}", eventType, properties.paymentEventsQueue());
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
