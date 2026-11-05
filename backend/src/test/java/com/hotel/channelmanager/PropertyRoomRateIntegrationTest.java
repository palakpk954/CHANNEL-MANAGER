package com.hotel.channelmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.channelmanager.common.security.JwtService;
import com.hotel.channelmanager.identity.domain.Organization;
import com.hotel.channelmanager.identity.domain.OrganizationRepository;
import com.hotel.channelmanager.identity.domain.Role;
import com.hotel.channelmanager.identity.domain.User;
import com.hotel.channelmanager.identity.domain.UserRepository;
import com.hotel.channelmanager.property.domain.PropertyRepository;
import com.hotel.channelmanager.property.domain.RatePlanRepository;
import com.hotel.channelmanager.property.domain.RoomTypeRepository;
import com.hotel.channelmanager.property.web.dto.CreatePropertyRequest;
import com.hotel.channelmanager.property.web.dto.CreateRatePlanRequest;
import com.hotel.channelmanager.property.web.dto.CreateRoomTypeRequest;
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

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PropertyRoomRateIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Organization orgA;
    private Organization orgB;
    private String adminTokenA;
    private String staffTokenA;

    @BeforeEach
    void setUp() {
        ratePlanRepository.deleteAll();
        roomTypeRepository.deleteAll();
        propertyRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        orgA = organizationRepository.save(new Organization("org-a", "Hotel Heritage (Tenant A)", "PRO", "ACTIVE"));
        orgB = organizationRepository.save(new Organization("org-b", "Beach Resort (Tenant B)", "PRO", "ACTIVE"));

        User adminA = userRepository.save(new User(
                orgA.getId(),
                "admin@heritagetest.com",
                passwordEncoder.encode("Password123!"),
                "Admin",
                "User",
                Role.ORG_ADMIN
        ));

        User staffA = userRepository.save(new User(
                orgA.getId(),
                "staff@heritagetest.com",
                passwordEncoder.encode("Password123!"),
                "Staff",
                "User",
                Role.RESERVATIONS_STAFF
        ));

        adminTokenA = jwtService.generateAccessToken(adminA.getId(), orgA.getId(), adminA.getEmail(), "ORG_ADMIN");
        staffTokenA = jwtService.generateAccessToken(staffA.getId(), orgA.getId(), staffA.getEmail(), "RESERVATIONS_STAFF");
    }

    @Test
    @DisplayName("US-2.3: Admin can create property with required fields and timezone")
    void testCreateProperty() throws Exception {
        CreatePropertyRequest request = new CreatePropertyRequest(
                "Heritage Palace",
                "100 Palace Road, Jaipur, Rajasthan",
                "Asia/Kolkata",
                "INR",
                "14:00",
                "11:00",
                "contact@heritagepalace.com",
                "+91 141 223344"
        );

        mockMvc.perform(post("/api/v1/properties")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Heritage Palace"))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.timezone").value("Asia/Kolkata"));
    }

    @Test
    @DisplayName("T9 & BR-7.5: Reservations staff cannot create properties or rate plans (403 Forbidden)")
    void testStaffRoleAccessDenied() throws Exception {
        CreatePropertyRequest request = new CreatePropertyRequest(
                "Unauthorized Hotel",
                "Some Address",
                "Asia/Kolkata",
                "INR",
                null, null, null, null
        );

        mockMvc.perform(post("/api/v1/properties")
                        .header("Authorization", "Bearer " + staffTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("US-2.4: Create room types and rate plans, then reject duplicate codes")
    void testCreateRoomAndRatePlan() throws Exception {
        // 1. Create Property
        CreatePropertyRequest propReq = new CreatePropertyRequest(
                "Heritage Palace",
                "100 Palace Road, Jaipur",
                "Asia/Kolkata",
                "INR",
                null, null, null, null
        );
        MvcResult propResult = mockMvc.perform(post("/api/v1/properties")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(propReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String propId = objectMapper.readTree(propResult.getResponse().getContentAsString()).get("id").asText();

        // 2. Create Room Type
        CreateRoomTypeRequest roomReq = new CreateRoomTypeRequest(
                "DELUXE",
                "Deluxe King Room",
                3,
                10,
                "King Bed",
                "WiFi, Air Conditioning, Safe"
        );
        MvcResult roomResult = mockMvc.perform(post("/api/v1/properties/" + propId + "/room-types")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("DELUXE"))
                .andExpect(jsonPath("$.totalUnits").value(10))
                .andReturn();

        String roomId = objectMapper.readTree(roomResult.getResponse().getContentAsString()).get("id").asText();

        // 3. Reject duplicate room code
        mockMvc.perform(post("/api/v1/properties/" + propId + "/room-types")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomReq)))
                .andExpect(status().isConflict());

        // 4. Create Rate Plan
        CreateRatePlanRequest rateReq = new CreateRatePlanRequest(
                "BAR",
                "Best Available Rate (Room Only)",
                roomId,
                "ROOM_ONLY",
                "FREE_CANCELLATION_24H",
                null, null, null
        );
        mockMvc.perform(post("/api/v1/properties/" + propId + "/rate-plans")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rateReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("BAR"))
                .andExpect(jsonPath("$.roomTypeId").value(roomId));
    }

    @Test
    @DisplayName("US-2.4 & FR-2.6: Bulk import rooms and rate plans from CSV")
    void testCsvImport() throws Exception {
        CreatePropertyRequest propReq = new CreatePropertyRequest(
                "Heritage Palace",
                "Jaipur",
                "Asia/Kolkata",
                "INR",
                null, null, null, null
        );
        MvcResult propResult = mockMvc.perform(post("/api/v1/properties")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(propReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String propId = objectMapper.readTree(propResult.getResponse().getContentAsString()).get("id").asText();

        String csvData = """
                type,code,name,max_occupancy,total_units,bed_type,amenities
                ROOM,STD,Standard Room,2,5,Queen,WiFi
                ROOM,STE,Executive Suite,4,2,King,WiFi+Lounge
                RATE,BAR_STD,Standard Room Only,STD,ROOM_ONLY,FREE_24H
                RATE,BB_STD,Standard with Breakfast,STD,BED_AND_BREAKFAST,NON_REFUNDABLE
                """;

        mockMvc.perform(post("/api/v1/properties/" + propId + "/import-csv-text")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedRoomsCount").value(2))
                .andExpect(jsonPath("$.importedRatesCount").value(2))
                .andExpect(jsonPath("$.failedRowsCount").value(0));
    }
}

