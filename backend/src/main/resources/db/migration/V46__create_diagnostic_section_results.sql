CREATE TABLE diagnostic_section_results (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES diagnostic_sessions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    skill VARCHAR(16) NOT NULL,
    state VARCHAR(24) NOT NULL,
    score INTEGER,
    total INTEGER,
    estimated_band NUMERIC(3,1),
    confidence VARCHAR(24) NOT NULL,
    source_submission_id UUID,
    source_reference VARCHAR(160),
    availability_message VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT diagnostic_section_skill_ck CHECK (skill IN ('READING', 'LISTENING', 'WRITING', 'SPEAKING')),
    CONSTRAINT diagnostic_section_state_ck CHECK (state IN ('READY', 'INSUFFICIENT_EVIDENCE', 'UNAVAILABLE')),
    CONSTRAINT diagnostic_section_confidence_ck CHECK (confidence IN ('HIGH', 'MEDIUM', 'LOW', 'INSUFFICIENT_DATA')),
    CONSTRAINT diagnostic_section_score_ck CHECK ((score IS NULL AND total IS NULL) OR (score >= 0 AND total > 0 AND score <= total)),
    UNIQUE(session_id, skill)
);
CREATE INDEX diagnostic_section_results_user_idx ON diagnostic_section_results(user_id, created_at DESC);
