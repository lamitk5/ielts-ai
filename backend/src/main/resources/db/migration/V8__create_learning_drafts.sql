CREATE TABLE learning_drafts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    skill VARCHAR(32) NOT NULL,
    reference_id VARCHAR(128) NOT NULL,
    content_snapshot TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ,
    CONSTRAINT learning_drafts_skill_ck CHECK (skill IN ('READING', 'LISTENING', 'WRITING', 'SPEAKING')),
    CONSTRAINT learning_drafts_status_ck CHECK (status IN ('ACTIVE', 'SUBMITTED', 'EXPIRED', 'DELETED')),
    CONSTRAINT learning_drafts_version_ck CHECK (version >= 1)
);

CREATE INDEX learning_drafts_lookup_idx ON learning_drafts(user_id, skill, reference_id, status);
