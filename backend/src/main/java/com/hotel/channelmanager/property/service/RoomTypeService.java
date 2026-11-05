package com.hotel.channelmanager.property.service;

import com.hotel.channelmanager.common.audit.AuditService;
import com.hotel.channelmanager.common.exception.ApiException;
import com.hotel.channelmanager.common.tenant.TenantContext;
import com.hotel.channelmanager.property.domain.Property;
import com.hotel.channelmanager.property.domain.PropertyRepository;
import com.hotel.channelmanager.property.domain.RoomType;
import com.hotel.channelmanager.property.domain.RoomTypeRepository;
import com.hotel.channelmanager.property.web.dto.CreateRoomTypeRequest;
import com.hotel.channelmanager.property.web.dto.RoomTypeDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;
    private final PropertyRepository propertyRepository;
    private final AuditService auditService;

    public RoomTypeService(
            RoomTypeRepository roomTypeRepository,
            PropertyRepository propertyRepository,
            AuditService auditService
    ) {
        this.roomTypeRepository = roomTypeRepository;
        this.propertyRepository = propertyRepository;
        this.auditService = auditService;
    }

    @Transactional
    public RoomTypeDto createRoomType(String propertyId, CreateRoomTypeRequest request, String actorId, String actorEmail) {
        String orgId = TenantContext.getOrganizationId();
        Property property = propertyRepository.findByIdAndOrganizationId(propertyId, orgId)
                .orElseThrow(() -> ApiException.notFound("The requested property was not found."));

        if (roomTypeRepository.existsByPropertyIdAndCodeAndOrganizationId(property.getId(), request.code().toUpperCase().trim(), orgId)) {
            throw ApiException.conflict(
                    "A room type with the code '" + request.code() + "' already exists for this property.",
                    "Please choose a different code or update the existing room type."
            );
        }

        RoomType roomType = new RoomType(
                orgId,
                property.getId(),
                request.code().toUpperCase().trim(),
                request.name().trim(),
                request.maxOccupancy(),
                request.totalUnits(),
                request.bedType(),
                request.amenities()
        );

        RoomType saved = roomTypeRepository.save(roomType);

        auditService.logChange(
                actorId,
                actorEmail,
                "ROOM_TYPE",
                saved.getId(),
                "CREATE",
                null,
                saved.getName() + " (" + saved.getCode() + ")",
                null
        );

        return RoomTypeDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<RoomTypeDto> getRoomTypes(String propertyId) {
        String orgId = TenantContext.getOrganizationId();
        return roomTypeRepository.findAllByPropertyIdAndOrganizationId(propertyId, orgId).stream()
                .filter(r -> "ACTIVE".equalsIgnoreCase(r.getStatus()))
                .map(RoomTypeDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomTypeDto getRoomTypeById(String propertyId, String roomTypeId) {
        String orgId = TenantContext.getOrganizationId();
        RoomType roomType = roomTypeRepository.findByIdAndPropertyIdAndOrganizationId(roomTypeId, propertyId, orgId)
                .orElseThrow(() -> ApiException.notFound("The requested room type was not found."));
        return RoomTypeDto.fromEntity(roomType);
    }

    @Transactional
    public RoomTypeDto updateTotalUnits(String propertyId, String roomTypeId, int newTotalUnits, String actorId, String actorEmail) {
        String orgId = TenantContext.getOrganizationId();
        RoomType roomType = roomTypeRepository.findByIdAndPropertyIdAndOrganizationId(roomTypeId, propertyId, orgId)
                .orElseThrow(() -> ApiException.notFound("The requested room type was not found."));

        if (newTotalUnits < 1) {
            throw ApiException.badRequest("Total rooms must be at least 1.", "Please enter 1 or more.");
        }

        // BR-1.5: Total units can be changed at any time, but cannot be set lower than units already sold on any future night.
        // We will integrate sold units lookup when DailyInventory is attached; if a sold ceiling exists, we validate here.
        int oldUnits = roomType.getTotalUnits();
        roomType.setTotalUnits(newTotalUnits);
        RoomType updated = roomTypeRepository.save(roomType);

        auditService.logChange(
                actorId,
                actorEmail,
                "ROOM_TYPE",
                updated.getId(),
                "UPDATE_TOTAL_UNITS",
                oldUnits,
                newTotalUnits,
                null
        );

        return RoomTypeDto.fromEntity(updated);
    }

    @Transactional
    public void archiveRoomType(String propertyId, String roomTypeId, String actorId, String actorEmail) {
        String orgId = TenantContext.getOrganizationId();
        RoomType roomType = roomTypeRepository.findByIdAndPropertyIdAndOrganizationId(roomTypeId, propertyId, orgId)
                .orElseThrow(() -> ApiException.notFound("The requested room type was not found."));

        // Edge case 6: Cannot delete a room type with future bookings or channel mappings; archive instead
        roomType.setStatus("ARCHIVED");
        roomTypeRepository.save(roomType);

        auditService.logChange(
                actorId,
                actorEmail,
                "ROOM_TYPE",
                roomType.getId(),
                "ARCHIVE",
                "ACTIVE",
                "ARCHIVED",
                null
        );
    }
}

