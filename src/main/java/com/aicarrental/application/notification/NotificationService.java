package com.aicarrental.application.notification;
import com.aicarrental.common.audit.AuditAction;
import com.aicarrental.common.audit.AuditEvent;
import com.aicarrental.common.audit.AuditEventPublisher;
import com.aicarrental.common.event.PaymentCompletedEvent;
import com.aicarrental.common.event.RefundCompletedEvent;
import com.aicarrental.domain.notification.*;
import com.aicarrental.domain.tenant.Tenant;
import com.aicarrental.infrastructure.persistence.NotificationRepository;
import com.aicarrental.infrastructure.persistence.TenantRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final TenantRepository tenantRepository;
    private final AuditEventPublisher auditEventPublisher;

    public void createPaymentCompletedNotification(
            PaymentCompletedEvent event
    ) {

        Tenant tenant = tenantRepository.findById(event.tenantId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Tenant not found")
                );

        Notification notification = Notification.builder()
                .tenant(tenant)
                .type(NotificationType.PAYMENT_COMPLETED)
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.PENDING)

                .recipient("customer@example.com")

                .subject("Payment Completed")

                .message(
                        "Your payment has been completed successfully. "
                                + "Payment ID: " + event.paymentId()
                                + ", Amount: " + event.amount()
                                + " " + event.currency()
                )
                .build();

        Notification savedNotification =
                notificationRepository.save(notification);

        auditEventPublisher.publish(
                new AuditEvent(
                        null,
                        "SYSTEM",
                        "SYSTEM",
                        tenant.getId(),

                        AuditAction.NOTIFICATION_CREATED,

                        "Notification",

                        savedNotification.getId(),

                        "Notification created. Type: "
                                + savedNotification.getType()
                                + ", Channel: "
                                + savedNotification.getChannel()
                                + ", Recipient: "
                                + savedNotification.getRecipient()
                )
        );

    }
    public void createRefundCompletedNotification(
            RefundCompletedEvent event
    ) {
        Tenant tenant = tenantRepository.findById(event.tenantId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Tenant not found")
                );

        Notification notification = Notification.builder()
                .tenant(tenant)
                .type(NotificationType.REFUND_COMPLETED)
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.PENDING)
                .recipient("customer@example.com")
                .subject("Refund Completed")
                .message(
                        "Your refund has been completed successfully. "
                                + "Refund Amount: " + event.refundAmount()
                                + " " + event.currency()
                )
                .build();

        Notification savedNotification =
                notificationRepository.save(notification);

        auditEventPublisher.publish(
                new AuditEvent(
                        null,
                        "SYSTEM",
                        "SYSTEM",
                        tenant.getId(),
                        AuditAction.NOTIFICATION_CREATED,
                        "Notification",
                        savedNotification.getId(),
                        "Refund notification created. Recipient: "
                                + savedNotification.getRecipient()
                )
        );
    }
}
