CREATE TABLE writing_submission_versions (
    id UUID PRIMARY KEY,
    submission_id UUID NOT NULL REFERENCES practice_submissions(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL CHECK (version_number >= 1),
    parent_version_id UUID REFERENCES writing_submission_versions(id) ON DELETE SET NULL,
    response_text TEXT NOT NULL,
    word_count INTEGER NOT NULL CHECK (word_count >= 0),
    content_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT writing_submission_versions_num_uq UNIQUE (submission_id, version_number)
);

CREATE INDEX writing_submission_versions_sub_created_idx
    ON writing_submission_versions(submission_id, created_at DESC);
