package com.ndz.notification_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "app.sqs.consumer-enabled=false",
        "spring.mail.host=localhost",
        "spring.mail.port=25",
        "app.mail.from=test@example.com",
        "app.mail.fallback-to=fallback@example.com"
})
class NotificationServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
