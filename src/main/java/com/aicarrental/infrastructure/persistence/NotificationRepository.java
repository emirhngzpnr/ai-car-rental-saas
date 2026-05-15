package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.notification.Notification;
import com.aicarrental.domain.notification.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

        List<Notification> findTop20ByStatusOrderByCreatedAtAsc(
                NotificationStatus status
        );


}
