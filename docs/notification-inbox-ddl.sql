-- Notification inbox (recipient-scoped) — issue #143
-- Replaces the previous recipient-less `notifications` schema (event_type/title/body/status only).
-- The old table stored one throwaway global row per dispatch and holds no reusable data,
-- so this migration drops and recreates it.
--
-- Apply to dev/prod BEFORE deploying code (ddl-auto=validate).
-- Local uses ddl-auto=create, so no manual apply is needed there.

-- 1. Drop the delivery table first.
--    The NotificationDelivery entity is removed in this change, and `notification_deliveries`
--    holds a foreign key to `notifications`, so it must go before the notifications table
--    can be dropped.
DROP TABLE IF EXISTS notification_deliveries;

-- 2. Recreate `notifications` with the recipient-scoped inbox schema.
DROP TABLE IF EXISTS notifications;

CREATE TABLE notifications (
    notification_id      BIGSERIAL PRIMARY KEY,
    notification_key     VARCHAR(255) NOT NULL UNIQUE,
    recipient_member_key VARCHAR(255) NOT NULL,
    event_type           VARCHAR(100) NOT NULL,
    title                VARCHAR(200) NOT NULL,
    body                 VARCHAR(1000) NOT NULL,
    data_payload         JSONB,
    created_at           TIMESTAMP NOT NULL,
    updated_at           TIMESTAMP
);

-- TODO: CREATE INDEX idx_notifications_recipient_created ON notifications(recipient_member_key, created_at DESC);

-- 3. Drop columns removed from the FcmDeviceToken entity.
--    `active` was declared NOT NULL with no default; leaving it in place would break inserts
--    from the slimmed-down entity (validate tolerates the extra columns, runtime inserts do not).
--    Dropping `active` also removes the composite index that referenced it.
ALTER TABLE fcm_device_tokens
    DROP COLUMN IF EXISTS active,
    DROP COLUMN IF EXISTS last_used_at,
    DROP COLUMN IF EXISTS deactivated_at,
    DROP COLUMN IF EXISTS deactivation_reason;
