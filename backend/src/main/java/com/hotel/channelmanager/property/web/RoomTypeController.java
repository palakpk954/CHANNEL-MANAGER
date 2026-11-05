package com.hotel.channelmanager.property.web;

import com.hotel.channelmanager.property.service.RoomTypeService;
import com.hotel.channelmanager.property.web.dto.CreateRoomTypeRequest;
import com.hotel.channelmanager.property.web.dto.RoomTypeDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/properties/{propertyId}/room-types")
@Tag(name = "Room Types", description = "Room category and inventory unit configuration")
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    public RoomTypeController(RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'PROPERTY_MANAGER')")
    @Operation(summary = "Add a new room type to property")
    public ResponseEntity<RoomTypeDto> createRoomType(
            @PathVariable String propertyId,
            @Valid @RequestBody CreateRoomTypeRequest request,
            @AuthenticationPrincipal String userId
    ) {
        RoomTypeDto created = roomTypeService.createRoomType(propertyId, request, userId, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "List room types for property")
    public ResponseEntity<List<RoomTypeDto>> getRoomTypes(@PathVariable String propertyId) {
        return ResponseEntity.ok(roomTypeService.getRoomTypes(propertyId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get room type details")
    public ResponseEntity<RoomTypeDto> getRoomTypeById(@PathVariable String propertyId, @PathVariable String id) {
        return ResponseEntity.ok(roomTypeService.getRoomTypeById(propertyId, id));
    }

    @PatchMapping("/{id}/units")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'PROPERTY_MANAGER')")
    @Operation(summary = "Update total room count with over-sold protection (BR-1.5)")
    public ResponseEntity<RoomTypeDto> updateTotalUnits(
            @PathVariable String propertyId,
            @PathVariable String id,
            @RequestBody Map<String, Integer> payload,
            @AuthenticationPrincipal String userId
    ) {
        Integer newUnits = payload.get("totalUnits");
        if (newUnits == null) {
            throw new IllegalArgumentException("totalUnits field is required");
        }
        return ResponseEntity.ok(roomTypeService.updateTotalUnits(propertyId, id, newUnits, userId, null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'PROPERTY_MANAGER')")
    @Operation(summary = "Archive room type safely (Edge case 6)")
    public ResponseEntity<Map<String, String>> archiveRoomType(
            @PathVariable String propertyId,
            @PathVariable String id,
            @AuthenticationPrincipal String userId
    ) {
        roomTypeService.archiveRoomType(propertyId, id, userId, null);
        return ResponseEntity.ok(Map.of("message", "Room type archived successfully."));
    }
}

