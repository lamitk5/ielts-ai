CREATE TABLE IF NOT EXISTS practice_generation_sources (
    id UUID PRIMARY KEY,
    title VARCHAR(240) NOT NULL,
    source_type VARCHAR(40) NOT NULL,
    author VARCHAR(240),
    rights_status VARCHAR(24) NOT NULL DEFAULT 'PENDING_REVIEW' CHECK (rights_status IN ('PENDING_REVIEW', 'APPROVED', 'RESTRICTED', 'REJECTED')),
    license_note TEXT NOT NULL DEFAULT '',
    normalized_content TEXT NOT NULL,
    checksum CHAR(64) NOT NULL UNIQUE,
    created_by UUID NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS practice_generation_blueprints (
    id UUID PRIMARY KEY,
    skill VARCHAR(16) NOT NULL CHECK (skill IN ('READING', 'LISTENING', 'WRITING', 'SPEAKING')),
    title VARCHAR(240) NOT NULL,
    target_band NUMERIC(3,1) NOT NULL,
    blueprint_schema JSONB NOT NULL,
    created_by UUID NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS practice_generation_jobs (
    id UUID PRIMARY KEY,
    source_id UUID NOT NULL REFERENCES practice_generation_sources(id) ON DELETE RESTRICT,
    blueprint_id UUID NOT NULL REFERENCES practice_generation_blueprints(id) ON DELETE RESTRICT,
    skill VARCHAR(16) NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('PENDING', 'GENERATING', 'VALIDATING', 'COMPLETED', 'FAILED')),
    model_id VARCHAR(80) NOT NULL,
    prompt_template_version VARCHAR(40) NOT NULL,
    error_message VARCHAR(2000),
    created_by UUID NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS generated_practice_sets (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES practice_generation_jobs(id) ON DELETE RESTRICT,
    skill VARCHAR(16) NOT NULL,
    title VARCHAR(240) NOT NULL,
    current_version_id UUID,
    state VARCHAR(24) NOT NULL CHECK (state IN ('DRAFT', 'GENERATING', 'AUTO_VALIDATING', 'PENDING_REVIEW', 'APPROVED', 'NEEDS_REVISION', 'REJECTED')),
    published_set_id VARCHAR(120) UNIQUE,
    approved_by UUID REFERENCES app_users(id) ON DELETE SET NULL,
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS generated_practice_versions (
    id UUID PRIMARY KEY,
    set_id UUID NOT NULL REFERENCES generated_practice_sets(id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    passage_content JSONB NOT NULL,
    questions_payload JSONB NOT NULL,
    novelty_report JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT generated_practice_versions_uq UNIQUE (set_id, version_number)
);

CREATE TABLE IF NOT EXISTS generation_validation_results (
    id UUID PRIMARY KEY,
    version_id UUID NOT NULL REFERENCES generated_practice_versions(id) ON DELETE CASCADE,
    validator_name VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('PASS', 'WARNING', 'FAIL')),
    findings JSONB NOT NULL DEFAULT '[]'::jsonb,
    executed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS practice_review_actions (
    id UUID PRIMARY KEY,
    set_id UUID NOT NULL REFERENCES generated_practice_sets(id) ON DELETE CASCADE,
    version_id UUID NOT NULL REFERENCES generated_practice_versions(id) ON DELETE CASCADE,
    admin_id UUID NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    action VARCHAR(24) NOT NULL CHECK (action IN ('APPROVE', 'REQUEST_REVISION', 'REJECT')),
    reviewer_notes TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS practice_provenance_meta (
    set_id VARCHAR(120) PRIMARY KEY,
    generation_job_id UUID REFERENCES practice_generation_jobs(id) ON DELETE SET NULL,
    blueprint_id UUID REFERENCES practice_generation_blueprints(id) ON DELETE SET NULL,
    approver_id UUID REFERENCES app_users(id) ON DELETE SET NULL,
    approved_at TIMESTAMPTZ NOT NULL,
    provenance_details JSONB NOT NULL DEFAULT '{}'::jsonb
);
