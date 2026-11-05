package com.hotel.channelmanager.common.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final String actionSuggestion;

    public ApiException(HttpStatus status, String errorCode, String message, String actionSuggestion) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.actionSuggestion = actionSuggestion;
    }

    public static ApiException badRequest(String message, String suggestion) {
        return new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message, suggestion);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message, "Please check your login credentials and try again.");
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", message, "Contact your organization administrator for access.");
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message, "Verify the identifier and try again.");
    }

    public static ApiException conflict(String message, String suggestion) {
        return new ApiException(HttpStatus.CONFLICT, "CONFLICT", message, suggestion);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getActionSuggestion() {
        return actionSuggestion;
    }
}

