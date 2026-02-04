package com.tcc.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO que representa o resultado de uma tentativa de envio de notificação
 * 
 * Usado para logging e rastreabilidade do processo de notificação
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResult {

    private String orderId;
    private String notificationType;    // "EMAIL", "SMS", "PUSH"
    private String channel;             // "SNS", "MOCK"
    private boolean success;
    private String message;
    private String errorMessage;
    
    // Metadados de processamento
    private Instant receivedAt;         // Quando o evento foi recebido
    private Instant processedAt;        // Quando a notificação foi processada
    private Long processingTimeMs;      // Tempo de processamento em ms
    private String messageId;           // ID da mensagem no SNS (se aplicável)
}
