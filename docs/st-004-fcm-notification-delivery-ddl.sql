CREATE TABLE IF NOT EXISTS fcm_device_tokens (
    fcm_device_token_id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    member_key VARCHAR(255) NOT NULL,
    recipient_type VARCHAR(20) NOT NULL,
    token VARCHAR(512) NOT NULL UNIQUE,
    platform VARCHAR(20),
    active BOOLEAN NOT NULL,
    last_used_at TIMESTAMP,
    deactivated_at TIMESTAMP,
    deactivation_reason VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_fcm_device_tokens_member_type_active
    ON fcm_device_tokens (member_key, recipient_type, active);

CREATE TABLE IF NOT EXISTS notifications (
    notification_id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    notification_key VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    data_payload TEXT,
    status VARCHAR(20) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    sent_at TIMESTAMP,
    failure_code VARCHAR(100),
    failure_reason VARCHAR(1000)
);

CREATE INDEX IF NOT EXISTS idx_notifications_status_requested_at
    ON notifications (status, requested_at);

CREATE TABLE IF NOT EXISTS notification_deliveries (
    notification_delivery_id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    notification_id BIGINT NOT NULL REFERENCES notifications (notification_id),
    recipient_member_key VARCHAR(255) NOT NULL,
    recipient_type VARCHAR(20) NOT NULL,
    fcm_device_token_id BIGINT,
    token_snapshot VARCHAR(512) NOT NULL,
    status VARCHAR(20) NOT NULL,
    fcm_message_id VARCHAR(255),
    failure_code VARCHAR(100),
    failure_reason VARCHAR(1000),
    attempt_count INTEGER NOT NULL,
    retryable BOOLEAN NOT NULL,
    next_retry_at TIMESTAMP,
    sent_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notification_deliveries_notification_id
    ON notification_deliveries (notification_id);

CREATE INDEX IF NOT EXISTS idx_notification_deliveries_retry
    ON notification_deliveries (status, retryable, next_retry_at);
