package com.hotel.channelmanager.channels.connector;

import java.time.LocalDate;

public record RestrictionUpdate(
        String ratePlanId,
        String channelRateId,
        LocalDate date,
        boolean stopSell,
        int minStay,
        Integer maxStay,
        boolean cta,
        boolean ctd
) {}

