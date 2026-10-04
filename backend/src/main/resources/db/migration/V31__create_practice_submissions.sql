CREATE TABLE IF NOT EXISTS practice_submissions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    skill VARCHAR(16) NOT NULL CHECK (skill IN ('READING', 'LISTENING', 'WRITING', 'SPEAKING')),
    practice_id VARCHAR(120) NOT NULL,
    practice_version_id VARCHAR(120) NOT NULL,
    published_set_id VARCHAR(120) NOT NULL REFERENCES practice_catalog_publications(published_set_id) ON DELETE RESTRICT,
    publication_revision INTEGER NOT NULL CHECK (publication_revision > 0),
    status VARCHAR(24) NOT NULL CHECK (status IN (
        'DRAFT', 'IN_PROGRESS', 'SUBMITTED', 'SCORING', 'AI_EVALUATING',
        'PENDING_REVIEW', 'GRADED', 'FAILED'
    )),
    started_at TIMESTAMPTZ NOT NULL,
    last_saved_at TIMESTAMPTZ,
    submitted_at TIMESTAMPTZ,
    scored_at TIMESTAMPTZ,
    duration_seconds BIGINT,
    autosave_revision BIGINT NOT NULL DEFAULT 0 CHECK (autosave_revision >= 0),
    start_idempotency_key VARCHAR(120),
    submit_idempotency_key VARCHAR(120),
    content_hash VARCHAR(128),
    failure_code VARCHAR(80),
    retryable BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS practice_submissions_owner_start_key_uq
    ON practice_submissions (user_id, start_idempotency_key)
    WHERE start_idempotency_key IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS practice_submissions_owner_submit_key_uq
    ON practice_submissions (user_id, submit_idempotency_key)
    WHERE submit_idempotency_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS practice_submissions_owner_status_time_idx
    ON practice_submissions (user_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS practice_submissions_practice_version_idx
    ON practice_submissions (published_set_id, practice_version_id);
