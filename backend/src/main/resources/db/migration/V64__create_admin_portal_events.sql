CREATE TABLE submission_reports (
    id UUID PRIMARY KEY,
    submission_id UUID NOT NULL REFERENCES practice_submissions(id) ON DELETE CASCADE,
    owner_user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    category VARCHAR(48) NOT NULL,
    comment TEXT NOT NULL DEFAULT '',
    status VARCHAR(24) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    resolved_by UUID REFERENCES app_users(id) ON DELETE SET NULL,
    CONSTRAINT submission_reports_category_ck CHECK (category IN ('CONTENT_ERROR', 'SCORING_CONCERN', 'TECHNICAL_ISSUE', 'OTHER')),
    CONSTRAINT submission_reports_status_ck CHECK (status IN ('OPEN', 'RESOLVED'))
);
CREATE UNIQUE INDEX submission_reports_open_uq ON submission_reports(submission_id, owner_user_id, category) WHERE status = 'OPEN';
CREATE INDEX submission_reports_queue_idx ON submission_reports(status, created_at DESC);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    actor_user_id UUID REFERENCES app_users(id) ON DELETE SET NULL,
    action VARCHAR(80) NOT NULL,
    object_type VARCHAR(80) NOT NULL,
    object_id VARCHAR(120),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX audit_events_time_idx ON audit_events(created_at DESC);

CREATE TABLE api_usage_events (
    id UUID PRIMARY KEY,
    provider VARCHAR(40),
    model VARCHAR(120),
    feature VARCHAR(80),
    status VARCHAR(24),
    latency_ms BIGINT,
    input_tokens INTEGER,
    output_tokens INTEGER,
    total_tokens INTEGER,
    fallback BOOLEAN NOT NULL DEFAULT FALSE,
    estimated_cost NUMERIC(14, 6),
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX api_usage_events_time_idx ON api_usage_events(created_at DESC);
