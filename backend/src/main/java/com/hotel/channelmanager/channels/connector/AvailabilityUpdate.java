package com.hotel.channelmanager.channels.connector;

import java.time.LocalDate;

public record AvailabilityUpdate(
        String roomTypeId,
        String channelRoomId,
        LocalDate date,
        int availableUnits
) {}

