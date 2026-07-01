package com.aicarrental.application.knowledge;

import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.exception.ResourceNotFoundException;
import com.aicarrental.common.security.CurrentUserService;
import com.aicarrental.domain.auth.User;
import com.aicarrental.domain.knowledge.TenantKnowledgeDocument;
import com.aicarrental.domain.tenant.Tenant;
import com.aicarrental.infrastructure.persistence.TenantKnowledgeDocumentRepository;
import com.aicarrental.infrastructure.persistence.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {
    private static final int MAX_CHUNK_LENGTH = 1_200;

    private final TenantKnowledgeDocumentRepository documentRepository;
    private final TenantRepository tenantRepository;
    private final CurrentUserService currentUserService;
    private final KnowledgeChunkStore chunkStore;

    @Transactional(readOnly = true)
    public List<KnowledgeDocumentResponse> listCurrentTenantDocuments() {
        return documentRepository.findByTenant_IdAndActiveTrueOrderByUpdatedAtDesc(resolveTenantId())
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional
    public KnowledgeDocumentResponse create(KnowledgeDocumentRequest request) {
        Tenant tenant = tenantRepository.findByIdAndActiveTrue(resolveTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        TenantKnowledgeDocument document = TenantKnowledgeDocument.builder()
                .tenant(tenant)
                .title(cleanTitle(request.title()))
                .category(request.category())
                .content(cleanContent(request.content()))
                .active(true)
                .build();
        TenantKnowledgeDocument saved = documentRepository.saveAndFlush(document);
        chunkStore.replaceChunks(saved, splitIntoChunks(saved.getContent()));
        return map(saved);
    }

    @Transactional
    public KnowledgeDocumentResponse update(Long id, KnowledgeDocumentRequest request) {
        TenantKnowledgeDocument document = documentRepository.findByIdAndTenant_IdAndActiveTrue(id, resolveTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge document not found"));
        document.setTitle(cleanTitle(request.title()));
        document.setCategory(request.category());
        document.setContent(cleanContent(request.content()));
        TenantKnowledgeDocument saved = documentRepository.saveAndFlush(document);
        chunkStore.replaceChunks(saved, splitIntoChunks(saved.getContent()));
        return map(saved);
    }

    @Transactional
    public KnowledgeDocumentResponse reembed(Long id) {
        TenantKnowledgeDocument document = documentRepository.findByIdAndTenant_IdAndActiveTrue(id, resolveTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge document not found"));
        chunkStore.replaceChunks(document, splitIntoChunks(document.getContent()));
        return map(document);
    }

    @Transactional
    public void deactivate(Long id) {
        TenantKnowledgeDocument document = documentRepository.findByIdAndTenant_IdAndActiveTrue(id, resolveTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge document not found"));
        document.setActive(false);
        documentRepository.save(document);
    }

    private Long resolveTenantId() {
        User user = currentUserService.getCurrentUser();
        if (user.getTenant() == null) {
            throw new BusinessException("Knowledge base requires a tenant-scoped user");
        }
        return user.getTenant().getId();
    }

    private String cleanTitle(String value) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.isBlank()) {
            throw new BusinessException("Document title is required");
        }
        return cleaned;
    }

    private String cleanContent(String value) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.length() < 20) {
            throw new BusinessException("Document content must be at least 20 characters");
        }
        return cleaned;
    }

    private List<String> splitIntoChunks(String content) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = content.split("\\R\\s*\\R");
        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            String cleaned = paragraph.trim();
            if (cleaned.isBlank()) {
                continue;
            }
            if (current.length() + cleaned.length() + 2 > MAX_CHUNK_LENGTH && !current.isEmpty()) {
                chunks.add(current.toString().trim());
                current.setLength(0);
            }
            if (cleaned.length() > MAX_CHUNK_LENGTH) {
                for (int offset = 0; offset < cleaned.length(); offset += MAX_CHUNK_LENGTH) {
                    chunks.add(cleaned.substring(offset, Math.min(cleaned.length(), offset + MAX_CHUNK_LENGTH)));
                }
            } else {
                current.append(cleaned).append("\n\n");
            }
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
        }
        return chunks.isEmpty() ? List.of(content) : chunks;
    }

    private KnowledgeDocumentResponse map(TenantKnowledgeDocument document) {
        return new KnowledgeDocumentResponse(
                document.getId(),
                document.getTenant().getId(),
                document.getTitle(),
                document.getCategory(),
                document.getContent(),
                document.getActive(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
