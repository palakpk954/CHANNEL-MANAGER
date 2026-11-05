package com.hotel.channelmanager.reservations.service;

import com.hotel.channelmanager.channels.domain.ChannelRoomMapping;
import com.hotel.channelmanager.channels.domain.ChannelRoomMappingRepository;
import com.hotel.channelmanager.channels.outbox.OutboxPublisher;
import com.hotel.channelmanager.channels.outbox.OutboxService;
import com.hotel.channelmanager.channels.sync.payload.RoomDateInventory;
import com.hotel.channelmanager.common.audit.AuditService;
import com.hotel.channelmanager.common.exception.ApiException;
import com.hotel.channelmanager.common.tenant.TenantContext;
import com.hotel.channelmanager.inventory.domain.DailyInventory;
import com.hotel.channelmanager.inventory.domain.DailyInventoryRepository;
import com.hotel.channelmanager.property.domain.*;
import com.hotel.channelmanager.reservations.domain.*;
import com.hotel.channelmanager.reservations.web.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository reservationRepository;
    private final ConflictRepository conflictRepository;
    private final DailyInventoryRepository inventoryRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RatePlanRepository ratePlanRepository;
    private final PropertyRepository propertyRepository;
    private final ChannelRoomMappingRepository channelRoomMappingRepository;
    private final OutboxService outboxService;
    private final OutboxPublisher outboxPublisher;
    private final AuditService auditService;
    private final TransactionTemplate transactionTemplate;
    private final Map<String, Object> roomLocks = new ConcurrentHashMap<>();

    public ReservationService(
            ReservationRepository reservationRepository,
            ConflictRepository conflictRepository,
            DailyInventoryRepository inventoryRepository,
            RoomTypeRepository roomTypeRepository,
            RatePlanRepository ratePlanRepository,
            PropertyRepository propertyRepository,
            ChannelRoomMappingRepository channelRoomMappingRepository,
            OutboxService outboxService,
            OutboxPublisher outboxPublisher,
            AuditService auditService,
            PlatformTransactionManager transactionManager
    ) {
        this.reservationRepository = reservationRepository;
        this.conflictRepository = conflictRepository;
        this.inventoryRepository = inventoryRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.ratePlanRepository = ratePlanRepository;
        this.propertyRepository = propertyRepository;
        this.channelRoomMappingRepository = channelRoomMappingRepository;
        this.outboxService = outboxService;
        this.outboxPublisher = outboxPublisher;
        this.auditService = auditService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Process inbound reservation from an OTA channel webhook or poller.
     * Guaranteed idempotent and protected against overbooking via ordered pessimistic locks.
     */
    public ReservationResponse processInboundReservation(String propertyId, InboundReservationRequest request) {
        String orgId = TenantContext.getOrganizationId();

        Property property = propertyRepository.findByIdAndOrganizationId(propertyId, orgId)
                .orElseThrow(() -> ApiException.notFound("The requested property was not found."));

        // Resolve room type mapping
        String roomTypeId = resolveRoomTypeId(orgId, property.getId(), request.channelRoomId());
        RoomType roomType = roomTypeRepository.findByIdAndOrganizationId(roomTypeId, orgId)
                .orElseThrow(() -> ApiException.notFound("Room type could not be resolved."));

        int units = Math.max(1, request.unitsBooked());
        LocalDate checkIn = request.checkInDate();
        LocalDate checkOut = request.checkOutDate();

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw ApiException.badRequest("Check-out date must be after check-in date.", "Please check reservation stay dates.");
        }

        LocalDate lastNight = checkOut.minusDays(1);

        Object roomLock = roomLocks.computeIfAbsent(orgId + ":" + roomTypeId, k -> new Object());
        synchronized (roomLock) {
            return transactionTemplate.execute(status -> {
                // 1. Idempotency Check inside lock & tx boundary (FR-6.3 / BR-3.4)
                Optional<Reservation> existingOpt = reservationRepository.findByOrganizationIdAndChannelTypeAndChannelReservationId(
                        orgId, request.channelType(), request.channelReservationId());

                if (existingOpt.isPresent()) {
                    log.info("Duplicate inbound reservation ignored (idempotent): channel={}, ref={}",
                            request.channelType(), request.channelReservationId());
                    return toResponse(existingOpt.get());
                }

                // 2. Ensure DailyInventory records exist for all dates before acquiring lock
                LocalDate d = checkIn;
                while (!d.isAfter(lastNight)) {
                    if (inventoryRepository.findByRoomTypeIdAndDate(roomTypeId, d).isEmpty()) {
                        DailyInventory initInv = new DailyInventory(orgId, property.getId(), roomTypeId, d, roomType.getTotalUnits());
                        inventoryRepository.save(initInv);
                    }
                    d = d.plusDays(1);
                }

                // 3. Acquire ordered pessimistic row locks (ADR-005 & BR-5.1)
                List<DailyInventory> inventoryNights = inventoryRepository.findOrderedForUpdate(roomTypeId, checkIn, lastNight);

                // 4. Overbooking check across all stay nights (FR-5.3 & BR-5.2)
                boolean hasShortage = false;
                LocalDate conflictDate = null;

                for (DailyInventory inv : inventoryNights) {
                    if (inv.getAvailableUnits() < units) {
                        hasShortage = true;
                        conflictDate = inv.getDate();
                        break;
                    }
                }

                if (hasShortage) {
                    log.warn("Overbooking detected! Room {} has insufficient availability on {}. Creating Conflict record.",
                            roomType.getName(), conflictDate);

                    // Record as CONFLICT status - NEVER drop availability below 0
                    Reservation conflictRes = new Reservation(
                            orgId,
                            property.getId(),
                            null,
                            request.channelType(),
                            request.channelReservationId(),
                            ReservationStatus.CONFLICT,
                            request.guestName(),
                            request.guestEmail(),
                            request.guestPhone(),
                            roomTypeId,
                            null,
                            checkIn,
                            checkOut,
                            units,
                            request.totalAmountCents(),
                            request.currency() != null ? request.currency() : property.getCurrency(),
                            request.specialRequests()
                    );
                    reservationRepository.save(conflictRes);

                    // Create Conflict entry for staff resolution
                    Conflict conflict = new Conflict(
                            orgId,
                            property.getId(),
                            conflictRes.getId(),
                            roomTypeId,
                            conflictDate,
                            "Double booking on " + conflictDate + ": room type " + roomType.getName() + " was already sold out."
                    );
                    conflictRepository.save(conflict);

                    // Outbox: Re-push zero availability for this date to all other channels immediately
                    List<RoomDateInventory> zeroUpdate = List.of(new RoomDateInventory(roomTypeId, conflictDate, 0));
                    outboxService.recordEvent(
                            orgId,
                            property.getId(),
                            "INVENTORY",
                            conflictRes.getId(),
                            "OVERBOOKING_ZERO_AVAILABILITY_PUSH",
                            zeroUpdate,
                            "overbook-" + conflictRes.getId()
                    );

                    auditService.recordAction("OVERBOOKING_CONFLICT_DETECTED", "Reservation", conflictRes.getId(),
                            "Double booking flagged for " + request.guestName() + " on " + conflictDate);

                    return toResponse(conflictRes);
                }

                // 5. Availability is confirmed - Atomic inventory decrement
                List<RoomDateInventory> updatedInvList = new ArrayList<>();
                for (DailyInventory inv : inventoryNights) {
                    inv.setSoldUnits(inv.getSoldUnits() + units);
                    inv.recalculateAvailability();
                    inventoryRepository.save(inv);
                    updatedInvList.add(new RoomDateInventory(inv.getRoomTypeId(), inv.getDate(), inv.getAvailableUnits()));
                }

                Reservation confirmed = new Reservation(
                        orgId,
                        property.getId(),
                        null,
                        request.channelType(),
                        request.channelReservationId(),
                        ReservationStatus.CONFIRMED,
                        request.guestName(),
                        request.guestEmail(),
                        request.guestPhone(),
                        roomTypeId,
                        null,
                        checkIn,
                        checkOut,
                        units,
                        request.totalAmountCents(),
                        request.currency() != null ? request.currency() : property.getCurrency(),
                        request.specialRequests()
                );
                reservationRepository.save(confirmed);

                // 6. Write transactional outbox event so all other channels receive reduced availability
                outboxService.recordEvent(
                        orgId,
                        property.getId(),
                        "INVENTORY",
                        confirmed.getId(),
                        "RESERVATION_AVAILABILITY_DECREMENT",
                        updatedInvList,
                        "res-" + confirmed.getId()
                );

                auditService.recordAction("INBOUND_RESERVATION_CONFIRMED", "Reservation", confirmed.getId(),
                        "Confirmed " + units + " room(s) for " + request.guestName() + " from " + request.channelType());

                return toResponse(confirmed);
            });
        }
    }

    /**
     * Create manual reservation (e.g. front-desk walk-in or telephone booking).
     */
    public ReservationResponse createManualReservation(String propertyId, ManualReservationRequest request) {
        String orgId = TenantContext.getOrganizationId();
        Property property = propertyRepository.findByIdAndOrganizationId(propertyId, orgId)
                .orElseThrow(() -> ApiException.notFound("The requested property was not found."));

        RoomType roomType = roomTypeRepository.findByIdAndOrganizationId(request.roomTypeId(), orgId)
                .orElseThrow(() -> ApiException.notFound("Room type not found."));

        int units = Math.max(1, request.unitsBooked());
        LocalDate checkIn = request.checkInDate();
        LocalDate checkOut = request.checkOutDate();

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw ApiException.badRequest("Check-out date must be after check-in date.", "Please choose valid stay dates.");
        }

        LocalDate lastNight = checkOut.minusDays(1);

        Object roomLock = roomLocks.computeIfAbsent(orgId + ":" + roomType.getId(), k -> new Object());
        synchronized (roomLock) {
            return transactionTemplate.execute(status -> {
                // Ensure records exist
                LocalDate d = checkIn;
                while (!d.isAfter(lastNight)) {
                    if (inventoryRepository.findByRoomTypeIdAndDate(roomType.getId(), d).isEmpty()) {
                        DailyInventory initInv = new DailyInventory(orgId, property.getId(), roomType.getId(), d, roomType.getTotalUnits());
                        inventoryRepository.save(initInv);
                    }
                    d = d.plusDays(1);
                }

                // Ordered lock
                List<DailyInventory> inventoryNights = inventoryRepository.findOrderedForUpdate(roomType.getId(), checkIn, lastNight);

                for (DailyInventory inv : inventoryNights) {
                    if (inv.getAvailableUnits() < units) {
                        throw ApiException.badRequest(
                                "Not enough rooms available on " + inv.getDate() + " (" + inv.getAvailableUnits() + " left, " + units + " requested).",
                                "Please choose different dates or select an alternate room type."
                        );
                    }
                }

                List<RoomDateInventory> updatedInvList = new ArrayList<>();
                for (DailyInventory inv : inventoryNights) {
                    inv.setSoldUnits(inv.getSoldUnits() + units);
                    inv.recalculateAvailability();
                    inventoryRepository.save(inv);
                    updatedInvList.add(new RoomDateInventory(inv.getRoomTypeId(), inv.getDate(), inv.getAvailableUnits()));
                }

                String directRef = "DIR-" + System.currentTimeMillis();
                Reservation reservation = new Reservation(
                        orgId,
                        property.getId(),
                        null,
                        "DIRECT",
                        directRef,
                        ReservationStatus.CONFIRMED,
                        request.guestName().trim(),
                        request.guestEmail(),
                        request.guestPhone(),
                        roomType.getId(),
                        request.ratePlanId(),
                        checkIn,
                        checkOut,
                        units,
                        request.totalAmountCents(),
                        request.currency() != null ? request.currency() : property.getCurrency(),
                        request.specialRequests()
                );
                reservationRepository.save(reservation);

                outboxService.recordEvent(
                        orgId,
                        property.getId(),
                        "INVENTORY",
                        reservation.getId(),
                        "MANUAL_RESERVATION_DECREMENT",
                        updatedInvList,
                        "res-" + reservation.getId()
                );

                auditService.recordAction("CREATE_MANUAL_RESERVATION", "Reservation", reservation.getId(),
                        "Direct booking created for " + request.guestName());

                return toResponse(reservation);
            });
        }
    }

    /**
     * Cancel an active reservation and restore inventory across all nights of the stay.
     */
    @Transactional
    public ReservationResponse cancelReservation(String propertyId, String reservationId, String reason) {
        String orgId = TenantContext.getOrganizationId();
        Reservation reservation = reservationRepository.findByIdAndOrganizationId(reservationId, orgId)
                .orElseThrow(() -> ApiException.notFound("Reservation not found."));

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            return toResponse(reservation);
        }

        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            // Restore inventory
            LocalDate lastNight = reservation.getCheckOutDate().minusDays(1);
            List<DailyInventory> inventoryNights = inventoryRepository.findOrderedForUpdate(
                    reservation.getRoomTypeId(), reservation.getCheckInDate(), lastNight);

            List<RoomDateInventory> restoredInv = new ArrayList<>();
            for (DailyInventory inv : inventoryNights) {
                inv.setSoldUnits(Math.max(0, inv.getSoldUnits() - reservation.getUnitsBooked()));
                inv.recalculateAvailability();
                inventoryRepository.save(inv);
                restoredInv.add(new RoomDateInventory(inv.getRoomTypeId(), inv.getDate(), inv.getAvailableUnits()));
            }

            outboxService.recordEvent(
                    orgId,
                    propertyId,
                    "INVENTORY",
                    reservation.getId(),
                    "CANCELLATION_INVENTORY_RESTORE",
                    restoredInv,
                    "cancel-" + reservation.getId() + "-" + System.currentTimeMillis()
            );
            outboxPublisher.publishPendingBatch(10);
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancellationReason(reason != null ? reason : "Cancelled by user");
        reservationRepository.save(reservation);

        auditService.recordAction("CANCEL_RESERVATION", "Reservation", reservation.getId(),
                "Reservation cancelled for " + reservation.getGuestName() + ". Inventory restored.");

        return toResponse(reservation);
    }

    /**
     * Resolve an overbooking conflict (MOVE_GUEST, CONTACT_GUEST, CANCEL_BOOKING, FREE_ROOM).
     */
    @Transactional
    public ConflictResponse resolveConflict(String propertyId, String conflictId, ResolveConflictRequest request, String resolvedBy) {
        String orgId = TenantContext.getOrganizationId();
        Conflict conflict = conflictRepository.findByIdAndOrganizationId(conflictId, orgId)
                .orElseThrow(() -> ApiException.notFound("Conflict not found."));

        Reservation reservation = reservationRepository.findByIdAndOrganizationId(conflict.getReservationId(), orgId)
                .orElseThrow(() -> ApiException.notFound("Associated reservation not found."));

        switch (request.action()) {
            case MOVE_GUEST -> {
                if (request.targetRoomTypeId() == null || request.targetRoomTypeId().isBlank()) {
                    throw ApiException.badRequest("Target room type is required to move guest.", "Select an alternate room type with availability.");
                }
                RoomType targetRoom = roomTypeRepository.findByIdAndOrganizationId(request.targetRoomTypeId(), orgId)
                        .orElseThrow(() -> ApiException.notFound("Target room type not found."));

                // Decrement inventory for new room type
                LocalDate lastNight = reservation.getCheckOutDate().minusDays(1);
                List<DailyInventory> targetInv = inventoryRepository.findOrderedForUpdate(
                        targetRoom.getId(), reservation.getCheckInDate(), lastNight);

                for (DailyInventory inv : targetInv) {
                    if (inv.getAvailableUnits() < reservation.getUnitsBooked()) {
                        throw ApiException.badRequest(
                                "Target room type " + targetRoom.getName() + " also has no availability on " + inv.getDate(),
                                "Please select a different room type or upgrade category."
                        );
                    }
                }

                for (DailyInventory inv : targetInv) {
                    inv.setSoldUnits(inv.getSoldUnits() + reservation.getUnitsBooked());
                    inv.recalculateAvailability();
                    inventoryRepository.save(inv);
                }

                reservation.setRoomTypeId(targetRoom.getId());
                reservation.setStatus(ReservationStatus.CONFIRMED);
                reservationRepository.save(reservation);
            }
            case CANCEL_BOOKING -> {
                reservation.setStatus(ReservationStatus.CANCELLED);
                reservation.setCancellationReason("Overbooking resolution: " + request.notes());
                reservationRepository.save(reservation);
            }
            case FREE_ROOM, CONTACT_GUEST -> {
                // Keep reservation notes updated
                if (request.notes() != null) {
                    reservation.setSpecialRequests((reservation.getSpecialRequests() != null ? reservation.getSpecialRequests() + "\n" : "") + "Staff Note: " + request.notes());
                    reservationRepository.save(reservation);
                }
            }
        }

        conflict.resolve(request.action(), request.notes(), resolvedBy);
        conflictRepository.save(conflict);

        auditService.recordAction("RESOLVE_OVERBOOKING_CONFLICT", "Conflict", conflict.getId(),
                "Conflict resolved via " + request.action() + " by " + resolvedBy);

        return toConflictResponse(conflict, reservation.getGuestName(), reservation.getChannelType(), reservation.getChannelReservationId());
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> getReservations(String propertyId, int page, int size) {
        String orgId = TenantContext.getOrganizationId();
        return reservationRepository.findAllByPropertyIdAndOrganizationIdOrderByCreatedAtDesc(
                propertyId, orgId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ConflictResponse> getUnresolvedConflicts(String propertyId) {
        String orgId = TenantContext.getOrganizationId();
        List<Conflict> conflicts = conflictRepository.findAllByPropertyIdAndOrganizationIdAndStatus(
                propertyId, orgId, ConflictStatus.UNRESOLVED);

        List<ConflictResponse> responses = new ArrayList<>();
        for (Conflict c : conflicts) {
            Optional<Reservation> resOpt = reservationRepository.findByIdAndOrganizationId(c.getReservationId(), orgId);
            String guest = resOpt.map(Reservation::getGuestName).orElse("Unknown Guest");
            String chType = resOpt.map(Reservation::getChannelType).orElse("OTA");
            String chRef = resOpt.map(Reservation::getChannelReservationId).orElse(c.getReservationId());
            responses.add(toConflictResponse(c, guest, chType, chRef));
        }
        return responses;
    }

    private String resolveRoomTypeId(String orgId, String propertyId, String channelRoomId) {
        // First check explicit mapping
        Optional<ChannelRoomMapping> mappingOpt = channelRoomMappingRepository.findAllByRoomTypeIdAndOrganizationId(channelRoomId, orgId)
                .stream().findFirst();
        if (mappingOpt.isPresent()) {
            return mappingOpt.get().getRoomTypeId();
        }

        // Check if channelRoomId is an existing room type ID
        if (roomTypeRepository.findByIdAndOrganizationId(channelRoomId, orgId).isPresent()) {
            return channelRoomId;
        }

        // Match by code or return first active room type
        List<RoomType> rooms = roomTypeRepository.findAllByPropertyIdAndOrganizationId(propertyId, orgId);
        for (RoomType r : rooms) {
            if (r.getCode().equalsIgnoreCase(channelRoomId) || channelRoomId.toLowerCase().contains(r.getCode().toLowerCase())) {
                return r.getId();
            }
        }

        if (!rooms.isEmpty()) {
            return rooms.get(0).getId();
        }

        throw ApiException.notFound("No room type could be resolved for channel room " + channelRoomId);
    }

    private ReservationResponse toResponse(Reservation r) {
        String roomName = "Unknown Room";
        String roomCode = "UNK";
        Optional<RoomType> roomOpt = roomTypeRepository.findByIdAndOrganizationId(r.getRoomTypeId(), r.getOrganizationId());
        if (roomOpt.isPresent()) {
            roomName = roomOpt.get().getName();
            roomCode = roomOpt.get().getCode();
        }

        String ratePlanName = null;
        if (r.getRatePlanId() != null) {
            ratePlanName = ratePlanRepository.findByIdAndOrganizationId(r.getRatePlanId(), r.getOrganizationId())
                    .map(RatePlan::getName).orElse(null);
        }

        return new ReservationResponse(
                r.getId(),
                r.getPropertyId(),
                r.getChannelAccountId(),
                r.getChannelType(),
                r.getChannelReservationId(),
                r.getStatus(),
                r.getGuestName(),
                r.getGuestEmail(),
                r.getGuestPhone(),
                r.getRoomTypeId(),
                roomName,
                roomCode,
                r.getRatePlanId(),
                ratePlanName,
                r.getCheckInDate(),
                r.getCheckOutDate(),
                r.getUnitsBooked(),
                r.getTotalAmountCents(),
                r.getTotalAmountCents() / 100.0,
                r.getCurrency(),
                r.getSpecialRequests(),
                r.getCancellationReason(),
                r.getCreatedAt()
        );
    }

    private ConflictResponse toConflictResponse(Conflict c, String guestName, String channelType, String channelReservationId) {
        String roomName = roomTypeRepository.findByIdAndOrganizationId(c.getRoomTypeId(), c.getOrganizationId())
                .map(RoomType::getName).orElse("Room");

        return new ConflictResponse(
                c.getId(),
                c.getPropertyId(),
                c.getReservationId(),
                guestName,
                channelType,
                channelReservationId,
                c.getRoomTypeId(),
                roomName,
                c.getConflictDate(),
                c.getReason(),
                c.getStatus(),
                c.getResolutionAction(),
                c.getResolutionNotes(),
                c.getResolvedBy(),
                c.getResolvedAt(),
                c.getCreatedAt()
        );
    }
}
