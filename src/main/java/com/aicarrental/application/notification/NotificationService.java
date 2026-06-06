package com.aicarrental.application.notification;
import com.aicarrental.api.notification.response.NotificationResponse;
import com.aicarrental.common.audit.AuditAction;
import com.aicarrental.common.audit.AuditEvent;
import com.aicarrental.common.audit.AuditEventPublisher;
import com.aicarrental.common.event.*;
import com.aicarrental.common.security.CurrentUserService;
import com.aicarrental.domain.auth.User;
import com.aicarrental.domain.notification.*;
import com.aicarrental.domain.tenant.Tenant;
import com.aicarrental.infrastructure.persistence.NotificationRepository;
import com.aicarrental.infrastructure.persistence.TenantRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final TenantRepository tenantRepository;
    private final AuditEventPublisher auditEventPublisher;
    private final CurrentUserService currentUserService;

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

    public void createReservationCreatedNotification(
            ReservationCreatedEvent event
    ) {
        Tenant tenant = tenantRepository.findById(event.tenantId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Tenant not found")
                );

        Notification notification = Notification.builder()
                .tenant(tenant)
                .type(NotificationType.RESERVATION_CREATED)
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.PENDING)
                .recipient(event.customerEmail())
                .subject("Reservation Created")
                .message(
                        "Dear " + event.customerFullName()
                                + ", your reservation for "
                                + event.vehicleBrand() + " "
                                + event.vehicleModel()
                                + " has been created successfully."
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
                        "Reservation created notification created. Recipient: "
                                + savedNotification.getRecipient()
                )
        );
    }
    public void createReservationExpiredNotification(
            ReservationExpiredEvent event
    ) {
        Tenant tenant = tenantRepository.findById(event.tenantId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Tenant not found")
                );

        Notification notification = Notification.builder()
                .tenant(tenant)
                .type(NotificationType.RESERVATION_EXPIRED)
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.PENDING)
                .recipient(event.customerEmail())
                .subject("Reservation Expired")
                .message(
                        "Dear " + event.customerFullName()
                                + ", your reservation for "
                                + event.vehicleBrand() + " "
                                + event.vehicleModel()
                                + " has expired because payment was not completed in time."
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
                        "Reservation expired notification created. Recipient: "
                                + savedNotification.getRecipient()
                )
        );
    }
    public void createAiPricingApprovedNotification(
            AiPricingApprovedEvent event
    ) {
        Tenant tenant = tenantRepository.findById(event.tenantId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Tenant not found")
                );

        Notification notification = Notification.builder()
                .tenant(tenant)
                .type(NotificationType.AI_PRICING_APPROVED)
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.PENDING)
                .recipient(event.approvedByEmail())
                .subject("AI Pricing Recommendation Approved")
                .message(
                        "AI pricing recommendation approved for vehicle "
                                + event.vehicleBrand() + " "
                                + event.vehicleModel()
                                + ". Old price: "
                                + event.oldPrice()
                                + ", New price: "
                                + event.newPrice()
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
                        "AI pricing approved notification created. Recipient: "
                                + savedNotification.getRecipient()
                )
        );
    }
    public Page<NotificationResponse> getNotifications(
            NotificationType type,
            NotificationChannel channel,
            NotificationStatus status,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore,
            Pageable pageable
    ) {
        User currentUser = currentUserService.getCurrentUser();

        Long tenantId = currentUserService.isSuperAdmin(currentUser)
                ? null
                : currentUserService.getCurrentTenantId();

        Specification<Notification> specification =
                NotificationSpecification.hasTenantId(tenantId)
                        .and(NotificationSpecification.hasType(type))
                        .and(NotificationSpecification.hasChannel(channel))
                        .and(NotificationSpecification.hasStatus(status))
                        .and(NotificationSpecification.createdAfter(createdAfter))
                        .and(NotificationSpecification.createdBefore(createdBefore));
        return notificationRepository
                .findAll(specification, pageable)
                .map(this::mapToResponse);
    }
    private NotificationResponse mapToResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTenant() != null
                        ? notification.getTenant().getId()
                        : null,
                notification.getType(),
                notification.getChannel(),
                notification.getStatus(),
                notification.getRecipient(),
                notification.getSubject(),
                notification.getMessage(),
                notification.getErrorMessage(),
                notification.getSentAt(),
                notification.getCreatedAt()
        );
    }
}
