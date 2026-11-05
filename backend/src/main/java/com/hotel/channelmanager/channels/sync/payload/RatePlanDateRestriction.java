package com.hotel.channelmanager.channels.sync.payload;

import java.time.LocalDate;

public record RatePlanDateRestriction(
        String ratePlanId,
        LocalDate date,
        boolean stopSell,
        int minStay,
        Integer maxStay,
        boolean cta,
        boolean ctd
) {}

