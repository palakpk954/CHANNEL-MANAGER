package com.hotel.channelmanager.property.web;

import com.hotel.channelmanager.property.service.CsvImportService;
import com.hotel.channelmanager.property.service.RatePlanService;
import com.hotel.channelmanager.property.web.dto.CreateRatePlanRequest;
import com.hotel.channelmanager.property.web.dto.CsvImportResultDto;
import com.hotel.channelmanager.property.web.dto.RatePlanDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/properties/{propertyId}")
@Tag(name = "Price Plans & CSV Import", description = "Price plan definition and bulk CSV import")
public class RatePlanController {

    private final RatePlanService ratePlanService;
    private final CsvImportService csvImportService;

    public RatePlanController(RatePlanService ratePlanService, CsvImportService csvImportService) {
        this.ratePlanService = ratePlanService;
        this.csvImportService = csvImportService;
    }

    @PostMapping("/rate-plans")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'PROPERTY_MANAGER', 'REVENUE_MANAGER')")
    @Operation(summary = "Create a price plan for a room type")
    public ResponseEntity<RatePlanDto> createRatePlan(
            @PathVariable String propertyId,
            @Valid @RequestBody CreateRatePlanRequest request,
            @AuthenticationPrincipal String userId
    ) {
        RatePlanDto created = ratePlanService.createRatePlan(propertyId, request, userId, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/rate-plans")
    @Operation(summary = "List price plans for property")
    public ResponseEntity<List<RatePlanDto>> getRatePlans(@PathVariable String propertyId) {
        return ResponseEntity.ok(ratePlanService.getRatePlans(propertyId));
    }

    @GetMapping("/rate-plans/{id}")
    @Operation(summary = "Get price plan details")
    public ResponseEntity<RatePlanDto> getRatePlanById(@PathVariable String propertyId, @PathVariable String id) {
        return ResponseEntity.ok(ratePlanService.getRatePlanById(propertyId, id));
    }

    @DeleteMapping("/rate-plans/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'PROPERTY_MANAGER', 'REVENUE_MANAGER')")
    @Operation(summary = "Archive price plan safely (Edge case 6)")
    public ResponseEntity<Map<String, String>> archiveRatePlan(
            @PathVariable String propertyId,
            @PathVariable String id,
            @AuthenticationPrincipal String userId
    ) {
        ratePlanService.archiveRatePlan(propertyId, id, userId, null);
        return ResponseEntity.ok(Map.of("message", "Price plan archived successfully."));
    }

    @PostMapping(value = "/import-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'PROPERTY_MANAGER')")
    @Operation(summary = "Import rooms and price plans from CSV file (FR-2.6 / US-2.4)")
    public ResponseEntity<CsvImportResultDto> importCsvFile(
            @PathVariable String propertyId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal String userId
    ) throws IOException {
        CsvImportResultDto result = csvImportService.importCsv(propertyId, file.getInputStream(), userId, null);
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/import-csv-text", consumes = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'PROPERTY_MANAGER')")
    @Operation(summary = "Import rooms and price plans from CSV string content")
    public ResponseEntity<CsvImportResultDto> importCsvText(
            @PathVariable String propertyId,
            @RequestBody String csvContent,
            @AuthenticationPrincipal String userId
    ) {
        ByteArrayInputStream stream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        CsvImportResultDto result = csvImportService.importCsv(propertyId, stream, userId, null);
        return ResponseEntity.ok(result);
    }
}

