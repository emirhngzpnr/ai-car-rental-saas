package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.outbox.ProcessedKafkaEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedKafkaEventRepository extends JpaRepository<ProcessedKafkaEvent, Long> {
    boolean existsByConsumerNameAndTopicAndMessageKey(
            String consumerName,
            String topic,
            String messageKey
    );
}
