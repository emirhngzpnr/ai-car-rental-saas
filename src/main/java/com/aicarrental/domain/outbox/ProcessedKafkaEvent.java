package com.aicarrental.domain.outbox;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "processed_kafka_events",
        schema = "rental",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_processed_kafka_event",
                columnNames = {"consumer_name", "topic", "message_key"}
        ),
        indexes = @Index(
                name = "idx_processed_kafka_events_processed_at",
                columnList = "processed_at"
        )
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedKafkaEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consumer_name", nullable = false, length = 100)
    private String consumerName;

    @Column(name = "topic", nullable = false, length = 100)
    private String topic;

    @Column(name = "message_key", nullable = false, length = 200)
    private String messageKey;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @PrePersist
    void onCreate() {
        if (processedAt == null) {
            processedAt = LocalDateTime.now();
        }
    }
}
