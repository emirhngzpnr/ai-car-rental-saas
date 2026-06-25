package com.aicarrental.application.outbox;

import com.aicarrental.domain.outbox.ProcessedKafkaEvent;
import com.aicarrental.infrastructure.persistence.ProcessedKafkaEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventProcessingService {
    private final ProcessedKafkaEventRepository processedEventRepository;

    @Transactional
    public void processOnce(
            String consumerName,
            String topic,
            String messageKey,
            Runnable handler
    ) {
        if (processedEventRepository.existsByConsumerNameAndTopicAndMessageKey(
                consumerName,
                topic,
                messageKey
        )) {
            log.info(
                    "Skipping previously processed Kafka event. consumer={}, topic={}, key={}",
                    consumerName,
                    topic,
                    messageKey
            );
            return;
        }

        handler.run();

        processedEventRepository.saveAndFlush(
                ProcessedKafkaEvent.builder()
                        .consumerName(consumerName)
                        .topic(topic)
                        .messageKey(messageKey)
                        .build()
        );
    }
}
