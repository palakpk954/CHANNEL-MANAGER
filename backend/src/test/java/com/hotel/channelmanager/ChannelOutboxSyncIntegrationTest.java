package com.hotel.channelmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.channelmanager.channels.connector.ChannelCredentials;
import com.hotel.channelmanager.channels.domain.*;
import com.hotel.channelmanager.channels.outbox.OutboxEvent;
import com.hotel.channelmanager.channels.outbox.OutboxEventRepository;
import com.hotel.channelmanager.channels.outbox.OutboxPublisher;
import com.hotel.channelmanager.channels.outbox.OutboxStatus;
import com.hotel.channelmanager.channels.web.dto.CreateChannelAccountRequest;
import com.hotel.channelmanager.channels.web.dto.SaveRateMappingRequest;
import com.hotel.channelmanager.channels.web.dto.SaveRoomMappingRequest;
import com.hotel.channelmanager.channels.web.dto.TestConnectionRequest;
import com.hotel.channelmanager.common.security.JwtService;
import com.hotel.channelmanager.identity.domain.Organization;
import com.hotel.channelmanager.identity.domain.OrganizationRepository;
import com.hotel.channelmanager.identity.domain.Role;
import com.hotel.channelmanager.identity.domain.User;
import com.hotel.channelmanager.identity.domain.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChannelOutboxSyncIntegrationTest {

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
    private ChannelRoomMappingRepository roomMappingRepository;

    @Autowired
    private ChannelRateMappingRepository rateMappingRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private SyncJobRepository syncJobRepository;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String token;
    private String orgId;
    private Property testProperty;
    private RoomType testRoom;
    private RatePlan testRate;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        syncJobRepository.deleteAll();
        rateMappingRepository.deleteAll();
        roomMappingRepository.deleteAll();
        channelAccountRepository.deleteAll();
        ratePlanRepository.deleteAll();
        roomTypeRepository.deleteAll();
        propertyRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        Organization org = organizationRepository.save(new Organization("org-conn", "Connector Org", "PRO", "ACTIVE"));
        orgId = org.getId();

        User owner = userRepository.save(new User(
                orgId,
                "owner@connector.com",
                passwordEncoder.encode("Pass123!"),
                "Owner",
                "Name",
                Role.ORG_ADMIN
        ));
        token = "Bearer " + jwtService.generateAccessToken(owner.getId(), orgId, owner.getEmail(), "ORG_ADMIN");

        testProperty = propertyRepository.save(new Property(
                orgId,
                "Connector Grand Hotel",
                "123 Ocean Drive",
                "UTC",
                "INR"
        ));

        testRoom = roomTypeRepository.save(new RoomType(
                orgId,
                testProperty.getId(),
                "DLX",
                "Deluxe Room",
                2,
                10,
                "King Bed",
                "WiFi"
        ));

        testRate = ratePlanRepository.save(new RatePlan(
                orgId,
                testProperty.getId(),
                testRoom.getId(),
                "BAR",
                "Standard Rate",
                "ROOM_ONLY",
                "FREE_24H"
        ));
    }

    @Test
    @DisplayName("Should test channel connection successfully")
    void testConnectionSuccess() throws Exception {
        ChannelCredentials creds = new ChannelCredentials();
        creds.setApiKey("test-valid-key");
        creds.setHotelId("mock-hotel-123");

        TestConnectionRequest request = new TestConnectionRequest(ChannelType.MOCK, creds);

        mockMvc.perform(post("/api/v1/channels/test-connection")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Mock Channel connection verified successfully."));
    }

    @Test
    @DisplayName("Should fail test connection when invalid API key is provided")
    void testConnectionFailure() throws Exception {
        ChannelCredentials creds = new ChannelCredentials();
        creds.setApiKey("invalid-key");
        creds.setHotelId("mock-hotel-123");

        TestConnectionRequest request = new TestConnectionRequest(ChannelType.MOCK, creds);

        mockMvc.perform(post("/api/v1/channels/test-connection")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid API credentials provided for Mock Channel."));
    }

    @Test
    @DisplayName("Should connect channel account, save room & rate mappings, and trigger outbox sync")
    void testChannelLifecycleAndOutboundSync() throws Exception {
        // 1. Create Mock Channel Account
        ChannelCredentials creds = new ChannelCredentials();
        creds.setApiKey("secret-api-key");
        creds.setHotelId("mock-hotel-999");

        CreateChannelAccountRequest accReq = new CreateChannelAccountRequest(
                ChannelType.MOCK,
                "Main Mock Account",
                creds
        );

        MvcResult accResult = mockMvc.perform(post("/api/v1/properties/" + testProperty.getId() + "/channels")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.channelType").value("MOCK"))
                .andExpect(jsonPath("$.accountName").value("Main Mock Account"))
                .andExpect(jsonPath("$.status").value("CONNECTED"))
                .andReturn();

        String channelAccountId = objectMapper.readTree(accResult.getResponse().getContentAsString()).get("id").asText();

        // Verify credentials encrypted in database (not plain text)
        ChannelAccount savedAccount = channelAccountRepository.findById(channelAccountId).orElseThrow();
        assertThat(savedAccount.getEncryptedCredentials()).doesNotContain("secret-api-key");

        // 2. Fetch Mapping Options
        mockMvc.perform(get("/api/v1/properties/" + testProperty.getId() + "/channels/" + channelAccountId + "/mapping-options")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.internalRooms[0].code").value("DLX"))
                .andExpect(jsonPath("$.channelRooms[0].channelRoomId").value("mock_deluxe"));

        // 3. Save Room Mapping
        SaveRoomMappingRequest roomMapReq = new SaveRoomMappingRequest(List.of(
                new SaveRoomMappingRequest.RoomMappingItem(testRoom.getId(), "mock_deluxe", "Deluxe King Room", true)
        ));

        mockMvc.perform(put("/api/v1/properties/" + testProperty.getId() + "/channels/" + channelAccountId + "/room-mappings")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomMapReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomMappings[0].channelRoomId").value("mock_deluxe"));

        // 4. Save Rate Mapping
        SaveRateMappingRequest rateMapReq = new SaveRateMappingRequest(List.of(
                new SaveRateMappingRequest.RateMappingItem(testRate.getId(), "mock_bar", "Best Available Rate (Flexible)", true)
        ));

        mockMvc.perform(put("/api/v1/properties/" + testProperty.getId() + "/channels/" + channelAccountId + "/rate-mappings")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rateMapReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rateMappings[0].channelRateId").value("mock_bar"));

        // 5. Trigger inventory change via Calendar Day Edit
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        UpdateCalendarDayRequest dayReq = new UpdateCalendarDayRequest(
                tomorrow,
                testRoom.getId(),
                7,
                testRate.getId(),
                550000L,
                false,
                1,
                null,
                false,
                false
        );

        mockMvc.perform(put("/api/v1/properties/" + testProperty.getId() + "/calendar/day")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dayReq)))
                .andExpect(status().isOk());

        // 6. Verify Outbox events were written in the same transaction
        List<OutboxEvent> pendingEvents = outboxEventRepository.findAll();
        assertThat(pendingEvents).isNotEmpty();
        assertThat(pendingEvents.stream().anyMatch(e -> "INVENTORY".equals(e.getAggregateType()))).isTrue();
        assertThat(pendingEvents.stream().anyMatch(e -> "RATE".equals(e.getAggregateType()))).isTrue();

        // 7. Execute Outbox publisher to deliver to channels
        int publishedCount = outboxPublisher.publishPendingBatch(10);
        assertThat(publishedCount).isGreaterThan(0);

        // 8. Verify Outbox events marked PUBLISHED
        List<OutboxEvent> publishedEvents = outboxEventRepository.findAll();
        assertThat(publishedEvents).allMatch(e -> e.getStatus() == OutboxStatus.PUBLISHED);

        // 9. Verify SyncJob records were created with SUCCESS status
        List<SyncJob> jobs = syncJobRepository.findAll();
        assertThat(jobs).isNotEmpty();
        assertThat(jobs.stream().anyMatch(j -> j.getStatus() == SyncJobStatus.SUCCESS)).isTrue();

        // 10. Verify ChannelAccount last_sync_at was updated
        ChannelAccount refreshedAcc = channelAccountRepository.findById(channelAccountId).orElseThrow();
        assertThat(refreshedAcc.getLastSyncAt()).isNotNull();
        assertThat(refreshedAcc.getStatus()).isEqualTo(ChannelStatus.CONNECTED);
    }

    @Test
    @DisplayName("Should handle channel sync failure with exponential backoff and manual retry")
    void testChannelSyncFailureAndRetry() throws Exception {
        // Create an account with name containing "fail_sync" to simulate OTA error
        ChannelCredentials creds = new ChannelCredentials();
        creds.setApiKey("test-key");

        CreateChannelAccountRequest accReq = new CreateChannelAccountRequest(
                ChannelType.MOCK,
                "fail_sync_account",
                creds
        );

        MvcResult accResult = mockMvc.perform(post("/api/v1/properties/" + testProperty.getId() + "/channels")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String channelAccountId = objectMapper.readTree(accResult.getResponse().getContentAsString()).get("id").asText();

        // Map room
        SaveRoomMappingRequest roomMapReq = new SaveRoomMappingRequest(List.of(
                new SaveRoomMappingRequest.RoomMappingItem(testRoom.getId(), "mock_deluxe", "Deluxe King Room", true)
        ));
        mockMvc.perform(put("/api/v1/properties/" + testProperty.getId() + "/channels/" + channelAccountId + "/room-mappings")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roomMapReq)));

        // Trigger inventory change
        LocalDate tomorrow = LocalDate.now().plusDays(2);
        UpdateCalendarDayRequest dayReq = new UpdateCalendarDayRequest(
                tomorrow, testRoom.getId(), 5, null, null, null, null, null, null, null);

        mockMvc.perform(put("/api/v1/properties/" + testProperty.getId() + "/calendar/day")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dayReq)));

        // Publish pending
        outboxPublisher.publishPendingBatch(10);

        // Check channel account status is ERROR
        ChannelAccount account = channelAccountRepository.findById(channelAccountId).orElseThrow();
        assertThat(account.getStatus()).isEqualTo(ChannelStatus.ERROR);
        assertThat(account.getErrorMessage()).contains("Simulated OTA upstream server timeout");

        // Check SyncJob recorded retryable attempt
        List<SyncJob> jobs = syncJobRepository.findAllByChannelAccountIdAndOrganizationIdOrderByCreatedAtDesc(channelAccountId, orgId);
        assertThat(jobs).isNotEmpty();
        SyncJob failedJob = jobs.get(0);
        assertThat(failedJob.getAttemptCount()).isEqualTo(1);
        assertThat(failedJob.getNextRetryAt()).isNotNull();

        // Manual retry via API
        mockMvc.perform(post("/api/v1/properties/" + testProperty.getId() + "/channels/" + channelAccountId + "/sync-jobs/" + failedJob.getId() + "/retry")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
