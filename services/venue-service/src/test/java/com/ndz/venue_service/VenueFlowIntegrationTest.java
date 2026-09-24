package com.ndz.venue_service;

import com.ndz.venue_service.entity.Role;
import com.ndz.venue_service.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class VenueFlowIntegrationTest {

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
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JwtService jwtService;

    @Test
    void adminCreatesShop_managerOwnsShop_canCreateResourceAndSlot_publicCanList() throws Exception {
        UUID adminId = UUID.randomUUID();
        String adminToken = jwtService.generateAccessToken(
                adminId, "admin@test.com", Role.ADMIN, List.of()
        );

        MvcResult shopResult = mockMvc.perform(post("/admin/shops")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Spa Alpha",
                                "address", "1 Main St",
                                "description", "Relax"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Spa Alpha"))
                .andReturn();

        UUID shopId = UUID.fromString(
                objectMapper.readTree(shopResult.getResponse().getContentAsString()).get("id").asString()
        );

        String managerToken = jwtService.generateAccessToken(
                UUID.randomUUID(), "manager@test.com", Role.SHOP_MANAGER, List.of(shopId)
        );

        MvcResult resourceResult = mockMvc.perform(post("/shops/" + shopId + "/resources")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Room 1",
                                "type", "ROOM"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Room 1"))
                .andReturn();

        UUID resourceId = UUID.fromString(
                objectMapper.readTree(resourceResult.getResponse().getContentAsString()).get("id").asString()
        );

        LocalDate day = LocalDate.of(2026, 10, 1);
        Instant start = day.atTime(10, 0).toInstant(ZoneOffset.UTC);
        Instant end = start.plus(1, ChronoUnit.HOURS);

        mockMvc.perform(post("/shops/" + shopId + "/slots")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "resourceId", resourceId,
                                "startTime", start.toString(),
                                "endTime", end.toString(),
                                "price", new BigDecimal("49.99")
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        mockMvc.perform(get("/shops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(shopId.toString()));

        mockMvc.perform(get("/shops/" + shopId + "/slots").param("date", day.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].resourceId").value(resourceId.toString()))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    void shopManagerCannotManageOtherShop() throws Exception {
        UUID adminId = UUID.randomUUID();
        String adminToken = jwtService.generateAccessToken(adminId, "admin2@test.com", Role.ADMIN, List.of());

        MvcResult shopA = mockMvc.perform(post("/admin/shops")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Shop A"))))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult shopB = mockMvc.perform(post("/admin/shops")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Shop B"))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode shopABody = objectMapper.readTree(shopA.getResponse().getContentAsString());
        JsonNode shopBBody = objectMapper.readTree(shopB.getResponse().getContentAsString());
        UUID shopAId = UUID.fromString(shopABody.get("id").asString());
        UUID shopBId = UUID.fromString(shopBBody.get("id").asString());

        String managerOfA = jwtService.generateAccessToken(
                UUID.randomUUID(), "manager-a@test.com", Role.SHOP_MANAGER, List.of(shopAId)
        );

        mockMvc.perform(post("/shops/" + shopBId + "/resources")
                        .header("Authorization", "Bearer " + managerOfA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Should Fail",
                                "type", "STAFF"
                        ))))
                .andExpect(status().isForbidden());

        Instant start = Instant.parse("2026-10-02T09:00:00Z");
        mockMvc.perform(post("/shops/" + shopBId + "/slots")
                        .header("Authorization", "Bearer " + managerOfA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "resourceId", UUID.randomUUID(),
                                "startTime", start.toString(),
                                "endTime", start.plus(1, ChronoUnit.HOURS).toString(),
                                "price", 10
                        ))))
                .andExpect(status().isForbidden());
    }
}
