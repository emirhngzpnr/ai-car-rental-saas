package com.aicarrental.application.knowledge;

import com.aicarrental.domain.knowledge.KnowledgeDocumentCategory;
import com.aicarrental.domain.knowledge.TenantKnowledgeDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class KnowledgeChunkStore {
    private final JdbcTemplate jdbcTemplate;
    private final KnowledgeEmbeddingService embeddingService;

    @Transactional
    public void replaceChunks(TenantKnowledgeDocument document, List<String> chunks) {
        jdbcTemplate.update(
                "DELETE FROM rental.tenant_knowledge_chunks WHERE document_id = ?",
                document.getId()
        );

        for (int index = 0; index < chunks.size(); index++) {
            jdbcTemplate.update("""
                    INSERT INTO rental.tenant_knowledge_chunks
                        (tenant_id, document_id, chunk_text, embedding, chunk_index, created_at)
                    VALUES (?, ?, ?, CAST(? AS rental.vector), ?, CURRENT_TIMESTAMP)
                    """,
                    document.getTenant().getId(),
                    document.getId(),
                    chunks.get(index),
                    embeddingService.embedAsVectorLiteral(chunks.get(index)),
                    index
            );
        }
    }

    @Transactional(readOnly = true)
    public List<KnowledgeChunkSearchResult> searchTenantChunks(Long tenantId, String query, int limit) {
        return jdbcTemplate.query("""
                SELECT
                    d.id AS document_id,
                    d.title,
                    d.category,
                    t.company_name AS tenant_name,
                    c.chunk_text,
                    (c.embedding OPERATOR(rental.<=>) CAST(? AS rental.vector)) AS distance
                FROM rental.tenant_knowledge_chunks c
                JOIN rental.tenant_knowledge_documents d ON d.id = c.document_id
                JOIN rental.tenants t ON t.id = d.tenant_id
                WHERE c.tenant_id = ?
                  AND d.active = true
                  AND t.active = true
                ORDER BY c.embedding OPERATOR(rental.<=>) CAST(? AS rental.vector)
                LIMIT ?
                """,
                (rs, rowNum) -> map(rs),
                embeddingService.embedAsVectorLiteral(query),
                tenantId,
                embeddingService.embedAsVectorLiteral(query),
                limit
        );
    }

    private KnowledgeChunkSearchResult map(ResultSet rs) throws SQLException {
        return new KnowledgeChunkSearchResult(
                rs.getLong("document_id"),
                rs.getString("title"),
                KnowledgeDocumentCategory.valueOf(rs.getString("category")),
                rs.getString("tenant_name"),
                rs.getString("chunk_text"),
                rs.getDouble("distance")
        );
    }
}
