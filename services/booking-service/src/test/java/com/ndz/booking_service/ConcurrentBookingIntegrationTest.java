package com.ndz.booking_service;

import com.ndz.booking_service.client.VenueClient;
import com.ndz.booking_service.client.VenueSlotResponse;
import com.ndz.booking_service.entity.Role;
import com.ndz.booking_service.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class ConcurrentBookingIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES;

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    static {
        POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
        POSTGRES.withDatabaseName("booking_platform");
        POSTGRES.withUsername("booking");
        POSTGRES.withPassword("123123");
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @TestConfiguration
    static class MockVenueConfig {
        @Bean
        @Primary
        VenueClient venueClient() {
            return Mockito.mock(VenueClient.class);
        }
    }

    @LocalServerPort
    int port;

    @Autowired
    JwtService jwtService;

    @Autowired
    VenueClient venueClient;

    @BeforeEach
    void stubVenue() {
        doNothing().when(venueClient).invalidateShopSlotCache(any());
    }

    @Test
    void onlyOneBookingSucceedsUnderConcurrency() throws Exception {
        UUID slotId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();

        when(venueClient.getSlot(slotId)).thenReturn(
                new VenueSlotResponse(slotId, shopId, UUID.randomUUID(), new BigDecimal("99.00"), "AVAILABLE")
        );

        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        List<String> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    String token = jwtService.generateAccessToken(
                            UUID.randomUUID(),
                            "user" + idx + "@test.com",
                            Role.USER,
                            List.of()
                    );
                    ready.countDown();
                    start.await(10, TimeUnit.SECONDS);

                    client.post()
                            .uri("/bookings")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(Map.of("slotId", slotId))
                            .retrieve()
                            .toBodilessEntity();
                    success.incrementAndGet();
                } catch (RestClientResponseException ex) {
                    if (ex.getStatusCode().value() == 409) {
                        conflict.incrementAndGet();
                    } else {
                        errors.add("unexpected status=" + ex.getStatusCode().value());
                    }
                } catch (Exception ex) {
                    errors.add(ex.getMessage());
                }
            });
        }

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(errors).isEmpty();
        assertThat(success.get()).isEqualTo(1);
        assertThat(conflict.get()).isEqualTo(threads - 1);
    }
}
