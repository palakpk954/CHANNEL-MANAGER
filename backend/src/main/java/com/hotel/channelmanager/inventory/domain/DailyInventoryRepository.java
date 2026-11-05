package com.hotel.channelmanager.inventory.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyInventoryRepository extends JpaRepository<DailyInventory, String> {

    List<DailyInventory> findAllByPropertyIdAndDateBetweenOrderByDateAsc(String propertyId, LocalDate startDate, LocalDate endDate);

    List<DailyInventory> findAllByRoomTypeIdAndDateBetweenOrderByDateAsc(String roomTypeId, LocalDate startDate, LocalDate endDate);

    Optional<DailyInventory> findByRoomTypeIdAndDate(String roomTypeId, LocalDate date);

    // ADR-005 & BR-5.1: Ordered row-level lock by room_type and date ASC to prevent deadlocks and overbooking
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DailyInventory d WHERE d.roomTypeId = :roomTypeId AND d.date BETWEEN :startDate AND :endDate ORDER BY d.date ASC")
    List<DailyInventory> findOrderedForUpdate(
            @Param("roomTypeId") String roomTypeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DailyInventory d WHERE d.roomTypeId = :roomTypeId AND d.date = :date")
    Optional<DailyInventory> findSingleForUpdate(
            @Param("roomTypeId") String roomTypeId,
            @Param("date") LocalDate date
    );
}

