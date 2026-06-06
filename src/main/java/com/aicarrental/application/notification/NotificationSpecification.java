package com.aicarrental.application.notification;

import com.aicarrental.domain.notification.Notification;
import com.aicarrental.domain.notification.NotificationChannel;
import com.aicarrental.domain.notification.NotificationStatus;
import com.aicarrental.domain.notification.NotificationType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class NotificationSpecification {
    public static Specification<Notification> hasTenantId(Long tenantId) {
        return (root, query, criteriaBuilder) ->
                tenantId == null
                        ? criteriaBuilder.conjunction()
                        : criteriaBuilder.equal(
                        root.get("tenant").get("id"),
                        tenantId
                );
    }

    public static Specification<Notification> hasType(NotificationType type) {
        return (root, query, criteriaBuilder) ->
                type == null
                        ? criteriaBuilder.conjunction()
                        : criteriaBuilder.equal(
                        root.get("type"),
                        type
                );
    }

    public static Specification<Notification> hasChannel(NotificationChannel channel) {
        return (root, query, criteriaBuilder) ->
                channel == null
                        ? criteriaBuilder.conjunction()
                        : criteriaBuilder.equal(
                        root.get("channel"),
                        channel
                );
    }

    public static Specification<Notification> hasStatus(NotificationStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null
                        ? criteriaBuilder.conjunction()
                        : criteriaBuilder.equal(
                        root.get("status"),
                        status
                );
    }

    public static Specification<Notification> createdAfter(LocalDateTime createdAfter) {
        return (root, query, criteriaBuilder) ->
                createdAfter == null
                        ? criteriaBuilder.conjunction()
                        : criteriaBuilder.greaterThanOrEqualTo(
                        root.get("createdAt"),
                        createdAfter
                );
    }

    public static Specification<Notification> createdBefore(LocalDateTime createdBefore) {
        return (root, query, criteriaBuilder) ->
                createdBefore == null
                        ? criteriaBuilder.conjunction()
                        : criteriaBuilder.lessThanOrEqualTo(
                        root.get("createdAt"),
                        createdBefore
                );
    }
}
