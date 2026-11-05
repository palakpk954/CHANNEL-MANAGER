package com.hotel.channelmanager.property.web.dto;

import com.hotel.channelmanager.property.domain.RoomType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoomTypeDto(
        String id,
        String organizationId,
        String propertyId,
        String code,
        String name,
        int maxOccupancy,
        int totalUnits,
        String bedType,
        String amenities,
        String status
) {
    public static RoomTypeDto fromEntity(RoomType roomType) {
        return new RoomTypeDto(
                roomType.getId(),
                roomType.getOrganizationId(),
                roomType.getPropertyId(),
                roomType.getCode(),
                roomType.getName(),
                roomType.getMaxOccupancy(),
                roomType.getTotalUnits(),
                roomType.getBedType(),
                roomType.getAmenities(),
                roomType.getStatus()
        );
    }
}

