CREATE TABLE submission_reviews (
    id UUID PRIMARY KEY,
    submission_id UUID NOT NULL REFERENCES practice_submissions(id) ON DELETE CASCADE,
    reviewer_user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    review_version INTEGER NOT NULL DEFAULT 1 CHECK (review_version > 0),
    overall_band NUMERIC(3,1) CHECK (overall_band IS NULL OR (overall_band >= 0.0 AND overall_band <= 9.0)),
    fluency_coherence NUMERIC(3,1) CHECK (fluency_coherence IS NULL OR (fluency_coherence >= 0.0 AND fluency_coherence <= 9.0)),
    lexical_resource NUMERIC(3,1) CHECK (lexical_resource IS NULL OR (lexical_resource >= 0.0 AND lexical_resource <= 9.0)),
    grammatical_range NUMERIC(3,1) CHECK (grammatical_range IS NULL OR (grammatical_range >= 0.0 AND grammatical_range <= 9.0)),
    pronunciation NUMERIC(3,1) CHECK (pronunciation IS NULL OR (pronunciation >= 0.0 AND pronunciation <= 9.0)),
    reviewer_feedback TEXT,
    criteria_json JSONB,
    status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_submission_reviews_submission_version UNIQUE (submission_id, review_version)
);

CREATE INDEX idx_submission_reviews_submission ON submission_reviews(submission_id);
CREATE INDEX idx_submission_reviews_reviewer ON submission_reviews(reviewer_user_id);
