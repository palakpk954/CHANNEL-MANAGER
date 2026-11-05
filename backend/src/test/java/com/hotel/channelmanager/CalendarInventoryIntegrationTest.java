package com.hotel.channelmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.channelmanager.common.security.JwtService;
import com.hotel.channelmanager.identity.domain.Organization;
import com.hotel.channelmanager.identity.domain.OrganizationRepository;
import com.hotel.channelmanager.identity.domain.Role;
import com.hotel.channelmanager.identity.domain.User;
import com.hotel.channelmanager.identity.domain.UserRepository;
import com.hotel.channelmanager.inventory.domain.BulkUpdateHistoryRepository;
import com.hotel.channelmanager.inventory.domain.DailyInventoryRepository;
import com.hotel.channelmanager.inventory.domain.DailyRateRepository;
import com.hotel.channelmanager.inventory.domain.ManualBlockRepository;
import com.hotel.channelmanager.inventory.web.dto.BulkUpdateRequest;
import com.hotel.channelmanager.inventory.web.dto.ManualBlockRequest;
import com.hotel.channelmanager.inventory.web.dto.UpdateCalendarDayRequest;
import com.hotel.channelmanager.property.domain.Property;
import com.hotel.channelmanager.property.domain.PropertyRepository;
import com.hotel.channelmanager.property.domain.RatePlan;
import com.hotel.channelmanager.property.domain.RatePlanRepository;
import com.hotel.channelmanager.property.domain.RoomType;
import com.hotel.channelmanager.property.domain.RoomTypeRepository;
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

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CalendarInventoryIntegrationTest {

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
    private DailyInventoryRepository inventoryRepository;

    @Autowired
    private DailyRateRepository rateRepository;

    @Autowired
    private ManualBlockRepository manualBlockRepository;

    @Autowired
    private BulkUpdateHistoryRepository bulkUpdateHistoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Property testProperty;
    private RoomType testRoom;
    private RatePlan testRatePlan;
    private String adminToken;

    @BeforeEach
    void setUp() {
        bulkUpdateHistoryRepository.deleteAll();
        manualBlockRepository.deleteAll();
        rateRepository.deleteAll();
        inventoryRepository.deleteAll();
        ratePlanRepository.deleteAll();
        roomTypeRepository.deleteAll();
        propertyRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        Organization org = organizationRepository.save(new Organization("org-cal", "Hotel Rajmahal", "PRO", "ACTIVE"));

        User admin = userRepository.save(new User(
                org.getId(),
                "admin@rajmahal.com",
                passwordEncoder.encode("Password123!"),
                "Priya",
                "Sharma",
                Role.ORG_ADMIN
        ));

        adminToken = jwtService.generateAccessToken(admin.getId(), org.getId(), admin.getEmail(), "ORG_ADMIN");

        testProperty = propertyRepository.save(new Property(
                org.getId(),
                "Hotel Rajmahal",
                "Palace Road, Jaipur",
                "Asia/Kolkata",
                "INR"
        ));

        testRoom = roomTypeRepository.save(new RoomType(
                org.getId(),
                testProperty.getId(),
                "DLX",
                "Deluxe Room",
                2,
                5,
                "King Bed",
                "WiFi, AC"
        ));

        testRatePlan = ratePlanRepository.save(new RatePlan(
                org.getId(),
                testProperty.getId(),
                testRoom.getId(),
                "BAR",
                "Room Only",
                "ROOM_ONLY",
                "FREE_24H"
        ));
    }

    @Test
    @DisplayName("US-3.1: Calendar grid returns rooms, rate plans, and date matrix")
    void testGetCalendarGrid() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(7);

        mockMvc.perform(get("/api/v1/properties/" + testProperty.getId() + "/calendar")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("from", today.toString())
                        .param("to", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propertyName").value("Hotel Rajmahal"))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.rooms[0].code").value("DLX"))
                .andExpect(jsonPath("$.ratePlans[0].code").value("BAR"));
    }

    @Test
    @DisplayName("US-3.2: Update single day price and stop-sell (close room for sale)")
    void testSingleDayUpdate() throws Exception {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        UpdateCalendarDayRequest req = new UpdateCalendarDayRequest(
                tomorrow,
                testRoom.getId(),
                4,
                testRatePlan.getId(),
                520000L, // ₹5,200.00
                true,   // Stop-sell / Close for sale
                2,      // Min stay
                null,
                false,
                false
        );

        mockMvc.perform(put("/api/v1/properties/" + testProperty.getId() + "/calendar/day")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Day updated successfully."));
    }

    @Test
    @DisplayName("BR-1.8: Changes to past dates are refused")
    void testPastDateRejected() throws Exception {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        UpdateCalendarDayRequest req = new UpdateCalendarDayRequest(
                yesterday,
                testRoom.getId(),
                2,
                testRatePlan.getId(),
                400000L,
                false, null, null, null, null
        );

        mockMvc.perform(put("/api/v1/properties/" + testProperty.getId() + "/calendar/day")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only today and future dates can be changed."));
    }

    @Test
    @DisplayName("T6 / BR-2.12 / BR-7.7: Apply bulk update and undo within 24 hours")
    void testBulkUpdateAndUndo() throws Exception {
        LocalDate start = LocalDate.now().plusDays(2);
        LocalDate end = LocalDate.now().plusDays(8);

        // 1. Initial bulk update
        BulkUpdateRequest bulkReq = new BulkUpdateRequest(
                start,
                end,
                List.of(1, 2, 3, 4, 5, 6, 7),
                List.of(testRoom.getId()),
                List.of(testRatePlan.getId()),
                3,
                "SET_EXACT",
                4800.0,
                true, // confirmed
                1,
                null,
                false,
                false,
                false
        );

        MvcResult bulkResult = mockMvc.perform(post("/api/v1/properties/" + testProperty.getId() + "/calendar/bulk")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historyId").isNotEmpty())
                .andReturn();

        String historyId = objectMapper.readTree(bulkResult.getResponse().getContentAsString()).get("historyId").asText();

        // 2. Undo bulk update
        mockMvc.perform(post("/api/v1/properties/" + testProperty.getId() + "/calendar/undo/" + historyId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Your last change has been undone."));
    }

    @Test
    @DisplayName("US-3.5 & BR-1.4: Hold rooms (manual block) reduces availability across dates")
    void testManualBlock() throws Exception {
        LocalDate start = LocalDate.now().plusDays(3);
        LocalDate end = LocalDate.now().plusDays(5);

        ManualBlockRequest blockReq = new ManualBlockRequest(
                testRoom.getId(),
                start,
                end,
                2,
                "Plumbing Maintenance"
        );

        mockMvc.perform(post("/api/v1/properties/" + testProperty.getId() + "/calendar/blocks")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}

