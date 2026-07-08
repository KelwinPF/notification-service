package com.tcc.notification.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Métricas customizadas para o Notification Service
 * 
 * Métricas coletadas para o experimento do TCC:
 * - notification_processing_total: Total de notificações processadas
 * - notification_email_sent_total: Total de emails enviados
 * - notification_email_failed_total: Total de emails que falharam
 * - notification_processing_time_seconds: Tempo de processamento
 * - notification_active_processing: Processamentos ativos no momento
 */
@Slf4j
@Component
@Getter
public class NotificationMetrics {

    private final Counter processingTotal;
    private final Counter emailSentSuccess;
    private final Counter emailSentFailure;
    private final Counter orderCompletedEvents;
    private final Counter orderFailedEvents;
    private final Timer processingTime;
    private final AtomicLong activeProcessing;

    public NotificationMetrics(MeterRegistry meterRegistry) {
        // Contador total de processamentos
        this.processingTotal = Counter.builder("notification.processing.total")
                .description("Total de notificações processadas")
                .tag("service", "notification-service")
                .register(meterRegistry);

        // Contador de emails enviados com sucesso
        this.emailSentSuccess = Counter.builder("notification.email.sent.total")
                .description("Total de emails enviados com sucesso")
                .tag("service", "notification-service")
                .tag("status", "success")
                .register(meterRegistry);

        // Contador de emails que falharam
        this.emailSentFailure = Counter.builder("notification.email.sent.total")
                .description("Total de emails que falharam")
                .tag("service", "notification-service")
                .tag("status", "failure")
                .register(meterRegistry);

        // Contador de eventos OrderCompleted recebidos
        this.orderCompletedEvents = Counter.builder("notification.events.received.total")
                .description("Total de eventos OrderCompleted recebidos")
                .tag("service", "notification-service")
                .tag("event_type", "OrderCompleted")
                .register(meterRegistry);

        // Contador de eventos OrderFailed recebidos
        this.orderFailedEvents = Counter.builder("notification.events.received.total")
                .description("Total de eventos OrderFailed recebidos")
                .tag("service", "notification-service")
                .tag("event_type", "OrderFailed")
                .register(meterRegistry);

        // Timer para medir tempo de processamento
        this.processingTime = Timer.builder("notification.processing.time")
                .description("Tempo de processamento de notificações")
                .tag("service", "notification-service")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry);

        // Gauge para processamentos ativos
        this.activeProcessing = new AtomicLong(0);
        Gauge.builder("notification.processing.active", activeProcessing, AtomicLong::get)
                .description("Número de processamentos ativos no momento")
                .tag("service", "notification-service")
                .register(meterRegistry);

        log.info("[NOTIFICATION-METRICS] Métricas customizadas registradas com sucesso");
    }

    public void recordOrderCompletedReceived() {
        processingTotal.increment();
        orderCompletedEvents.increment();
        activeProcessing.incrementAndGet();
    }

    public void recordOrderFailedReceived() {
        processingTotal.increment();
        orderFailedEvents.increment();
        activeProcessing.incrementAndGet();
    }

    public void recordEmailSuccess(String notificationType) {
        emailSentSuccess.increment();
        activeProcessing.decrementAndGet();
        log.debug("[NOTIFICATION-METRICS] Email enviado com sucesso - tipo: {}", notificationType);
    }

    public void recordEmailFailure(String notificationType) {
        emailSentFailure.increment();
        activeProcessing.decrementAndGet();
        log.debug("[NOTIFICATION-METRICS] Email falhou - tipo: {}", notificationType);
    }
    
    public void recordProcessingTime(long processingTimeMs) {
        processingTime.record(java.time.Duration.ofMillis(processingTimeMs));
    }
}
