package com.hotel.channelmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.channelmanager.identity.domain.Organization;
import com.hotel.channelmanager.identity.domain.OrganizationRepository;
import com.hotel.channelmanager.identity.domain.Role;
import com.hotel.channelmanager.identity.domain.User;
import com.hotel.channelmanager.identity.domain.UserRepository;
import com.hotel.channelmanager.identity.web.dto.AuthResponse;
import com.hotel.channelmanager.identity.web.dto.LoginRequest;
import com.hotel.channelmanager.identity.web.dto.RefreshTokenRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Organization testOrg;
    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        testOrg = new Organization("org-test-1", "Test Hotel Chain", "PRO", "ACTIVE");
        organizationRepository.save(testOrg);

        testUser = new User(
                testOrg.getId(),
                "manager@testhotel.com",
                passwordEncoder.encode("Secret123!"),
                "Jane",
                "Doe",
                Role.ORG_ADMIN
        );
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("Login with valid credentials returns JWT access and refresh tokens")
    void testSuccessfulLogin() throws Exception {
        LoginRequest request = new LoginRequest("manager@testhotel.com", "Secret123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("manager@testhotel.com"))
                .andExpect(jsonPath("$.user.role").value("ORG_ADMIN"));
    }

    @Test
    @DisplayName("Login with wrong password returns 401 with plain-language action suggestion")
    void testFailedLoginWrongPassword() throws Exception {
        LoginRequest request = new LoginRequest("manager@testhotel.com", "WrongPassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("The email address or password is incorrect."))
                .andExpect(jsonPath("$.actionSuggestion").value("Please try again or reset your password."));
    }

    @Test
    @DisplayName("Accessing /me with valid JWT returns user and tenant context")
    void testGetMeAuthenticated() throws Exception {
        LoginRequest request = new LoginRequest("manager@testhotel.com", "Secret123!");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        AuthResponse auth = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponse.class);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("manager@testhotel.com"))
                .andExpect(jsonPath("$.organizationId").value(testOrg.getId()));
    }

    @Test
    @DisplayName("Refresh token exchange produces new access token")
    void testRefreshTokenSuccess() throws Exception {
        LoginRequest request = new LoginRequest("manager@testhotel.com", "Secret123!");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        AuthResponse auth = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponse.class);

        RefreshTokenRequest refreshReq = new RefreshTokenRequest(auth.refreshToken());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("manager@testhotel.com"));
    }
}

