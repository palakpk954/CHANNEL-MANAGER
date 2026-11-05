package com.hotel.channelmanager.inventory.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ManualBlockRepository extends JpaRepository<ManualBlock, String> {
    List<ManualBlock> findAllByPropertyIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(String propertyId, LocalDate endDate, LocalDate startDate);
    List<ManualBlock> findAllByPropertyIdOrderByCreatedAtDesc(String propertyId);
    List<ManualBlock> findAllByPropertyIdAndRoomTypeIdAndEndDateGreaterThanEqual(String propertyId, String roomTypeId, LocalDate date);
}

