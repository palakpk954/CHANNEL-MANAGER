package com.hotel.channelmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.channelmanager.channels.connector.ChannelCredentials;
import com.hotel.channelmanager.channels.domain.ChannelAccount;
import com.hotel.channelmanager.channels.domain.ChannelAccountRepository;
import com.hotel.channelmanager.channels.domain.ChannelType;
import com.hotel.channelmanager.channels.web.dto.CreateChannelAccountRequest;
import com.hotel.channelmanager.common.security.JwtService;
import com.hotel.channelmanager.identity.domain.Organization;
import com.hotel.channelmanager.identity.domain.OrganizationRepository;
import com.hotel.channelmanager.identity.domain.Role;
import com.hotel.channelmanager.identity.domain.User;
import com.hotel.channelmanager.identity.domain.UserRepository;
import com.hotel.channelmanager.inventory.domain.DailyInventory;
import com.hotel.channelmanager.inventory.domain.DailyInventoryRepository;
import com.hotel.channelmanager.inventory.web.dto.BulkUpdateRequest;
import com.hotel.channelmanager.inventory.web.dto.ManualBlockRequest;
import com.hotel.channelmanager.inventory.web.dto.UpdateCalendarDayRequest;
import com.hotel.channelmanager.property.domain.Property;
import com.hotel.channelmanager.property.domain.PropertyRepository;
import com.hotel.channelmanager.property.domain.RatePlan;
import com.hotel.channelmanager.property.domain.RatePlanRepository;
import com.hotel.channelmanager.property.domain.RoomType;
import com.hotel.channelmanager.property.domain.RoomTypeRepository;
import com.hotel.channelmanager.property.web.dto.CreatePropertyRequest;
import com.hotel.channelmanager.property.web.dto.CreateRatePlanRequest;
import com.hotel.channelmanager.property.web.dto.CreateRoomTypeRequest;
import com.hotel.channelmanager.reservations.domain.Reservation;
import com.hotel.channelmanager.reservations.domain.ReservationRepository;
import com.hotel.channelmanager.reservations.domain.ReservationStatus;
import com.hotel.channelmanager.reservations.web.dto.ManualReservationRequest;
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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserRoleMatrixIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RatePlanRepository ratePlanRepository;

    @Autowired
    private ChannelAccountRepository channelAccountRepository;

    @Autowired
    private DailyInventoryRepository inventoryRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Organization org;
    private Property property;
    private RoomType standardRoom;
    private RatePlan standardBar;

    private final Map<Role, String> tokenMap = new HashMap<>();

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        inventoryRepository.deleteAll();
        channelAccountRepository.deleteAll();
        ratePlanRepository.deleteAll();
        roomTypeRepository.deleteAll();
        propertyRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        org = organizationRepository.save(new Organization("org-rbac-test", "RBAC Test Org", "ENTERPRISE", "ACTIVE"));

        property = propertyRepository.save(new Property(
                org.getId(),
                "Grand Test Hotel",
                "100 Ocean Drive",
                "Asia/Kolkata",
                "INR"
        ));

        standardRoom = roomTypeRepository.save(new RoomType(
                org.getId(),
                property.getId(),
                "STD-01",
                "Standard Room",
                2,
                10,
                "Queen",
                "WiFi, AC"
        ));

        standardBar = ratePlanRepository.save(new RatePlan(
                org.getId(),
                property.getId(),
                standardRoom.getId(),
                "BAR-01",
                "Best Available Rate",
                "ROOM_ONLY",
                "FREE_CANCELLATION"
        ));

        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            inventoryRepository.save(new DailyInventory(
                    org.getId(),
                    property.getId(),
                    standardRoom.getId(),
                    today.plusDays(i),
                    10
            ));
        }

        // Create a user and token for each role
        for (Role role : Role.values()) {
            User user = userRepository.save(new User(
                    org.getId(),
                    role.name().toLowerCase() + "@test.com",
                    passwordEncoder.encode("Password123!"),
                    "Test",
                    role.name(),
                    role
            ));
            String token = jwtService.generateAccessToken(user.getId(), org.getId(), user.getEmail(), role.name());
            tokenMap.put(role, token);
        }
    }

    private String token(Role role) {
        return "Bearer " + tokenMap.get(role);
    }

    // =========================================================================
    // 1. PROPERTY MANAGEMENT FEATURE
    // Allowed: SUPER_ADMIN, ORG_ADMIN
    // Denied: PROPERTY_MANAGER, REVENUE_MANAGER, RESERVATIONS_STAFF, READ_ONLY
    // =========================================================================
    @Test
    @DisplayName("RBAC: Property creation permissions across all roles")
    void testPropertyCreationAcrossRoles() throws Exception {
        CreatePropertyRequest req1 = new CreatePropertyRequest(
                "Super Admin Palace Resort",
                "200 Hill Road",
                "Asia/Kolkata",
                "INR",
                "14:00",
                "11:00",
                "info1@newpalace.com",
                "+91 9999999991"
        );
        CreatePropertyRequest req2 = new CreatePropertyRequest(
                "Org Admin Palace Resort",
                "201 Hill Road",
                "Asia/Kolkata",
                "INR",
                "14:00",
                "11:00",
                "info2@newpalace.com",
                "+91 9999999992"
        );
        CreatePropertyRequest reqDenied = new CreatePropertyRequest(
                "Denied Palace Resort",
                "202 Hill Road",
                "Asia/Kolkata",
                "INR",
                "14:00",
                "11:00",
                "info3@newpalace.com",
                "+91 9999999993"
        );

        // Allowed roles -> 201 Created
        mockMvc.perform(post("/api/v1/properties")
                        .header("Authorization", token(Role.SUPER_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/properties")
                        .header("Authorization", token(Role.ORG_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated());

        // Denied roles -> 403 Forbidden
        String deniedBody = objectMapper.writeValueAsString(reqDenied);
        mockMvc.perform(post("/api/v1/properties")
                        .header("Authorization", token(Role.PROPERTY_MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deniedBody))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/properties")
                        .header("Authorization", token(Role.REVENUE_MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deniedBody))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/properties")
                        .header("Authorization", token(Role.RESERVATIONS_STAFF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deniedBody))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/properties")
                        .header("Authorization", token(Role.READ_ONLY))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deniedBody))
                .andExpect(status().isForbidden());

        // All roles can read properties -> 200 OK
        for (Role role : Role.values()) {
            mockMvc.perform(get("/api/v1/properties")
                            .header("Authorization", token(role)))
                    .andExpect(status().isOk());
        }
    }

    // =========================================================================
    // 2. ROOM TYPE MANAGEMENT FEATURE
    // Allowed: SUPER_ADMIN, ORG_ADMIN, PROPERTY_MANAGER
    // Denied: REVENUE_MANAGER, RESERVATIONS_STAFF, READ_ONLY
    // =========================================================================
    @Test
    @DisplayName("RBAC: Room type configuration permissions across all roles")
    void testRoomTypeManagementAcrossRoles() throws Exception {
        CreateRoomTypeRequest req1 = new CreateRoomTypeRequest(
                "DLX-SA",
                "Deluxe Suite SA",
                3,
                5,
                "King Bed",
                "Balcony, Sea View"
        );
        CreateRoomTypeRequest req2 = new CreateRoomTypeRequest(
                "DLX-OA",
                "Deluxe Suite OA",
                3,
                5,
                "King Bed",
                "Balcony, Sea View"
        );
        CreateRoomTypeRequest req3 = new CreateRoomTypeRequest(
                "DLX-PM",
                "Deluxe Suite PM",
                3,
                5,
                "King Bed",
                "Balcony, Sea View"
        );
        CreateRoomTypeRequest reqDenied = new CreateRoomTypeRequest(
                "DLX-DENIED",
                "Deluxe Suite Denied",
                3,
                5,
                "King Bed",
                "Balcony, Sea View"
        );

        // Allowed roles -> 201 Created
        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/room-types")
                        .header("Authorization", token(Role.SUPER_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/room-types")
                        .header("Authorization", token(Role.ORG_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/room-types")
                        .header("Authorization", token(Role.PROPERTY_MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req3)))
                .andExpect(status().isCreated());

        // Denied roles -> 403 Forbidden
        String deniedBody = objectMapper.writeValueAsString(reqDenied);
        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/room-types")
                        .header("Authorization", token(Role.REVENUE_MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deniedBody))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/room-types")
                        .header("Authorization", token(Role.RESERVATIONS_STAFF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deniedBody))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/room-types")
                        .header("Authorization", token(Role.READ_ONLY))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deniedBody))
                .andExpect(status().isForbidden());

        // All roles can read room types -> 200 OK
        for (Role role : Role.values()) {
            mockMvc.perform(get("/api/v1/properties/" + property.getId() + "/room-types")
                            .header("Authorization", token(role)))
                    .andExpect(status().isOk());
        }
    }

    // =========================================================================
    // 3. RATE PLAN MANAGEMENT FEATURE
    // Allowed: SUPER_ADMIN, ORG_ADMIN, PROPERTY_MANAGER, REVENUE_MANAGER
    // Denied: RESERVATIONS_STAFF, READ_ONLY
    // =========================================================================
    @Test
    @DisplayName("RBAC: Rate plan configuration permissions across all roles")
    void testRatePlanManagementAcrossRoles() throws Exception {
        CreateRatePlanRequest req1 = new CreateRatePlanRequest(
                "BB-SA",
                "Bed & Breakfast SA",
                standardRoom.getId(),
                "BREAKFAST",
                "FREE_CANCELLATION",
                null,
                null,
                null
        );
        CreateRatePlanRequest req2 = new CreateRatePlanRequest(
                "BB-OA",
                "Bed & Breakfast OA",
                standardRoom.getId(),
                "BREAKFAST",
                "FREE_CANCELLATION",
                null,
                null,
                null
        );
        CreateRatePlanRequest req3 = new CreateRatePlanRequest(
                "BB-PM",
                "Bed & Breakfast PM",
                standardRoom.getId(),
                "BREAKFAST",
                "FREE_CANCELLATION",
                null,
                null,
                null
        );
        CreateRatePlanRequest req4 = new CreateRatePlanRequest(
                "BB-RM",
                "Bed & Breakfast RM",
                standardRoom.getId(),
                "BREAKFAST",
                "FREE_CANCELLATION",
                null,
                null,
                null
        );
        CreateRatePlanRequest reqDenied = new CreateRatePlanRequest(
                "BB-DENIED",
                "Bed & Breakfast Denied",
                standardRoom.getId(),
                "BREAKFAST",
                "FREE_CANCELLATION",
                null,
                null,
                null
        );

        // Allowed roles -> 201 Created
        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/rate-plans")
                        .header("Authorization", token(Role.SUPER_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/rate-plans")
                        .header("Authorization", token(Role.ORG_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/rate-plans")
                        .header("Authorization", token(Role.PROPERTY_MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req3)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/rate-plans")
                        .header("Authorization", token(Role.REVENUE_MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req4)))
                .andExpect(status().isCreated());

        // Denied roles -> 403 Forbidden
        String deniedBody = objectMapper.writeValueAsString(reqDenied);
        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/rate-plans")
                        .header("Authorization", token(Role.RESERVATIONS_STAFF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deniedBody))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/rate-plans")
                        .header("Authorization", token(Role.READ_ONLY))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deniedBody))
                .andExpect(status().isForbidden());

        // All roles can read rate plans -> 200 OK
        for (Role role : Role.values()) {
            mockMvc.perform(get("/api/v1/properties/" + property.getId() + "/rate-plans")
                            .header("Authorization", token(role)))
                    .andExpect(status().isOk());
        }
    }

    // =========================================================================
    // 4. CALENDAR & DAILY RATES/RESTRICTIONS FEATURE
    // Allowed: SUPER_ADMIN, ORG_ADMIN, PROPERTY_MANAGER, REVENUE_MANAGER
    // Denied: RESERVATIONS_STAFF, READ_ONLY
    // =========================================================================
    @Test
    @DisplayName("RBAC: Daily rate and restriction updates across all roles")
    void testCalendarRateUpdatesAcrossRoles() throws Exception {
        UpdateCalendarDayRequest req = new UpdateCalendarDayRequest(
                LocalDate.now(),
                standardRoom.getId(),
                8,
                standardBar.getId(),
                1250000L,
                false,
                2,
                7,
                false,
                false
        );
        String body = objectMapper.writeValueAsString(req);

        // Allowed roles -> 200 OK
        mockMvc.perform(put("/api/v1/properties/" + property.getId() + "/calendar/day")
                        .header("Authorization", token(Role.SUPER_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/properties/" + property.getId() + "/calendar/day")
                        .header("Authorization", token(Role.ORG_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/properties/" + property.getId() + "/calendar/day")
                        .header("Authorization", token(Role.PROPERTY_MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/properties/" + property.getId() + "/calendar/day")
                        .header("Authorization", token(Role.REVENUE_MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Denied roles -> 403 Forbidden
        mockMvc.perform(put("/api/v1/properties/" + property.getId() + "/calendar/day")
                        .header("Authorization", token(Role.RESERVATIONS_STAFF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/properties/" + property.getId() + "/calendar/day")
                        .header("Authorization", token(Role.READ_ONLY))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        // All roles can view the calendar -> 200 OK
        for (Role role : Role.values()) {
            mockMvc.perform(get("/api/v1/properties/" + property.getId() + "/calendar")
                            .header("Authorization", token(role)))
                    .andExpect(status().isOk());
        }
    }

    // =========================================================================
    // 5. BULK CALENDAR UPDATE FEATURE
    // Allowed: SUPER_ADMIN, ORG_ADMIN, PROPERTY_MANAGER, REVENUE_MANAGER
    // Denied: RESERVATIONS_STAFF, READ_ONLY
    // =========================================================================
    @Test
    @DisplayName("RBAC: Bulk calendar updates across all roles")
    void testBulkUpdateAcrossRoles() throws Exception {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(3);
        BulkUpdateRequest req = new BulkUpdateRequest(
                start,
                end,
                List.of(1, 2, 3, 4, 5, 6, 7),
                List.of(standardRoom.getId()),
                List.of(standardBar.getId()),
                null,
                "SET_EXACT",
                15000.0,
                true,
                null,
                null,
                null,
                null,
                null
        );
        String body = objectMapper.writeValueAsString(req);

        // Allowed roles -> 200 OK
        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/calendar/bulk")
                        .header("Authorization", token(Role.SUPER_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/calendar/bulk")
                        .header("Authorization", token(Role.ORG_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/calendar/bulk")
                        .header("Authorization", token(Role.PROPERTY_MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/calendar/bulk")
                        .header("Authorization", token(Role.REVENUE_MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Denied roles -> 403 Forbidden
        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/calendar/bulk")
                        .header("Authorization", token(Role.RESERVATIONS_STAFF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/calendar/bulk")
                        .header("Authorization", token(Role.READ_ONLY))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 6. MANUAL BLOCKS FEATURE (Maintenance / Owner use)
    // Allowed: SUPER_ADMIN, ORG_ADMIN, PROPERTY_MANAGER, REVENUE_MANAGER, RESERVATIONS_STAFF
    // Denied: READ_ONLY
    // =========================================================================
    @Test
    @DisplayName("RBAC: Manual room blocks permissions across all roles")
    void testManualBlocksAcrossRoles() throws Exception {
        ManualBlockRequest req = new ManualBlockRequest(
                standardRoom.getId(),
                LocalDate.now(),
                LocalDate.now().plusDays(2),
                1,
                "AC Maintenance"
        );
        String body = objectMapper.writeValueAsString(req);

        // Allowed roles -> 200 OK
        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/calendar/blocks")
                        .header("Authorization", token(Role.SUPER_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/calendar/blocks")
                        .header("Authorization", token(Role.ORG_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/calendar/blocks")
                        .header("Authorization", token(Role.PROPERTY_MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/calendar/blocks")
                        .header("Authorization", token(Role.REVENUE_MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/calendar/blocks")
                        .header("Authorization", token(Role.RESERVATIONS_STAFF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Denied role -> 403 Forbidden
        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/calendar/blocks")
                        .header("Authorization", token(Role.READ_ONLY))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 7. OTA CHANNELS MANAGEMENT FEATURE
    // Connect/Update: SUPER_ADMIN, ORG_ADMIN, PROPERTY_MANAGER
    // Delete: SUPER_ADMIN, ORG_ADMIN only
    // Denied to connect: REVENUE_MANAGER, RESERVATIONS_STAFF, READ_ONLY
    // =========================================================================
    @Test
    @DisplayName("RBAC: OTA Channel configuration and delete across all roles")
    void testChannelManagementAcrossRoles() throws Exception {
        ChannelCredentials creds1 = new ChannelCredentials();
        creds1.setHotelId("hotel-101");
        creds1.setApiKey("booking-api-key-1");
        creds1.setApiSecret("booking-api-secret-1");
        CreateChannelAccountRequest req1 = new CreateChannelAccountRequest(
                ChannelType.BOOKING_COM,
                "Booking.com Production SA",
                creds1
        );

        ChannelCredentials creds2 = new ChannelCredentials();
        creds2.setHotelId("hotel-102");
        creds2.setApiKey("booking-api-key-2");
        creds2.setApiSecret("booking-api-secret-2");
        CreateChannelAccountRequest req2 = new CreateChannelAccountRequest(
                ChannelType.BOOKING_COM,
                "Booking.com Production OA",
                creds2
        );

        ChannelCredentials creds3 = new ChannelCredentials();
        creds3.setHotelId("hotel-103");
        creds3.setApiKey("booking-api-key-3");
        creds3.setApiSecret("booking-api-secret-3");
        CreateChannelAccountRequest req3 = new CreateChannelAccountRequest(
                ChannelType.BOOKING_COM,
                "Booking.com Production PM",
                creds3
        );

        ChannelCredentials credsDenied = new ChannelCredentials();
        credsDenied.setHotelId("hotel-104");
        credsDenied.setApiKey("booking-api-key-4");
        credsDenied.setApiSecret("booking-api-secret-4");
        CreateChannelAccountRequest reqDenied = new CreateChannelAccountRequest(
                ChannelType.BOOKING_COM,
                "Booking.com Production Denied",
                credsDenied
        );

        // Allowed roles to create channel -> 201 Created
        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/channels")
                        .header("Authorization", token(Role.SUPER_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/channels")
                        .header("Authorization", token(Role.ORG_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/channels")
                        .header("Authorization", token(Role.PROPERTY_MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req3)))
                .andExpect(status().isCreated());

        // Denied roles -> 403 Forbidden
        String deniedBody = objectMapper.writeValueAsString(reqDenied);
        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/channels")
                        .header("Authorization", token(Role.REVENUE_MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deniedBody))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/channels")
                        .header("Authorization", token(Role.RESERVATIONS_STAFF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deniedBody))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/channels")
                        .header("Authorization", token(Role.READ_ONLY))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deniedBody))
                .andExpect(status().isForbidden());

        // Channel Hard Delete: Only SUPER_ADMIN and ORG_ADMIN are allowed
        ChannelAccount dummyAccount = channelAccountRepository.save(new ChannelAccount(
                org.getId(),
                property.getId(),
                ChannelType.AGODA,
                "Agoda Account",
                "{\"apiKey\":\"dummy\"}"
        ));

        // PROPERTY_MANAGER cannot delete channel -> 403 Forbidden
        mockMvc.perform(delete("/api/v1/properties/" + property.getId() + "/channels/" + dummyAccount.getId())
                        .header("Authorization", token(Role.PROPERTY_MANAGER)))
                .andExpect(status().isForbidden());

        // ORG_ADMIN can delete channel -> 200 OK
        mockMvc.perform(delete("/api/v1/properties/" + property.getId() + "/channels/" + dummyAccount.getId())
                        .header("Authorization", token(Role.ORG_ADMIN)))
                .andExpect(status().isOk());

        // All roles can read channels list -> 200 OK
        for (Role role : Role.values()) {
            mockMvc.perform(get("/api/v1/properties/" + property.getId() + "/channels")
                            .header("Authorization", token(role)))
                    .andExpect(status().isOk());
        }
    }

    // =========================================================================
    // 8. RESERVATIONS & BOOKINGS FEATURE
    // Manual Reservation: SUPER_ADMIN, ORG_ADMIN, PROPERTY_MANAGER, REVENUE_MANAGER, RESERVATIONS_STAFF
    // Cancel Reservation: SUPER_ADMIN, ORG_ADMIN, PROPERTY_MANAGER, RESERVATIONS_STAFF
    // Denied Cancel: REVENUE_MANAGER (403), READ_ONLY (403)
    // =========================================================================
    @Test
    @DisplayName("RBAC: Reservation creation and cancellation permissions across all roles")
    void testReservationPermissionsAcrossRoles() throws Exception {
        ManualReservationRequest req = new ManualReservationRequest(
                "Jane Doe",
                "jane@example.com",
                "+1 555-0199",
                standardRoom.getId(),
                standardBar.getId(),
                LocalDate.now(),
                LocalDate.now().plusDays(2),
                1,
                2500000L,
                "INR",
                "Late check-in requested"
        );
        String body = objectMapper.writeValueAsString(req);

        // Allowed roles to create manual reservation -> 201 Created
        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/reservations/manual")
                        .header("Authorization", token(Role.SUPER_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/reservations/manual")
                        .header("Authorization", token(Role.ORG_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/reservations/manual")
                        .header("Authorization", token(Role.PROPERTY_MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/reservations/manual")
                        .header("Authorization", token(Role.REVENUE_MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/reservations/manual")
                        .header("Authorization", token(Role.RESERVATIONS_STAFF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // Denied role -> 403 Forbidden
        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/reservations/manual")
                        .header("Authorization", token(Role.READ_ONLY))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        // Test Reservation Cancellation
        Reservation res = reservationRepository.save(new Reservation(
                org.getId(),
                property.getId(),
                null,
                "MANUAL",
                "BK-RBAC-CANCEL",
                ReservationStatus.CONFIRMED,
                "Bob Smith",
                "bob@example.com",
                null,
                standardRoom.getId(),
                standardBar.getId(),
                LocalDate.now(),
                LocalDate.now().plusDays(2),
                1,
                1500000L,
                "INR",
                "None"
        ));

        // REVENUE_MANAGER CANNOT cancel reservations -> 403 Forbidden
        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/reservations/" + res.getId() + "/cancel")
                        .header("Authorization", token(Role.REVENUE_MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Cancel test\"}"))
                .andExpect(status().isForbidden());

        // READ_ONLY CANNOT cancel reservations -> 403 Forbidden
        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/reservations/" + res.getId() + "/cancel")
                        .header("Authorization", token(Role.READ_ONLY))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Cancel test\"}"))
                .andExpect(status().isForbidden());

        // RESERVATIONS_STAFF CAN cancel reservations -> 200 OK
        mockMvc.perform(post("/api/v1/properties/" + property.getId() + "/reservations/" + res.getId() + "/cancel")
                        .header("Authorization", token(Role.RESERVATIONS_STAFF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Guest requested cancellation\"}"))
                .andExpect(status().isOk());

        // All roles can view reservations list -> 200 OK
        for (Role role : Role.values()) {
            mockMvc.perform(get("/api/v1/properties/" + property.getId() + "/reservations")
                            .header("Authorization", token(role)))
                    .andExpect(status().isOk());
        }
    }

    // =========================================================================
    // 9. DASHBOARD, ACTIVITY & NOTIFICATIONS READ ACCESS
    // All authenticated roles should be able to view dashboards, activity feeds,
    // and notifications.
    // =========================================================================
    @Test
    @DisplayName("RBAC: Dashboard, Activity and Notifications accessible to all roles")
    void testDashboardAndAuditAccessAcrossRoles() throws Exception {
        for (Role role : Role.values()) {
            mockMvc.perform(get("/api/v1/properties/" + property.getId() + "/dashboard")
                            .header("Authorization", token(role)))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/audit")
                            .header("Authorization", token(role)))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/properties/" + property.getId() + "/notifications")
                            .header("Authorization", token(role)))
                    .andExpect(status().isOk());
        }
    }
}

