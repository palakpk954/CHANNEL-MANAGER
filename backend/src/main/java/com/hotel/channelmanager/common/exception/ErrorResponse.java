package com.hotel.channelmanager.common.exception;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        String error,
        String message,
        String actionSuggestion,
        int status,
        Instant timestamp,
        Map<String, String> validationErrors
) {
    public static ErrorResponse of(String error, String message, String actionSuggestion, int status) {
        return new ErrorResponse(error, message, actionSuggestion, status, Instant.now(), null);
    }

    public static ErrorResponse validation(Map<String, String> errors) {
        return new ErrorResponse(
                "VALIDATION_ERROR",
                "One or more input fields failed validation.",
                "Please review the highlighted fields and resubmit.",
                400,
                Instant.now(),
                errors
        );
    }
}

