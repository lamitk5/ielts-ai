CREATE TABLE speaking_attempts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    prompt_id VARCHAR(120) NOT NULL,
    transcript TEXT,
    audio_filename VARCHAR(255),
    attempt_status VARCHAR(24) NOT NULL,
    overall_band_estimate NUMERIC(3,1),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT speaking_attempts_status_ck CHECK (attempt_status IN ('RECORDED', 'INPUT_SAVED', 'STT_NOT_CONFIGURED'))
);

CREATE INDEX speaking_attempts_user_created_idx ON speaking_attempts(user_id, created_at DESC);
