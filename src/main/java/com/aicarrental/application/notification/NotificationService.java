package com.aicarrental.application.notification;
import com.aicarrental.common.event.PaymentCompletedEvent;
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

        notificationRepository.save(notification);
    }
}
