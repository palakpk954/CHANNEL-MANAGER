package com.hotel.channelmanager.identity.service;

import com.hotel.channelmanager.common.audit.AuditService;
import com.hotel.channelmanager.common.exception.ApiException;
import com.hotel.channelmanager.common.security.JwtService;
import com.hotel.channelmanager.common.tenant.TenantContext;
import com.hotel.channelmanager.identity.domain.Organization;
import com.hotel.channelmanager.identity.domain.OrganizationRepository;
import com.hotel.channelmanager.identity.domain.Role;
import com.hotel.channelmanager.identity.domain.User;
import com.hotel.channelmanager.identity.domain.UserRepository;
import com.hotel.channelmanager.identity.web.dto.AuthResponse;
import com.hotel.channelmanager.identity.web.dto.LoginRequest;
import com.hotel.channelmanager.identity.web.dto.RefreshTokenRequest;
import com.hotel.channelmanager.identity.web.dto.UserDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final long accessTokenExpirationMs;

    public AuthService(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditService auditService,
            @Value("${security.jwt.access-token-expiration-ms:3600000}") long accessTokenExpirationMs
    ) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new ApiException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED,
                        "INVALID_CREDENTIALS",
                        "The email address or password is incorrect.",
                        "Please try again or reset your password."
                ));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS",
                    "The email address or password is incorrect.",
                    "Please try again or reset your password."
            );
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw ApiException.forbidden("Your account is deactivated. Please contact support.");
        }

        Organization org = organizationRepository.findById(user.getOrganizationId())
                .orElseThrow(() -> ApiException.forbidden("Organization not found."));

        if (!"ACTIVE".equalsIgnoreCase(org.getStatus())) {
            throw ApiException.forbidden("Organization subscription is suspended.");
        }

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getOrganizationId(),
                user.getEmail(),
                user.getRole().name()
        );

        String refreshToken = jwtService.generateRefreshToken(
                user.getId(),
                user.getOrganizationId()
        );

        TenantContext.setOrganizationId(user.getOrganizationId());
        try {
            auditService.logChange(
                    user.getId(),
                    user.getEmail(),
                    "USER",
                    user.getId(),
                    "LOGIN",
                    null,
                    "SUCCESS",
                    ipAddress
            );
        } finally {
            // Keep TenantContext clean
        }

        return AuthResponse.of(accessToken, refreshToken, accessTokenExpirationMs, UserDto.fromEntity(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (!jwtService.isTokenValid(request.refreshToken())) {
            throw ApiException.unauthorized("Refresh token is invalid or expired.");
        }

        String tokenType = jwtService.extractTokenType(request.refreshToken());
        if (!"refresh".equals(tokenType)) {
            throw ApiException.unauthorized("Provided token is not a valid refresh token.");
        }

        String userId = jwtService.extractUserId(request.refreshToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("User account no longer exists."));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw ApiException.forbidden("Account is inactive.");
        }

        String newAccessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getOrganizationId(),
                user.getEmail(),
                user.getRole().name()
        );

        return AuthResponse.of(newAccessToken, request.refreshToken(), accessTokenExpirationMs, UserDto.fromEntity(user));
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found."));
        return UserDto.fromEntity(user);
    }

    @Transactional
    public void seedDemoOrganization() {
        if (organizationRepository.findByName("Grand Palace Resorts").isEmpty()) {
            Organization org = new Organization(
                    "org-demo-001",
                    "Grand Palace Resorts",
                    "ENTERPRISE",
                    "ACTIVE"
            );
            organizationRepository.save(org);

            User admin = new User(
                    org.getId(),
                    "admin@grandpalace.com",
                    passwordEncoder.encode("Password123!"),
                    "Alex",
                    "Sterling",
                    Role.ORG_ADMIN
            );
            admin.setId("user-admin-001");
            userRepository.save(admin);

            User revenueManager = new User(
                    org.getId(),
                    "revenue@grandpalace.com",
                    passwordEncoder.encode("Password123!"),
                    "Maya",
                    "Patel",
                    Role.REVENUE_MANAGER
            );
            revenueManager.setId("user-rev-001");
            userRepository.save(revenueManager);

            User frontDesk = new User(
                    org.getId(),
                    "desk@grandpalace.com",
                    passwordEncoder.encode("Password123!"),
                    "Sam",
                    "Taylor",
                    Role.RESERVATIONS_STAFF
            );
            frontDesk.setId("user-desk-001");
            userRepository.save(frontDesk);
        }
    }
}

