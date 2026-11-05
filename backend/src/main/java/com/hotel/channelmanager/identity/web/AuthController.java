package com.hotel.channelmanager.identity.web;

import com.hotel.channelmanager.identity.service.AuthService;
import com.hotel.channelmanager.identity.web.dto.AuthResponse;
import com.hotel.channelmanager.identity.web.dto.LoginRequest;
import com.hotel.channelmanager.identity.web.dto.RefreshTokenRequest;
import com.hotel.channelmanager.identity.web.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Tenant user login, token refresh, and session status")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and issue JWT tokens")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = httpRequest.getRemoteAddr();
        }
        AuthResponse response = authService.login(request, ip);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange refresh token for a new access token")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Retrieve currently authenticated user profile and tenant")
    public ResponseEntity<UserDto> getMe(@AuthenticationPrincipal String userId) {
        UserDto user = authService.getCurrentUser(userId);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/logout")
    @Operation(summary = "Acknowledge client logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/register-demo")
    @Operation(summary = "Seed demo organization and test accounts")
    public ResponseEntity<Map<String, String>> seedDemo() {
        authService.seedDemoOrganization();
        return ResponseEntity.ok(Map.of(
                "message", "Demo accounts initialized",
                "adminEmail", "admin@grandpalace.com",
                "defaultPassword", "Password123!"
        ));
    }
}

