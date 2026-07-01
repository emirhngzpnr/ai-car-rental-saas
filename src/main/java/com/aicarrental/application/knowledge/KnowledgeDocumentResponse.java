package com.aicarrental.application.knowledge;

import com.aicarrental.domain.knowledge.KnowledgeDocumentCategory;

import java.time.LocalDateTime;

public record KnowledgeDocumentResponse(
        Long id,
        Long tenantId,
        String title,
        KnowledgeDocumentCategory category,
        String content,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
