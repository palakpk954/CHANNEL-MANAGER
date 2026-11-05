package com.hotel.channelmanager.dashboard.dto;

import java.time.LocalDate;
import java.util.List;

public record DashboardSummaryResponse(
        String propertyId,
        String propertyName,
        String currency,
        int todayArrivalsCount,
        int todayDeparturesCount,
        int inHouseStayoversCount,
        int occupiedRoomsToday,
        int totalRooms,
        double occupancyRatePercentage,
        long todayRevenueCents,
        long monthToDateRevenueCents,
        int unresolvedConflictsCount,
        int activeChannelsCount,
        int totalChannelsCount,
        List<DashboardGuestItem> todayArrivals,
        List<DashboardGuestItem> todayDepartures,
        List<DashboardGuestItem> recentBookings,
        List<DailyOccupancyTrend> sevenDayOccupancyTrend,
        List<ChannelHealthItem> channelHealthList
) {
    public record DashboardGuestItem(
            String reservationId,
            String guestName,
            String channelType,
            String channelReservationId,
            String roomTypeName,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int unitsBooked,
            long totalAmountCents,
            String status
    ) {}

    public record DailyOccupancyTrend(
            LocalDate date,
            int totalUnits,
            int soldUnits,
            int availableUnits,
            double occupancyPercentage
    ) {}

    public record ChannelHealthItem(
            String channelAccountId,
            String channelType,
            String accountName,
            String status,
            String lastSyncAt,
            String errorMessage,
            int mappedRoomsCount
    ) {}
}

