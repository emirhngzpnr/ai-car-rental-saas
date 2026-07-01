package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.knowledge.TenantKnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantKnowledgeDocumentRepository extends JpaRepository<TenantKnowledgeDocument, Long> {
    List<TenantKnowledgeDocument> findByTenant_IdAndActiveTrueOrderByUpdatedAtDesc(Long tenantId);

    Optional<TenantKnowledgeDocument> findByIdAndTenant_Id(Long id, Long tenantId);

    Optional<TenantKnowledgeDocument> findByIdAndTenant_IdAndActiveTrue(Long id, Long tenantId);
}
