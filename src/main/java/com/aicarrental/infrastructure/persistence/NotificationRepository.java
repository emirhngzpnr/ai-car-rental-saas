package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.notification.Notification;
import com.aicarrental.domain.notification.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long>,
                                                JpaSpecificationExecutor<Notification> {

        List<Notification> findTop20ByStatusOrderByCreatedAtAsc(
                NotificationStatus status
        );


}
