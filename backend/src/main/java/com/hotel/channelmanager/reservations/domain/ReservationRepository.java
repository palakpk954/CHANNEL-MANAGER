package com.hotel.channelmanager.reservations.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, String> {
    Optional<Reservation> findByIdAndOrganizationId(String id, String organizationId);
    Optional<Reservation> findByOrganizationIdAndChannelTypeAndChannelReservationId(
            String organizationId, String channelType, String channelReservationId);
    boolean existsByOrganizationIdAndChannelTypeAndChannelReservationId(
            String organizationId, String channelType, String channelReservationId);
    Page<Reservation> findAllByPropertyIdAndOrganizationIdOrderByCreatedAtDesc(
            String propertyId, String organizationId, Pageable pageable);
    List<Reservation> findAllByPropertyIdAndOrganizationIdAndStatus(
            String propertyId, String organizationId, ReservationStatus status);
    List<Reservation> findAllByPropertyIdAndOrganizationIdAndCheckInDateBetween(
            String propertyId, String organizationId, LocalDate fromDate, LocalDate toDate);
    List<Reservation> findAllByPropertyIdAndRoomTypeIdAndStatusInAndCheckOutDateGreaterThanEqual(
            String propertyId, String roomTypeId, List<ReservationStatus> statuses, LocalDate date);
}

