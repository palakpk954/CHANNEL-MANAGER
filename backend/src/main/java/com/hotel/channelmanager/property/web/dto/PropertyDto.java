package com.hotel.channelmanager.property.web.dto;

import com.hotel.channelmanager.property.domain.Property;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PropertyDto(
        String id,
        String organizationId,
        String name,
        String address,
        String timezone,
        String currency,
        String checkinTime,
        String checkoutTime,
        String contactEmail,
        String contactPhone,
        String status
) {
    public static PropertyDto fromEntity(Property property) {
        return new PropertyDto(
                property.getId(),
                property.getOrganizationId(),
                property.getName(),
                property.getAddress(),
                property.getTimezone(),
                property.getCurrency(),
                property.getCheckinTime(),
                property.getCheckoutTime(),
                property.getContactEmail(),
                property.getContactPhone(),
                property.getStatus()
        );
    }
}

