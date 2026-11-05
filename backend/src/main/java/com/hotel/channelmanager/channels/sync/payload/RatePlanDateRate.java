package com.hotel.channelmanager.channels.sync.payload;

import java.time.LocalDate;

public record RatePlanDateRate(
        String ratePlanId,
        LocalDate date,
        long amountCents,
        String currency
) {}

