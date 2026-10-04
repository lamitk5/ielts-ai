CREATE TABLE learning_mistake_acknowledgements (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    mistake_id UUID NOT NULL REFERENCES learning_mistakes(id) ON DELETE CASCADE,
    state VARCHAR(24) NOT NULL DEFAULT 'ACKNOWLEDGED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT learning_mistake_ack_state_ck CHECK (state IN ('ACKNOWLEDGED', 'REOPENED')),
    CONSTRAINT learning_mistake_ack_owner_unique UNIQUE (user_id, mistake_id)
);
CREATE INDEX learning_mistake_ack_user_idx ON learning_mistake_acknowledgements(user_id, updated_at DESC);
