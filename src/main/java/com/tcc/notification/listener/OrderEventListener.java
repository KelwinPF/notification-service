package com.tcc.notification.listener;

import com.tcc.notification.config.NotificationMetrics;
import com.tcc.notification.dto.NotificationResult;
import com.tcc.notification.entity.NotificationLog;
import com.tcc.notification.event.OrderCompletedEvent;
import com.tcc.notification.event.OrderFailedEvent;
import com.tcc.notification.repository.NotificationLogRepository;
import com.tcc.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Listener Kafka para consumir eventos de pedidos
 * 
 * Tópicos consumidos:
 * - orders.completed: Pedidos processados com sucesso pelo Billing Service
 * - orders.failed: Pedidos que falharam no Billing Service
 * 
 * Este componente é o ponto de entrada do Notification Service,
 * recebendo eventos via Kafka e delegando o processamento ao NotificationService.
 * 
 * Inclui métricas de observabilidade para o experimento do TCC.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final NotificationService notificationService;
    private final NotificationMetrics notificationMetrics;
    private final NotificationLogRepository notificationLogRepository;

    /**
     * Consome eventos do tópico 'orders.completed'
     * 
     * Acionado quando o Billing Service processa um pedido com sucesso.
     * Dispara o envio de notificação de confirmação ao cliente.
     * 
     * @param event Evento de pedido concluído
     */
    @KafkaListener(
            topics = "orders.completed",
            groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderCompleted(OrderCompletedEvent event) {
        long startTime = System.currentTimeMillis();
        Instant receivedTimestamp = Instant.now();
        
        // Registra evento recebido nas métricas
        notificationMetrics.recordOrderCompletedReceived();
        
        log.info("");
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║  [NOTIFICATION-SERVICE] EVENTO KAFKA RECEBIDO              ║");
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║  Topic:     orders.completed  ");
        log.info("║  OrderId:   {}  ", event.getOrderId());
        log.info("║  Customer:  {}  ", event.getCustomerId());
        log.info("║  BillingId: {}  ", event.getBillingId());
        log.info("║  Total:     R$ {}  ", event.getTotalAmount());
        log.info("╚══════════════════════════════════════════════════════════════╝");

        try {
            NotificationResult result = notificationService.processOrderCompleted(event);
            
            // Registra métricas de sucesso/falha
            if (result.isSuccess()) {
                notificationMetrics.recordEmailSuccess("order_confirmation");
                notificationMetrics.recordProcessingTime(System.currentTimeMillis() - startTime);
            } else {
                notificationMetrics.recordEmailFailure("order_confirmation");
            }
            
            logNotificationResult(result, "orders.completed");
            persistNotificationLog(result, "OrderCompleted", event.getCustomerId(), event.getCorrelationId());
            
        } catch (Exception e) {
            // Registra falha nas métricas
            notificationMetrics.recordEmailFailure("order_confirmation");
            persistNotificationLog(event.getOrderId(), "OrderCompleted", event.getCustomerId(), event.getCorrelationId(), e.getMessage());
            
            log.error("[KAFKA] Erro ao processar OrderCompleted - OrderId: {}, Erro: {}", 
                    event.getOrderId(), e.getMessage(), e);
            // Re-lançar exceção para que o Kafka possa fazer retry ou enviar para DLQ
            throw e;
        }
    }

    /**
     * Consome eventos do tópico 'orders.failed'
     * 
     * Acionado quando o Billing Service falha ao processar um pedido.
     * Dispara o envio de notificação de falha ao cliente.
     * 
     * @param event Evento de falha de pedido
     */
    @KafkaListener(
            topics = "orders.failed",
            groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderFailed(OrderFailedEvent event) {
        long startTime = System.currentTimeMillis();
        Instant receivedTimestamp = Instant.now();
        
        // Registra evento recebido nas métricas
        notificationMetrics.recordOrderFailedReceived();
        
        log.info("");
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║  [NOTIFICATION-SERVICE] ⚠️ EVENTO DE FALHA RECEBIDO        ║");
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║  Topic:    orders.failed  ");
        log.info("║  OrderId:  {}  ", event.getOrderId());
        log.info("║  Customer: {}  ", event.getCustomerId());
        log.info("║  Erro:     {}  ", event.getErrorMessage());
        log.info("╚══════════════════════════════════════════════════════════════╝");

        try {
            NotificationResult result = notificationService.processOrderFailed(event);
            
            // Registra métricas de sucesso/falha
            if (result.isSuccess()) {
                notificationMetrics.recordEmailSuccess("order_failure_alert");
                notificationMetrics.recordProcessingTime(System.currentTimeMillis() - startTime);
            } else {
                notificationMetrics.recordEmailFailure("order_failure_alert");
            }
            
            logNotificationResult(result, "orders.failed");
            persistNotificationLog(result, "OrderFailed", event.getCustomerId(), event.getCorrelationId());
            
        } catch (Exception e) {
            // Registra falha nas métricas
            notificationMetrics.recordEmailFailure("order_failure_alert");
            persistNotificationLog(event.getOrderId(), "OrderFailed", event.getCustomerId(), event.getCorrelationId(), e.getMessage());
            
            log.error("[KAFKA] Erro ao processar OrderFailed - OrderId: {}, Erro: {}", 
                    event.getOrderId(), e.getMessage(), e);
            // Re-lançar exceção para que o Kafka possa fazer retry ou enviar para DLQ
            throw e;
        }
    }

    /**
     * Loga o resultado do processamento da notificação
     */
    private void logNotificationResult(NotificationResult result, String eventType) {
        if (result.isSuccess()) {
            log.info("");
            log.info("╔══════════════════════════════════════════════════════════════╗");
            log.info("║  [NOTIFICATION-SERVICE] ✅ NOTIFICAÇÃO ENVIADA              ║");
            log.info("╠══════════════════════════════════════════════════════════════╣");
            log.info("║  OrderId:   {}  ", result.getOrderId());
            log.info("║  Canal:     {} via {}  ", result.getNotificationType(), result.getChannel());
            log.info("║  MessageId: {}  ", result.getMessageId());
            log.info("║  Tempo:     {}ms  ", result.getProcessingTimeMs());
            log.info("╚══════════════════════════════════════════════════════════════╝");
            log.info("");
        } else {
            log.error("");
            log.error("╔══════════════════════════════════════════════════════════════╗");
            log.error("║  [NOTIFICATION-SERVICE] ❌ FALHA AO ENVIAR                ║");
            log.error("╠══════════════════════════════════════════════════════════════╣");
            log.error("║  OrderId: {}  ", result.getOrderId());
            log.error("║  Erro:    {}  ", result.getErrorMessage());
            log.error("╚══════════════════════════════════════════════════════════════╝");
            log.error("");
        }
    }

    private void persistNotificationLog(NotificationResult result, String eventType, String customerId, String correlationId) {
        try {
            NotificationLog logEntry = NotificationLog.builder()
                    .id(UUID.randomUUID().toString())
                    .orderId(result.getOrderId())
                    .correlationId(correlationId)
                    .customerId(customerId)
                    .eventType(eventType)
                    .notificationType(result.getNotificationType())
                    .channel(result.getChannel())
                    .success(result.isSuccess())
                    .messageId(result.getMessageId())
                    .errorMessage(result.getErrorMessage())
                    .processingTimeMs(result.getProcessingTimeMs())
                    .build();
            notificationLogRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("[NOTIFICATION-SERVICE] Falha ao persistir log de notificacao: {}", e.getMessage());
        }
    }

    private void persistNotificationLog(String orderId, String eventType, String customerId, String correlationId, String errorMessage) {
        try {
            NotificationLog logEntry = NotificationLog.builder()
                    .id(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .correlationId(correlationId)
                    .customerId(customerId)
                    .eventType(eventType)
                    .success(false)
                    .errorMessage(errorMessage != null && errorMessage.length() > 500 ? errorMessage.substring(0, 500) : errorMessage)
                    .build();
            notificationLogRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("[NOTIFICATION-SERVICE] Falha ao persistir log de notificacao: {}", e.getMessage());
        }
    }
}
