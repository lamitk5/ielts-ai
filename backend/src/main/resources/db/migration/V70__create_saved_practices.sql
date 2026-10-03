ALTER TABLE app_users ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(512);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE IF NOT EXISTS saved_practices (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    published_set_id VARCHAR(120) NOT NULL REFERENCES practice_catalog_publications(published_set_id) ON DELETE CASCADE,
    skill VARCHAR(16) NOT NULL CHECK (skill IN ('reading', 'listening', 'writing', 'speaking', 'READING', 'LISTENING', 'WRITING', 'SPEAKING')),
    title VARCHAR(240) NOT NULL,
    saved_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT saved_practices_user_set_uq UNIQUE (user_id, published_set_id)
);

CREATE INDEX IF NOT EXISTS saved_practices_user_idx ON saved_practices(user_id, saved_at DESC);
