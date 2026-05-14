package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.outbox.OutboxMessage;
import com.aicarrental.domain.outbox.OutboxMessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {
    @Query(value = """
        SELECT *
        FROM rental.outbox_messages
        WHERE status = 'PENDING'
        ORDER BY created_at ASC
        LIMIT 20
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<OutboxMessage> findAndLockNextPendingMessages();
}
