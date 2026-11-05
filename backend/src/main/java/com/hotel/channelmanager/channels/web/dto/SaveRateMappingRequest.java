package com.hotel.channelmanager.channels.web.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SaveRateMappingRequest(
        @NotEmpty(message = "At least one rate mapping must be provided")
        List<RateMappingItem> mappings
) {
    public record RateMappingItem(
            String ratePlanId,
            String channelRateId,
            String channelRateName,
            boolean active
    ) {}
}

