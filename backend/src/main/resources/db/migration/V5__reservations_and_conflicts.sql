-- V5__reservations_and_conflicts.sql
-- Inbound reservations, overbooking conflicts, and resolution audit

CREATE TABLE reservations (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    property_id VARCHAR(36) NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    channel_account_id VARCHAR(36) REFERENCES channel_accounts(id) ON DELETE SET NULL,
    channel_type VARCHAR(50) NOT NULL,
    channel_reservation_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'CONFIRMED',
    guest_name VARCHAR(255) NOT NULL,
    guest_email VARCHAR(255),
    guest_phone VARCHAR(50),
    room_type_id VARCHAR(36) NOT NULL REFERENCES room_types(id) ON DELETE RESTRICT,
    rate_plan_id VARCHAR(36) REFERENCES rate_plans(id) ON DELETE SET NULL,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    units_booked INT NOT NULL DEFAULT 1,
    total_amount_cents BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    special_requests TEXT,
    cancellation_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_res_channel_ref UNIQUE (organization_id, channel_type, channel_reservation_id)
);

CREATE INDEX idx_res_prop_dates ON reservations(property_id, check_in_date, check_out_date);
CREATE INDEX idx_res_status ON reservations(status);
CREATE INDEX idx_res_org ON reservations(organization_id);

CREATE TABLE conflicts (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    property_id VARCHAR(36) NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    reservation_id VARCHAR(36) NOT NULL REFERENCES reservations(id) ON DELETE CASCADE,
    room_type_id VARCHAR(36) NOT NULL REFERENCES room_types(id) ON DELETE CASCADE,
    conflict_date DATE NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'UNRESOLVED',
    resolution_action VARCHAR(50),
    resolution_notes TEXT,
    resolved_by VARCHAR(36),
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_conflicts_prop_status ON conflicts(property_id, status);
CREATE INDEX idx_conflicts_org ON conflicts(organization_id);

