package com.hotel.channelmanager.channels.service;

import com.hotel.channelmanager.common.audit.AuditService;
import com.hotel.channelmanager.common.exception.ApiException;
import com.hotel.channelmanager.common.tenant.TenantContext;
import com.hotel.channelmanager.channels.connector.ChannelConnector;
import com.hotel.channelmanager.channels.connector.ChannelConnectorRegistry;
import com.hotel.channelmanager.channels.connector.MappingOptions;
import com.hotel.channelmanager.channels.domain.*;
import com.hotel.channelmanager.channels.web.dto.*;
import com.hotel.channelmanager.property.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ChannelMappingService {

    private final ChannelAccountRepository channelAccountRepository;
    private final ChannelRoomMappingRepository roomMappingRepository;
    private final ChannelRateMappingRepository rateMappingRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RatePlanRepository ratePlanRepository;
    private final ChannelConnectorRegistry connectorRegistry;
    private final AuditService auditService;

    public ChannelMappingService(
            ChannelAccountRepository channelAccountRepository,
            ChannelRoomMappingRepository roomMappingRepository,
            ChannelRateMappingRepository rateMappingRepository,
            RoomTypeRepository roomTypeRepository,
            RatePlanRepository ratePlanRepository,
            ChannelConnectorRegistry connectorRegistry,
            AuditService auditService
    ) {
        this.channelAccountRepository = channelAccountRepository;
        this.roomMappingRepository = roomMappingRepository;
        this.rateMappingRepository = rateMappingRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.ratePlanRepository = ratePlanRepository;
        this.connectorRegistry = connectorRegistry;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public MappingOptionsResponse getMappingOptions(String propertyId, String accountId) {
        String orgId = TenantContext.getOrganizationId();
        ChannelAccount account = channelAccountRepository.findByIdAndOrganizationId(accountId, orgId)
                .orElseThrow(() -> ApiException.notFound("Channel account not found."));

        ChannelConnector connector = connectorRegistry.getRequiredConnector(account.getChannelType());
        MappingOptions extOptions = connector.fetchMappingOptions(account);

        List<RoomType> internalRooms = roomTypeRepository.findAllByPropertyIdAndOrganizationId(propertyId, orgId)
                .stream().filter(r -> "ACTIVE".equalsIgnoreCase(r.getStatus())).toList();

        List<RatePlan> internalRates = ratePlanRepository.findAllByPropertyIdAndOrganizationId(propertyId, orgId)
                .stream().filter(rp -> "ACTIVE".equalsIgnoreCase(rp.getStatus())).toList();

        List<MappingOptionsResponse.InternalRoomOption> roomOptions = internalRooms.stream()
                .map(r -> new MappingOptionsResponse.InternalRoomOption(r.getId(), r.getName(), r.getCode(), r.getTotalUnits()))
                .toList();

        List<MappingOptionsResponse.InternalRateOption> rateOptions = internalRates.stream()
                .map(rp -> new MappingOptionsResponse.InternalRateOption(rp.getId(), rp.getName(), rp.getCode(), rp.getRoomTypeId()))
                .toList();

        List<MappingOptionsResponse.ChannelRoomOption> chRooms = extOptions.roomOptions().stream()
                .map(cr -> new MappingOptionsResponse.ChannelRoomOption(cr.channelRoomId(), cr.channelRoomName()))
                .toList();

        List<MappingOptionsResponse.ChannelRateOption> chRates = extOptions.rateOptions().stream()
                .map(cr -> new MappingOptionsResponse.ChannelRateOption(cr.channelRateId(), cr.channelRateName(), cr.channelRoomId()))
                .toList();

        return new MappingOptionsResponse(roomOptions, rateOptions, chRooms, chRates);
    }

    @Transactional
    public ChannelMappingsResponse getMappings(String propertyId, String accountId) {
        String orgId = TenantContext.getOrganizationId();
        channelAccountRepository.findByIdAndOrganizationId(accountId, orgId)
                .orElseThrow(() -> ApiException.notFound("Channel account not found."));

        List<ChannelRoomMapping> roomMappings = roomMappingRepository.findAllByChannelAccountIdAndOrganizationId(accountId, orgId);
        List<ChannelRateMapping> rateMappings = rateMappingRepository.findAllByChannelAccountIdAndOrganizationId(accountId, orgId);

        List<ChannelMappingsResponse.RoomMappingView> roomViews = roomMappings.stream()
                .map(m -> new ChannelMappingsResponse.RoomMappingView(m.getId(), m.getRoomTypeId(), m.getChannelRoomId(), m.getChannelRoomName(), m.isActive()))
                .toList();

        List<ChannelMappingsResponse.RateMappingView> rateViews = rateMappings.stream()
                .map(m -> new ChannelMappingsResponse.RateMappingView(m.getId(), m.getRatePlanId(), m.getChannelRateId(), m.getChannelRateName(), m.isActive()))
                .toList();

        return new ChannelMappingsResponse(accountId, roomViews, rateViews);
    }

    @Transactional
    public ChannelMappingsResponse saveRoomMappings(String propertyId, String accountId, SaveRoomMappingRequest request) {
        String orgId = TenantContext.getOrganizationId();
        ChannelAccount account = channelAccountRepository.findByIdAndOrganizationId(accountId, orgId)
                .orElseThrow(() -> ApiException.notFound("Channel account not found."));

        for (SaveRoomMappingRequest.RoomMappingItem item : request.mappings()) {
            roomTypeRepository.findByIdAndOrganizationId(item.roomTypeId(), orgId)
                    .orElseThrow(() -> ApiException.notFound("Room type " + item.roomTypeId() + " not found."));

            Optional<ChannelRoomMapping> existingOpt = roomMappingRepository.findByChannelAccountIdAndRoomTypeIdAndOrganizationId(
                    accountId, item.roomTypeId(), orgId);

            if (existingOpt.isPresent()) {
                ChannelRoomMapping existing = existingOpt.get();
                existing.setChannelRoomId(item.channelRoomId());
                existing.setChannelRoomName(item.channelRoomName());
                existing.setActive(item.active());
                roomMappingRepository.save(existing);
            } else {
                ChannelRoomMapping mapping = new ChannelRoomMapping(
                        orgId,
                        accountId,
                        item.roomTypeId(),
                        item.channelRoomId(),
                        item.channelRoomName()
                );
                mapping.setActive(item.active());
                roomMappingRepository.save(mapping);
            }
        }

        auditService.recordAction("UPDATE_ROOM_MAPPINGS", "ChannelAccount", accountId,
                "Updated " + request.mappings().size() + " room mappings for " + account.getChannelType());

        return getMappings(propertyId, accountId);
    }

    @Transactional
    public ChannelMappingsResponse saveRateMappings(String propertyId, String accountId, SaveRateMappingRequest request) {
        String orgId = TenantContext.getOrganizationId();
        ChannelAccount account = channelAccountRepository.findByIdAndOrganizationId(accountId, orgId)
                .orElseThrow(() -> ApiException.notFound("Channel account not found."));

        for (SaveRateMappingRequest.RateMappingItem item : request.mappings()) {
            ratePlanRepository.findByIdAndOrganizationId(item.ratePlanId(), orgId)
                    .orElseThrow(() -> ApiException.notFound("Rate plan " + item.ratePlanId() + " not found."));

            Optional<ChannelRateMapping> existingOpt = rateMappingRepository.findByChannelAccountIdAndRatePlanIdAndOrganizationId(
                    accountId, item.ratePlanId(), orgId);

            if (existingOpt.isPresent()) {
                ChannelRateMapping existing = existingOpt.get();
                existing.setChannelRateId(item.channelRateId());
                existing.setChannelRateName(item.channelRateName());
                existing.setActive(item.active());
                rateMappingRepository.save(existing);
            } else {
                ChannelRateMapping mapping = new ChannelRateMapping(
                        orgId,
                        accountId,
                        item.ratePlanId(),
                        item.channelRateId(),
                        item.channelRateName()
                );
                mapping.setActive(item.active());
                rateMappingRepository.save(mapping);
            }
        }

        auditService.recordAction("UPDATE_RATE_MAPPINGS", "ChannelAccount", accountId,
                "Updated " + request.mappings().size() + " rate mappings for " + account.getChannelType());

        return getMappings(propertyId, accountId);
    }
}

