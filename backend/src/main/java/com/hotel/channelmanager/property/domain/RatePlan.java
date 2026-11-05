package com.hotel.channelmanager.property.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rate_plans", uniqueConstraints = {
        @UniqueConstraint(name = "uq_property_rate_code", columnNames = {"property_id", "code"})
})
public class RatePlan {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "organization_id", nullable = false, length = 36)
    private String organizationId;

    @Column(name = "property_id", nullable = false, length = 36)
    private String propertyId;

    @Column(name = "room_type_id", nullable = false, length = 36)
    private String roomTypeId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "meal_plan", nullable = false, length = 100)
    private String mealPlan = "ROOM_ONLY";

    @Column(name = "cancellation_policy", nullable = false, length = 100)
    private String cancellationPolicy = "FREE_CANCELLATION_24H";

    @Column(name = "parent_rate_plan_id", length = 36)
    private String parentRatePlanId;

    @Column(name = "derivation_type", length = 50)
    private String derivationType; // PERCENTAGE, FIXED_AMOUNT

    @Column(name = "derivation_value", precision = 10, scale = 2)
    private BigDecimal derivationValue;

    @Column(nullable = false, length = 50)
    private String status = "ACTIVE"; // ACTIVE, ARCHIVED

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public RatePlan() {
        this.id = UUID.randomUUID().toString();
    }

    public RatePlan(String organizationId, String propertyId, String roomTypeId, String code, String name, String mealPlan, String cancellationPolicy) {
        this.id = UUID.randomUUID().toString();
        this.organizationId = organizationId;
        this.propertyId = propertyId;
        this.roomTypeId = roomTypeId;
        this.code = code;
        this.name = name;
        this.mealPlan = mealPlan != null ? mealPlan : "ROOM_ONLY";
        this.cancellationPolicy = cancellationPolicy != null ? cancellationPolicy : "FREE_CANCELLATION_24H";
        this.status = "ACTIVE";
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

    public String getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(String roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMealPlan() {
        return mealPlan;
    }

    public void setMealPlan(String mealPlan) {
        this.mealPlan = mealPlan;
    }

    public String getCancellationPolicy() {
        return cancellationPolicy;
    }

    public void setCancellationPolicy(String cancellationPolicy) {
        this.cancellationPolicy = cancellationPolicy;
    }

    public String getParentRatePlanId() {
        return parentRatePlanId;
    }

    public void setParentRatePlanId(String parentRatePlanId) {
        this.parentRatePlanId = parentRatePlanId;
    }

    public String getDerivationType() {
        return derivationType;
    }

    public void setDerivationType(String derivationType) {
        this.derivationType = derivationType;
    }

    public BigDecimal getDerivationValue() {
        return derivationValue;
    }

    public void setDerivationValue(BigDecimal derivationValue) {
        this.derivationValue = derivationValue;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

