package com.hotel.channelmanager.channels.service;

import com.hotel.channelmanager.channels.connector.ICalConnector;
import com.hotel.channelmanager.channels.connector.InboundReservation;
import com.hotel.channelmanager.channels.domain.*;
import com.hotel.channelmanager.common.tenant.TenantContext;
import com.hotel.channelmanager.property.domain.RoomType;
import com.hotel.channelmanager.property.domain.RoomTypeRepository;
import com.hotel.channelmanager.reservations.service.ReservationService;
import com.hotel.channelmanager.reservations.web.dto.InboundReservationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Scheduled and on-demand synchronizer for iCal channels.
 * Periodically pulls external iCal feeds and updates local reservations.
 */
@Service
public class ICalSyncService {

    private static final Logger log = LoggerFactory.getLogger(ICalSyncService.class);

    private final ChannelAccountRepository channelAccountRepository;
    private final ChannelRoomMappingRepository channelRoomMappingRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final ICalConnector iCalConnector;
    private final ReservationService reservationService;
    private final SyncJobRepository syncJobRepository;

    public ICalSyncService(
            ChannelAccountRepository channelAccountRepository,
            ChannelRoomMappingRepository channelRoomMappingRepository,
            RoomTypeRepository roomTypeRepository,
            ICalConnector iCalConnector,
            ReservationService reservationService,
            SyncJobRepository syncJobRepository
    ) {
        this.channelAccountRepository = channelAccountRepository;
        this.channelRoomMappingRepository = channelRoomMappingRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.iCalConnector = iCalConnector;
        this.reservationService = reservationService;
        this.syncJobRepository = syncJobRepository;
    }

    /**
     * Poll external iCal calendars every 15 minutes to ingest new bookings.
     */
    @Scheduled(cron = "0 */15 * * * *")
    public void pollAllActiveIcalChannels() {
        log.info("Starting scheduled 15-minute iCal sync run...");
        List<ChannelAccount> icalAccounts = channelAccountRepository.findAllByChannelTypeAndSyncEnabledTrue(ChannelType.ICAL);
        for (ChannelAccount account : icalAccounts) {
            try {
                syncAccount(account);
            } catch (Exception e) {
                log.error("Failed to sync iCal account {}: {}", account.getId(), e.getMessage());
            }
        }
    }

    /**
     * Synchronize a specific iCal account on demand.
     */
    public int syncAccount(ChannelAccount account) {
        log.info("Syncing iCal account {} for property {}", account.getAccountName(), account.getPropertyId());
        String prevOrg = TenantContext.getOrganizationId();
        int importedCount = 0;

        try {
            TenantContext.setOrganizationId(account.getOrganizationId());

            // 1. Pull events from remote .ics URL
            List<InboundReservation> inboundList = iCalConnector.pullReservations(account, null);

            // 2. Resolve mapped internal room type
            String roomTypeId = null;
            List<ChannelRoomMapping> mappings = channelRoomMappingRepository
                    .findAllByChannelAccountIdAndOrganizationId(account.getId(), account.getOrganizationId());

            if (!mappings.isEmpty()) {
                roomTypeId = mappings.get(0).getRoomTypeId();
            } else {
                List<RoomType> propertyRooms = roomTypeRepository.findAllByPropertyIdAndOrganizationId(
                        account.getPropertyId(), account.getOrganizationId());
                if (!propertyRooms.isEmpty()) {
                    roomTypeId = propertyRooms.get(0).getId();
                }
            }

            if (roomTypeId == null) {
                log.warn("Cannot import iCal events: no room type configured for property {}", account.getPropertyId());
                return 0;
            }

            // 3. Ingest each reservation through standard reservation pipeline
            for (InboundReservation res : inboundList) {
                try {
                    InboundReservationRequest req = new InboundReservationRequest(
                            "ICAL",
                            res.channelReservationId(),
                            res.guestName(),
                            res.guestEmail(),
                            res.guestPhone(),
                            roomTypeId,
                            null,
                            res.checkInDate(),
                            res.checkOutDate(),
                            res.totalUnits(),
                            res.totalAmountCents(),
                            res.currency(),
                            "Imported via iCal sync from " + account.getAccountName()
                    );

                    reservationService.processInboundReservation(account.getPropertyId(), req);
                    importedCount++;
                } catch (Exception e) {
                    log.error("Failed to ingest iCal reservation {}: {}", res.channelReservationId(), e.getMessage());
                }
            }

            account.setLastSyncAt(Instant.now());
            account.setErrorMessage(null);
            channelAccountRepository.save(account);

            SyncJob job = new SyncJob(
                    account.getOrganizationId(),
                    account.getPropertyId(),
                    account.getId(),
                    SyncJobType.RESERVATION_PULL,
                    "Remote URL: " + account.getAccountName()
            );
            job.markSuccess("Imported/verified " + importedCount + " events");
            syncJobRepository.save(job);

            return importedCount;
        } finally {
            if (prevOrg != null) {
                TenantContext.setOrganizationId(prevOrg);
            } else {
                TenantContext.clear();
            }
        }
    }

    /**
     * Ingest manual or mock iCal content directly (e.g. for testing or direct feed submission).
     */
    public int ingestIcsContent(ChannelAccount account, String icsContent) {
        String prevOrg = TenantContext.getOrganizationId();
        int count = 0;
        try {
            TenantContext.setOrganizationId(account.getOrganizationId());
            List<InboundReservation> inboundList = iCalConnector.parseIcs(icsContent);

            String roomTypeId = null;
            List<ChannelRoomMapping> mappings = channelRoomMappingRepository
                    .findAllByChannelAccountIdAndOrganizationId(account.getId(), account.getOrganizationId());

            if (!mappings.isEmpty()) {
                roomTypeId = mappings.get(0).getRoomTypeId();
            } else {
                List<RoomType> rooms = roomTypeRepository.findAllByPropertyIdAndOrganizationId(
                        account.getPropertyId(), account.getOrganizationId());
                if (!rooms.isEmpty()) roomTypeId = rooms.get(0).getId();
            }

            if (roomTypeId == null) return 0;

            for (InboundReservation res : inboundList) {
                InboundReservationRequest req = new InboundReservationRequest(
                        "ICAL",
                        res.channelReservationId(),
                        res.guestName(),
                        res.guestEmail(),
                        res.guestPhone(),
                        roomTypeId,
                        null,
                        res.checkInDate(),
                        res.checkOutDate(),
                        res.totalUnits(),
                        res.totalAmountCents(),
                        res.currency(),
                        "Direct iCal feed ingestion"
                );
                reservationService.processInboundReservation(account.getPropertyId(), req);
                count++;
            }
            return count;
        } finally {
            if (prevOrg != null) {
                TenantContext.setOrganizationId(prevOrg);
            } else {
                TenantContext.clear();
            }
        }
    }
}

