package com.hotel.channelmanager.property.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePropertyRequest(
        @NotBlank(message = "Property name is required")
        @Size(max = 255, message = "Property name must not exceed 255 characters")
        String name,

        @NotBlank(message = "Address is required")
        @Size(max = 500, message = "Address must not exceed 500 characters")
        String address,

        @NotBlank(message = "Timezone is required")
        String timezone,

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String currency,

        String checkinTime,
        String checkoutTime,
        String contactEmail,
        String contactPhone
) {}

