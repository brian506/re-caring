CREATE TABLE IF NOT EXISTS member_withdrawal (
    member_withdrawal_id BIGSERIAL PRIMARY KEY,
    member_key VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    withdrawn_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_member_withdrawal_member_key
    ON member_withdrawal (member_key);
