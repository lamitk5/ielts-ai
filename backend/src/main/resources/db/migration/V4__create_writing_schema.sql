CREATE TABLE writing_submissions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    task_id VARCHAR(120) NOT NULL,
    response_text TEXT NOT NULL,
    word_count INTEGER NOT NULL CHECK (word_count >= 0),
    assessment_status VARCHAR(24) NOT NULL,
    assessment_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT writing_submissions_status_ck CHECK (assessment_status IN ('ANSWERED', 'UNAVAILABLE'))
);

CREATE INDEX writing_submissions_user_created_idx ON writing_submissions(user_id, created_at DESC);
