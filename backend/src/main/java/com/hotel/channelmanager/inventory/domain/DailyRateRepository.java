package com.hotel.channelmanager.inventory.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyRateRepository extends JpaRepository<DailyRate, String> {

    List<DailyRate> findAllByPropertyIdAndDateBetweenOrderByDateAsc(String propertyId, LocalDate startDate, LocalDate endDate);

    List<DailyRate> findAllByRatePlanIdAndDateBetweenOrderByDateAsc(String ratePlanId, LocalDate startDate, LocalDate endDate);

    Optional<DailyRate> findByRatePlanIdAndDate(String ratePlanId, LocalDate date);
}

