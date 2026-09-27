CREATE TABLE IF NOT EXISTS practice_catalog_publications (
    published_set_id VARCHAR(120) PRIMARY KEY,
    generated_set_id UUID NOT NULL REFERENCES generated_practice_sets(id) ON DELETE RESTRICT,
    generated_version_id UUID NOT NULL REFERENCES generated_practice_versions(id) ON DELETE RESTRICT,
    skill VARCHAR(16) NOT NULL CHECK (skill IN ('reading', 'listening', 'writing', 'speaking')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    publication_revision INTEGER NOT NULL DEFAULT 1 CHECK (publication_revision > 0),
    provenance_reference VARCHAR(240) NOT NULL DEFAULT '',
    published_at TIMESTAMPTZ NOT NULL,
    UNIQUE (generated_set_id, generated_version_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS practice_catalog_publications_one_active_set
    ON practice_catalog_publications (published_set_id) WHERE active = TRUE;

ALTER TABLE learning_attempts ADD COLUMN IF NOT EXISTS practice_version_id VARCHAR(120);
ALTER TABLE learning_attempts ADD COLUMN IF NOT EXISTS attempt_status VARCHAR(24) NOT NULL DEFAULT 'SUBMITTED';
ALTER TABLE learning_attempts ADD COLUMN IF NOT EXISTS answer_payload JSONB;
ALTER TABLE learning_attempts ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMPTZ;
ALTER TABLE learning_attempts ADD COLUMN IF NOT EXISTS result_payload JSONB;
ALTER TABLE learning_attempts ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(120);
CREATE UNIQUE INDEX IF NOT EXISTS learning_attempts_user_idempotency_uq ON learning_attempts (user_id, idempotency_key) WHERE idempotency_key IS NOT NULL;

ALTER TABLE writing_submissions ADD COLUMN IF NOT EXISTS practice_version_id UUID;
ALTER TABLE writing_submissions ADD COLUMN IF NOT EXISTS attempt_status VARCHAR(24) NOT NULL DEFAULT 'SUBMITTED';
ALTER TABLE writing_submissions ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMPTZ;
ALTER TABLE speaking_attempts ADD COLUMN IF NOT EXISTS practice_version_id UUID;
ALTER TABLE speaking_attempts ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMPTZ;
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS language VARCHAR(12) NOT NULL DEFAULT 'vi';
