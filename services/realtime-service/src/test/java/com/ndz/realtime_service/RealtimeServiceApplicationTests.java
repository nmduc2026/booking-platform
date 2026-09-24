package com.ndz.realtime_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "app.sqs.consumer-enabled=false",
        "app.jwt.secret=change-me-to-a-very-long-secret-key-at-least-256-bits-long!!"
})
class RealtimeServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
