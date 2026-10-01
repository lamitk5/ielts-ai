CREATE TABLE IF NOT EXISTS submission_answers (
    submission_id UUID PRIMARY KEY REFERENCES practice_submissions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    answer_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    content_hash VARCHAR(128) NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS submission_answers_user_submitted_idx
    ON submission_answers (user_id, submitted_at DESC);
