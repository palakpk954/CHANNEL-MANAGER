-- V2__property_rooms_and_rates.sql
-- Property, Room Types, and Rate Plans schema with multi-tenant isolation

CREATE TABLE properties (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(500) NOT NULL,
    timezone VARCHAR(100) NOT NULL DEFAULT 'Asia/Kolkata',
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    checkin_time VARCHAR(10) NOT NULL DEFAULT '14:00',
    checkout_time VARCHAR(10) NOT NULL DEFAULT '11:00',
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_properties_org ON properties(organization_id);

CREATE TABLE room_types (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    property_id VARCHAR(36) NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    max_occupancy INT NOT NULL DEFAULT 2,
    total_units INT NOT NULL DEFAULT 1,
    bed_type VARCHAR(100),
    amenities TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_property_room_code UNIQUE(property_id, code)
);

CREATE INDEX idx_room_types_property ON room_types(property_id);
CREATE INDEX idx_room_types_org ON room_types(organization_id);

CREATE TABLE rate_plans (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    property_id VARCHAR(36) NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    room_type_id VARCHAR(36) NOT NULL REFERENCES room_types(id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    meal_plan VARCHAR(100) NOT NULL DEFAULT 'ROOM_ONLY',
    cancellation_policy VARCHAR(100) NOT NULL DEFAULT 'FREE_CANCELLATION_24H',
    parent_rate_plan_id VARCHAR(36) REFERENCES rate_plans(id),
    derivation_type VARCHAR(50),
    derivation_value NUMERIC(10,2),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_property_rate_code UNIQUE(property_id, code)
);

CREATE INDEX idx_rate_plans_property ON rate_plans(property_id);
CREATE INDEX idx_rate_plans_room_type ON rate_plans(room_type_id);
CREATE INDEX idx_rate_plans_org ON rate_plans(organization_id);

