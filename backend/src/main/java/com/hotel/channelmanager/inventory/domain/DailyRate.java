package com.hotel.channelmanager.inventory.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_rates", uniqueConstraints = {
        @UniqueConstraint(name = "uq_rate_plan_date", columnNames = {"rate_plan_id", "date"})
})
public class DailyRate {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "organization_id", nullable = false, length = 36)
    private String organizationId;

    @Column(name = "property_id", nullable = false, length = 36)
    private String propertyId;

    @Column(name = "rate_plan_id", nullable = false, length = 36)
    private String ratePlanId;

    @Column(nullable = false)
    private LocalDate date;

    // Minor units (e.g. 450000 = ₹4,500.00) per BR-6.4 & Engineering Handbook 2.1
    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(nullable = false, length = 10)
    private String currency = "INR";

    @Column(name = "min_stay", nullable = false)
    private int minStay = 1;

    @Column(name = "max_stay")
    private Integer maxStay;

    @Column(nullable = false)
    private boolean cta = false; // Closed to arrival (No check-ins on this day)

    @Column(nullable = false)
    private boolean ctd = false; // Closed to departure (No check-outs on this day)

    @Column(name = "stop_sell", nullable = false)
    private boolean stopSell = false; // Close this room for sale (BR-2.3)

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public DailyRate() {
        this.id = UUID.randomUUID().toString();
    }

    public DailyRate(String organizationId, String propertyId, String ratePlanId, LocalDate date, long amountCents, String currency) {
        this.id = UUID.randomUUID().toString();
        this.organizationId = organizationId;
        this.propertyId = propertyId;
        this.ratePlanId = ratePlanId;
        this.date = date;
        this.amountCents = amountCents;
        this.currency = currency != null ? currency : "INR";
        this.minStay = 1;
        this.cta = false;
        this.ctd = false;
        this.stopSell = false;
        this.createdAt = Instant.now();
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

    public String getRatePlanId() {
        return ratePlanId;
    }

    public void setRatePlanId(String ratePlanId) {
        this.ratePlanId = ratePlanId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(long amountCents) {
        this.amountCents = amountCents;
        this.updatedAt = Instant.now();
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public int getMinStay() {
        return minStay;
    }

    public void setMinStay(int minStay) {
        this.minStay = minStay;
        this.updatedAt = Instant.now();
    }

    public Integer getMaxStay() {
        return maxStay;
    }

    public void setMaxStay(Integer maxStay) {
        this.maxStay = maxStay;
        this.updatedAt = Instant.now();
    }

    public boolean isCta() {
        return cta;
    }

    public void setCta(boolean cta) {
        this.cta = cta;
        this.updatedAt = Instant.now();
    }

    public boolean isCtd() {
        return ctd;
    }

    public void setCtd(boolean ctd) {
        this.ctd = ctd;
        this.updatedAt = Instant.now();
    }

    public boolean isStopSell() {
        return stopSell;
    }

    public void setStopSell(boolean stopSell) {
        this.stopSell = stopSell;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

