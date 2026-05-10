CREATE TABLE IF NOT EXISTS notification_settings (
    notification_setting_id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    deleted_at TIMESTAMP(6),
    ward_member_key VARCHAR(255) NOT NULL,
    safe_zone_entry_enabled BOOLEAN NOT NULL,
    safe_zone_exit_enabled BOOLEAN NOT NULL,
    route_deviation_enabled BOOLEAN NOT NULL,
    speed_anomaly_enabled BOOLEAN NOT NULL,
    wandering_anomaly_enabled BOOLEAN NOT NULL,
    anomaly_sensitivity VARCHAR(20) NOT NULL,
    emergency_call_enabled BOOLEAN NOT NULL,
    low_battery_enabled BOOLEAN NOT NULL,
    battery_threshold_percent INTEGER NOT NULL,
    CONSTRAINT uk_notification_settings_ward_member_key UNIQUE (ward_member_key)
);
