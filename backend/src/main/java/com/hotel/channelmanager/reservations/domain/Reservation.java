package com.hotel.channelmanager.reservations.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    private String id;

    @Column(name = "organization_id", nullable = false)
    private String organizationId;

    @Column(name = "property_id", nullable = false)
    private String propertyId;

    @Column(name = "channel_account_id")
    private String channelAccountId;

    @Column(name = "channel_type", nullable = false, length = 50)
    private String channelType;

    @Column(name = "channel_reservation_id", nullable = false)
    private String channelReservationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ReservationStatus status = ReservationStatus.CONFIRMED;

    @Column(name = "guest_name", nullable = false)
    private String guestName;

    @Column(name = "guest_email")
    private String guestEmail;

    @Column(name = "guest_phone", length = 50)
    private String guestPhone;

    @Column(name = "room_type_id", nullable = false)
    private String roomTypeId;

    @Column(name = "rate_plan_id")
    private String ratePlanId;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "units_booked", nullable = false)
    private int unitsBooked = 1;

    @Column(name = "total_amount_cents", nullable = false)
    private long totalAmountCents;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "INR";

    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Reservation() {
        this.id = UUID.randomUUID().toString();
    }

    public Reservation(
            String organizationId,
            String propertyId,
            String channelAccountId,
            String channelType,
            String channelReservationId,
            ReservationStatus status,
            String guestName,
            String guestEmail,
            String guestPhone,
            String roomTypeId,
            String ratePlanId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int unitsBooked,
            long totalAmountCents,
            String currency,
            String specialRequests
    ) {
        this.id = UUID.randomUUID().toString();
        this.organizationId = organizationId;
        this.propertyId = propertyId;
        this.channelAccountId = channelAccountId;
        this.channelType = channelType;
        this.channelReservationId = channelReservationId;
        this.status = status;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestPhone = guestPhone;
        this.roomTypeId = roomTypeId;
        this.ratePlanId = ratePlanId;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.unitsBooked = unitsBooked;
        this.totalAmountCents = totalAmountCents;
        this.currency = currency;
        this.specialRequests = specialRequests;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public String getChannelAccountId() {
        return channelAccountId;
    }

    public void setChannelAccountId(String channelAccountId) {
        this.channelAccountId = channelAccountId;
    }

    public String getChannelType() {
        return channelType;
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    public String getChannelReservationId() {
        return channelReservationId;
    }

    public void setChannelReservationId(String channelReservationId) {
        this.channelReservationId = channelReservationId;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }

    public String getGuestPhone() {
        return guestPhone;
    }

    public void setGuestPhone(String guestPhone) {
        this.guestPhone = guestPhone;
    }

    public String getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(String roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public String getRatePlanId() {
        return ratePlanId;
    }

    public void setRatePlanId(String ratePlanId) {
        this.ratePlanId = ratePlanId;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public int getUnitsBooked() {
        return unitsBooked;
    }

    public void setUnitsBooked(int unitsBooked) {
        this.unitsBooked = unitsBooked;
    }

    public long getTotalAmountCents() {
        return totalAmountCents;
    }

    public void setTotalAmountCents(long totalAmountCents) {
        this.totalAmountCents = totalAmountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getSpecialRequests() {
        return specialRequests;
    }

    public void setSpecialRequests(String specialRequests) {
        this.specialRequests = specialRequests;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

