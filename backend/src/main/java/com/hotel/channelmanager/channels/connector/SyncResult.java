package com.hotel.channelmanager.channels.connector;

public record SyncResult(
        boolean success,
        int statusCode,
        String message,
        String externalReference,
        boolean retryable
) {
    public static SyncResult ok(String externalReference, String message) {
        return new SyncResult(true, 200, message, externalReference, false);
    }

    public static SyncResult failure(int statusCode, String message, boolean retryable) {
        return new SyncResult(false, statusCode, message, null, retryable);
    }
}

