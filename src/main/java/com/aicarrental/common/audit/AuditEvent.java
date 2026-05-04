package com.aicarrental.common.audit;

public record AuditEvent( Long actorUserId,
                          String actorEmail,
                          String actorRole,
                          Long tenantId,
                          AuditAction action,
                          String targetType,
                          Long targetId,
                          String description) {
}
