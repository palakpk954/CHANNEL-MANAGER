package com.hotel.channelmanager.dashboard.service;

import com.hotel.channelmanager.channels.domain.*;
import com.hotel.channelmanager.common.exception.ApiException;
import com.hotel.channelmanager.common.tenant.TenantContext;
import com.hotel.channelmanager.dashboard.dto.DashboardSummaryResponse;
import com.hotel.channelmanager.dashboard.dto.DashboardSummaryResponse.*;
import com.hotel.channelmanager.inventory.domain.DailyInventory;
import com.hotel.channelmanager.inventory.domain.DailyInventoryRepository;
import com.hotel.channelmanager.property.domain.Property;
import com.hotel.channelmanager.property.domain.PropertyRepository;
import com.hotel.channelmanager.property.domain.RoomType;
import com.hotel.channelmanager.property.domain.RoomTypeRepository;
import com.hotel.channelmanager.reservations.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class DashboardService {

    private final PropertyRepository propertyRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final DailyInventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final ConflictRepository conflictRepository;
    private final ChannelAccountRepository channelAccountRepository;
    private final ChannelRoomMappingRepository channelRoomMappingRepository;

    public DashboardService(
            PropertyRepository propertyRepository,
            RoomTypeRepository roomTypeRepository,
            DailyInventoryRepository inventoryRepository,
            ReservationRepository reservationRepository,
            ConflictRepository conflictRepository,
            ChannelAccountRepository channelAccountRepository,
            ChannelRoomMappingRepository channelRoomMappingRepository
    ) {
        this.propertyRepository = propertyRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.conflictRepository = conflictRepository;
        this.channelAccountRepository = channelAccountRepository;
        this.channelRoomMappingRepository = channelRoomMappingRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary(String propertyId) {
        String orgId = TenantContext.getOrganizationId();
        Property property = propertyRepository.findByIdAndOrganizationId(propertyId, orgId)
                .orElseThrow(() -> ApiException.notFound("Property not found."));

        LocalDate today = LocalDate.now();
        LocalDate firstOfMonth = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth());

        List<RoomType> roomTypes = roomTypeRepository.findAllByPropertyIdAndOrganizationId(propertyId, orgId);
        Map<String, String> roomNameMap = new HashMap<>();
        int totalPropertyUnits = 0;
        for (RoomType rt : roomTypes) {
            roomNameMap.put(rt.getId(), rt.getName());
            totalPropertyUnits += rt.getTotalUnits();
        }

        // Active confirmed reservations
        List<Reservation> allConfirmed = reservationRepository.findAllByPropertyIdAndOrganizationIdAndStatus(
                propertyId, orgId, ReservationStatus.CONFIRMED);

        List<DashboardGuestItem> todayArrivals = new ArrayList<>();
        List<DashboardGuestItem> todayDepartures = new ArrayList<>();
        int stayoversCount = 0;
        int occupiedUnitsToday = 0;
        long todayRevenueCents = 0;
        long monthToDateRevenueCents = 0;

        for (Reservation res : allConfirmed) {
            String roomName = roomNameMap.getOrDefault(res.getRoomTypeId(), "Standard Room");

            boolean isArrival = res.getCheckInDate().isEqual(today);
            boolean isDeparture = res.getCheckOutDate().isEqual(today);
            boolean isStayover = res.getCheckInDate().isBefore(today) && res.getCheckOutDate().isAfter(today);

            if (isArrival) {
                todayArrivals.add(toGuestItem(res, roomName));
                occupiedUnitsToday += res.getUnitsBooked();
                todayRevenueCents += res.getTotalAmountCents();
            } else if (isStayover) {
                stayoversCount++;
                occupiedUnitsToday += res.getUnitsBooked();
            }

            if (isDeparture) {
                todayDepartures.add(toGuestItem(res, roomName));
            }

            // Month to date revenue calculation
            if (!res.getCheckInDate().isBefore(firstOfMonth) && !res.getCheckInDate().isAfter(today)) {
                monthToDateRevenueCents += res.getTotalAmountCents();
            }
        }

        // Occupancy Percentage
        double occupancyRate = totalPropertyUnits > 0
                ? Math.min(100.0, Math.round(((double) occupiedUnitsToday / totalPropertyUnits) * 1000.0) / 10.0)
                : 0.0;

        // Conflicts
        List<Conflict> unresolvedConflicts = conflictRepository.findAllByPropertyIdAndOrganizationIdAndStatus(
                propertyId, orgId, ConflictStatus.UNRESOLVED);

        // Recent 5 bookings
        Page<Reservation> recentPage = reservationRepository.findAllByPropertyIdAndOrganizationIdOrderByCreatedAtDesc(
                propertyId, orgId, PageRequest.of(0, 5));
        List<DashboardGuestItem> recentBookings = recentPage.getContent().stream()
                .map(res -> toGuestItem(res, roomNameMap.getOrDefault(res.getRoomTypeId(), "Standard Room")))
                .toList();

        // 7-Day Occupancy Trend
        List<DailyOccupancyTrend> sevenDayTrend = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = today.plusDays(i);
            int dayTotal = totalPropertyUnits;
            int daySold = 0;
            for (RoomType rt : roomTypes) {
                Optional<DailyInventory> invOpt = inventoryRepository.findByRoomTypeIdAndDate(rt.getId(), d);
                if (invOpt.isPresent()) {
                    daySold += invOpt.get().getSoldUnits();
                }
            }
            int dayAvail = Math.max(0, dayTotal - daySold);
            double dayOcc = dayTotal > 0 ? Math.round(((double) daySold / dayTotal) * 1000.0) / 10.0 : 0.0;
            sevenDayTrend.add(new DailyOccupancyTrend(d, dayTotal, daySold, dayAvail, dayOcc));
        }

        // Channel Health Summary
        List<ChannelAccount> accounts = channelAccountRepository.findAllByPropertyIdAndOrganizationId(propertyId, orgId);
        int activeChannelsCount = 0;
        List<ChannelHealthItem> channelHealthList = new ArrayList<>();

        for (ChannelAccount acc : accounts) {
            if (acc.isSyncEnabled() && acc.getStatus() == ChannelStatus.CONNECTED) {
                activeChannelsCount++;
            }
            int mappedCount = channelRoomMappingRepository.findAllByChannelAccountIdAndOrganizationId(acc.getId(), orgId).size();
            channelHealthList.add(new ChannelHealthItem(
                    acc.getId(),
                    acc.getChannelType().name(),
                    acc.getAccountName(),
                    acc.getStatus().name(),
                    acc.getLastSyncAt() != null ? acc.getLastSyncAt().toString() : "Never",
                    acc.getErrorMessage(),
                    mappedCount
            ));
        }

        return new DashboardSummaryResponse(
                property.getId(),
                property.getName(),
                property.getCurrency(),
                todayArrivals.size(),
                todayDepartures.size(),
                stayoversCount,
                occupiedUnitsToday,
                totalPropertyUnits,
                occupancyRate,
                todayRevenueCents,
                monthToDateRevenueCents,
                unresolvedConflicts.size(),
                activeChannelsCount,
                accounts.size(),
                todayArrivals,
                todayDepartures,
                recentBookings,
                sevenDayTrend,
                channelHealthList
        );
    }

    private DashboardGuestItem toGuestItem(Reservation res, String roomTypeName) {
        return new DashboardGuestItem(
                res.getId(),
                res.getGuestName(),
                res.getChannelType(),
                res.getChannelReservationId(),
                roomTypeName,
                res.getCheckInDate(),
                res.getCheckOutDate(),
                res.getUnitsBooked(),
                res.getTotalAmountCents(),
                res.getStatus().name()
        );
    }
}

