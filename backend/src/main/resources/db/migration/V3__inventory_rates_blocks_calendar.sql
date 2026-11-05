-- V3__inventory_rates_blocks_calendar.sql
-- Daily Inventory, Daily Rates, Manual Blocks, and Bulk Update History

CREATE TABLE daily_inventory (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    property_id VARCHAR(36) NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    room_type_id VARCHAR(36) NOT NULL REFERENCES room_types(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    total_units INT NOT NULL DEFAULT 1,
    sold_units INT NOT NULL DEFAULT 0,
    blocked_units INT NOT NULL DEFAULT 0,
    available_units INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_available_non_negative CHECK (available_units >= 0),
    CONSTRAINT uq_room_type_date UNIQUE (room_type_id, date)
);

CREATE INDEX idx_daily_inv_lookup ON daily_inventory(property_id, date);
CREATE INDEX idx_daily_inv_room_date ON daily_inventory(room_type_id, date);
CREATE INDEX idx_daily_inv_org ON daily_inventory(organization_id);

CREATE TABLE daily_rates (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    property_id VARCHAR(36) NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    rate_plan_id VARCHAR(36) NOT NULL REFERENCES rate_plans(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    min_stay INT NOT NULL DEFAULT 1,
    max_stay INT,
    cta BOOLEAN NOT NULL DEFAULT FALSE,
    ctd BOOLEAN NOT NULL DEFAULT FALSE,
    stop_sell BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_rate_positive CHECK (amount_cents > 0),
    CONSTRAINT uq_rate_plan_date UNIQUE (rate_plan_id, date)
);

CREATE INDEX idx_daily_rates_lookup ON daily_rates(property_id, date);
CREATE INDEX idx_daily_rates_plan_date ON daily_rates(rate_plan_id, date);
CREATE INDEX idx_daily_rates_org ON daily_rates(organization_id);

CREATE TABLE manual_blocks (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    property_id VARCHAR(36) NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    room_type_id VARCHAR(36) NOT NULL REFERENCES room_types(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    units_blocked INT NOT NULL DEFAULT 1,
    reason VARCHAR(255) NOT NULL,
    created_by VARCHAR(36),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_manual_blocks_prop ON manual_blocks(property_id, start_date, end_date);
CREATE INDEX idx_manual_blocks_org ON manual_blocks(organization_id);

CREATE TABLE bulk_update_history (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    property_id VARCHAR(36) NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    description VARCHAR(500) NOT NULL,
    snapshot_before TEXT NOT NULL,
    created_by VARCHAR(36),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    undone_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_bulk_history_prop ON bulk_update_history(property_id, created_at DESC);

