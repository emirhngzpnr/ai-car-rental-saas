package com.aicarrental.infrastructure.scheduler;

import com.aicarrental.common.event.PaymentCompletedEvent;
import com.aicarrental.domain.outbox.OutboxEventType;
import com.aicarrental.domain.outbox.OutboxMessage;
import com.aicarrental.domain.outbox.OutboxMessageStatus;
import com.aicarrental.infrastructure.kafka.*;
import com.aicarrental.infrastructure.persistence.OutboxMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherSchedulerTests {
    @Mock OutboxMessageRepository repository;
    @Mock PaymentEventProducer paymentProducer;
    @Mock RentalEventProducer rentalProducer;
    @Mock RefundEventProducer refundProducer;
    @Mock ReservationEventProducer reservationProducer;
    @Mock AiPricingEventProducer aiPricingProducer;

    private OutboxPublisherScheduler scheduler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        scheduler = new OutboxPublisherScheduler(
                repository,
                paymentProducer,
                objectMapper,
                rentalProducer,
                refundProducer,
                reservationProducer,
                aiPricingProducer
        );
        ReflectionTestUtils.setField(scheduler, "maxRetryCount", 3);
        ReflectionTestUtils.setField(scheduler, "baseDelaySeconds", 10L);
        ReflectionTestUtils.setField(scheduler, "maxDelaySeconds", 60L);
        ReflectionTestUtils.setField(scheduler, "sendTimeoutSeconds", 1L);
    }

    @Test
    void marksMessagePublishedAfterKafkaAcknowledgement() throws Exception {
        OutboxMessage message = paymentMessage();
        when(repository.findAndLockNextPendingMessages()).thenReturn(List.of(message));
        when(paymentProducer.publishPaymentCompleted(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        scheduler.publishPendingMessages();

        assertEquals(OutboxMessageStatus.PUBLISHED, message.getStatus());
        assertNotNull(message.getProcessedAt());
        assertNotNull(message.getLastAttemptAt());
        assertNull(message.getNextAttemptAt());
        assertNull(message.getErrorMessage());
    }

    @Test
    void schedulesExponentialRetryWhenKafkaPublishFails() throws Exception {
        OutboxMessage message = paymentMessage();
        when(repository.findAndLockNextPendingMessages()).thenReturn(List.of(message));
        when(paymentProducer.publishPaymentCompleted(any(), any()))
                .thenReturn(CompletableFuture.failedFuture(
                        new IllegalStateException("Kafka unavailable")
                ));

        LocalDateTime before = LocalDateTime.now();
        scheduler.publishPendingMessages();

        assertEquals(OutboxMessageStatus.PENDING, message.getStatus());
        assertEquals(1, message.getRetryCount());
        assertNotNull(message.getNextAttemptAt());
        assertTrue(message.getNextAttemptAt().isAfter(before.plusSeconds(9)));
        assertEquals("java.lang.IllegalStateException: Kafka unavailable", message.getErrorMessage());
    }

    @Test
    void marksMessageFailedAfterMaximumAttempts() throws Exception {
        OutboxMessage message = paymentMessage();
        message.setRetryCount(2);
        when(repository.findAndLockNextPendingMessages()).thenReturn(List.of(message));
        when(paymentProducer.publishPaymentCompleted(any(), any()))
                .thenReturn(CompletableFuture.failedFuture(
                        new IllegalStateException("Kafka unavailable")
                ));

        scheduler.publishPendingMessages();

        assertEquals(OutboxMessageStatus.FAILED, message.getStatus());
        assertEquals(3, message.getRetryCount());
        assertNull(message.getNextAttemptAt());
    }

    private OutboxMessage paymentMessage() throws Exception {
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                42L,
                24L,
                3L,
                BigDecimal.valueOf(500),
                "TRY",
                "DEPOSIT_PAYMENT",
                LocalDateTime.now()
        );

        return OutboxMessage.builder()
                .id(1L)
                .topic("payment-completed")
                .messageKey("42")
                .eventType(OutboxEventType.PAYMENT_COMPLETED)
                .payload(objectMapper.writeValueAsString(event))
                .status(OutboxMessageStatus.PENDING)
                .retryCount(0)
                .build();
    }
}
