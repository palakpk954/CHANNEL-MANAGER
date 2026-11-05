package com.hotel.channelmanager.inventory.web;

import com.hotel.channelmanager.inventory.domain.ManualBlock;
import com.hotel.channelmanager.inventory.service.CalendarService;
import com.hotel.channelmanager.inventory.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/properties/{propertyId}/calendar")
@Tag(name = "Calendar & Distribution", description = "Availability, pricing, and restrictions distribution calendar")
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping
    @Operation(summary = "Fetch calendar grid for dates range (90 days x 50 rows under 2s)")
    public ResponseEntity<CalendarGridResponse> getCalendar(
            @PathVariable String propertyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        CalendarGridResponse response = calendarService.getCalendar(propertyId, from, to);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/day")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'PROPERTY_MANAGER', 'REVENUE_MANAGER')")
    @Operation(summary = "Update availability, price, or restrictions for a single day")
    public ResponseEntity<Map<String, String>> updateDay(
            @PathVariable String propertyId,
            @Valid @RequestBody UpdateCalendarDayRequest request,
            @AuthenticationPrincipal String userId
    ) {
        calendarService.updateSingleDay(propertyId, request, userId, null);
        return ResponseEntity.ok(Map.of("message", "Day updated successfully."));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'PROPERTY_MANAGER', 'REVENUE_MANAGER')")
    @Operation(summary = "Apply bulk updates across date range and weekdays with undo support (BR-2.12)")
    public ResponseEntity<Map<String, Object>> applyBulkUpdate(
            @PathVariable String propertyId,
            @Valid @RequestBody BulkUpdateRequest request,
            @AuthenticationPrincipal String userId
    ) {
        String historyId = calendarService.applyBulkUpdate(propertyId, request, userId, null);
        return ResponseEntity.ok(Map.of(
                "historyId", historyId,
                "message", "Saved. The updated prices are being sent to Booking.com and Agoda.",
                "undoEligible", true
        ));
    }

    @PostMapping("/undo/{historyId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'PROPERTY_MANAGER', 'REVENUE_MANAGER')")
    @Operation(summary = "Undo bulk update within 24 hours (BR-7.7)")
    public ResponseEntity<UndoResponse> undoBulkUpdate(
            @PathVariable String propertyId,
            @PathVariable String historyId,
            @AuthenticationPrincipal String userId
    ) {
        UndoResponse response = calendarService.undoBulkUpdate(propertyId, historyId, userId, null);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/blocks")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'PROPERTY_MANAGER', 'REVENUE_MANAGER', 'RESERVATIONS_STAFF')")
    @Operation(summary = "Hold rooms for maintenance or owner use (US-3.5 / BR-1.4)")
    public ResponseEntity<Map<String, Object>> createManualBlock(
            @PathVariable String propertyId,
            @Valid @RequestBody ManualBlockRequest request,
            @AuthenticationPrincipal String userId
    ) {
        ManualBlock block = calendarService.createManualBlock(propertyId, request, userId, null);
        return ResponseEntity.ok(Map.of(
                "id", block.getId(),
                "message", request.unitsBlocked() + " rooms are on hold for " + request.reason() + " from " + request.startDate() + " to " + request.endDate() + "."
        ));
    }
}

