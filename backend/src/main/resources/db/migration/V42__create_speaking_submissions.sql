CREATE TABLE speaking_submissions (
    id UUID PRIMARY KEY,
    submission_id UUID NOT NULL REFERENCES practice_submissions(id) ON DELETE CASCADE,
    prompt_id VARCHAR(120) NOT NULL,
    prompt_version VARCHAR(64) NOT NULL DEFAULT 'v1',
    preparation_seconds INTEGER NOT NULL DEFAULT 0 CHECK (preparation_seconds >= 0),
    response_seconds INTEGER NOT NULL DEFAULT 0 CHECK (response_seconds >= 0),
    audio_storage_key VARCHAR(512),
    audio_mime_type VARCHAR(120),
    audio_size_bytes BIGINT CHECK (audio_size_bytes IS NULL OR audio_size_bytes > 0),
    transcript TEXT,
    transcript_source VARCHAR(32) NOT NULL DEFAULT 'UNAVAILABLE',
    status VARCHAR(24) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT speaking_submissions_source_ck CHECK (transcript_source IN ('MANUAL', 'STT_GENERATED', 'UNAVAILABLE')),
    CONSTRAINT speaking_submissions_status_ck CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'PENDING_REVIEW', 'GRADED', 'FAILED'))
);

CREATE INDEX speaking_submissions_submission_idx ON speaking_submissions(submission_id);
