package com.aicarrental.application.knowledge;

import com.aicarrental.domain.knowledge.KnowledgeDocumentCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record KnowledgeDocumentRequest(
        @NotBlank @Size(max = 180) String title,
        @NotNull KnowledgeDocumentCategory category,
        @NotBlank @Size(max = 20000) String content
) {
}
