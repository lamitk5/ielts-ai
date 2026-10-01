CREATE TABLE learning_attempts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    skill VARCHAR(16) NOT NULL,
    set_id VARCHAR(120) NOT NULL,
    score INTEGER NOT NULL CHECK (score >= 0),
    total INTEGER NOT NULL CHECK (total > 0 AND score <= total),
    answer_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT learning_attempts_skill_ck CHECK (skill IN ('READING', 'LISTENING', 'WRITING', 'SPEAKING'))
);

CREATE INDEX learning_attempts_user_skill_idx ON learning_attempts(user_id, skill, created_at DESC);

CREATE TABLE learning_activity (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    skill VARCHAR(16) NOT NULL,
    activity_type VARCHAR(48) NOT NULL,
    reference_id VARCHAR(120) NOT NULL,
    score NUMERIC(4,2),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT learning_activity_skill_ck CHECK (skill IN ('READING', 'LISTENING', 'WRITING', 'SPEAKING'))
);

CREATE INDEX learning_activity_user_created_idx ON learning_activity(user_id, created_at DESC);
