package com.hotel.channelmanager.property.web;

import com.hotel.channelmanager.property.service.PropertyService;
import com.hotel.channelmanager.property.web.dto.CreatePropertyRequest;
import com.hotel.channelmanager.property.web.dto.PropertyDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/properties")
@Tag(name = "Properties", description = "Property configuration and management")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')")
    @Operation(summary = "Create a new property within the organization")
    public ResponseEntity<PropertyDto> createProperty(
            @Valid @RequestBody CreatePropertyRequest request,
            @AuthenticationPrincipal String userId
    ) {
        PropertyDto created = propertyService.createProperty(request, userId, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "List all properties for the active tenant")
    public ResponseEntity<List<PropertyDto>> getProperties() {
        return ResponseEntity.ok(propertyService.getProperties());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve property details by ID")
    public ResponseEntity<PropertyDto> getPropertyById(@PathVariable String id) {
        return ResponseEntity.ok(propertyService.getPropertyById(id));
    }
}

