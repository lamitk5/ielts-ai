CREATE TABLE learner_onboarding_profiles (
    user_id UUID PRIMARY KEY REFERENCES app_users(id) ON DELETE CASCADE,
    self_reported_level VARCHAR(24),
    target_band NUMERIC(3,1) CHECK (target_band IS NULL OR (target_band >= 0.0 AND target_band <= 9.0)),
    target_exam_date DATE,
    perceived_weakest_skill VARCHAR(16) CHECK (perceived_weakest_skill IS NULL OR perceived_weakest_skill IN ('READING', 'LISTENING', 'WRITING', 'SPEAKING')),
    daily_study_minutes INTEGER CHECK (daily_study_minutes IS NULL OR (daily_study_minutes >= 5 AND daily_study_minutes <= 240)),
    study_days_per_week INTEGER CHECK (study_days_per_week IS NULL OR (study_days_per_week >= 1 AND study_days_per_week <= 7)),
    state VARCHAR(16) NOT NULL DEFAULT 'NOT_STARTED',
    source VARCHAR(24) NOT NULL DEFAULT 'SELF_REPORTED',
    basis VARCHAR(32) NOT NULL DEFAULT 'LEARNER_DECLARATION',
    measured_level VARCHAR(24),
    measured_weakest_skill VARCHAR(16) CHECK (measured_weakest_skill IS NULL OR measured_weakest_skill IN ('READING', 'LISTENING', 'WRITING', 'SPEAKING')),
    evidence_reference VARCHAR(160),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT learner_onboarding_state_ck CHECK (state IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'SKIPPED')),
    CONSTRAINT learner_onboarding_source_ck CHECK (source IN ('SELF_REPORTED', 'MEASURED_EVIDENCE')),
    CONSTRAINT learner_onboarding_level_ck CHECK (self_reported_level IS NULL OR self_reported_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT learner_onboarding_version_ck CHECK (version >= 0),
    CONSTRAINT learner_onboarding_self_report_not_evidence CHECK (
        (source = 'SELF_REPORTED' AND evidence_reference IS NULL AND measured_level IS NULL AND measured_weakest_skill IS NULL)
        OR source = 'MEASURED_EVIDENCE'
    )
);

CREATE INDEX learner_onboarding_profiles_state_idx ON learner_onboarding_profiles(state);