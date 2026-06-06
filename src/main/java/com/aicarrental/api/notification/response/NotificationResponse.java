package com.aicarrental.api.notification.response;

import com.aicarrental.domain.notification.NotificationChannel;
import com.aicarrental.domain.notification.NotificationStatus;
import com.aicarrental.domain.notification.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(Long id,
                                   Long tenantId,
                                   NotificationType type,
                                   NotificationChannel channel,
                                   NotificationStatus status,
                                   String recipient,
                                   String subject,
                                   String message,
                                   String errorMessage,
                                   LocalDateTime sentAt,
                                   LocalDateTime createdAt) {
}
