CREATE TABLE mock_test_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    mock_test_id VARCHAR(120) NOT NULL,
    mock_test_version VARCHAR(64) NOT NULL DEFAULT 'v1',
    status VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED',
    current_section_index INTEGER NOT NULL DEFAULT 0,
    total_time_limit_seconds INTEGER NOT NULL DEFAULT 10800,
    elapsed_seconds INTEGER NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT mock_test_sessions_status_ck CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'PAUSED', 'SUBMITTED', 'COMPLETED', 'EXPIRED'))
);

CREATE INDEX idx_mock_test_sessions_user ON mock_test_sessions(user_id, status);

CREATE TABLE mock_test_sections (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES mock_test_sessions(id) ON DELETE CASCADE,
    section_order INTEGER NOT NULL CHECK (section_order >= 0),
    skill VARCHAR(32) NOT NULL,
    practice_id VARCHAR(120) NOT NULL,
    practice_version_id VARCHAR(64) NOT NULL DEFAULT 'v1',
    published_set_id VARCHAR(120) NOT NULL DEFAULT 'default-set',
    submission_id UUID REFERENCES practice_submissions(id) ON DELETE SET NULL,
    time_limit_seconds INTEGER NOT NULL DEFAULT 1800,
    status VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT mock_test_sections_skill_ck CHECK (skill IN ('LISTENING', 'READING', 'WRITING', 'SPEAKING')),
    CONSTRAINT mock_test_sections_status_ck CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'SKIPPED')),
    CONSTRAINT uq_mock_sections_session_order UNIQUE (session_id, section_order)
);

CREATE INDEX idx_mock_test_sections_session ON mock_test_sections(session_id);
