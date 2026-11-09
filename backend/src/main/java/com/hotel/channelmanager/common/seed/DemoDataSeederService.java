package com.hotel.channelmanager.common.seed;

import com.hotel.channelmanager.channels.domain.*;
import com.hotel.channelmanager.common.security.EncryptionService;
import com.hotel.channelmanager.common.tenant.TenantContext;
import com.hotel.channelmanager.identity.domain.Organization;
import com.hotel.channelmanager.identity.domain.OrganizationRepository;
import com.hotel.channelmanager.identity.domain.Role;
import com.hotel.channelmanager.identity.domain.User;
import com.hotel.channelmanager.identity.domain.UserRepository;
import com.hotel.channelmanager.inventory.domain.*;
import com.hotel.channelmanager.notifications.domain.Notification;
import com.hotel.channelmanager.notifications.domain.NotificationRepository;
import com.hotel.channelmanager.notifications.domain.NotificationType;
import com.hotel.channelmanager.property.domain.*;
import com.hotel.channelmanager.reservations.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Automatically seeds a rich, demo-ready organization on application startup
 * with 1 property, 3 room types, 2 rate plans, 90 days of inventory/rates,
 * a connected mock channel, and sample bookings/conflicts for instant evaluation.
 */
@Service
public class DemoDataSeederService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeederService.class);

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RatePlanRepository ratePlanRepository;
    private final DailyInventoryRepository inventoryRepository;
    private final DailyRateRepository rateRepository;
    private final ManualBlockRepository manualBlockRepository;
    private final ChannelAccountRepository channelAccountRepository;
    private final ChannelRoomMappingRepository channelRoomMappingRepository;
    private final ChannelRateMappingRepository channelRateMappingRepository;
    private final ReservationRepository reservationRepository;
    private final ConflictRepository conflictRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionService encryptionService;
    private final Environment environment;

    public DemoDataSeederService(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            PropertyRepository propertyRepository,
            RoomTypeRepository roomTypeRepository,
            RatePlanRepository ratePlanRepository,
            DailyInventoryRepository inventoryRepository,
            DailyRateRepository rateRepository,
            ManualBlockRepository manualBlockRepository,
            ChannelAccountRepository channelAccountRepository,
            ChannelRoomMappingRepository channelRoomMappingRepository,
            ChannelRateMappingRepository channelRateMappingRepository,
            ReservationRepository reservationRepository,
            ConflictRepository conflictRepository,
            NotificationRepository notificationRepository,
            PasswordEncoder passwordEncoder,
            EncryptionService encryptionService,
            Environment environment
    ) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.ratePlanRepository = ratePlanRepository;
        this.inventoryRepository = inventoryRepository;
        this.rateRepository = rateRepository;
        this.manualBlockRepository = manualBlockRepository;
        this.channelAccountRepository = channelAccountRepository;
        this.channelRoomMappingRepository = channelRoomMappingRepository;
        this.channelRateMappingRepository = channelRateMappingRepository;
        this.reservationRepository = reservationRepository;
        this.conflictRepository = conflictRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.encryptionService = encryptionService;
        this.environment = environment;
    }

    @Override
    public void run(String... args) {
        if (environment != null && Arrays.asList(environment.getActiveProfiles()).contains("test")) {
            log.info("Test profile active - skipping automatic demo data seeding.");
            return;
        }
        seedDemoDataIfEmpty();
    }

    @Transactional
    public void seedDemoDataIfEmpty() {
        if (organizationRepository.findByName("Grand Palace Resorts").isPresent()) {
            log.info("Demo organization already seeded.");
            return;
        }

        log.info("Seeding complete demo environment for Grand Palace Resorts...");

        // 1. Organization & Users
        Organization org = new Organization("org-demo-001", "Grand Palace Resorts", "ENTERPRISE", "ACTIVE");
        organizationRepository.save(org);
        String orgId = org.getId();
        TenantContext.setOrganizationId(orgId);

        User admin = new User(
                orgId,
                "admin@grandpalace.com",
                passwordEncoder.encode("Password123!"),
                "Alex",
                "Sterling",
                Role.ORG_ADMIN
        );
        admin.setId("user-admin-001");
        userRepository.save(admin);

        User revenueManager = new User(
                orgId,
                "revenue@grandpalace.com",
                passwordEncoder.encode("Password123!"),
                "Maya",
                "Patel",
                Role.REVENUE_MANAGER
        );
        revenueManager.setId("user-rev-001");
        userRepository.save(revenueManager);

        User frontDesk = new User(
                orgId,
                "desk@grandpalace.com",
                passwordEncoder.encode("Password123!"),
                "Sam",
                "Taylor",
                Role.RESERVATIONS_STAFF
        );
        frontDesk.setId("user-desk-001");
        userRepository.save(frontDesk);

        // 2. Property
        Property property = new Property(
                orgId,
                "The Grand Palace Resort & Spa",
                "100 Beachfront Promenade, Goa",
                "Asia/Kolkata",
                "INR"
        );
        property.setId("prop-demo-001");
        propertyRepository.save(property);
        String propId = property.getId();

        // 3. 3 Room Types
        RoomType deluxe = new RoomType(
                orgId,
                propId,
                "DLX-OCN",
                "Deluxe Ocean View",
                2,
                10,
                "1 King Bed",
                "Private Ocean Balcony, AC, Mini-bar, High-speed WiFi"
        );
        deluxe.setId("room-deluxe-001");
        roomTypeRepository.save(deluxe);

        RoomType suite = new RoomType(
                orgId,
                propId,
                "EXEC-STE",
                "Executive Sea Suite",
                4,
                5,
                "2 King Beds",
                "Living Lounge, Private Jacuzzi, Panoramic Sea View, Kitchenette"
        );
        suite.setId("room-suite-001");
        roomTypeRepository.save(suite);

        RoomType villa = new RoomType(
                orgId,
                propId,
                "PRES-VIL",
                "Presidential Beach Villa",
                6,
                2,
                "3 King Beds",
                "Private Infinity Pool, Personal Butler, Direct Beach Access, Dining Terrace"
        );
        villa.setId("room-villa-001");
        roomTypeRepository.save(villa);

        // 4. Rate Plans (per Room Type)
        RatePlan deluxeBar = new RatePlan(
                orgId,
                propId,
                deluxe.getId(),
                "DLX-BAR",
                "Deluxe Flexible Rate",
                "ROOM_ONLY",
                "FREE_CANCELLATION_24H"
        );
        deluxeBar.setId("rate-dlx-bar");
        ratePlanRepository.save(deluxeBar);

        RatePlan deluxeBb = new RatePlan(
                orgId,
                propId,
                deluxe.getId(),
                "DLX-BB",
                "Deluxe Bed & Breakfast",
                "BED_AND_BREAKFAST",
                "NON_REFUNDABLE"
        );
        deluxeBb.setId("rate-dlx-bb");
        ratePlanRepository.save(deluxeBb);

        RatePlan suiteBar = new RatePlan(
                orgId,
                propId,
                suite.getId(),
                "STE-BAR",
                "Executive Suite Flexible Rate",
                "ROOM_ONLY",
                "FREE_CANCELLATION_24H"
        );
        suiteBar.setId("rate-ste-bar");
        ratePlanRepository.save(suiteBar);

        RatePlan suiteBb = new RatePlan(
                orgId,
                propId,
                suite.getId(),
                "STE-BB",
                "Executive Suite Bed & Breakfast",
                "BED_AND_BREAKFAST",
                "NON_REFUNDABLE"
        );
        suiteBb.setId("rate-ste-bb");
        ratePlanRepository.save(suiteBb);

        RatePlan villaBar = new RatePlan(
                orgId,
                propId,
                villa.getId(),
                "VIL-BAR",
                "Beach Villa Flexible Rate",
                "ROOM_ONLY",
                "FREE_CANCELLATION_48H"
        );
        villaBar.setId("rate-vil-bar");
        ratePlanRepository.save(villaBar);

        RatePlan villaBb = new RatePlan(
                orgId,
                propId,
                villa.getId(),
                "VIL-BB",
                "Beach Villa Bed & Breakfast",
                "BED_AND_BREAKFAST",
                "NON_REFUNDABLE"
        );
        villaBb.setId("rate-vil-bb");
        ratePlanRepository.save(villaBb);

        // 5. 90 Days of Daily Inventory & Daily Rates
        LocalDate today = LocalDate.now();
        List<DailyInventory> inventoryBatch = new ArrayList<>();
        List<DailyRate> ratesBatch = new ArrayList<>();

        for (int i = 0; i < 90; i++) {
            LocalDate d = today.plusDays(i);

            // Deluxe (10 units, ₹6,500 BAR / ₹7,800 B&B)
            // Seeded with confirmed booking for days 0-2 (res1)
            DailyInventory invDeluxe = new DailyInventory(orgId, propId, deluxe.getId(), d, 10);
            if (i >= 0 && i < 3) {
                invDeluxe.setSoldUnits(1);
            }
            inventoryBatch.add(invDeluxe);
            ratesBatch.add(new DailyRate(orgId, propId, deluxeBar.getId(), d, 650000L, "INR"));
            ratesBatch.add(new DailyRate(orgId, propId, deluxeBb.getId(), d, 780000L, "INR"));

            // Suite (5 units, ₹14,000 BAR / ₹16,500 B&B)
            // Seeded with confirmed booking for days 0-1 (res2)
            DailyInventory invSuite = new DailyInventory(orgId, propId, suite.getId(), d, 5);
            if (i >= 0 && i < 2) {
                invSuite.setSoldUnits(1);
            }
            inventoryBatch.add(invSuite);
            ratesBatch.add(new DailyRate(orgId, propId, suiteBar.getId(), d, 1400000L, "INR"));
            ratesBatch.add(new DailyRate(orgId, propId, suiteBb.getId(), d, 1650000L, "INR"));

            // Villa (2 units, ₹42,000 BAR / ₹46,000 B&B)
            // Seeded with confirmed booking for days 5-8 (res4)
            DailyInventory invVilla = new DailyInventory(orgId, propId, villa.getId(), d, 2);
            if (i >= 5 && i < 9) {
                invVilla.setSoldUnits(1);
            }
            inventoryBatch.add(invVilla);
            ratesBatch.add(new DailyRate(orgId, propId, villaBar.getId(), d, 4200000L, "INR"));
            ratesBatch.add(new DailyRate(orgId, propId, villaBb.getId(), d, 4600000L, "INR"));
        }

        inventoryRepository.saveAll(inventoryBatch);
        rateRepository.saveAll(ratesBatch);

        // 6. Connected Mock Channel Account + Room & Rate Mappings
        ChannelAccount mockAccount = new ChannelAccount(
                orgId,
                propId,
                ChannelType.MOCK,
                "Mock Global OTA Channel",
                encryptionService.encrypt("{\"apiKey\":\"mock-key-prod-001\"}")
        );
        mockAccount.setId("chan-mock-001");
        channelAccountRepository.save(mockAccount);

        channelRoomMappingRepository.save(new ChannelRoomMapping(orgId, mockAccount.getId(), deluxe.getId(), "mock_deluxe", "Deluxe Room (OTA)"));
        channelRoomMappingRepository.save(new ChannelRoomMapping(orgId, mockAccount.getId(), suite.getId(), "mock_standard", "Sea Suite (OTA)"));
        channelRoomMappingRepository.save(new ChannelRoomMapping(orgId, mockAccount.getId(), villa.getId(), "mock_suite", "Beach Villa (OTA)"));

        channelRateMappingRepository.save(new ChannelRateMapping(orgId, mockAccount.getId(), deluxeBar.getId(), "mock_rate_deluxe_bar", "Deluxe Flexible (OTA)"));
        channelRateMappingRepository.save(new ChannelRateMapping(orgId, mockAccount.getId(), deluxeBb.getId(), "mock_rate_deluxe_bb", "Deluxe Bed & Breakfast (OTA)"));
        channelRateMappingRepository.save(new ChannelRateMapping(orgId, mockAccount.getId(), suiteBar.getId(), "mock_rate_suite_bar", "Suite Flexible (OTA)"));
        channelRateMappingRepository.save(new ChannelRateMapping(orgId, mockAccount.getId(), villaBar.getId(), "mock_rate_villa_bar", "Villa Flexible (OTA)"));

        // 7. Connected iCal Feed Channel Account
        ChannelAccount icalAccount = new ChannelAccount(
                orgId,
                propId,
                ChannelType.ICAL,
                "Airbnb Beach Villa Feed",
                encryptionService.encrypt("{\"icalUrl\":\"https://www.airbnb.com/calendar/ical/sample-villa.ics\"}")
        );
        icalAccount.setId("chan-ical-001");
        channelAccountRepository.save(icalAccount);
        channelRoomMappingRepository.save(new ChannelRoomMapping(orgId, icalAccount.getId(), villa.getId(), "ical_calendar", "Villa iCal Calendar"));

        // 8. Sample Bookings
        // Booking 1: Today's Arrival from Booking.com
        Reservation res1 = new Reservation(
                orgId,
                propId,
                null,
                "BOOKING_COM",
                "BCOM-98712",
                ReservationStatus.CONFIRMED,
                "Alexander Wright",
                "alexander.wright@techcorp.com",
                "+1 415 555 2671",
                deluxe.getId(),
                deluxeBar.getId(),
                today,
                today.plusDays(3),
                1,
                1950000L,
                "INR",
                "High floor requested. Late check-in around 8 PM."
        );
        res1.setId("res-demo-001");
        reservationRepository.save(res1);

        // Booking 2: Today's Arrival from Direct Walk-in
        Reservation res2 = new Reservation(
                orgId,
                propId,
                null,
                "DIRECT",
                "DIR-7721",
                ReservationStatus.CONFIRMED,
                "Priya Sharma",
                "priya.sharma@travelindia.org",
                "+91 98201 55432",
                suite.getId(),
                suiteBb.getId(),
                today,
                today.plusDays(2),
                1,
                3300000L,
                "INR",
                "Anniversary celebration. Complimentary cake requested."
        );
        res2.setId("res-demo-002");
        reservationRepository.save(res2);

        // Booking 3: Today's Departure
        Reservation res3 = new Reservation(
                orgId,
                propId,
                null,
                "EXPEDIA",
                "EXP-4412",
                ReservationStatus.CONFIRMED,
                "Marcus Vance",
                "marcus.vance@vancemedia.com",
                "+44 20 7946 0912",
                deluxe.getId(),
                deluxeBar.getId(),
                today.minusDays(2),
                today,
                1,
                1300000L,
                "INR",
                "Airport taxi transfer booked for 11:30 AM."
        );
        res3.setId("res-demo-003");
        reservationRepository.save(res3);

        // Booking 4: Future Presidential Villa Booking from Airbnb
        Reservation res4 = new Reservation(
                orgId,
                propId,
                null,
                "AIRBNB",
                "HM-88992211",
                ReservationStatus.CONFIRMED,
                "David Beckham",
                "david@beckhamventures.com",
                "+44 7700 900077",
                villa.getId(),
                villaBb.getId(),
                today.plusDays(5),
                today.plusDays(9),
                1,
                18400000L,
                "INR",
                "VIP guest. Butler service and private yacht transfer requested."
        );
        res4.setId("res-demo-004");
        reservationRepository.save(res4);

        // Booking 5: Overbooking Conflict Sample (Architecture Rule 6 Showcase!)
        Reservation resConflict = new Reservation(
                orgId,
                propId,
                null,
                "AGODA",
                "AGD-55112",
                ReservationStatus.CONFLICT,
                "John Wick",
                "john.wick@continental.com",
                "+1 212 555 0199",
                deluxe.getId(),
                deluxeBar.getId(),
                today.plusDays(4),
                today.plusDays(6),
                1,
                1300000L,
                "INR",
                "Quiet room. Do not disturb."
        );
        resConflict.setId("res-demo-005");
        reservationRepository.save(resConflict);

        Conflict conflict = new Conflict(
                orgId,
                propId,
                resConflict.getId(),
                deluxe.getId(),
                today.plusDays(4),
                "Double booking on " + today.plusDays(4) + ": Deluxe Ocean View was already sold out."
        );
        conflict.setId("conf-demo-001");
        conflictRepository.save(conflict);

        // 9. Manual Hold / Maintenance Block
        manualBlockRepository.save(new ManualBlock(
                orgId,
                propId,
                villa.getId(),
                today.plusDays(10),
                today.plusDays(12),
                1,
                "Infinity Pool Tile Maintenance",
                admin.getId()
        ));

        // 10. Sample In-App Staff Notifications
        notificationRepository.save(new Notification(
                orgId,
                propId,
                NotificationType.DOUBLE_BOOKING,
                "Double Booking Detected: Action Required",
                "John Wick booked Deluxe Ocean View on " + today.plusDays(4) + " via Agoda after room was sold out. Please reassign room.",
                "/bookings"
        ));

        notificationRepository.save(new Notification(
                orgId,
                propId,
                NotificationType.NEW_BOOKING,
                "New Booking: Alexander Wright",
                "Alexander Wright confirmed 3 nights in Deluxe Ocean View via Booking.com (₹19,500).",
                "/bookings"
        ));

        notificationRepository.save(new Notification(
                orgId,
                propId,
                NotificationType.SYSTEM_ALERT,
                "Outbox Synchronizer Healthy",
                "All 90 days of inventory and rates are synchronized with connected channels.",
                "/channels/health"
        ));

        TenantContext.clear();
        log.info("Demo data seeding completed successfully! Hotel owner can now login with admin@grandpalace.com / Password123!");
    }
}
