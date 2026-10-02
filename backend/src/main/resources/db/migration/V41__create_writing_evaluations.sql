CREATE TABLE writing_evaluations (
    id UUID PRIMARY KEY,
    version_id UUID NOT NULL REFERENCES writing_submission_versions(id) ON DELETE CASCADE,
    evaluation_version INTEGER NOT NULL CHECK (evaluation_version >= 1),
    overall_band_estimate NUMERIC(3,1) CHECK (overall_band_estimate IS NULL OR (overall_band_estimate >= 0.0 AND overall_band_estimate <= 9.0)),
    criteria JSONB NOT NULL DEFAULT '{}'::jsonb,
    strengths JSONB NOT NULL DEFAULT '[]'::jsonb,
    issues JSONB NOT NULL DEFAULT '[]'::jsonb,
    suggestions JSONB NOT NULL DEFAULT '[]'::jsonb,
    evidence_spans JSONB NOT NULL DEFAULT '[]'::jsonb,
    priority_improvements JSONB NOT NULL DEFAULT '[]'::jsonb,
    grounding_status VARCHAR(64) NOT NULL DEFAULT 'NOT_ENABLED',
    disclaimer TEXT NOT NULL,
    status VARCHAR(24) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT writing_evaluations_status_ck CHECK (status IN ('GRADED', 'FAILED', 'MALFORMED', 'UNAVAILABLE'))
);

CREATE INDEX writing_evaluations_version_idx ON writing_evaluations(version_id, created_at DESC);
