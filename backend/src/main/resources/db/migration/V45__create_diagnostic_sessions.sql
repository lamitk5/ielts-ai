CREATE TABLE diagnostic_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    state VARCHAR(16) NOT NULL DEFAULT 'IN_PROGRESS',
    definition_version VARCHAR(32) NOT NULL,
    attempt_number INTEGER NOT NULL DEFAULT 1,
    content_snapshot JSONB NOT NULL DEFAULT '[]'::jsonb,
    started_at TIMESTAMPTZ NOT NULL,
    submitted_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT diagnostic_session_state_ck CHECK (state IN ('IN_PROGRESS', 'SUBMITTED', 'COMPLETED', 'SKIPPED')),
    CONSTRAINT diagnostic_session_definition_ck CHECK (definition_version IS NOT NULL AND definition_version <> ''),
    CONSTRAINT diagnostic_session_attempt_ck CHECK (attempt_number >= 1),
    CONSTRAINT diagnostic_session_version_ck CHECK (version >= 0),
    CONSTRAINT diagnostic_session_submitted_ck CHECK (
        (state IN ('SUBMITTED', 'COMPLETED', 'SKIPPED') AND submitted_at IS NOT NULL)
        OR (state = 'IN_PROGRESS' AND submitted_at IS NULL)
    ),
    CONSTRAINT diagnostic_sessions_owner_attempt_uq UNIQUE (user_id, attempt_number)
);

CREATE INDEX diagnostic_sessions_owner_idx ON diagnostic_sessions(user_id, started_at DESC);
CREATE INDEX diagnostic_sessions_owner_state_idx ON diagnostic_sessions(user_id, state);