package com.hotel.channelmanager.channels.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.channelmanager.common.audit.AuditService;
import com.hotel.channelmanager.common.exception.ApiException;
import com.hotel.channelmanager.common.security.EncryptionService;
import com.hotel.channelmanager.common.tenant.TenantContext;
import com.hotel.channelmanager.channels.connector.*;
import com.hotel.channelmanager.channels.domain.*;
import com.hotel.channelmanager.channels.outbox.OutboxPublisher;
import com.hotel.channelmanager.channels.outbox.OutboxService;
import com.hotel.channelmanager.channels.sync.payload.RoomDateInventory;
import com.hotel.channelmanager.channels.web.dto.*;
import com.hotel.channelmanager.inventory.domain.DailyInventory;
import com.hotel.channelmanager.inventory.domain.DailyInventoryRepository;
import com.hotel.channelmanager.property.domain.Property;
import com.hotel.channelmanager.property.domain.PropertyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class ChannelAccountService {

    private final ChannelAccountRepository channelAccountRepository;
    private final ChannelRoomMappingRepository roomMappingRepository;
    private final ChannelRateMappingRepository rateMappingRepository;
    private final PropertyRepository propertyRepository;
    private final DailyInventoryRepository inventoryRepository;
    private final ChannelConnectorRegistry connectorRegistry;
    private final EncryptionService encryptionService;
    private final OutboxService outboxService;
    private final OutboxPublisher outboxPublisher;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public ChannelAccountService(
            ChannelAccountRepository channelAccountRepository,
            ChannelRoomMappingRepository roomMappingRepository,
            ChannelRateMappingRepository rateMappingRepository,
            PropertyRepository propertyRepository,
            DailyInventoryRepository inventoryRepository,
            ChannelConnectorRegistry connectorRegistry,
            EncryptionService encryptionService,
            OutboxService outboxService,
            OutboxPublisher outboxPublisher,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.channelAccountRepository = channelAccountRepository;
        this.roomMappingRepository = roomMappingRepository;
        this.rateMappingRepository = rateMappingRepository;
        this.propertyRepository = propertyRepository;
        this.inventoryRepository = inventoryRepository;
        this.connectorRegistry = connectorRegistry;
        this.encryptionService = encryptionService;
        this.outboxService = outboxService;
        this.outboxPublisher = outboxPublisher;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    public ConnectionResult testConnection(TestConnectionRequest request) {
        ChannelConnector connector = connectorRegistry.getRequiredConnector(request.channelType());
        return connector.testConnection(request.credentials());
    }

    @Transactional
    public ChannelAccountResponse createAccount(String propertyId, CreateChannelAccountRequest request) {
        String orgId = TenantContext.getOrganizationId();
        Property property = propertyRepository.findByIdAndOrganizationId(propertyId, orgId)
                .orElseThrow(() -> ApiException.notFound("The requested property was not found."));

        if (channelAccountRepository.existsByPropertyIdAndChannelTypeAndAccountNameAndOrganizationId(
                property.getId(), request.channelType(), request.accountName(), orgId)) {
            throw ApiException.badRequest("An account named '" + request.accountName() + "' for channel "
                    + request.channelType() + " already exists on this property.", "Please choose a different account name.");
        }

        String serializedCreds;
        try {
            serializedCreds = objectMapper.writeValueAsString(request.credentials());
        } catch (JsonProcessingException e) {
            throw ApiException.badRequest("Invalid credentials format.", "Please provide valid credentials JSON.");
        }

        String encryptedCreds = encryptionService.encrypt(serializedCreds);

        ChannelAccount account = new ChannelAccount(
                orgId,
                property.getId(),
                request.channelType(),
                request.accountName(),
                encryptedCreds
        );
        channelAccountRepository.save(account);

        auditService.recordAction("CREATE_CHANNEL_ACCOUNT", "ChannelAccount", account.getId(),
                "Connected " + account.getChannelType() + " account '" + account.getAccountName() + "'");

        return toResponse(account, 0, 0);
    }

    @Transactional(readOnly = true)
    public List<ChannelAccountResponse> getAccountsForProperty(String propertyId) {
        String orgId = TenantContext.getOrganizationId();
        propertyRepository.findByIdAndOrganizationId(propertyId, orgId)
                .orElseThrow(() -> ApiException.notFound("The requested property was not found."));

        List<ChannelAccount> accounts = channelAccountRepository.findAllByPropertyIdAndOrganizationId(propertyId, orgId);
        List<ChannelAccountResponse> responses = new ArrayList<>();

        for (ChannelAccount acc : accounts) {
            int roomCount = roomMappingRepository.findAllByChannelAccountIdAndOrganizationId(acc.getId(), orgId).size();
            int rateCount = rateMappingRepository.findAllByChannelAccountIdAndOrganizationId(acc.getId(), orgId).size();
            responses.add(toResponse(acc, roomCount, rateCount));
        }

        return responses;
    }

    @Transactional
    public ChannelAccountResponse updateAccount(String propertyId, String accountId, UpdateChannelAccountRequest request) {
        String orgId = TenantContext.getOrganizationId();
        ChannelAccount account = channelAccountRepository.findByIdAndOrganizationId(accountId, orgId)
                .orElseThrow(() -> ApiException.notFound("Channel account not found."));

        if (request.accountName() != null && !request.accountName().isBlank()) {
            account.setAccountName(request.accountName());
        }

        if (request.syncEnabled() != null) {
            account.setSyncEnabled(request.syncEnabled());
        }

        if (request.credentials() != null) {
            try {
                String serialized = objectMapper.writeValueAsString(request.credentials());
                account.setEncryptedCredentials(encryptionService.encrypt(serialized));
            } catch (JsonProcessingException e) {
                throw ApiException.badRequest("Invalid credentials format.", "Please provide valid credentials JSON.");
            }
        }

        channelAccountRepository.save(account);

        auditService.recordAction("UPDATE_CHANNEL_ACCOUNT", "ChannelAccount", account.getId(),
                "Updated " + account.getChannelType() + " account settings");

        int roomCount = roomMappingRepository.findAllByChannelAccountIdAndOrganizationId(account.getId(), orgId).size();
        int rateCount = rateMappingRepository.findAllByChannelAccountIdAndOrganizationId(account.getId(), orgId).size();
        return toResponse(account, roomCount, rateCount);
    }

    @Transactional
    public void deleteAccount(String propertyId, String accountId) {
        String orgId = TenantContext.getOrganizationId();
        ChannelAccount account = channelAccountRepository.findByIdAndOrganizationId(accountId, orgId)
                .orElseThrow(() -> ApiException.notFound("Channel account not found."));

        auditService.recordAction("DELETE_CHANNEL_ACCOUNT", "ChannelAccount", account.getId(),
                "Disconnected " + account.getChannelType() + " account '" + account.getAccountName() + "'");

        channelAccountRepository.delete(account);
    }

    @Transactional
    public void syncNow(String propertyId, String accountId) {
        String orgId = TenantContext.getOrganizationId();
        Property property = propertyRepository.findByIdAndOrganizationId(propertyId, orgId)
                .orElseThrow(() -> ApiException.notFound("Property not found."));

        ChannelAccount account = channelAccountRepository.findByIdAndOrganizationId(accountId, orgId)
                .orElseThrow(() -> ApiException.notFound("Channel account not found."));

        ZoneId zone = ZoneId.of(property.getTimezone());
        LocalDate today = LocalDate.now(zone);
        LocalDate end = today.plusDays(30);

        List<DailyInventory> inventory = inventoryRepository.findAllByPropertyIdAndDateBetweenOrderByDateAsc(propertyId, today, end);
        List<RoomDateInventory> invPayload = inventory.stream()
                .map(inv -> new RoomDateInventory(inv.getRoomTypeId(), inv.getDate(), inv.getAvailableUnits()))
                .toList();

        if (!invPayload.isEmpty()) {
            String idempotencyKey = "manual-sync-avail-" + account.getId() + "-" + System.currentTimeMillis();
            outboxService.recordEvent(
                    orgId,
                    propertyId,
                    "INVENTORY",
                    propertyId,
                    "MANUAL_SYNC",
                    invPayload,
                    idempotencyKey
            );
        }

        outboxPublisher.publishPendingBatch(10);
    }

    private ChannelAccountResponse toResponse(ChannelAccount acc, int roomCount, int rateCount) {
        return new ChannelAccountResponse(
                acc.getId(),
                acc.getPropertyId(),
                acc.getChannelType(),
                acc.getAccountName(),
                acc.getStatus(),
                acc.isSyncEnabled(),
                acc.getLastSyncAt(),
                acc.getErrorMessage(),
                roomCount,
                rateCount,
                acc.getCreatedAt()
        );
    }
}
