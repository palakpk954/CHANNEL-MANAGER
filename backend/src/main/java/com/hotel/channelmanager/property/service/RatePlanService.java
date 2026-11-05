package com.hotel.channelmanager.property.service;

import com.hotel.channelmanager.common.audit.AuditService;
import com.hotel.channelmanager.common.exception.ApiException;
import com.hotel.channelmanager.common.tenant.TenantContext;
import com.hotel.channelmanager.property.domain.Property;
import com.hotel.channelmanager.property.domain.PropertyRepository;
import com.hotel.channelmanager.property.domain.RatePlan;
import com.hotel.channelmanager.property.domain.RatePlanRepository;
import com.hotel.channelmanager.property.domain.RoomTypeRepository;
import com.hotel.channelmanager.property.web.dto.CreateRatePlanRequest;
import com.hotel.channelmanager.property.web.dto.RatePlanDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RatePlanService {

    private final RatePlanRepository ratePlanRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final PropertyRepository propertyRepository;
    private final AuditService auditService;

    public RatePlanService(
            RatePlanRepository ratePlanRepository,
            RoomTypeRepository roomTypeRepository,
            PropertyRepository propertyRepository,
            AuditService auditService
    ) {
        this.ratePlanRepository = ratePlanRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.propertyRepository = propertyRepository;
        this.auditService = auditService;
    }

    @Transactional
    public RatePlanDto createRatePlan(String propertyId, CreateRatePlanRequest request, String actorId, String actorEmail) {
        String orgId = TenantContext.getOrganizationId();
        Property property = propertyRepository.findByIdAndOrganizationId(propertyId, orgId)
                .orElseThrow(() -> ApiException.notFound("The requested property was not found."));

        roomTypeRepository.findByIdAndPropertyIdAndOrganizationId(request.roomTypeId(), property.getId(), orgId)
                .orElseThrow(() -> ApiException.badRequest(
                        "The specified room type does not belong to this property.",
                        "Please select a valid room type."
                ));

        if (ratePlanRepository.existsByPropertyIdAndCodeAndOrganizationId(property.getId(), request.code().toUpperCase().trim(), orgId)) {
            throw ApiException.conflict(
                    "A price plan with the code '" + request.code() + "' already exists for this property.",
                    "Please choose a different code or edit the existing price plan."
            );
        }

        RatePlan ratePlan = new RatePlan(
                orgId,
                property.getId(),
                request.roomTypeId(),
                request.code().toUpperCase().trim(),
                request.name().trim(),
                request.mealPlan().toUpperCase().trim(),
                request.cancellationPolicy().toUpperCase().trim()
        );

        if (request.parentRatePlanId() != null && !request.parentRatePlanId().isBlank()) {
            ratePlan.setParentRatePlanId(request.parentRatePlanId());
            ratePlan.setDerivationType(request.derivationType());
            ratePlan.setDerivationValue(request.derivationValue());
        }

        RatePlan saved = ratePlanRepository.save(ratePlan);

        auditService.logChange(
                actorId,
                actorEmail,
                "RATE_PLAN",
                saved.getId(),
                "CREATE",
                null,
                saved.getName() + " (" + saved.getCode() + ")",
                null
        );

        return RatePlanDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<RatePlanDto> getRatePlans(String propertyId) {
        String orgId = TenantContext.getOrganizationId();
        return ratePlanRepository.findAllByPropertyIdAndOrganizationId(propertyId, orgId).stream()
                .filter(r -> "ACTIVE".equalsIgnoreCase(r.getStatus()))
                .map(RatePlanDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public RatePlanDto getRatePlanById(String propertyId, String ratePlanId) {
        String orgId = TenantContext.getOrganizationId();
        RatePlan ratePlan = ratePlanRepository.findByIdAndPropertyIdAndOrganizationId(ratePlanId, propertyId, orgId)
                .orElseThrow(() -> ApiException.notFound("The requested price plan was not found."));
        return RatePlanDto.fromEntity(ratePlan);
    }

    @Transactional
    public void archiveRatePlan(String propertyId, String ratePlanId, String actorId, String actorEmail) {
        String orgId = TenantContext.getOrganizationId();
        RatePlan ratePlan = ratePlanRepository.findByIdAndPropertyIdAndOrganizationId(ratePlanId, propertyId, orgId)
                .orElseThrow(() -> ApiException.notFound("The requested price plan was not found."));

        // Edge case 6: Cannot delete if bookings or mappings exist; archive instead
        ratePlan.setStatus("ARCHIVED");
        ratePlanRepository.save(ratePlan);

        auditService.logChange(
                actorId,
                actorEmail,
                "RATE_PLAN",
                ratePlan.getId(),
                "ARCHIVE",
                "ACTIVE",
                "ARCHIVED",
                null
        );
    }
}

