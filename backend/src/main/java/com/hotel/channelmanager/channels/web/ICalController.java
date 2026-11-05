package com.hotel.channelmanager.channels.web;

import com.hotel.channelmanager.channels.domain.ChannelAccount;
import com.hotel.channelmanager.channels.domain.ChannelAccountRepository;
import com.hotel.channelmanager.channels.domain.ChannelType;
import com.hotel.channelmanager.channels.service.ICalFeedService;
import com.hotel.channelmanager.channels.service.ICalSyncService;
import com.hotel.channelmanager.common.exception.ApiException;
import com.hotel.channelmanager.common.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/properties/{propertyId}")
@Tag(name = "iCal Two-Way Sync", description = "RFC 5545 calendar export feeds and external OTA calendar imports")
public class ICalController {

    private final ICalFeedService iCalFeedService;
    private final ICalSyncService iCalSyncService;
    private final ChannelAccountRepository channelAccountRepository;

    public ICalController(
            ICalFeedService iCalFeedService,
            ICalSyncService iCalSyncService,
            ChannelAccountRepository channelAccountRepository
    ) {
        this.iCalFeedService = iCalFeedService;
        this.iCalSyncService = iCalSyncService;
        this.channelAccountRepository = channelAccountRepository;
    }

    @GetMapping(value = "/ical/{roomTypeId}.ics", produces = "text/calendar;charset=UTF-8")
    @Operation(summary = "Export room type calendar feed in RFC 5545 .ics format for Airbnb/VRBO")
    public ResponseEntity<String> exportIcsFeed(
            @PathVariable String propertyId,
            @PathVariable String roomTypeId
    ) {
        String icsFeed = iCalFeedService.generateRoomTypeIcsFeed(propertyId, roomTypeId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/calendar; charset=UTF-8"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"room-" + roomTypeId + ".ics\"");
        headers.set(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");

        return new ResponseEntity<>(icsFeed, headers, HttpStatus.OK);
    }

    @PostMapping("/channels/{channelAccountId}/sync-ical")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'PROPERTY_MANAGER')")
    @Operation(summary = "Trigger manual sync of remote iCal calendar feed")
    public ResponseEntity<Map<String, Object>> triggerIcalSync(
            @PathVariable String propertyId,
            @PathVariable String channelAccountId
    ) {
        String orgId = TenantContext.getOrganizationId();
        ChannelAccount account = channelAccountRepository.findByIdAndOrganizationId(channelAccountId, orgId)
                .orElseThrow(() -> ApiException.notFound("iCal channel account not found."));

        if (account.getChannelType() != ChannelType.ICAL) {
            throw ApiException.badRequest("Account is not an iCal channel.", "Select a channel of type iCal.");
        }

        int imported = iCalSyncService.syncAccount(account);
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "importedEvents", imported,
                "message", "Successfully synced iCal feed with " + imported + " reservations/events."
        ));
    }

    @PostMapping("/channels/{channelAccountId}/import-ical-text")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'PROPERTY_MANAGER')")
    @Operation(summary = "Ingest raw .ics text content directly into the property")
    public ResponseEntity<Map<String, Object>> importIcsText(
            @PathVariable String propertyId,
            @PathVariable String channelAccountId,
            @RequestBody Map<String, String> body
    ) {
        String icsContent = body.get("icsContent");
        if (icsContent == null || icsContent.isBlank()) {
            throw ApiException.badRequest("Missing icsContent parameter.", "Please provide valid .ics file content.");
        }

        String orgId = TenantContext.getOrganizationId();
        ChannelAccount account = channelAccountRepository.findByIdAndOrganizationId(channelAccountId, orgId)
                .orElseThrow(() -> ApiException.notFound("iCal channel account not found."));

        int count = iCalSyncService.ingestIcsContent(account, icsContent);
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "importedEvents", count,
                "message", "Ingested " + count + " events from provided iCal content."
        ));
    }
}

