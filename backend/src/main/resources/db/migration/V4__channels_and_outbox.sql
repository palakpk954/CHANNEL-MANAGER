-- V4__channels_and_outbox.sql
-- Channel accounts, Room and Rate mappings, Transactional Outbox, and Sync Jobs

CREATE TABLE channel_accounts (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    property_id VARCHAR(36) NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    channel_type VARCHAR(50) NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    encrypted_credentials TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'CONNECTED',
    sync_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_sync_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_channel_acc UNIQUE (property_id, channel_type, account_name)
);

CREATE INDEX idx_channel_acc_prop ON channel_accounts(property_id);
CREATE INDEX idx_channel_acc_org ON channel_accounts(organization_id);

CREATE TABLE channel_room_mappings (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    channel_account_id VARCHAR(36) NOT NULL REFERENCES channel_accounts(id) ON DELETE CASCADE,
    room_type_id VARCHAR(36) NOT NULL REFERENCES room_types(id) ON DELETE CASCADE,
    channel_room_id VARCHAR(255) NOT NULL,
    channel_room_name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_channel_room_mapping UNIQUE (channel_account_id, room_type_id)
);

CREATE INDEX idx_ch_room_map_acc ON channel_room_mappings(channel_account_id);
CREATE INDEX idx_ch_room_map_org ON channel_room_mappings(organization_id);

CREATE TABLE channel_rate_mappings (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    channel_account_id VARCHAR(36) NOT NULL REFERENCES channel_accounts(id) ON DELETE CASCADE,
    rate_plan_id VARCHAR(36) NOT NULL REFERENCES rate_plans(id) ON DELETE CASCADE,
    channel_rate_id VARCHAR(255) NOT NULL,
    channel_rate_name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_channel_rate_mapping UNIQUE (channel_account_id, rate_plan_id)
);

CREATE INDEX idx_ch_rate_map_acc ON channel_rate_mappings(channel_account_id);
CREATE INDEX idx_ch_rate_map_org ON channel_rate_mappings(organization_id);

CREATE TABLE outbox_events (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    property_id VARCHAR(36) NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_outbox_status ON outbox_events(status, created_at);
CREATE INDEX idx_outbox_org ON outbox_events(organization_id);

CREATE TABLE sync_jobs (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    property_id VARCHAR(36) NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    channel_account_id VARCHAR(36) NOT NULL REFERENCES channel_accounts(id) ON DELETE CASCADE,
    job_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    payload_summary TEXT,
    response_summary TEXT,
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_sync_jobs_acc ON sync_jobs(channel_account_id, created_at DESC);
CREATE INDEX idx_sync_jobs_status ON sync_jobs(status, next_retry_at);
CREATE INDEX idx_sync_jobs_prop ON sync_jobs(property_id);
CREATE INDEX idx_sync_jobs_org ON sync_jobs(organization_id);

