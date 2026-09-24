package com.ndz.auth_service;

import com.ndz.auth_service.entity.Role;
import com.ndz.auth_service.entity.User;
import com.ndz.auth_service.entity.UserStatus;
import com.ndz.auth_service.repository.UserRepository;
import com.ndz.auth_service.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AuthFlowIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES;

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
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtService jwtService;

    @Test
    void registerLoginRefreshAndMe_flowWorks() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";

        MvcResult registerResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "password123",
                                "fullName", "Test User",
                                "phone", "0900000000"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andReturn();

        JsonNode registerBody = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String accessToken = registerBody.get("accessToken").asString();

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"));

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "password123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String refreshToken = loginBody.get("refreshToken").asString();

        MvcResult refreshResult = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", refreshToken
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode refreshBody = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        assertThat(refreshBody.get("accessToken").asString()).isNotBlank();

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", refreshToken
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanCreateShopManagerAndAssignShop() throws Exception {
        User admin = new User();
        admin.setEmail("admin-" + UUID.randomUUID() + "@example.com");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123456"));
        admin.setFullName("Test Admin");
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        userRepository.save(admin);

        String adminToken = jwtService.generateAccessToken(
                admin.getId(),
                admin.getEmail(),
                Role.ADMIN,
                List.of()
        );

        String managerEmail = "manager-" + UUID.randomUUID() + "@example.com";
        MvcResult createManagerResult = mockMvc.perform(post("/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", managerEmail,
                                "password", "password123",
                                "fullName", "Shop Manager",
                                "role", "SHOP_MANAGER"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("SHOP_MANAGER"))
                .andReturn();

        JsonNode managerBody = objectMapper.readTree(createManagerResult.getResponse().getContentAsString());
        UUID managerId = UUID.fromString(managerBody.get("id").asString());
        UUID shopId = UUID.randomUUID();

        mockMvc.perform(post("/admin/user-shop-mapping")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", managerId,
                                "shopId", shopId
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(managerId.toString()))
                .andExpect(jsonPath("$.shopId").value(shopId.toString()));

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", managerEmail,
                                "password", "password123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.shopIds[0]").value(shopId.toString()))
                .andReturn();

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String managerAccessToken = loginBody.get("accessToken").asString();

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + managerAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shopIds[0]").value(shopId.toString()));
    }

    @Test
    void nonAdminCannotAccessAdminEndpoints() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";

        MvcResult registerResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "password123",
                                "fullName", "Normal User"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        String accessToken = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("accessToken")
                .asString();

        mockMvc.perform(post("/admin/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "x@example.com",
                                "password", "password123",
                                "fullName", "X",
                                "role", "USER"
                        ))))
                .andExpect(status().isForbidden());
    }
}
