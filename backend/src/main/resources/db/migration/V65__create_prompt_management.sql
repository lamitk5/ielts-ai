CREATE TABLE prompt_definitions (
    id UUID PRIMARY KEY,
    prompt_key VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    purpose TEXT NOT NULL DEFAULT '',
    created_by UUID REFERENCES app_users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE prompt_versions (
    id UUID PRIMARY KEY,
    definition_id UUID NOT NULL REFERENCES prompt_definitions(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL CHECK (version_number > 0),
    content TEXT NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    created_by UUID REFERENCES app_users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL,
    activated_by UUID REFERENCES app_users(id) ON DELETE SET NULL,
    activated_at TIMESTAMPTZ,
    CONSTRAINT prompt_versions_uq UNIQUE (definition_id, version_number)
);
CREATE UNIQUE INDEX prompt_versions_one_active_uq ON prompt_versions(definition_id) WHERE status = 'ACTIVE';
