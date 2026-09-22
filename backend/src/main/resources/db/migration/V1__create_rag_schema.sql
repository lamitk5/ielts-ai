CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE rag_documents (
    id UUID PRIMARY KEY,
    title VARCHAR(240) NOT NULL,
    source_type VARCHAR(40) NOT NULL,
    author VARCHAR(240),
    organization VARCHAR(240),
    language VARCHAR(32) NOT NULL,
    skill VARCHAR(16) NOT NULL,
    rights_status VARCHAR(24) NOT NULL DEFAULT 'PENDING_REVIEW',
    rights_note VARCHAR(2000) NOT NULL DEFAULT '',
    active BOOLEAN NOT NULL DEFAULT FALSE,
    current_version_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT rag_documents_skill_ck CHECK (skill IN ('GENERAL', 'READING', 'LISTENING', 'WRITING', 'SPEAKING')),
    CONSTRAINT rag_documents_rights_ck CHECK (rights_status IN ('PENDING_REVIEW', 'APPROVED', 'RESTRICTED', 'REJECTED'))
);

CREATE TABLE rag_document_versions (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES rag_documents(id) ON DELETE RESTRICT,
    version VARCHAR(64) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(120) NOT NULL,
    file_size_bytes BIGINT NOT NULL CHECK (file_size_bytes > 0),
    checksum CHAR(64) NOT NULL,
    storage_path VARCHAR(1000) NOT NULL,
    extraction_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    index_status VARCHAR(32) NOT NULL DEFAULT 'NOT_INDEXED',
    approved_at TIMESTAMPTZ,
    indexed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT rag_document_versions_version_uq UNIQUE (document_id, version)
);

ALTER TABLE rag_documents
    ADD CONSTRAINT rag_documents_current_version_fk
    FOREIGN KEY (current_version_id) REFERENCES rag_document_versions(id) ON DELETE RESTRICT;

CREATE INDEX rag_document_versions_checksum_idx ON rag_document_versions(checksum);

CREATE TABLE rag_chunks (
    id UUID PRIMARY KEY,
    document_version_id UUID NOT NULL REFERENCES rag_document_versions(id) ON DELETE RESTRICT,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    page_number INTEGER,
    section_title VARCHAR(500),
    token_count INTEGER NOT NULL CHECK (token_count > 0),
    embedding vector(768),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT rag_chunks_version_index_uq UNIQUE (document_version_id, chunk_index)
);

CREATE INDEX rag_chunks_embedding_hnsw_idx
    ON rag_chunks USING hnsw (embedding vector_cosine_ops);

CREATE TABLE rag_ingestion_jobs (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES rag_documents(id) ON DELETE RESTRICT,
    document_version_id UUID NOT NULL REFERENCES rag_document_versions(id) ON DELETE RESTRICT,
    status VARCHAR(24) NOT NULL,
    error_code VARCHAR(64),
    error_message VARCHAR(2000),
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX rag_documents_status_idx ON rag_documents(rights_status, active);
CREATE INDEX rag_document_versions_status_idx ON rag_document_versions(index_status, approved_at, indexed_at);
CREATE INDEX rag_ingestion_jobs_created_idx ON rag_ingestion_jobs(created_at DESC);
