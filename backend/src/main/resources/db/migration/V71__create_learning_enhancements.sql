CREATE TABLE vocabulary_items (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    word VARCHAR(180) NOT NULL,
    normalized_word VARCHAR(180) NOT NULL,
    meaning TEXT NOT NULL,
    example_sentence TEXT,
    note TEXT,
    source VARCHAR(64) NOT NULL DEFAULT 'manual',
    source_reference_id VARCHAR(180),
    status VARCHAR(16) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    last_reviewed_at TIMESTAMPTZ,
    review_count INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT vocabulary_status_ck CHECK (status IN ('NEW', 'LEARNING', 'MASTERED')),
    CONSTRAINT vocabulary_review_count_ck CHECK (review_count >= 0),
    CONSTRAINT vocabulary_user_word_uq UNIQUE (user_id, normalized_word)
);
CREATE INDEX vocabulary_items_user_status_idx ON vocabulary_items(user_id, status, updated_at DESC);

CREATE TABLE mock_test_definitions (
    id UUID PRIMARY KEY,
    slug VARCHAR(120) NOT NULL UNIQUE,
    title VARCHAR(240) NOT NULL,
    version VARCHAR(64) NOT NULL DEFAULT 'v1',
    total_time_limit_seconds INTEGER NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT mock_definition_time_ck CHECK (total_time_limit_seconds > 0)
);

CREATE TABLE mock_test_definition_sections (
    id UUID PRIMARY KEY,
    mock_test_id UUID NOT NULL REFERENCES mock_test_definitions(id) ON DELETE CASCADE,
    section_order INTEGER NOT NULL CHECK (section_order >= 0),
    skill VARCHAR(32) NOT NULL,
    practice_set_id VARCHAR(120) NOT NULL,
    time_limit_seconds INTEGER NOT NULL CHECK (time_limit_seconds > 0),
    CONSTRAINT mock_definition_skill_ck CHECK (skill IN ('LISTENING', 'READING', 'WRITING', 'SPEAKING')),
    CONSTRAINT mock_definition_section_uq UNIQUE (mock_test_id, section_order)
);
CREATE INDEX mock_definition_sections_idx ON mock_test_definition_sections(mock_test_id, section_order);

INSERT INTO mock_test_definitions (id, slug, title, version, total_time_limit_seconds, published, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000071', 'mock-test-academic-01', 'IELTS Academic Full Mock Test 1', 'v1', 10800, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (slug) DO NOTHING;
INSERT INTO mock_test_definition_sections (id, mock_test_id, section_order, skill, practice_set_id, time_limit_seconds)
VALUES
 ('00000000-0000-0000-0000-000000000072', '00000000-0000-0000-0000-000000000071', 0, 'LISTENING', 'default-listening-set', 1800),
 ('00000000-0000-0000-0000-000000000073', '00000000-0000-0000-0000-000000000071', 1, 'READING', 'default-reading-set', 3600),
 ('00000000-0000-0000-0000-000000000074', '00000000-0000-0000-0000-000000000071', 2, 'WRITING', 'default-writing-set', 3600)
ON CONFLICT (mock_test_id, section_order) DO NOTHING;
