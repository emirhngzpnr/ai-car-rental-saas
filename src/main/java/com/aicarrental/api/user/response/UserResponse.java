package com.aicarrental.api.user.response;

import java.time.LocalDateTime;

public record UserResponse(Long id,
                           String firstName,
                           String lastName,
                           String email,
                           String role,
                           Boolean active,
                           Long tenantId,
                           String tenantName,
                           LocalDateTime createdAt,
                           LocalDateTime updatedAt) {
}
