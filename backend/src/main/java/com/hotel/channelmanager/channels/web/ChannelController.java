package com.hotel.channelmanager.channels.web;

import com.hotel.channelmanager.channels.connector.ConnectionResult;
import com.hotel.channelmanager.channels.service.ChannelAccountService;
import com.hotel.channelmanager.channels.service.ChannelHealthService;
import com.hotel.channelmanager.channels.service.ChannelMappingService;
import com.hotel.channelmanager.channels.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Channels", description = "OTA Channel management, room/rate mapping, outbox sync, and health monitoring")
public class ChannelController {

    private final ChannelAccountService channelAccountService;
    private final ChannelMappingService channelMappingService;
    private final ChannelHealthService channelHealthService;

    public ChannelController(
            ChannelAccountService channelAccountService,
            ChannelMappingService channelMappingService,
            ChannelHealthService channelHealthService
    ) {
        this.channelAccountService = channelAccountService;
        this.channelMappingService = channelMappingService;
        this.channelHealthService = channelHealthService;
    }

    @PostMapping("/channels/test-connection")
    @Operation(summary = "Test credentials with an external channel")
    public ResponseEntity<ConnectionResult> testConnection(@Valid @RequestBody TestConnectionRequest request) {
        ConnectionResult result = channelAccountService.testConnection(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/properties/{propertyId}/channels")
    @Operation(summary = "List all channel accounts connected to a property")
    public ResponseEntity<List<ChannelAccountResponse>> getAccounts(@PathVariable String propertyId) {
        List<ChannelAccountResponse> accounts = channelAccountService.getAccountsForProperty(propertyId);
        return ResponseEntity.ok(accounts);
    }

    @PostMapping("/properties/{propertyId}/channels")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'PROPERTY_MANAGER')")
    @Operation(summary = "Connect a new OTA channel account with encrypted credentials")
    public ResponseEntity<ChannelAccountResponse> createAccount(
            @PathVariable String propertyId,
            @Valid @RequestBody CreateChannelAccountRequest request
    ) {
        ChannelAccountResponse response = channelAccountService.createAccount(propertyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/properties/{propertyId}/channels/{channelAccountId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'PROPERTY_MANAGER')")
    @Operation(summary = "Update channel account settings or credentials")
    public ResponseEntity<ChannelAccountResponse> updateAccount(
            @PathVariable String propertyId,
            @PathVariable String channelAccountId,
            @Valid @RequestBody UpdateChannelAccountRequest request
    ) {
        ChannelAccountResponse response = channelAccountService.updateAccount(propertyId, channelAccountId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/properties/{propertyId}/channels/{channelAccountId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')")
    @Operation(summary = "Disconnect and remove a channel account")
    public ResponseEntity<Map<String, String>> deleteAccount(
            @PathVariable String propertyId,
            @PathVariable String channelAccountId
    ) {
        channelAccountService.deleteAccount(propertyId, channelAccountId);
        return ResponseEntity.ok(Map.of("message", "Channel account disconnected successfully."));
    }

    @PostMapping("/properties/{propertyId}/channels/{channelAccountId}/sync-now")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'PROPERTY_MANAGER')")
    @Operation(summary = "Trigger immediate full sync to channel")
    public ResponseEntity<Map<String, String>> syncNow(
            @PathVariable String propertyId,
            @PathVariable String channelAccountId
    ) {
        channelAccountService.syncNow(propertyId, channelAccountId);
        return ResponseEntity.ok(Map.of("message", "Sync triggered. Sending inventory and rates to channel."));
    }

    @GetMapping("/properties/{propertyId}/channels/{channelAccountId}/mapping-options")
    @Operation(summary = "Fetch available internal and external room/rate mapping options")
    public ResponseEntity<MappingOptionsResponse> getMappingOptions(
            @PathVariable String propertyId,
            @PathVariable String channelAccountId
    ) {
        MappingOptionsResponse response = channelMappingService.getMappingOptions(propertyId, channelAccountId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/properties/{propertyId}/channels/{channelAccountId}/mappings")
    @Operation(summary = "Get current room and rate mappings for channel")
    public ResponseEntity<ChannelMappingsResponse> getMappings(
            @PathVariable String propertyId,
            @PathVariable String channelAccountId
    ) {
        ChannelMappingsResponse response = channelMappingService.getMappings(propertyId, channelAccountId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/properties/{propertyId}/channels/{channelAccountId}/room-mappings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'PROPERTY_MANAGER')")
    @Operation(summary = "Save room type mappings for channel")
    public ResponseEntity<ChannelMappingsResponse> saveRoomMappings(
            @PathVariable String propertyId,
            @PathVariable String channelAccountId,
            @Valid @RequestBody SaveRoomMappingRequest request
    ) {
        ChannelMappingsResponse response = channelMappingService.saveRoomMappings(propertyId, channelAccountId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/properties/{propertyId}/channels/{channelAccountId}/rate-mappings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'PROPERTY_MANAGER')")
    @Operation(summary = "Save rate plan mappings for channel")
    public ResponseEntity<ChannelMappingsResponse> saveRateMappings(
            @PathVariable String propertyId,
            @PathVariable String channelAccountId,
            @Valid @RequestBody SaveRateMappingRequest request
    ) {
        ChannelMappingsResponse response = channelMappingService.saveRateMappings(propertyId, channelAccountId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/properties/{propertyId}/channels/{channelAccountId}/sync-jobs")
    @Operation(summary = "List sync attempts and logs for channel account")
    public ResponseEntity<List<SyncJobResponse>> getSyncJobs(
            @PathVariable String propertyId,
            @PathVariable String channelAccountId
    ) {
        List<SyncJobResponse> jobs = channelHealthService.getSyncJobsForAccount(channelAccountId);
        return ResponseEntity.ok(jobs);
    }

    @PostMapping("/properties/{propertyId}/channels/{channelAccountId}/sync-jobs/{jobId}/retry")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'PROPERTY_MANAGER')")
    @Operation(summary = "Retry a failed sync job")
    public ResponseEntity<SyncJobResponse> retrySyncJob(
            @PathVariable String propertyId,
            @PathVariable String channelAccountId,
            @PathVariable String jobId
    ) {
        SyncJobResponse response = channelHealthService.retrySyncJob(propertyId, channelAccountId, jobId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/properties/{propertyId}/channels/health/sync-jobs")
    @Operation(summary = "Paginated sync jobs across all channels for property health view")
    public ResponseEntity<Page<SyncJobResponse>> getPropertySyncJobs(
            @PathVariable String propertyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<SyncJobResponse> jobs = channelHealthService.getSyncJobsForProperty(propertyId, page, size);
        return ResponseEntity.ok(jobs);
    }
}
