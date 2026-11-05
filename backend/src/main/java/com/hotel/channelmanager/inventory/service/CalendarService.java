package com.hotel.channelmanager.inventory.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.channelmanager.channels.outbox.OutboxService;
import com.hotel.channelmanager.channels.sync.payload.RatePlanDateRate;
import com.hotel.channelmanager.channels.sync.payload.RatePlanDateRestriction;
import com.hotel.channelmanager.channels.sync.payload.RoomDateInventory;
import com.hotel.channelmanager.common.audit.AuditService;
import com.hotel.channelmanager.common.exception.ApiException;
import com.hotel.channelmanager.common.tenant.TenantContext;
import com.hotel.channelmanager.inventory.domain.*;
import com.hotel.channelmanager.inventory.web.dto.*;
import com.hotel.channelmanager.property.domain.Property;
import com.hotel.channelmanager.property.domain.PropertyRepository;
import com.hotel.channelmanager.property.domain.RatePlan;
import com.hotel.channelmanager.property.domain.RatePlanRepository;
import com.hotel.channelmanager.property.domain.RoomType;
import com.hotel.channelmanager.property.domain.RoomTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class CalendarService {

    private final PropertyRepository propertyRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RatePlanRepository ratePlanRepository;
    private final DailyInventoryRepository inventoryRepository;
    private final DailyRateRepository rateRepository;
    private final ManualBlockRepository manualBlockRepository;
    private final BulkUpdateHistoryRepository bulkUpdateHistoryRepository;
    private final OutboxService outboxService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public CalendarService(
            PropertyRepository propertyRepository,
            RoomTypeRepository roomTypeRepository,
            RatePlanRepository ratePlanRepository,
            DailyInventoryRepository inventoryRepository,
            DailyRateRepository rateRepository,
            ManualBlockRepository manualBlockRepository,
            BulkUpdateHistoryRepository bulkUpdateHistoryRepository,
            OutboxService outboxService,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.propertyRepository = propertyRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.ratePlanRepository = ratePlanRepository;
        this.inventoryRepository = inventoryRepository;
        this.rateRepository = rateRepository;
        this.manualBlockRepository = manualBlockRepository;
        this.bulkUpdateHistoryRepository = bulkUpdateHistoryRepository;
        this.outboxService = outboxService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CalendarGridResponse getCalendar(String propertyId, LocalDate fromDate, LocalDate toDate) {
        String orgId = TenantContext.getOrganizationId();
        Property property = propertyRepository.findByIdAndOrganizationId(propertyId, orgId)
                .orElseThrow(() -> ApiException.notFound("The requested property was not found."));

        ZoneId propertyZone = ZoneId.of(property.getTimezone());
        LocalDate today = LocalDate.now(propertyZone);

        LocalDate start = fromDate != null ? fromDate : today;
        LocalDate end = toDate != null ? toDate : start.plusDays(30);

        if (end.isBefore(start)) {
            end = start.plusDays(30);
        }

        List<RoomType> rooms = roomTypeRepository.findAllByPropertyIdAndOrganizationId(property.getId(), orgId)
                .stream().filter(r -> "ACTIVE".equalsIgnoreCase(r.getStatus())).toList();

        List<RatePlan> ratePlans = ratePlanRepository.findAllByPropertyIdAndOrganizationId(property.getId(), orgId)
                .stream().filter(rp -> "ACTIVE".equalsIgnoreCase(rp.getStatus())).toList();

        List<LocalDate> dates = new ArrayList<>();
        LocalDate curr = start;
        while (!curr.isAfter(end)) {
            dates.add(curr);
            curr = curr.plusDays(1);
        }

        // Fetch existing inventory and rates
        List<DailyInventory> existingInv = inventoryRepository.findAllByPropertyIdAndDateBetweenOrderByDateAsc(property.getId(), start, end);
        Map<String, DailyInventory> invMap = new HashMap<>();
        for (DailyInventory inv : existingInv) {
            invMap.put(inv.getRoomTypeId() + ":" + inv.getDate(), inv);
        }

        List<DailyRate> existingRates = rateRepository.findAllByPropertyIdAndDateBetweenOrderByDateAsc(property.getId(), start, end);
        Map<String, DailyRate> ratesMap = new HashMap<>();
        for (DailyRate rate : existingRates) {
            ratesMap.put(rate.getRatePlanId() + ":" + rate.getDate(), rate);
        }

        // Build room rows
        List<CalendarGridResponse.RoomRow> roomRows = new ArrayList<>();
        for (RoomType room : rooms) {
            Map<String, CalendarGridResponse.DayInventoryDto> days = new HashMap<>();
            for (LocalDate date : dates) {
                String key = room.getId() + ":" + date;
                DailyInventory inv = invMap.get(key);
                if (inv == null) {
                    // Initialize default inventory on the fly
                    inv = new DailyInventory(orgId, property.getId(), room.getId(), date, room.getTotalUnits());
                    inv = inventoryRepository.save(inv);
                    invMap.put(key, inv);
                }

                String statusToken = "available";
                if (inv.getAvailableUnits() == 0) {
                    statusToken = "soldout";
                } else if (inv.getAvailableUnits() <= 2) {
                    statusToken = "low";
                }

                days.put(date.toString(), new CalendarGridResponse.DayInventoryDto(
                        inv.getTotalUnits(),
                        inv.getSoldUnits(),
                        inv.getBlockedUnits(),
                        inv.getAvailableUnits(),
                        statusToken
                ));
            }
            roomRows.add(new CalendarGridResponse.RoomRow(room.getId(), room.getCode(), room.getName(), room.getTotalUnits(), days));
        }

        // Build rate plan rows
        List<CalendarGridResponse.RatePlanRow> ratePlanRows = new ArrayList<>();
        for (RatePlan plan : ratePlans) {
            Map<String, CalendarGridResponse.DayRateDto> days = new HashMap<>();
            for (LocalDate date : dates) {
                String key = plan.getId() + ":" + date;
                DailyRate rate = ratesMap.get(key);
                if (rate == null) {
                    // Default base price: 4500.00 INR (450000 minor units)
                    rate = new DailyRate(orgId, property.getId(), plan.getId(), date, 450000L, property.getCurrency());
                    rate = rateRepository.save(rate);
                    ratesMap.put(key, rate);
                }

                boolean hasRestrictions = rate.getMinStay() > 1 || rate.getMaxStay() != null || rate.isCta() || rate.isCtd() || rate.isStopSell();

                days.put(date.toString(), new CalendarGridResponse.DayRateDto(
                        rate.getAmountCents(),
                        rate.getAmountCents() / 100.0,
                        rate.getCurrency(),
                        rate.isStopSell(),
                        rate.isCta(),
                        rate.isCtd(),
                        rate.getMinStay(),
                        rate.getMaxStay(),
                        hasRestrictions
                ));
            }
            ratePlanRows.add(new CalendarGridResponse.RatePlanRow(plan.getId(), plan.getRoomTypeId(), plan.getCode(), plan.getName(), days));
        }

        return new CalendarGridResponse(
                property.getId(),
                property.getName(),
                property.getCurrency(),
                property.getTimezone(),
                start,
                end,
                dates,
                roomRows,
                ratePlanRows
        );
    }

    @Transactional
    public void updateSingleDay(String propertyId, UpdateCalendarDayRequest request, String actorId, String actorEmail) {
        String orgId = TenantContext.getOrganizationId();
        Property property = propertyRepository.findByIdAndOrganizationId(propertyId, orgId)
                .orElseThrow(() -> ApiException.notFound("The requested property was not found."));

        ZoneId propertyZone = ZoneId.of(property.getTimezone());
        LocalDate today = LocalDate.now(propertyZone);

        // BR-1.8: Past dates are read-only
        if (request.date().isBefore(today)) {
            throw ApiException.badRequest(
                    "Only today and future dates can be changed.",
                    "Please select today or a future date to modify."
            );
        }

        // Handle inventory update
        if (request.roomTypeId() != null && request.availableUnits() != null) {
            DailyInventory inv = inventoryRepository.findByRoomTypeIdAndDate(request.roomTypeId(), request.date())
                    .orElseGet(() -> new DailyInventory(orgId, property.getId(), request.roomTypeId(), request.date(), 1));

            int before = inv.getAvailableUnits();
            inv.setTotalUnits(Math.max(request.availableUnits() + inv.getSoldUnits() + inv.getBlockedUnits(), 1));
            inv.recalculateAvailability();
            inventoryRepository.save(inv);

            outboxService.recordEvent(
                    orgId,
                    property.getId(),
                    "INVENTORY",
                    inv.getId(),
                    "AVAILABILITY_CHANGED",
                    List.of(new RoomDateInventory(inv.getRoomTypeId(), inv.getDate(), inv.getAvailableUnits())),
                    "day-inv-" + inv.getId() + "-" + System.currentTimeMillis()
            );

            auditService.logChange(
                    actorId,
                    actorEmail,
                    "INVENTORY",
                    inv.getId(),
                    "UPDATE_DAY",
                    Map.of("date", request.date().toString(), "available", before),
                    Map.of("date", request.date().toString(), "available", inv.getAvailableUnits()),
                    null
            );
        }

        // Handle rate plan & restriction update
        if (request.ratePlanId() != null) {
            DailyRate rate = rateRepository.findByRatePlanIdAndDate(request.ratePlanId(), request.date())
                    .orElseGet(() -> new DailyRate(orgId, property.getId(), request.ratePlanId(), request.date(), 450000L, property.getCurrency()));

            long beforeAmount = rate.getAmountCents();
            boolean beforeStopSell = rate.isStopSell();

            if (request.amountCents() != null) {
                // BR-2.10: Rate must be greater than 0
                if (request.amountCents() <= 0) {
                    throw ApiException.badRequest("Please enter a price greater than 0.", "Enter a positive price.");
                }
                rate.setAmountCents(request.amountCents());
            }

            if (request.stopSell() != null) {
                rate.setStopSell(request.stopSell());
            }
            if (request.minStay() != null) {
                rate.setMinStay(Math.max(1, request.minStay()));
            }
            if (request.maxStay() != null) {
                rate.setMaxStay(request.maxStay());
            }
            if (request.cta() != null) {
                rate.setCta(request.cta());
            }
            if (request.ctd() != null) {
                rate.setCtd(request.ctd());
            }

            rateRepository.save(rate);

            if (request.amountCents() != null) {
                outboxService.recordEvent(
                        orgId,
                        property.getId(),
                        "RATE",
                        rate.getId(),
                        "RATES_CHANGED",
                        List.of(new RatePlanDateRate(rate.getRatePlanId(), rate.getDate(), rate.getAmountCents(), rate.getCurrency())),
                        "day-rate-" + rate.getId() + "-" + System.currentTimeMillis()
                );
            }

            if (request.stopSell() != null || request.minStay() != null || request.maxStay() != null || request.cta() != null || request.ctd() != null) {
                outboxService.recordEvent(
                        orgId,
                        property.getId(),
                        "RESTRICTION",
                        rate.getId(),
                        "RESTRICTIONS_CHANGED",
                        List.of(new RatePlanDateRestriction(rate.getRatePlanId(), rate.getDate(), rate.isStopSell(), rate.getMinStay(), rate.getMaxStay(), rate.isCta(), rate.isCtd())),
                        "day-restr-" + rate.getId() + "-" + System.currentTimeMillis()
                );
            }

            auditService.logChange(
                    actorId,
                    actorEmail,
                    "RATE",
                    rate.getId(),
                    "UPDATE_DAY",
                    Map.of("date", request.date().toString(), "amount", beforeAmount, "stopSell", beforeStopSell),
                    Map.of("date", request.date().toString(), "amount", rate.getAmountCents(), "stopSell", rate.isStopSell()),
                    null
            );
        }
    }

    @Transactional
    public String applyBulkUpdate(String propertyId, BulkUpdateRequest request, String actorId, String actorEmail) {
        String orgId = TenantContext.getOrganizationId();
        Property property = propertyRepository.findByIdAndOrganizationId(propertyId, orgId)
                .orElseThrow(() -> ApiException.notFound("The requested property was not found."));

        ZoneId propertyZone = ZoneId.of(property.getTimezone());
        LocalDate today = LocalDate.now(propertyZone);

        // BR-1.8: Range in past rejected
        if (request.startDate().isBefore(today)) {
            throw ApiException.badRequest(
                    "Only today and future dates can be changed.",
                    "Please select today or future dates for bulk updates."
            );
        }

        if (request.endDate().isBefore(request.startDate())) {
            throw ApiException.badRequest(
                    "End date cannot be earlier than start date.",
                    "Please choose an end date on or after the start date."
            );
        }

        Set<Integer> weekdayFilter = request.daysOfWeek() != null && !request.daysOfWeek().isEmpty()
                ? new HashSet<>(request.daysOfWeek())
                : Set.of(1, 2, 3, 4, 5, 6, 7);

        List<Map<String, Object>> snapshotList = new ArrayList<>();
        List<RatePlanDateRate> changedRates = new ArrayList<>();
        List<RatePlanDateRestriction> changedRestrictions = new ArrayList<>();
        List<RoomDateInventory> changedInventories = new ArrayList<>();
        int updatedDaysCount = 0;

        LocalDate curr = request.startDate();
        while (!curr.isAfter(request.endDate())) {
            final LocalDate targetDate = curr;
            if (weekdayFilter.contains(curr.getDayOfWeek().getValue())) {
                updatedDaysCount++;

                // Rate plans bulk update
                if (request.ratePlanIds() != null) {
                    for (String planId : request.ratePlanIds()) {
                        DailyRate rate = rateRepository.findByRatePlanIdAndDate(planId, targetDate)
                                .orElseGet(() -> new DailyRate(orgId, property.getId(), planId, targetDate, 450000L, property.getCurrency()));

                        // Snapshot for 24h undo
                        snapshotList.add(Map.of(
                                "entity", "RATE",
                                "id", rate.getId(),
                                "ratePlanId", planId,
                                "date", targetDate.toString(),
                                "amountCents", rate.getAmountCents(),
                                "stopSell", rate.isStopSell(),
                                "minStay", rate.getMinStay(),
                                "maxStay", rate.getMaxStay() != null ? rate.getMaxStay() : 0,
                                "cta", rate.isCta(),
                                "ctd", rate.isCtd()
                        ));

                        if (request.priceValue() != null) {
                            long newAmountCents = Math.round(request.priceValue() * 100);

                            // BR-2.10: Confirmation threshold for > 50% change
                            if (rate.getAmountCents() > 0 && Boolean.TRUE.equals(request.confirmLargePriceChange()) == false) {
                                double diffRatio = Math.abs((double) (newAmountCents - rate.getAmountCents()) / rate.getAmountCents());
                                if (diffRatio > 0.50) {
                                    throw ApiException.badRequest(
                                            "This price is more than 50% different from the previous price. Please confirm to apply.",
                                            "Check the 'Confirm large price change' box to proceed."
                                    );
                                }
                            }
                            rate.setAmountCents(Math.max(100L, newAmountCents));
                            changedRates.add(new RatePlanDateRate(rate.getRatePlanId(), rate.getDate(), rate.getAmountCents(), rate.getCurrency()));
                        }

                        if (request.stopSell() != null) rate.setStopSell(request.stopSell());
                        if (request.minStay() != null) rate.setMinStay(request.minStay());
                        if (request.maxStay() != null) rate.setMaxStay(request.maxStay());
                        if (request.cta() != null) rate.setCta(request.cta());
                        if (request.ctd() != null) rate.setCtd(request.ctd());

                        if (request.stopSell() != null || request.minStay() != null || request.maxStay() != null || request.cta() != null || request.ctd() != null) {
                            changedRestrictions.add(new RatePlanDateRestriction(rate.getRatePlanId(), rate.getDate(), rate.isStopSell(), rate.getMinStay(), rate.getMaxStay(), rate.isCta(), rate.isCtd()));
                        }

                        rateRepository.save(rate);
                    }
                }

                // Inventory bulk update
                if (request.roomTypeIds() != null && request.availableUnits() != null) {
                    for (String roomId : request.roomTypeIds()) {
                        DailyInventory inv = inventoryRepository.findByRoomTypeIdAndDate(roomId, targetDate)
                                .orElseGet(() -> new DailyInventory(orgId, property.getId(), roomId, targetDate, 1));

                        snapshotList.add(Map.of(
                                "entity", "INVENTORY",
                                "id", inv.getId(),
                                "roomTypeId", roomId,
                                "date", targetDate.toString(),
                                "totalUnits", inv.getTotalUnits(),
                                "availableUnits", inv.getAvailableUnits()
                        ));

                        inv.setTotalUnits(Math.max(request.availableUnits() + inv.getSoldUnits() + inv.getBlockedUnits(), 1));
                        inv.recalculateAvailability();
                        inventoryRepository.save(inv);
                        changedInventories.add(new RoomDateInventory(inv.getRoomTypeId(), inv.getDate(), inv.getAvailableUnits()));
                    }
                }
            }
            curr = curr.plusDays(1);
        }

        try {
            String snapshotJson = objectMapper.writeValueAsString(snapshotList);
            String desc = "Bulk update applied across " + updatedDaysCount + " days from " + request.startDate() + " to " + request.endDate();
            BulkUpdateHistory history = new BulkUpdateHistory(orgId, property.getId(), desc, snapshotJson, actorId);
            bulkUpdateHistoryRepository.save(history);

            if (!changedInventories.isEmpty()) {
                outboxService.recordEvent(
                        orgId,
                        property.getId(),
                        "INVENTORY",
                        property.getId(),
                        "BULK_AVAILABILITY_CHANGED",
                        changedInventories,
                        "bulk-inv-" + history.getId() + "-" + System.currentTimeMillis()
                );
            }

            if (!changedRates.isEmpty()) {
                outboxService.recordEvent(
                        orgId,
                        property.getId(),
                        "RATE",
                        property.getId(),
                        "BULK_RATES_CHANGED",
                        changedRates,
                        "bulk-rate-" + history.getId() + "-" + System.currentTimeMillis()
                );
            }

            if (!changedRestrictions.isEmpty()) {
                outboxService.recordEvent(
                        orgId,
                        property.getId(),
                        "RESTRICTION",
                        property.getId(),
                        "BULK_RESTRICTIONS_CHANGED",
                        changedRestrictions,
                        "bulk-restr-" + history.getId() + "-" + System.currentTimeMillis()
                );
            }

            auditService.logChange(
                    actorId,
                    actorEmail,
                    "CALENDAR",
                    history.getId(),
                    "BULK_UPDATE",
                    null,
                    desc,
                    null
            );

            return history.getId();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to record bulk update history for undo", e);
        }
    }

    @Transactional
    public UndoResponse undoBulkUpdate(String propertyId, String historyId, String actorId, String actorEmail) {
        String orgId = TenantContext.getOrganizationId();
        BulkUpdateHistory history = bulkUpdateHistoryRepository.findByIdAndPropertyIdAndOrganizationId(historyId, propertyId, orgId)
                .orElseThrow(() -> ApiException.notFound("The bulk update record was not found."));

        // BR-7.7: Undo available for changes within last 24 hours
        if (!history.isEligibleForUndo(24)) {
            throw ApiException.badRequest(
                    "This change is older than 24 hours or has already been undone.",
                    "Changes older than 24 hours cannot be undone automatically."
            );
        }

        try {
            List<Map<String, Object>> snapshot = objectMapper.readValue(
                    history.getSnapshotBefore(),
                    new TypeReference<>() {}
            );

            List<RatePlanDateRate> restoredRates = new ArrayList<>();
            List<RoomDateInventory> restoredInv = new ArrayList<>();

            for (Map<String, Object> item : snapshot) {
                String entity = (String) item.get("entity");
                LocalDate date = LocalDate.parse((String) item.get("date"));

                if ("RATE".equals(entity)) {
                    String planId = (String) item.get("ratePlanId");
                    DailyRate rate = rateRepository.findByRatePlanIdAndDate(planId, date).orElse(null);
                    if (rate != null) {
                        rate.setAmountCents(((Number) item.get("amountCents")).longValue());
                        rate.setStopSell((Boolean) item.get("stopSell"));
                        rate.setMinStay(((Number) item.get("minStay")).intValue());
                        int maxStay = ((Number) item.get("maxStay")).intValue();
                        rate.setMaxStay(maxStay > 0 ? maxStay : null);
                        rate.setCta((Boolean) item.get("cta"));
                        rate.setCtd((Boolean) item.get("ctd"));
                        rateRepository.save(rate);
                        restoredRates.add(new RatePlanDateRate(rate.getRatePlanId(), rate.getDate(), rate.getAmountCents(), rate.getCurrency()));
                    }
                } else if ("INVENTORY".equals(entity)) {
                    String roomId = (String) item.get("roomTypeId");
                    DailyInventory inv = inventoryRepository.findByRoomTypeIdAndDate(roomId, date).orElse(null);
                    if (inv != null) {
                        inv.setTotalUnits(((Number) item.get("totalUnits")).intValue());
                        inv.recalculateAvailability();
                        inventoryRepository.save(inv);
                        restoredInv.add(new RoomDateInventory(inv.getRoomTypeId(), inv.getDate(), inv.getAvailableUnits()));
                    }
                }
            }

            history.setUndoneAt(java.time.Instant.now());
            bulkUpdateHistoryRepository.save(history);

            if (!restoredInv.isEmpty()) {
                outboxService.recordEvent(
                        orgId,
                        propertyId,
                        "INVENTORY",
                        propertyId,
                        "BULK_AVAILABILITY_CHANGED",
                        restoredInv,
                        "undo-inv-" + history.getId() + "-" + System.currentTimeMillis()
                );
            }

            if (!restoredRates.isEmpty()) {
                outboxService.recordEvent(
                        orgId,
                        propertyId,
                        "RATE",
                        propertyId,
                        "BULK_RATES_CHANGED",
                        restoredRates,
                        "undo-rate-" + history.getId() + "-" + System.currentTimeMillis()
                );
            }

            auditService.logChange(
                    actorId,
                    actorEmail,
                    "CALENDAR",
                    history.getId(),
                    "UNDO_BULK_UPDATE",
                    "APPLIED",
                    "UNDONE",
                    null
            );

            return new UndoResponse(history.getId(), "Your last change has been undone.", true);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to restore bulk update snapshot", e);
        }
    }

    @Transactional
    public ManualBlock createManualBlock(String propertyId, ManualBlockRequest request, String actorId, String actorEmail) {
        String orgId = TenantContext.getOrganizationId();
        Property property = propertyRepository.findByIdAndOrganizationId(propertyId, orgId)
                .orElseThrow(() -> ApiException.notFound("The requested property was not found."));

        ZoneId propertyZone = ZoneId.of(property.getTimezone());
        LocalDate today = LocalDate.now(propertyZone);

        if (request.startDate().isBefore(today)) {
            throw ApiException.badRequest("Hold start date cannot be in the past.", "Please choose a future date.");
        }

        if (request.endDate().isBefore(request.startDate())) {
            throw ApiException.badRequest("End date cannot be earlier than start date.", "Check the date range.");
        }

        // Apply hold to inventory across all nights in range (BR-1.4)
        List<RoomDateInventory> blockInv = new ArrayList<>();
        LocalDate curr = request.startDate();
        while (!curr.isAfter(request.endDate())) {
            final LocalDate targetDate = curr;
            DailyInventory inv = inventoryRepository.findByRoomTypeIdAndDate(request.roomTypeId(), targetDate)
                    .orElseGet(() -> new DailyInventory(orgId, property.getId(), request.roomTypeId(), targetDate, 1));

            inv.setBlockedUnits(inv.getBlockedUnits() + request.unitsBlocked());
            inv.recalculateAvailability();
            inventoryRepository.save(inv);
            blockInv.add(new RoomDateInventory(inv.getRoomTypeId(), inv.getDate(), inv.getAvailableUnits()));

            curr = curr.plusDays(1);
        }

        ManualBlock block = new ManualBlock(
                orgId,
                property.getId(),
                request.roomTypeId(),
                request.startDate(),
                request.endDate(),
                request.unitsBlocked(),
                request.reason().trim(),
                actorId
        );
        ManualBlock saved = manualBlockRepository.save(block);

        if (!blockInv.isEmpty()) {
            outboxService.recordEvent(
                    orgId,
                    property.getId(),
                    "INVENTORY",
                    saved.getId(),
                    "MANUAL_BLOCK_AVAILABILITY_CHANGED",
                    blockInv,
                    "block-inv-" + saved.getId() + "-" + System.currentTimeMillis()
            );
        }

        auditService.logChange(
                actorId,
                actorEmail,
                "MANUAL_BLOCK",
                saved.getId(),
                "CREATE_HOLD",
                null,
                request.unitsBlocked() + " rooms held from " + request.startDate() + " to " + request.endDate() + " for: " + request.reason(),
                null
        );

        return saved;
    }
}
