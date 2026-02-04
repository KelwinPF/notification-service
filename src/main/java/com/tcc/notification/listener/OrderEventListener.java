package com.tcc.notification.listener;

import com.tcc.notification.dto.NotificationResult;
import com.tcc.notification.event.OrderCompletedEvent;
import com.tcc.notification.event.OrderFailedEvent;
import com.tcc.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Listener Kafka para consumir eventos de pedidos
 * 
 * Tópicos consumidos:
 * - orders.completed: Pedidos processados com sucesso pelo Billing Service
 * - orders.failed: Pedidos que falharam no Billing Service
 * 
 * Este componente é o ponto de entrada do Notification Service,
 * recebendo eventos via Kafka e delegando o processamento ao NotificationService.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final NotificationService notificationService;

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
        Instant receivedTimestamp = Instant.now();
        
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
            
            logNotificationResult(result, "orders.completed");
            
        } catch (Exception e) {
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
        Instant receivedTimestamp = Instant.now();
        
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
            
            logNotificationResult(result, "orders.failed");
            
        } catch (Exception e) {
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
}
