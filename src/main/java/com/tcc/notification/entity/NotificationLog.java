package com.tcc.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "notification_logs", schema = "notification")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLog {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "correlation_id", length = 36)
    private String correlationId;

    @Column(name = "customer_id", length = 100)
    private String customerId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "notification_type", length = 30)
    private String notificationType;

    @Column(name = "channel", length = 30)
    private String channel;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "message_id", length = 255)
    private String messageId;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
