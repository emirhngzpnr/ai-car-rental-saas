package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.notification.Notification;
import com.aicarrental.domain.notification.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long>,
                                                JpaSpecificationExecutor<Notification> {

        List<Notification> findTop20ByStatusOrderByCreatedAtAsc(
                NotificationStatus status
        );

        @Query(value = """
                SELECT *
                FROM rental.notifications
                WHERE status = 'PENDING'
                ORDER BY created_at ASC
                LIMIT 20
                FOR UPDATE SKIP LOCKED
                """, nativeQuery = true)
        List<Notification> findAndLockNextPendingNotifications();
}
