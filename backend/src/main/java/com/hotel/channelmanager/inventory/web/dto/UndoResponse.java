package com.hotel.channelmanager.inventory.web.dto;

public record UndoResponse(
        String historyId,
        String message,
        boolean success
) {}

