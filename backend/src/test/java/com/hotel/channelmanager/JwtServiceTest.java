package com.hotel.channelmanager;

import com.hotel.channelmanager.common.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secret, 3600000, 604800000);
    }

    @Test
    @DisplayName("Should generate valid access token with correct claims")
    void testAccessTokenClaims() {
        String token = jwtService.generateAccessToken("user-123", "org-456", "test@hotel.com", "ORG_ADMIN");

        assertTrue(jwtService.isTokenValid(token));
        assertEquals("user-123", jwtService.extractUserId(token));
        assertEquals("org-456", jwtService.extractOrganizationId(token));
        assertEquals("test@hotel.com", jwtService.extractEmail(token));
        assertEquals("ORG_ADMIN", jwtService.extractRole(token));
    }

    @Test
    @DisplayName("Should generate refresh token marked with refresh type")
    void testRefreshToken() {
        String refreshToken = jwtService.generateRefreshToken("user-123", "org-456");

        assertTrue(jwtService.isTokenValid(refreshToken));
        assertEquals("user-123", jwtService.extractUserId(refreshToken));
        assertEquals("org-456", jwtService.extractOrganizationId(refreshToken));
        assertEquals("refresh", jwtService.extractTokenType(refreshToken));
    }

    @Test
    @DisplayName("Should reject malformed or tampered token")
    void testInvalidToken() {
        assertFalse(jwtService.isTokenValid("invalid.token.signature"));
    }
}

