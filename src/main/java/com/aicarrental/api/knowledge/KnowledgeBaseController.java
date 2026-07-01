package com.aicarrental.api.knowledge;

import com.aicarrental.application.knowledge.KnowledgeBaseService;
import com.aicarrental.application.knowledge.KnowledgeDocumentRequest;
import com.aicarrental.application.knowledge.KnowledgeDocumentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-base/documents")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TENANT_ADMIN','TENANT_STAFF')")
public class KnowledgeBaseController {
    private final KnowledgeBaseService knowledgeBaseService;

    @GetMapping
    public ResponseEntity<List<KnowledgeDocumentResponse>> list() {
        return ResponseEntity.ok(knowledgeBaseService.listCurrentTenantDocuments());
    }

    @PostMapping
    public ResponseEntity<KnowledgeDocumentResponse> create(
            @Valid @RequestBody KnowledgeDocumentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(knowledgeBaseService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeDocumentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody KnowledgeDocumentRequest request
    ) {
        return ResponseEntity.ok(knowledgeBaseService.update(id, request));
    }

    @PostMapping("/{id}/reembed")
    public ResponseEntity<KnowledgeDocumentResponse> reembed(@PathVariable Long id) {
        return ResponseEntity.ok(knowledgeBaseService.reembed(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        knowledgeBaseService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
