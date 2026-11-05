package com.hotel.channelmanager.property.service;

import com.hotel.channelmanager.common.audit.AuditService;
import com.hotel.channelmanager.common.exception.ApiException;
import com.hotel.channelmanager.common.tenant.TenantContext;
import com.hotel.channelmanager.property.domain.Property;
import com.hotel.channelmanager.property.domain.PropertyRepository;
import com.hotel.channelmanager.property.web.dto.CreatePropertyRequest;
import com.hotel.channelmanager.property.web.dto.PropertyDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final AuditService auditService;

    public PropertyService(PropertyRepository propertyRepository, AuditService auditService) {
        this.propertyRepository = propertyRepository;
        this.auditService = auditService;
    }

    @Transactional
    public PropertyDto createProperty(CreatePropertyRequest request, String actorId, String actorEmail) {
        String orgId = TenantContext.getOrganizationId();
        if (orgId == null) {
            throw ApiException.unauthorized("Authentication required.");
        }

        if (propertyRepository.existsByNameAndOrganizationId(request.name().trim(), orgId)) {
            throw ApiException.conflict(
                    "A property with the name '" + request.name() + "' already exists.",
                    "Please choose a different name or edit the existing property."
            );
        }

        Property property = new Property(
                orgId,
                request.name().trim(),
                request.address().trim(),
                request.timezone(),
                request.currency().toUpperCase().trim()
        );

        if (request.checkinTime() != null && !request.checkinTime().isBlank()) {
            property.setCheckinTime(request.checkinTime().trim());
        }
        if (request.checkoutTime() != null && !request.checkoutTime().isBlank()) {
            property.setCheckoutTime(request.checkoutTime().trim());
        }
        if (request.contactEmail() != null) {
            property.setContactEmail(request.contactEmail().trim());
        }
        if (request.contactPhone() != null) {
            property.setContactPhone(request.contactPhone().trim());
        }

        Property saved = propertyRepository.save(property);

        auditService.logChange(
                actorId,
                actorEmail,
                "PROPERTY",
                saved.getId(),
                "CREATE",
                null,
                saved.getName(),
                null
        );

        return PropertyDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<PropertyDto> getProperties() {
        String orgId = TenantContext.getOrganizationId();
        return propertyRepository.findAllByOrganizationId(orgId).stream()
                .map(PropertyDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public PropertyDto getPropertyById(String propertyId) {
        String orgId = TenantContext.getOrganizationId();
        Property property = propertyRepository.findByIdAndOrganizationId(propertyId, orgId)
                .orElseThrow(() -> ApiException.notFound("The requested property was not found."));
        return PropertyDto.fromEntity(property);
    }
}

