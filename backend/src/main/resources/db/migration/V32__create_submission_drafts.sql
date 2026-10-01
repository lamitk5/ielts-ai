CREATE TABLE IF NOT EXISTS practice_submission_drafts (
    submission_id UUID PRIMARY KEY REFERENCES practice_submissions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    draft_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    revision BIGINT NOT NULL CHECK (revision >= 1),
    idempotency_key VARCHAR(120),
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS practice_submission_drafts_user_updated_idx
    ON practice_submission_drafts (user_id, updated_at DESC);
