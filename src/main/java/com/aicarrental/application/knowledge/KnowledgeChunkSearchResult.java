package com.aicarrental.application.knowledge;

import com.aicarrental.domain.knowledge.KnowledgeDocumentCategory;

public record KnowledgeChunkSearchResult(
        Long documentId,
        String title,
        KnowledgeDocumentCategory category,
        String tenantName,
        String chunkText,
        double distance
) {
}
