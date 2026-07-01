CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE rental.tenant_knowledge_documents (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES rental.tenants(id),
    title VARCHAR(180) NOT NULL,
    category VARCHAR(40) NOT NULL,
    content TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_tenant_knowledge_document_category CHECK (
        category IN (
            'RENTAL_POLICY',
            'DEPOSIT_POLICY',
            'INSURANCE_POLICY',
            'CANCELLATION_POLICY',
            'FUEL_POLICY',
            'DELIVERY_POLICY',
            'GENERAL'
        )
    )
);

CREATE TABLE rental.tenant_knowledge_chunks (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES rental.tenants(id),
    document_id BIGINT NOT NULL REFERENCES rental.tenant_knowledge_documents(id) ON DELETE CASCADE,
    chunk_text TEXT NOT NULL,
    embedding vector(384) NOT NULL,
    chunk_index INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenant_knowledge_chunk_document_index UNIQUE (document_id, chunk_index)
);

CREATE INDEX idx_tenant_knowledge_documents_tenant_active
    ON rental.tenant_knowledge_documents(tenant_id, active);

CREATE INDEX idx_tenant_knowledge_documents_category
    ON rental.tenant_knowledge_documents(category);

CREATE INDEX idx_tenant_knowledge_chunks_tenant
    ON rental.tenant_knowledge_chunks(tenant_id);

CREATE INDEX idx_tenant_knowledge_chunks_document
    ON rental.tenant_knowledge_chunks(document_id);
