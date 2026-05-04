package com.aicarrental.common.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AuditEventListener {
    private final AuditLogRepository auditLogRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT,
                                fallbackExecution = true)
    public void handle(AuditEvent event) {
        AuditLog auditLog = AuditLog.builder()
                .actorUserId(event.actorUserId())
                .actorEmail(event.actorEmail())
                .actorRole(event.actorRole())
                .tenantId(event.tenantId())
                .action(event.action())
                .targetType(event.targetType())
                .targetId(event.targetId())
                .description(event.description())
                .createdAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
    }
}
