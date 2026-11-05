package com.hotel.channelmanager.channels.sync.payload;

import java.time.LocalDate;

public record RoomDateInventory(
        String roomTypeId,
        LocalDate date,
        int availableUnits
) {}

