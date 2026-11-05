package com.hotel.channelmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.channelmanager.channels.outbox.OutboxEventRepository;
import com.hotel.channelmanager.common.security.JwtService;
import com.hotel.channelmanager.identity.domain.Organization;
import com.hotel.channelmanager.identity.domain.OrganizationRepository;
import com.hotel.channelmanager.identity.domain.Role;
import com.hotel.channelmanager.identity.domain.User;
import com.hotel.channelmanager.identity.domain.UserRepository;
import com.hotel.channelmanager.inventory.domain.DailyInventory;
import com.hotel.channelmanager.inventory.domain.DailyInventoryRepository;
import com.hotel.channelmanager.property.domain.Property;
import com.hotel.channelmanager.property.domain.PropertyRepository;
import com.hotel.channelmanager.property.domain.RoomType;
import com.hotel.channelmanager.property.domain.RoomTypeRepository;
import com.hotel.channelmanager.reservations.domain.*;
import com.hotel.channelmanager.reservations.web.dto.InboundReservationRequest;
import com.hotel.channelmanager.reservations.web.dto.ResolveConflictRequest;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationOverbookingIntegrationTest {

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
    private DailyInventoryRepository inventoryRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ConflictRepository conflictRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String token;
    private String orgId;
    private Property testProperty;
    private RoomType testRoom;
    private RoomType suiteRoom;

    @BeforeEach
    void setUp() {
        conflictRepository.deleteAll();
        reservationRepository.deleteAll();
        outboxEventRepository.deleteAll();
        inventoryRepository.deleteAll();
        roomTypeRepository.deleteAll();
        propertyRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        Organization org = organizationRepository.save(new Organization("org-res", "Res Hotel Org", "PRO", "ACTIVE"));
        orgId = org.getId();

        User admin = userRepository.save(new User(
                orgId,
                "admin@reshotel.com",
                passwordEncoder.encode("Password123!"),
                "Admin",
                "User",
                Role.ORG_ADMIN
        ));
        token = "Bearer " + jwtService.generateAccessToken(admin.getId(), orgId, admin.getEmail(), "ORG_ADMIN");

        testProperty = propertyRepository.save(new Property(
                orgId,
                "Overbooking Grand Resort",
                "100 Sea Breeze",
                "UTC",
                "INR"
        ));

        testRoom = roomTypeRepository.save(new RoomType(
                orgId,
                testProperty.getId(),
                "DLX",
                "Deluxe Room",
                2,
                1, // Exactly 1 unit available for testing overbooking
                "King Bed",
                "AC, WiFi"
        ));

        suiteRoom = roomTypeRepository.save(new RoomType(
                orgId,
                testProperty.getId(),
                "STE",
                "Executive Suite",
                4,
                5,
                "2 King Beds",
                "Full Kitchen, Jacuzzi"
        ));
    }

    @Test
    @DisplayName("Should confirm inbound booking, atomically decrement inventory, and write outbox event")
    void testInboundReservationConfirmedAndIdempotent() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(7);

        InboundReservationRequest req = new InboundReservationRequest(
                "BOOKING_COM",
                "bcom-res-101",
                "Sarah Connor",
                "sarah@terminator.com",
                "+15551234567",
                testRoom.getId(),
                null,
                checkIn,
                checkOut,
                1,
                800000L,
                "INR",
                "Quiet room please"
        );

        // First attempt -> CONFIRMED
        mockMvc.perform(post("/api/v1/properties/" + testProperty.getId() + "/reservations/inbound")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.guestName").value("Sarah Connor"));

        // Verify inventory decremented
        DailyInventory night1 = inventoryRepository.findByRoomTypeIdAndDate(testRoom.getId(), checkIn).orElseThrow();
        assertThat(night1.getSoldUnits()).isEqualTo(1);
        assertThat(night1.getAvailableUnits()).isEqualTo(0);

        // Verify Outbox event created
        assertThat(outboxEventRepository.findAll()).isNotEmpty();

        // Second attempt with same (channel, channelReservationId) -> Idempotent, returns existing CONFIRMED
        mockMvc.perform(post("/api/v1/properties/" + testProperty.getId() + "/reservations/inbound")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.channelReservationId").value("bcom-res-101"));

        // Sold units still 1 (not double-decremented)
        DailyInventory night1Check = inventoryRepository.findByRoomTypeIdAndDate(testRoom.getId(), checkIn).orElseThrow();
        assertThat(night1Check.getSoldUnits()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should detect overbooking when sold out, create Conflict record, and allow resolving via Move Guest")
    void testOverbookingDetectionAndResolution() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = LocalDate.now().plusDays(12);

        // Initialize inventory with 0 available units (sold out)
        DailyInventory soldOutNight1 = new DailyInventory(orgId, testProperty.getId(), testRoom.getId(), checkIn, 1);
        soldOutNight1.setSoldUnits(1);
        soldOutNight1.recalculateAvailability();
        inventoryRepository.save(soldOutNight1);

        DailyInventory soldOutNight2 = new DailyInventory(orgId, testProperty.getId(), testRoom.getId(), checkIn.plusDays(1), 1);
        soldOutNight2.setSoldUnits(1);
        soldOutNight2.recalculateAvailability();
        inventoryRepository.save(soldOutNight2);

        // Overbooking inbound reservation arrives from Agoda
        InboundReservationRequest overbookReq = new InboundReservationRequest(
                "AGODA",
                "agoda-res-999",
                "John Wick",
                "john@continental.com",
                "+15559876543",
                testRoom.getId(),
                null,
                checkIn,
                checkOut,
                1,
                950000L,
                "INR",
                "Late check-in"
        );

        MvcResult res = mockMvc.perform(post("/api/v1/properties/" + testProperty.getId() + "/reservations/inbound")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overbookReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFLICT"))
                .andReturn();

        // Check Conflict record created
        List<Conflict> conflicts = conflictRepository.findAll();
        assertThat(conflicts).hasSize(1);
        Conflict conflict = conflicts.get(0);
        assertThat(conflict.getStatus()).isEqualTo(ConflictStatus.UNRESOLVED);
        assertThat(conflict.getReason()).contains("was already sold out");

        // Verify available units NEVER dropped below 0
        DailyInventory checkNight1 = inventoryRepository.findByRoomTypeIdAndDate(testRoom.getId(), checkIn).orElseThrow();
        assertThat(checkNight1.getAvailableUnits()).isEqualTo(0);

        // Resolve conflict via MOVE_GUEST to suiteRoom
        ResolveConflictRequest resolveReq = new ResolveConflictRequest(
                ConflictResolutionAction.MOVE_GUEST,
                suiteRoom.getId(),
                "Upgraded guest to Executive Suite free of charge"
        );

        mockMvc.perform(post("/api/v1/properties/" + testProperty.getId() + "/conflicts/" + conflict.getId() + "/resolve")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolutionAction").value("MOVE_GUEST"));

        // Reservation now confirmed in suiteRoom
        Reservation updatedRes = reservationRepository.findById(conflict.getReservationId()).orElseThrow();
        assertThat(updatedRes.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(updatedRes.getRoomTypeId()).isEqualTo(suiteRoom.getId());
    }

    @Test
    @DisplayName("Architecture Rule 6 Concurrency Test T1: Simultaneous bookings for the last room")
    void testConcurrentBookingsForLastRoom() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(20);
        LocalDate checkOut = LocalDate.now().plusDays(21); // 1 night stay

        // Setup room with exactly 1 unit
        DailyInventory lastUnit = new DailyInventory(orgId, testProperty.getId(), testRoom.getId(), checkIn, 1);
        lastUnit.recalculateAvailability();
        inventoryRepository.save(lastUnit);

        int totalConcurrentRequests = 10;
        ExecutorService executor = Executors.newFixedThreadPool(totalConcurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalConcurrentRequests);

        AtomicInteger confirmedCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < totalConcurrentRequests; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronize all threads to start at the exact same millisecond
                    InboundReservationRequest req = new InboundReservationRequest(
                            "CHANNEL_" + index,
                            "sim-ref-" + index,
                            "Guest " + index,
                            "guest" + index + "@test.com",
                            null,
                            testRoom.getId(),
                            null,
                            checkIn,
                            checkOut,
                            1,
                            500000L,
                            "INR",
                            null
                    );

                    MvcResult result = mockMvc.perform(post("/api/v1/properties/" + testProperty.getId() + "/reservations/inbound")
                                    .header("Authorization", token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(req)))
                            .andReturn();

                    String status = objectMapper.readTree(result.getResponse().getContentAsString()).get("status").asText();
                    if ("CONFIRMED".equals(status)) {
                        confirmedCount.incrementAndGet();
                    } else if ("CONFLICT".equals(status)) {
                        conflictCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Release all threads simultaneously
        startLatch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Exact Assertion: Exactly 1 booking was CONFIRMED, the other 9 became CONFLICT
        assertThat(confirmedCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(totalConcurrentRequests - 1);

        // Availability must NEVER drop below 0
        DailyInventory finalState = inventoryRepository.findByRoomTypeIdAndDate(testRoom.getId(), checkIn).orElseThrow();
        assertThat(finalState.getAvailableUnits()).isEqualTo(0);
        assertThat(finalState.getSoldUnits()).isEqualTo(1);
    }
}

