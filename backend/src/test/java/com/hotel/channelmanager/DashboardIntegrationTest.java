package com.hotel.channelmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.channelmanager.channels.domain.*;
import com.hotel.channelmanager.common.security.JwtService;
import com.hotel.channelmanager.identity.domain.Organization;
import com.hotel.channelmanager.identity.domain.OrganizationRepository;
import com.hotel.channelmanager.identity.domain.Role;
import com.hotel.channelmanager.identity.domain.User;
import com.hotel.channelmanager.identity.domain.UserRepository;
import com.hotel.channelmanager.inventory.domain.DailyInventory;
import com.hotel.channelmanager.inventory.domain.DailyInventoryRepository;
import com.hotel.channelmanager.notifications.domain.Notification;
import com.hotel.channelmanager.notifications.domain.NotificationRepository;
import com.hotel.channelmanager.notifications.domain.NotificationType;
import com.hotel.channelmanager.property.domain.Property;
import com.hotel.channelmanager.property.domain.PropertyRepository;
import com.hotel.channelmanager.property.domain.RoomType;
import com.hotel.channelmanager.property.domain.RoomTypeRepository;
import com.hotel.channelmanager.reservations.domain.Reservation;
import com.hotel.channelmanager.reservations.domain.ReservationRepository;
import com.hotel.channelmanager.reservations.domain.ReservationStatus;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardIntegrationTest {

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
    private NotificationRepository notificationRepository;

    @Autowired
    private ChannelAccountRepository channelAccountRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String token;
    private String orgId;
    private Property testProperty;
    private RoomType deluxeRoom;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        channelAccountRepository.deleteAll();
        reservationRepository.deleteAll();
        inventoryRepository.deleteAll();
        roomTypeRepository.deleteAll();
        propertyRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        Organization org = organizationRepository.save(new Organization("org-dash", "Dash Resort", "PRO", "ACTIVE"));
        orgId = org.getId();

        User admin = userRepository.save(new User(
                orgId,
                "admin@dashresort.com",
                passwordEncoder.encode("Password123!"),
                "Dash",
                "Admin",
                Role.ORG_ADMIN
        ));
        token = "Bearer " + jwtService.generateAccessToken(admin.getId(), orgId, admin.getEmail(), "ORG_ADMIN");

        testProperty = propertyRepository.save(new Property(
                orgId,
                "Dash Grand Palace",
                "10 Ocean Boulevard",
                "UTC",
                "INR"
        ));

        deluxeRoom = roomTypeRepository.save(new RoomType(
                orgId,
                testProperty.getId(),
                "DLX",
                "Deluxe Ocean Villa",
                2,
                10,
                "King Bed",
                "Ocean Front"
        ));
    }

    @Test
    @DisplayName("Should return accurate real-time dashboard KPIs, arrivals, departures, and 7-day occupancy")
    void testDashboardSummary() throws Exception {
        LocalDate today = LocalDate.now();

        // 1. Create Today's Arrival
        reservationRepository.save(new Reservation(
                orgId,
                testProperty.getId(),
                null,
                "BOOKING_COM",
                "bcom-dash-1",
                ReservationStatus.CONFIRMED,
                "Michael Jordan",
                "mj@bulls.com",
                null,
                deluxeRoom.getId(),
                null,
                today,
                today.plusDays(3),
                2,
                1500000L,
                "INR",
                "High floor requested"
        ));

        // 2. Create Today's Departure
        reservationRepository.save(new Reservation(
                orgId,
                testProperty.getId(),
                null,
                "DIRECT",
                "dir-dash-2",
                ReservationStatus.CONFIRMED,
                "Kobe Bryant",
                "kobe@lakers.com",
                null,
                deluxeRoom.getId(),
                null,
                today.minusDays(2),
                today,
                1,
                800000L,
                "INR",
                null
        ));

        // 3. Connect a Channel Account
        channelAccountRepository.save(new ChannelAccount(
                orgId,
                testProperty.getId(),
                ChannelType.MOCK,
                "Mock Channel Booking",
                "encrypted-secret"
        ));

        mockMvc.perform(get("/api/v1/properties/" + testProperty.getId() + "/dashboard")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propertyName").value("Dash Grand Palace"))
                .andExpect(jsonPath("$.todayArrivalsCount").value(1))
                .andExpect(jsonPath("$.todayDeparturesCount").value(1))
                .andExpect(jsonPath("$.occupiedRoomsToday").value(2))
                .andExpect(jsonPath("$.totalRooms").value(10))
                .andExpect(jsonPath("$.occupancyRatePercentage").value(20.0))
                .andExpect(jsonPath("$.todayRevenueCents").value(1500000L))
                .andExpect(jsonPath("$.todayArrivals[0].guestName").value("Michael Jordan"))
                .andExpect(jsonPath("$.todayDepartures[0].guestName").value("Kobe Bryant"))
                .andExpect(jsonPath("$.sevenDayOccupancyTrend").isArray())
                .andExpect(jsonPath("$.sevenDayOccupancyTrend.length()").value(7))
                .andExpect(jsonPath("$.channelHealthList").isArray())
                .andExpect(jsonPath("$.channelHealthList[0].channelType").value("MOCK"));
    }

    @Test
    @DisplayName("Should create notifications, track unread count, and mark notifications as read")
    void testNotificationsLifecycle() throws Exception {
        Notification n1 = notificationRepository.save(new Notification(
                orgId,
                testProperty.getId(),
                NotificationType.DOUBLE_BOOKING,
                "Double booking detected",
                "Action needed: Deluxe Ocean Villa has conflicting reservations on tomorrow.",
                "/bookings"
        ));

        Notification n2 = notificationRepository.save(new Notification(
                orgId,
                testProperty.getId(),
                NotificationType.NEW_BOOKING,
                "New Booking Confirmed",
                "Sarah Connor booked 1 night via Booking.com.",
                "/bookings"
        ));

        // Unread count should be 2
        mockMvc.perform(get("/api/v1/properties/" + testProperty.getId() + "/notifications/unread-count")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(2));

        // Mark n1 as read
        mockMvc.perform(post("/api/v1/properties/" + testProperty.getId() + "/notifications/" + n1.getId() + "/read")
                        .header("Authorization", token))
                .andExpect(status().isOk());

        // Unread count should now be 1
        mockMvc.perform(get("/api/v1/properties/" + testProperty.getId() + "/notifications/unread-count")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));

        // Mark all as read
        mockMvc.perform(post("/api/v1/properties/" + testProperty.getId() + "/notifications/mark-all-read")
                        .header("Authorization", token))
                .andExpect(status().isOk());

        // Unread count should now be 0
        mockMvc.perform(get("/api/v1/properties/" + testProperty.getId() + "/notifications/unread-count")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }
}

