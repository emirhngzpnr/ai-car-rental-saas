package com.aicarrental.infrastructure.scheduler;

import com.aicarrental.common.event.*;
import com.aicarrental.domain.outbox.OutboxEventType;
import com.aicarrental.domain.outbox.OutboxMessage;
import com.aicarrental.domain.outbox.OutboxMessageStatus;
import com.aicarrental.infrastructure.kafka.*;
import com.aicarrental.infrastructure.persistence.OutboxMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisherScheduler {
    private static final int MAX_RETRY_COUNT = 3;

    private final OutboxMessageRepository outboxMessageRepository;
    private final PaymentEventProducer paymentEventProducer;
    private final ObjectMapper objectMapper;
    private final RentalEventProducer rentalEventProducer;
    private final RefundEventProducer refundEventProducer;
    private final ReservationEventProducer reservationEventProducer;
    private final AiPricingEventProducer aiPricingEventProducer;

    @Scheduled(fixedRate = 10_000)
    @Transactional
    public void publishPendingMessages() {
        List<OutboxMessage> pendingMessages =
                outboxMessageRepository.findAndLockNextPendingMessages();

        if (pendingMessages.isEmpty()) {
            return;
        }

        for (OutboxMessage message : pendingMessages) {
            try {
                publishMessage(message);

                message.setStatus(OutboxMessageStatus.PUBLISHED);
                message.setProcessedAt(LocalDateTime.now());
                message.setErrorMessage(null);

            } catch (Exception exception) {
                int retryCount = message.getRetryCount() + 1;

                message.setRetryCount(retryCount);
                message.setErrorMessage(exception.getMessage());

                if (retryCount >= MAX_RETRY_COUNT) {
                    message.setStatus(OutboxMessageStatus.FAILED);
                }

                log.error(
                        "Failed to publish outbox message id={}, retryCount={}",
                        message.getId(),
                        retryCount,
                        exception
                );
            }
        }

        outboxMessageRepository.saveAll(pendingMessages);
    }

    private void publishMessage(OutboxMessage message) throws Exception {
        if (message.getEventType() == OutboxEventType.PAYMENT_COMPLETED) {
            PaymentCompletedEvent event =
                    objectMapper.readValue(
                            message.getPayload(),
                            PaymentCompletedEvent.class
                    );

            CompletableFuture<SendResult<String, Object>> future =
                    paymentEventProducer.publishPaymentCompleted(
                            event,
                            message.getMessageKey()
                    );

            future.get(3, java.util.concurrent.TimeUnit.SECONDS);
            return;
        }
        if (message.getEventType() == OutboxEventType.RENTAL_COMPLETED) {
            RentalCompletedEvent event =
                    objectMapper.readValue(
                            message.getPayload(),
                            RentalCompletedEvent.class
                    );

            CompletableFuture<SendResult<String, Object>> future =
                    rentalEventProducer.publishRentalCompleted(
                            event,
                            message.getMessageKey()
                    );

            future.get(3, TimeUnit.SECONDS);
            return;
        }
        if (message.getEventType() == OutboxEventType.REFUND_COMPLETED) {
            RefundCompletedEvent event =
                    objectMapper.readValue(
                            message.getPayload(),
                            RefundCompletedEvent.class
                    );

            CompletableFuture<SendResult<String, Object>> future =
                    refundEventProducer.publishRefundCompleted(
                            event,
                            message.getMessageKey()
                    );

            future.get(3, TimeUnit.SECONDS);
            return;
        }
        if (message.getEventType() == OutboxEventType.RESERVATION_CREATED) {
            ReservationCreatedEvent event =
                    objectMapper.readValue(
                            message.getPayload(),
                            ReservationCreatedEvent.class
                    );

            CompletableFuture<SendResult<String, Object>> future =
                    reservationEventProducer.publishReservationCreated(
                            event,
                            message.getMessageKey()
                    );

            future.get(3, TimeUnit.SECONDS);
            return;
        }

        if (message.getEventType() == OutboxEventType.RESERVATION_EXPIRED) {
            ReservationExpiredEvent event =
                    objectMapper.readValue(
                            message.getPayload(),
                            ReservationExpiredEvent.class
                    );

            CompletableFuture<SendResult<String, Object>> future =
                    reservationEventProducer.publishReservationExpired(
                            event,
                            message.getMessageKey()
                    );

            future.get(3, TimeUnit.SECONDS);
            return;
        }

        if (message.getEventType() == OutboxEventType.AI_PRICING_APPROVED) {
            AiPricingApprovedEvent event =
                    objectMapper.readValue(
                            message.getPayload(),
                            AiPricingApprovedEvent.class
                    );

            CompletableFuture<SendResult<String, Object>> future =
                    aiPricingEventProducer.publishAiPricingApproved(
                            event,
                            message.getMessageKey()
                    );

            future.get(3, TimeUnit.SECONDS);
            return;
        }
        throw new IllegalArgumentException(
                "Unsupported outbox event type: " + message.getEventType()
        );
    }
}
