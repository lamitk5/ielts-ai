CREATE TABLE learning_events (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    event_type VARCHAR(48) NOT NULL,
    skill VARCHAR(16) NOT NULL,
    session_id UUID,
    practice_set_id VARCHAR(120),
    attempt_id UUID,
    question_id VARCHAR(120),
    roadmap_item_id UUID,
    source_reference VARCHAR(160) NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    client_event_id VARCHAR(160),
    occurred_at TIMESTAMPTZ NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT learning_events_skill_ck CHECK (skill IN ('READING', 'LISTENING', 'WRITING', 'SPEAKING'))
);
CREATE UNIQUE INDEX learning_events_user_client_idx ON learning_events(user_id, client_event_id) WHERE client_event_id IS NOT NULL;
CREATE UNIQUE INDEX learning_events_user_source_idx ON learning_events(user_id, source_reference);
CREATE INDEX learning_events_user_skill_time_idx ON learning_events(user_id, skill, recorded_at DESC);

CREATE TABLE learning_mistakes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    skill VARCHAR(16) NOT NULL,
    practice_set_id VARCHAR(120),
    attempt_id UUID,
    question_id VARCHAR(120),
    question_type VARCHAR(120),
    learner_answer_snapshot VARCHAR(4000),
    correct_answer_ref VARCHAR(160),
    category VARCHAR(120) NOT NULL,
    method VARCHAR(24) NOT NULL,
    confidence NUMERIC(4,3) NOT NULL CHECK (confidence >= 0 AND confidence <= 1),
    evidence_code VARCHAR(120),
    evidence_text VARCHAR(500),
    detected_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    status VARCHAR(16) NOT NULL,
    classification_revision INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT learning_mistakes_unique_revision UNIQUE(user_id, attempt_id, question_id, category, classification_revision)
);
CREATE INDEX learning_mistakes_user_skill_idx ON learning_mistakes(user_id, skill, detected_at DESC);

CREATE TABLE student_learning_profiles (
    user_id UUID PRIMARY KEY REFERENCES app_users(id) ON DELETE CASCADE,
    total_practice_attempts INTEGER NOT NULL DEFAULT 0,
    total_answered_questions INTEGER NOT NULL DEFAULT 0,
    active_roadmap_item_count INTEGER NOT NULL DEFAULT 0,
    last_activity_at TIMESTAMPTZ,
    evidence_state VARCHAR(32) NOT NULL DEFAULT 'INSUFFICIENT_DATA',
    overall_trend VARCHAR(32) NOT NULL DEFAULT 'INSUFFICIENT_DATA',
    profile_confidence NUMERIC(4,3) NOT NULL DEFAULT 0,
    as_of TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE student_skill_profiles (
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    skill VARCHAR(16) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    evaluated_item_count INTEGER NOT NULL DEFAULT 0,
    accuracy_rate NUMERIC(6,5) NOT NULL DEFAULT 0,
    latest_band_estimate NUMERIC(3,1),
    evidence_state VARCHAR(32) NOT NULL DEFAULT 'INSUFFICIENT_DATA',
    recent_trend VARCHAR(32) NOT NULL DEFAULT 'INSUFFICIENT_DATA',
    strength_count INTEGER NOT NULL DEFAULT 0,
    weakness_count INTEGER NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMPTZ,
    as_of TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(user_id, skill)
);

CREATE TABLE learning_issues (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    kind VARCHAR(16) NOT NULL,
    skill VARCHAR(16) NOT NULL,
    category VARCHAR(120) NOT NULL,
    evidence_state VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    confidence NUMERIC(4,3) NOT NULL,
    occurrence_count INTEGER NOT NULL,
    attempt_count INTEGER NOT NULL,
    last_observed_at TIMESTAMPTZ,
    evidence_code VARCHAR(120),
    UNIQUE(user_id, kind, skill, category)
);

CREATE TABLE learning_roadmaps (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    status VARCHAR(16) NOT NULL,
    version INTEGER NOT NULL,
    generated_from_as_of TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE UNIQUE INDEX learning_roadmaps_user_active_idx ON learning_roadmaps(user_id) WHERE status = 'ACTIVE';

CREATE TABLE learning_roadmap_items (
    id UUID PRIMARY KEY,
    roadmap_id UUID NOT NULL REFERENCES learning_roadmaps(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    skill VARCHAR(16) NOT NULL,
    learning_objective VARCHAR(240) NOT NULL,
    activity_type VARCHAR(64) NOT NULL,
    target_error_type VARCHAR(120),
    target_question_type VARCHAR(120),
    priority INTEGER NOT NULL CHECK(priority BETWEEN 1 AND 5),
    estimated_workload VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    reason_code VARCHAR(120) NOT NULL,
    evidence_issue_id UUID,
    evidence_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    UNIQUE(roadmap_id, learning_objective)
);
CREATE INDEX learning_roadmap_items_user_status_idx ON learning_roadmap_items(user_id, status, priority);
