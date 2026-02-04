package com.tcc.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Notification Service - Serviço de Notificação
 * 
 * Microsserviço orientado a eventos responsável por:
 * - Consumir eventos Kafka (orders.completed, orders.failed)
 * - Simular envio de notificações externas (e-mail/SMS)
 * - Publicar notificações via SNS simulado
 * 
 * Este serviço NÃO expõe endpoints HTTP de negócio.
 * Apenas /actuator/health está disponível para healthchecks.
 * 
 * Arquitetura híbrida de microsserviços - Protótipo experimental
 */
@SpringBootApplication
public class NotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}
