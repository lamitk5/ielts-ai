CREATE TABLE IF NOT EXISTS submission_question_results (
    id UUID PRIMARY KEY,
    submission_id UUID NOT NULL REFERENCES practice_submissions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    question_id VARCHAR(160) NOT NULL,
    question_type VARCHAR(80) NOT NULL,
    learner_answer TEXT NOT NULL DEFAULT '',
    normalized_learner_answer TEXT NOT NULL DEFAULT '',
    correct_answer TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL,
    evidence_reference VARCHAR(240) NOT NULL DEFAULT '',
    explanation TEXT NOT NULL DEFAULT '',
    scoring_policy_version VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (submission_id, question_id)
);

CREATE INDEX IF NOT EXISTS submission_question_results_owner_idx
    ON submission_question_results (user_id, submission_id, question_id);

CREATE INDEX IF NOT EXISTS submission_question_results_submission_correct_idx
    ON submission_question_results (submission_id, is_correct);
