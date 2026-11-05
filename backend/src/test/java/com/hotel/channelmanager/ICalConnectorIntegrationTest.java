package com.hotel.channelmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.channelmanager.channels.connector.ICalConnector;
import com.hotel.channelmanager.channels.connector.InboundReservation;
import com.hotel.channelmanager.channels.domain.*;
import com.hotel.channelmanager.channels.service.ICalFeedService;
import com.hotel.channelmanager.channels.service.ICalSyncService;
import com.hotel.channelmanager.common.security.EncryptionService;
import com.hotel.channelmanager.common.security.JwtService;
import com.hotel.channelmanager.identity.domain.Organization;
import com.hotel.channelmanager.identity.domain.OrganizationRepository;
import com.hotel.channelmanager.identity.domain.Role;
import com.hotel.channelmanager.identity.domain.User;
import com.hotel.channelmanager.identity.domain.UserRepository;
import com.hotel.channelmanager.inventory.domain.DailyInventory;
import com.hotel.channelmanager.inventory.domain.DailyInventoryRepository;
import com.hotel.channelmanager.inventory.domain.ManualBlock;
import com.hotel.channelmanager.inventory.domain.ManualBlockRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ICalConnectorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ICalConnector iCalConnector;

    @Autowired
    private ICalFeedService iCalFeedService;

    @Autowired
    private ICalSyncService iCalSyncService;

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
    private ManualBlockRepository manualBlockRepository;

    @Autowired
    private ChannelAccountRepository channelAccountRepository;

    @Autowired
    private ChannelRoomMappingRepository roomMappingRepository;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String orgId;
    private Property testProperty;
    private RoomType testRoom;
    private ChannelAccount icalAccount;

    @BeforeEach
    void setUp() {
        roomMappingRepository.deleteAll();
        channelAccountRepository.deleteAll();
        manualBlockRepository.deleteAll();
        reservationRepository.deleteAll();
        inventoryRepository.deleteAll();
        roomTypeRepository.deleteAll();
        propertyRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        Organization org = organizationRepository.save(new Organization("org-ical", "iCal Hotel Group", "PRO", "ACTIVE"));
        orgId = org.getId();

        userRepository.save(new User(
                orgId,
                "admin@icalhotel.com",
                passwordEncoder.encode("Password123!"),
                "ICal",
                "Admin",
                Role.ORG_ADMIN
        ));

        testProperty = propertyRepository.save(new Property(
                orgId,
                "Sunset Valley Cottages",
                "50 Pine Crest",
                "UTC",
                "INR"
        ));

        testRoom = roomTypeRepository.save(new RoomType(
                orgId,
                testProperty.getId(),
                "COT",
                "Pine Cottage",
                2,
                3,
                "King Bed",
                "Mountain View"
        ));

        icalAccount = channelAccountRepository.save(new ChannelAccount(
                orgId,
                testProperty.getId(),
                ChannelType.ICAL,
                "Airbnb Pine Cottage Feed",
                encryptionService.encrypt("{\"icalUrl\":\"https://www.airbnb.com/calendar/ical/123456.ics?s=abcdef\"}")
        ));

        roomMappingRepository.save(new ChannelRoomMapping(
                orgId,
                icalAccount.getId(),
                testRoom.getId(),
                "ical_calendar",
                "Pine Cottage Calendar"
        ));
    }

    @Test
    @DisplayName("Should parse RFC 5545 iCalendar stream accurately")
    void testParseIcs() {
        String icsData = """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//Airbnb Inc//Hosting Calendar 0.8.8//EN
                CALSCALE:GREGORIAN
                METHOD:PUBLISH
                BEGIN:VEVENT
                DTEND;VALUE=DATE:20261205
                DTSTART;VALUE=DATE:20261202
                UID:airbnb-reservation-999@airbnb.com
                DESCRIPTION:Checkin: 2026-12-02\\nCheckout: 2026-12-05
                SUMMARY:Reserved - Alice Wonderland (HM-7788)
                STATUS:CONFIRMED
                END:VEVENT
                BEGIN:VEVENT
                DTEND;VALUE=DATE:20261212
                DTSTART;VALUE=DATE:20261210
                UID:airbnb-reservation-1000@airbnb.com
                SUMMARY:Reserved - Bob Builder
                STATUS:CONFIRMED
                END:VEVENT
                END:VCALENDAR
                """;

        List<InboundReservation> parsed = iCalConnector.parseIcs(icsData);
        assertThat(parsed).hasSize(2);

        InboundReservation res1 = parsed.get(0);
        assertThat(res1.channelReservationId()).isEqualTo("airbnb-reservation-999@airbnb.com");
        assertThat(res1.guestName()).isEqualTo("Reserved - Alice Wonderland (HM-7788)");
        assertThat(res1.checkInDate()).isEqualTo(LocalDate.of(2026, 12, 2));
        assertThat(res1.checkOutDate()).isEqualTo(LocalDate.of(2026, 12, 5));
        assertThat(res1.status()).isEqualTo("CONFIRMED");

        InboundReservation res2 = parsed.get(1);
        assertThat(res2.channelReservationId()).isEqualTo("airbnb-reservation-1000@airbnb.com");
        assertThat(res2.checkInDate()).isEqualTo(LocalDate.of(2026, 12, 10));
        assertThat(res2.checkOutDate()).isEqualTo(LocalDate.of(2026, 12, 12));
    }

    @Test
    @DisplayName("Should export valid RFC 5545 .ics calendar feed publicly for OTAs")
    void testExportIcsFeedEndpoint() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(2);
        LocalDate checkOut = LocalDate.now().plusDays(5);

        // Create a confirmed reservation
        reservationRepository.save(new Reservation(
                orgId,
                testProperty.getId(),
                null,
                "BOOKING_COM",
                "bcom-ical-test",
                ReservationStatus.CONFIRMED,
                "David Hasselhoff",
                "david@baywatch.com",
                null,
                testRoom.getId(),
                null,
                checkIn,
                checkOut,
                1,
                300000L,
                "INR",
                null
        ));

        // Create a manual block
        LocalDate blockStart = LocalDate.now().plusDays(10);
        LocalDate blockEnd = LocalDate.now().plusDays(12);
        manualBlockRepository.save(new ManualBlock(
                orgId,
                testProperty.getId(),
                testRoom.getId(),
                blockStart,
                blockEnd,
                1,
                "Roof Maintenance",
                "admin-1"
        ));

        // Call public export endpoint
        mockMvc.perform(get("/api/v1/properties/" + testProperty.getId() + "/ical/" + testRoom.getId() + ".ics"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/calendar;charset=UTF-8"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("BEGIN:VCALENDAR")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PRODID:-//Hotel Channel Manager//EN")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Pine Cottage")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Reserved (BOOKING_COM)")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Closed for Sale (Roof Maintenance)")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("END:VCALENDAR")));
    }

    @Test
    @DisplayName("Should ingest iCal feed, decrement inventory atomically, and enforce idempotency")
    void testIngestIcsContentAndIdempotency() {
        LocalDate checkIn = LocalDate.now().plusDays(15);
        LocalDate checkOut = LocalDate.now().plusDays(18);

        // Pre-initialize inventory for date
        DailyInventory initialInv = new DailyInventory(orgId, testProperty.getId(), testRoom.getId(), checkIn, 3);
        inventoryRepository.save(initialInv);

        String sampleIcs = String.format("""
                BEGIN:VCALENDAR
                VERSION:2.0
                BEGIN:VEVENT
                UID:vrbo-booking-777
                DTSTART;VALUE=DATE:%s
                DTEND;VALUE=DATE:%s
                SUMMARY:VRBO Guest - Captain Kirk
                STATUS:CONFIRMED
                END:VEVENT
                END:VCALENDAR
                """, checkIn.toString().replace("-", ""), checkOut.toString().replace("-", ""));

        // First ingestion -> processes booking and decrements inventory
        int imported = iCalSyncService.ingestIcsContent(icalAccount, sampleIcs);
        assertThat(imported).isEqualTo(1);

        List<Reservation> reservations = reservationRepository.findAll();
        assertThat(reservations).hasSize(1);
        Reservation res = reservations.get(0);
        assertThat(res.getGuestName()).isEqualTo("VRBO Guest - Captain Kirk");
        assertThat(res.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(res.getChannelType()).isEqualTo("ICAL");

        DailyInventory updatedInv = inventoryRepository.findByRoomTypeIdAndDate(testRoom.getId(), checkIn).orElseThrow();
        assertThat(updatedInv.getSoldUnits()).isEqualTo(1);
        assertThat(updatedInv.getAvailableUnits()).isEqualTo(2);

        // Second ingestion with identical UID -> Idempotent, doesn't duplicate or re-decrement
        int secondImport = iCalSyncService.ingestIcsContent(icalAccount, sampleIcs);
        assertThat(secondImport).isEqualTo(1);

        assertThat(reservationRepository.findAll()).hasSize(1);
        DailyInventory checkInv = inventoryRepository.findByRoomTypeIdAndDate(testRoom.getId(), checkIn).orElseThrow();
        assertThat(checkInv.getSoldUnits()).isEqualTo(1);
    }
}

