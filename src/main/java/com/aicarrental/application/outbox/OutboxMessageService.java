package com.aicarrental.application.outbox;

import com.aicarrental.domain.outbox.OutboxEventType;
import com.aicarrental.domain.outbox.OutboxMessage;
import com.aicarrental.domain.outbox.OutboxMessageStatus;
import com.aicarrental.infrastructure.persistence.OutboxMessageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class OutboxMessageService {
    private final OutboxMessageRepository outboxMessageRepository;
    private final ObjectMapper objectMapper;

    public void createOutboxMessage(
            String topic,
            String messageKey,
            OutboxEventType eventType,
            Object payload
    ) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);

            OutboxMessage outboxMessage = OutboxMessage.builder()
                    .topic(topic)
                    .messageKey(messageKey)
                    .eventType(eventType)
                    .payload(jsonPayload)
                    .status(OutboxMessageStatus.PENDING)
                    .build();

            outboxMessageRepository.save(outboxMessage);

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize outbox payload", exception);
        }
    }
}
