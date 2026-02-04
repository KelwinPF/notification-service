package com.tcc.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Evento recebido do Kafka quando o faturamento falha
 *
 * Publicado por: Billing Service
 * Tópico: orders.failed
 *
 * Representa uma falha no processamento de faturamento
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderFailedEvent {

    private String orderId;
    private String customerId;
    private BigDecimal amount;
    
    // Informações da falha
    private String errorMessage;
    private String errorType;
    
    // Metadados do evento
    private String eventType;       // "OrderFailed"
    private Instant eventTime;      // Momento da publicação
    private Instant failedAt;       // Momento da falha
    private Long processingTimeMs;  // Tempo até a falha em ms
}
