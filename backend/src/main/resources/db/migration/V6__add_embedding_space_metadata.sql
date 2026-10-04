-- Additive only: old vectors remain available for audit and rollback, but are
-- not retrievable until a complete embedding profile is recorded by reindexing.
ALTER TABLE rag_document_versions
    ADD COLUMN IF NOT EXISTS embedding_provider VARCHAR(32),
    ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(240),
    ADD COLUMN IF NOT EXISTS embedding_dimension INTEGER,
    ADD COLUMN IF NOT EXISTS embedding_version VARCHAR(64);

ALTER TABLE rag_chunks
    ADD COLUMN IF NOT EXISTS embedding_provider VARCHAR(32),
    ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(240),
    ADD COLUMN IF NOT EXISTS embedding_dimension INTEGER,
    ADD COLUMN IF NOT EXISTS embedding_version VARCHAR(64);

CREATE INDEX IF NOT EXISTS rag_document_versions_embedding_space_idx
    ON rag_document_versions(embedding_provider, embedding_model, embedding_dimension, embedding_version);

CREATE INDEX IF NOT EXISTS rag_chunks_embedding_space_idx
    ON rag_chunks(embedding_provider, embedding_model, embedding_dimension, embedding_version);

UPDATE rag_document_versions
SET index_status = 'REINDEX_REQUIRED'
WHERE index_status = 'INDEXED'
  AND (embedding_provider IS NULL OR embedding_model IS NULL OR embedding_dimension IS NULL
       OR embedding_version IS NULL OR embedding_dimension <> 768);
