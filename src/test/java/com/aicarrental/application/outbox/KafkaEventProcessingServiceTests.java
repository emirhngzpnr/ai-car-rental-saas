package com.aicarrental.application.outbox;

import com.aicarrental.domain.outbox.ProcessedKafkaEvent;
import com.aicarrental.infrastructure.persistence.ProcessedKafkaEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaEventProcessingServiceTests {
    @Mock
    private ProcessedKafkaEventRepository repository;

    private KafkaEventProcessingService service;

    @BeforeEach
    void setUp() {
        service = new KafkaEventProcessingService(repository);
    }

    @Test
    void skipsHandlerWhenEventWasAlreadyProcessed() {
        Runnable handler = mock(Runnable.class);
        when(repository.existsByConsumerNameAndTopicAndMessageKey(
                "payment-notification",
                "payment-completed",
                "42"
        )).thenReturn(true);

        service.processOnce(
                "payment-notification",
                "payment-completed",
                "42",
                handler
        );

        verifyNoInteractions(handler);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void runsHandlerBeforeRecordingSuccessfulProcessing() {
        Runnable handler = mock(Runnable.class);
        when(repository.existsByConsumerNameAndTopicAndMessageKey(
                "payment-notification",
                "payment-completed",
                "42"
        )).thenReturn(false);

        service.processOnce(
                "payment-notification",
                "payment-completed",
                "42",
                handler
        );

        var order = inOrder(handler, repository);
        order.verify(handler).run();
        order.verify(repository).saveAndFlush(any(ProcessedKafkaEvent.class));
    }
}
