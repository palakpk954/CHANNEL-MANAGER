package com.hotel.channelmanager.channels.connector;

import java.time.LocalDate;

public record RateUpdate(
        String ratePlanId,
        String channelRateId,
        LocalDate date,
        long amountCents,
        String currency
) {}

