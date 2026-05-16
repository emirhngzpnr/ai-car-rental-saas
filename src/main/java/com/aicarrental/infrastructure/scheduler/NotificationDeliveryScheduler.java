package com.aicarrental.infrastructure.scheduler;

import com.aicarrental.common.audit.AuditAction;
import com.aicarrental.common.audit.AuditEvent;
import com.aicarrental.common.audit.AuditEventPublisher;
import com.aicarrental.domain.notification.Notification;
import com.aicarrental.domain.notification.NotificationStatus;
import com.aicarrental.infrastructure.notification.MockEmailSenderService;
import com.aicarrental.infrastructure.persistence.NotificationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDeliveryScheduler {
    private final NotificationRepository notificationRepository;
    private final MockEmailSenderService mockEmailSenderService;
    private final AuditEventPublisher auditEventPublisher;

    @Scheduled(fixedRate = 15_000)
    @Transactional
    public void processPendingNotifications() {

        List<Notification> pendingNotifications =
                notificationRepository.findTop20ByStatusOrderByCreatedAtAsc(
                        NotificationStatus.PENDING
                );

        if (pendingNotifications.isEmpty()) {
            return;
        }

        for (Notification notification : pendingNotifications) {

            try {

                mockEmailSenderService.sendEmail(
                        notification.getRecipient(),
                        notification.getSubject(),
                        notification.getMessage()
                );

                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                notification.setErrorMessage(null);

                auditEventPublisher.publish(
                        new AuditEvent(
                                null,
                                "SYSTEM",
                                "SYSTEM",
                                notification.getTenant().getId(),

                                AuditAction.NOTIFICATION_SENT,

                                "Notification",

                                notification.getId(),

                                "Notification sent successfully. Recipient: "
                                        + notification.getRecipient()
                        )
                );

            } catch (Exception exception) {

                notification.setStatus(NotificationStatus.FAILED);
                notification.setErrorMessage(exception.getMessage());

                auditEventPublisher.publish(
                        new AuditEvent(
                                null,
                                "SYSTEM",
                                "SYSTEM",
                                notification.getTenant().getId(),

                                AuditAction.NOTIFICATION_FAILED,

                                "Notification",

                                notification.getId(),

                                "Notification delivery failed. Reason: "
                                        + exception.getMessage()
                        )
                );

                log.error(
                        "Notification delivery failed for notificationId={}",
                        notification.getId(),
                        exception
                );
            }
        }

        notificationRepository.saveAll(pendingNotifications);
    }
}
