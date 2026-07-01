package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.knowledge.TenantKnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantKnowledgeChunkRepository extends JpaRepository<TenantKnowledgeChunk, Long> {
    void deleteByDocument_Id(Long documentId);
}
