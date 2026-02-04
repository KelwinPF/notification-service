package com.tcc.notification.exception;

/**
 * Exceção customizada para erros no processo de notificação
 * 
 * Usada para:
 * - Falhas no envio de notificação
 * - Erros de comunicação com SNS
 * - Simulação de falhas para testes
 */
public class NotificationException extends RuntimeException {

    public NotificationException(String message) {
        super(message);
    }

    public NotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
