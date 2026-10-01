CREATE TABLE ai_conversations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    skill VARCHAR(16),
    practice_set_id VARCHAR(120),
    attempt_id UUID,
    question_id VARCHAR(120),
    title VARCHAR(240),
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ai_conversations_user_updated_idx ON ai_conversations(user_id, updated_at DESC);

CREATE TABLE ai_messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    sequence_no INTEGER NOT NULL,
    role VARCHAR(16) NOT NULL,
    content VARCHAR(12000) NOT NULL,
    response_status VARCHAR(48),
    grounding_status VARCHAR(48),
    citations_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    context_snapshot_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE(conversation_id, sequence_no)
);
CREATE INDEX ai_messages_conversation_sequence_idx ON ai_messages(conversation_id, sequence_no);

CREATE TABLE ai_conversation_summaries (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    revision INTEGER NOT NULL,
    summary_text VARCHAR(6000) NOT NULL,
    covered_through_sequence INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE(conversation_id, revision)
);
