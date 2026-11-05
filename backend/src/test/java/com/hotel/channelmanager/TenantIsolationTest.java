package com.hotel.channelmanager;

import com.hotel.channelmanager.common.tenant.TenantContext;
import com.hotel.channelmanager.identity.domain.Organization;
import com.hotel.channelmanager.identity.domain.OrganizationRepository;
import com.hotel.channelmanager.identity.domain.Role;
import com.hotel.channelmanager.identity.domain.User;
import com.hotel.channelmanager.identity.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class TenantIsolationTest {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Organization tenantA;
    private Organization tenantB;
    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        tenantA = organizationRepository.save(new Organization("org-a", "Hotel Grandeur (Tenant A)", "PRO", "ACTIVE"));
        tenantB = organizationRepository.save(new Organization("org-b", "Seaside Villas (Tenant B)", "PRO", "ACTIVE"));

        userA = userRepository.save(new User(
                tenantA.getId(),
                "admin@tenanta.com",
                passwordEncoder.encode("Password123!"),
                "Alice",
                "Grandeur",
                Role.ORG_ADMIN
        ));

        userB = userRepository.save(new User(
                tenantB.getId(),
                "admin@tenantb.com",
                passwordEncoder.encode("Password123!"),
                "Bob",
                "Seaside",
                Role.ORG_ADMIN
        ));
    }

    @Test
    @DisplayName("Tenant A queries must NEVER return Tenant B's users or records")
    void testTenantScopedQueriesCannotLeakData() {
        TenantContext.setOrganizationId(tenantA.getId());
        try {
            List<User> tenantAUsers = userRepository.findAllByOrganizationId(tenantA.getId());
            assertEquals(1, tenantAUsers.size());
            assertEquals("admin@tenanta.com", tenantAUsers.get(0).getEmail());

            // Attempting to look up userB within tenant A's scope must return empty
            Optional<User> crossTenantUser = userRepository.findByIdAndOrganizationId(userB.getId(), tenantA.getId());
            assertTrue(crossTenantUser.isEmpty(), "Cross-tenant record retrieval should be empty");

            // Look up userA within tenant A's scope succeeds
            Optional<User> ownTenantUser = userRepository.findByIdAndOrganizationId(userA.getId(), tenantA.getId());
            assertTrue(ownTenantUser.isPresent());
            assertEquals("admin@tenanta.com", ownTenantUser.get().getEmail());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("Tenant B queries must only return Tenant B's records")
    void testTenantBIsolation() {
        TenantContext.setOrganizationId(tenantB.getId());
        try {
            List<User> tenantBUsers = userRepository.findAllByOrganizationId(tenantB.getId());
            assertEquals(1, tenantBUsers.size());
            assertEquals("admin@tenantb.com", tenantBUsers.get(0).getEmail());

            // Attempting to look up userA within tenant B's scope must return empty
            Optional<User> crossTenantUser = userRepository.findByIdAndOrganizationId(userA.getId(), tenantB.getId());
            assertTrue(crossTenantUser.isEmpty(), "Cross-tenant record retrieval must be denied");
        } finally {
            TenantContext.clear();
        }
    }
}

