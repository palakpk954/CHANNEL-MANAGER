package com.hotel.channelmanager.channels.connector;

import java.util.Map;

public record ConnectionResult(
        boolean success,
        String message,
        Map<String, Object> details
) {
    public static ConnectionResult ok(String message) {
        return new ConnectionResult(true, message, Map.of());
    }

    public static ConnectionResult error(String message) {
        return new ConnectionResult(false, message, Map.of());
    }
}

