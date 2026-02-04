package com.tcc.notification.service;

import com.tcc.notification.config.NotificationProperties;
import com.tcc.notification.dto.NotificationResult;
import com.tcc.notification.event.OrderCompletedEvent;
import com.tcc.notification.event.OrderFailedEvent;
import com.tcc.notification.exception.NotificationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;

/**
 * Serviço responsável por processar e enviar notificações
 * 
 * Funcionalidades:
 * - Processa eventos de pedidos concluídos e falhos
 * - Envia emails via AWS SES (Simple Email Service)
 * - Permite simulação de falhas e latência para testes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EmailService emailService;
    private final NotificationProperties properties;
    private final Random random = new Random();
    
    @Value("${aws.ses.to-email:}")
    private String toEmail;

    /**
     * Processa evento de pedido concluído com sucesso
     * 
     * @param event Evento de pedido concluído
     * @return Resultado do processamento da notificação
     */
    public NotificationResult processOrderCompleted(OrderCompletedEvent event) {
        Instant receivedAt = Instant.now();
        log.info("[NOTIFICATION-SERVICE] 📧 ETAPA 1: Preparando notificação de SUCESSO...");
        log.info("[NOTIFICATION-SERVICE]    └── Cliente: {} | Pedido: {} | Total: R$ {}", 
                event.getCustomerId(), event.getOrderId(), event.getTotalAmount());

        try {
            // Simular latência de processamento
            simulateLatency();

            // Verificar se deve simular falha
            checkForSimulatedFailure(event.getOrderId());

            log.info("[NOTIFICATION-SERVICE] 📤 ETAPA 2: Enviando email via AWS SES...");

            // Gerar email do cliente (em produção viria do cadastro)
            String customerEmail = generateCustomerEmail(event.getCustomerId());
            String customerName = event.getCustomerId(); // Simplificado para demo
            String product = "Pedido"; // Em produção viria do evento
            String totalAmount = event.getTotalAmount() != null ? event.getTotalAmount().toString() : "N/A";

            // Enviar email via SES
            String messageId = emailService.sendOrderCompletedEmail(
                    customerEmail,
                    event.getOrderId(),
                    customerName,
                    product,
                    totalAmount
            );

            Instant processedAt = Instant.now();
            long processingTime = processedAt.toEpochMilli() - receivedAt.toEpochMilli();

            NotificationResult result = NotificationResult.builder()
                    .orderId(event.getOrderId())
                    .notificationType("EMAIL")
                    .channel("SES")
                    .success(true)
                    .message("Email de confirmação enviado para " + customerEmail)
                    .receivedAt(receivedAt)
                    .processedAt(processedAt)
                    .processingTimeMs(processingTime)
                    .messageId(messageId)
                    .build();

            log.info("[NOTIFICATION-SERVICE] ✅ Email enviado com sucesso via AWS SES");
            log.info("[NOTIFICATION-SERVICE]    └── MessageId: {}", messageId);

            return result;

        } catch (NotificationException e) {
            return handleNotificationError(event.getOrderId(), receivedAt, e);
        } catch (Exception e) {
            return handleNotificationError(event.getOrderId(), receivedAt, 
                    new NotificationException("Erro inesperado: " + e.getMessage(), e));
        }
    }

    /**
     * Processa evento de pedido que falhou
     * 
     * @param event Evento de falha de pedido
     * @return Resultado do processamento da notificação
     */
    public NotificationResult processOrderFailed(OrderFailedEvent event) {
        Instant receivedAt = Instant.now();
        log.info("[NOTIFICATION-SERVICE] 📧 ETAPA 1: Preparando notificação de FALHA...");
        log.info("[NOTIFICATION-SERVICE]    └── Cliente: {} | Pedido: {} | Erro: {}", 
                event.getCustomerId(), event.getOrderId(), event.getErrorMessage());

        try {
            // Simular latência de processamento
            simulateLatency();

            // Verificar se deve simular falha
            checkForSimulatedFailure(event.getOrderId());

            log.info("[NOTIFICATION-SERVICE] 📤 ETAPA 2: Enviando email de falha via AWS SES...");

            // Gerar email do cliente
            String customerEmail = generateCustomerEmail(event.getCustomerId());
            String customerName = event.getCustomerId();

            // Enviar email de falha via SES
            String messageId = emailService.sendOrderFailedEmail(
                    customerEmail,
                    event.getOrderId(),
                    customerName,
                    event.getErrorMessage()
            );

            Instant processedAt = Instant.now();
            long processingTime = processedAt.toEpochMilli() - receivedAt.toEpochMilli();

            NotificationResult result = NotificationResult.builder()
                    .orderId(event.getOrderId())
                    .notificationType("EMAIL")
                    .channel("SES")
                    .success(true)
                    .message("Email de falha enviado para " + customerEmail)
                    .receivedAt(receivedAt)
                    .processedAt(processedAt)
                    .processingTimeMs(processingTime)
                    .messageId(messageId)
                    .build();

            log.info("[NOTIFICATION-SERVICE] ✅ Email de falha enviado com sucesso");
            log.info("[NOTIFICATION-SERVICE]    └── MessageId: {}", messageId);

            return result;

        } catch (NotificationException e) {
            return handleNotificationError(event.getOrderId(), receivedAt, e);
        } catch (Exception e) {
            return handleNotificationError(event.getOrderId(), receivedAt, 
                    new NotificationException("Erro inesperado: " + e.getMessage(), e));
        }
    }

    /**
     * Gera email do cliente baseado no customerId
     * Em produção, isso viria do cadastro do cliente
     * No Sandbox do SES, usa o email configurado para testes
     */
    private String generateCustomerEmail(String customerId) {
        // No sandbox do SES, só podemos enviar para emails verificados
        // Então usamos o email configurado em aws.ses.to-email
        if (toEmail != null && !toEmail.isEmpty()) {
            return toEmail;
        }
        // Fallback: email fictício (só funciona fora do sandbox)
        return customerId.toLowerCase().replace(" ", ".") + "@exemplo.com";
    }

    /**
     * Simula latência de processamento para testes de performance
     */
    private void simulateLatency() {
        if (properties.getLatency().isEnabled()) {
            long minMs = properties.getLatency().getMinMs();
            long maxMs = properties.getLatency().getMaxMs();
            long delay = minMs + random.nextLong(maxMs - minMs + 1);
            
            log.debug("[SIMULATION] Aplicando latência artificial: {}ms", delay);
            
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[SIMULATION] Latência interrompida");
            }
        }
    }

    /**
     * Verifica se deve simular uma falha no envio da notificação
     */
    private void checkForSimulatedFailure(String orderId) {
        if (properties.getFailure().isEnabled()) {
            double failureRate = properties.getFailure().getRate();
            double randomValue = random.nextDouble();
            
            if (randomValue < failureRate) {
                log.warn("[SIMULATION] Simulando falha no envio - OrderId: {}", orderId);
                throw new NotificationException("Falha simulada no envio da notificação");
            }
        }
    }

    /**
     * Trata erros de notificação e retorna resultado de falha
     */
    private NotificationResult handleNotificationError(String orderId, Instant receivedAt, NotificationException e) {
        Instant processedAt = Instant.now();
        long processingTime = processedAt.toEpochMilli() - receivedAt.toEpochMilli();

        log.error("[NOTIFICATION-SERVICE] ❌ Falha - OrderId: {}, Erro: {}", orderId, e.getMessage());

        return NotificationResult.builder()
                .orderId(orderId)
                .notificationType("EMAIL")
                .channel("SES")
                .success(false)
                .errorMessage(e.getMessage())
                .receivedAt(receivedAt)
                .processedAt(processedAt)
                .processingTimeMs(processingTime)
                .build();
    }
}
