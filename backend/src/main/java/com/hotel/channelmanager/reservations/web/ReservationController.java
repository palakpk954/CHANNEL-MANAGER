package com.hotel.channelmanager.reservations.web;

import com.hotel.channelmanager.reservations.service.ReservationService;
import com.hotel.channelmanager.reservations.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/properties/{propertyId}")
@Tag(name = "Reservations", description = "Unified inbox, inbound webhooks, atomic inventory decrement, and overbooking conflict engine")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/reservations/inbound")
    @Operation(summary = "Inbound reservation webhook from OTA channel or poller")
    public ResponseEntity<ReservationResponse> processInbound(
            @PathVariable String propertyId,
            @Valid @RequestBody InboundReservationRequest request
    ) {
        ReservationResponse response = reservationService.processInboundReservation(propertyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/reservations/manual")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'PROPERTY_MANAGER', 'REVENUE_MANAGER', 'RESERVATIONS_STAFF')")
    @Operation(summary = "Create manual direct / front-desk reservation")
    public ResponseEntity<ReservationResponse> createManual(
            @PathVariable String propertyId,
            @Valid @RequestBody ManualReservationRequest request
    ) {
        ReservationResponse response = reservationService.createManualReservation(propertyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/reservations")
    @Operation(summary = "Unified reservation inbox with pagination")
    public ResponseEntity<Page<ReservationResponse>> getReservations(
            @PathVariable String propertyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<ReservationResponse> responses = reservationService.getReservations(propertyId, page, size);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/reservations/{reservationId}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'PROPERTY_MANAGER', 'RESERVATIONS_STAFF')")
    @Operation(summary = "Cancel a reservation and restore room inventory")
    public ResponseEntity<ReservationResponse> cancelReservation(
            @PathVariable String propertyId,
            @PathVariable String reservationId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String reason = body != null ? body.get("reason") : "Guest request";
        ReservationResponse response = reservationService.cancelReservation(propertyId, reservationId, reason);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conflicts")
    @Operation(summary = "List unresolved overbooking conflicts")
    public ResponseEntity<List<ConflictResponse>> getConflicts(@PathVariable String propertyId) {
        List<ConflictResponse> conflicts = reservationService.getUnresolvedConflicts(propertyId);
        return ResponseEntity.ok(conflicts);
    }

    @PostMapping("/conflicts/{conflictId}/resolve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'PROPERTY_MANAGER', 'RESERVATIONS_STAFF')")
    @Operation(summary = "Resolve double booking conflict (Move, Contact, Cancel, Free Room)")
    public ResponseEntity<ConflictResponse> resolveConflict(
            @PathVariable String propertyId,
            @PathVariable String conflictId,
            @Valid @RequestBody ResolveConflictRequest request,
            @AuthenticationPrincipal String userId
    ) {
        ConflictResponse response = reservationService.resolveConflict(propertyId, conflictId, request, userId != null ? userId : "System");
        return ResponseEntity.ok(response);
    }
}

