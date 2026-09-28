CREATE TABLE ai_attachments (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    original_filename VARCHAR(255) NOT NULL,
    sanitized_filename VARCHAR(255) NOT NULL,
    media_type VARCHAR(120) NOT NULL,
    attachment_kind VARCHAR(16) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0 AND size_bytes <= 10485760),
    sha256 CHAR(64),
    storage_key VARCHAR(512) NOT NULL UNIQUE,
    status VARCHAR(16) NOT NULL,
    processing_error_code VARCHAR(96),
    processing_started_at TIMESTAMPTZ,
    processing_attempts INTEGER NOT NULL DEFAULT 0 CHECK (processing_attempts >= 0),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ,
    CONSTRAINT ai_attachments_kind_ck CHECK (attachment_kind IN ('DOCUMENT', 'IMAGE')),
    CONSTRAINT ai_attachments_status_ck CHECK (status IN ('STORED', 'PROCESSING', 'READY', 'FAILED', 'REMOVED', 'EXPIRED'))
);

CREATE INDEX ai_attachments_owner_conversation_status_idx
    ON ai_attachments(owner_user_id, conversation_id, status);
CREATE INDEX ai_attachments_storage_key_idx ON ai_attachments(storage_key);
CREATE INDEX ai_attachments_checksum_idx ON ai_attachments(sha256);

CREATE TABLE ai_attachment_chunks (
    id UUID PRIMARY KEY,
    attachment_id UUID NOT NULL REFERENCES ai_attachments(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL CHECK (chunk_index >= 0),
    page_number INTEGER,
    section_label VARCHAR(500),
    content TEXT NOT NULL,
    token_estimate INTEGER NOT NULL CHECK (token_estimate > 0),
    embedding vector(768),
    embedding_provider VARCHAR(64),
    embedding_model VARCHAR(240),
    embedding_dimension INTEGER,
    embedding_version VARCHAR(96),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ai_attachment_chunks_embedding_dimension_ck CHECK (embedding_dimension IS NULL OR embedding_dimension = 768),
    CONSTRAINT ai_attachment_chunks_attachment_index_uq UNIQUE (attachment_id, chunk_index)
);

CREATE INDEX ai_attachment_chunks_attachment_idx ON ai_attachment_chunks(attachment_id);
CREATE INDEX ai_attachment_chunks_embedding_space_idx
    ON ai_attachment_chunks(embedding_provider, embedding_model, embedding_dimension, embedding_version);

CREATE TABLE ai_message_attachments (
    message_id UUID NOT NULL REFERENCES ai_messages(id) ON DELETE CASCADE,
    attachment_id UUID NOT NULL REFERENCES ai_attachments(id) ON DELETE RESTRICT,
    ordinal INTEGER NOT NULL CHECK (ordinal >= 0),
    PRIMARY KEY (message_id, attachment_id),
    UNIQUE (message_id, ordinal)
);

CREATE INDEX ai_message_attachments_attachment_idx ON ai_message_attachments(attachment_id);
