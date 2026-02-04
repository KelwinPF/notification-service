package com.tcc.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.time.Instant;

/**
 * Serviço de envio de emails via AWS SES
 * 
 * Envia emails transacionais para notificar clientes sobre:
 * - Pedidos processados com sucesso
 * - Falhas no processamento de pedidos
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final SesClient sesClient;

    @Value("${aws.ses.from-email:noreply@exemplo.com}")
    private String fromEmail;

    @Value("${aws.ses.enabled:true}")
    private boolean sesEnabled;

    @Value("${aws.ses.mode:LOCAL}")
    private String mode;

    /**
     * Envia email de confirmação de pedido processado
     */
    public String sendOrderCompletedEmail(String toEmail, String orderId, String customerName, 
                                          String product, String totalAmount) {
        
        String subject = "✅ Pedido #" + orderId.substring(0, 8) + " confirmado!";
        
        String htmlBody = buildOrderCompletedHtml(orderId, customerName, product, totalAmount);
        String textBody = buildOrderCompletedText(orderId, customerName, product, totalAmount);
        
        return sendEmail(toEmail, subject, htmlBody, textBody, orderId);
    }

    /**
     * Envia email de falha no processamento do pedido
     */
    public String sendOrderFailedEmail(String toEmail, String orderId, String customerName, 
                                       String errorMessage) {
        
        String subject = "⚠️ Problema com seu pedido #" + orderId.substring(0, 8);
        
        String htmlBody = buildOrderFailedHtml(orderId, customerName, errorMessage);
        String textBody = buildOrderFailedText(orderId, customerName, errorMessage);
        
        return sendEmail(toEmail, subject, htmlBody, textBody, orderId);
    }

    /**
     * Envia email genérico via SES
     */
    private String sendEmail(String toEmail, String subject, String htmlBody, String textBody, String orderId) {
        log.info("");
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║  [EMAIL-SERVICE] 📧 ENVIANDO EMAIL                          ║");
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║  Para:    {}  ", toEmail);
        log.info("║  Assunto: {}  ", subject);
        log.info("║  Modo:    {}  ", mode);
        log.info("╚══════════════════════════════════════════════════════════════╝");

        if (!sesEnabled || sesClient == null) {
            log.warn("[EMAIL-SERVICE] ⚠️ SES desabilitado - Email simulado");
            return "MOCK-" + System.currentTimeMillis();
        }

        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(fromEmail)
                    .destination(Destination.builder()
                            .toAddresses(toEmail)
                            .build())
                    .message(Message.builder()
                            .subject(Content.builder()
                                    .charset("UTF-8")
                                    .data(subject)
                                    .build())
                            .body(Body.builder()
                                    .html(Content.builder()
                                            .charset("UTF-8")
                                            .data(htmlBody)
                                            .build())
                                    .text(Content.builder()
                                            .charset("UTF-8")
                                            .data(textBody)
                                            .build())
                                    .build())
                            .build())
                    .build();

            SendEmailResponse response = sesClient.sendEmail(request);
            String messageId = response.messageId();

            log.info("");
            log.info("╔══════════════════════════════════════════════════════════════╗");
            log.info("║  [EMAIL-SERVICE] ✅ EMAIL ENVIADO COM SUCESSO!              ║");
            log.info("╠══════════════════════════════════════════════════════════════╣");
            log.info("║  MessageId: {}  ", messageId);
            log.info("║  OrderId:   {}  ", orderId);
            log.info("╚══════════════════════════════════════════════════════════════╝");
            log.info("");

            return messageId;

        } catch (Exception e) {
            log.error("");
            log.error("╔══════════════════════════════════════════════════════════════╗");
            log.error("║  [EMAIL-SERVICE] ❌ FALHA AO ENVIAR EMAIL                   ║");
            log.error("╠══════════════════════════════════════════════════════════════╣");
            log.error("║  OrderId: {}  ", orderId);
            log.error("║  Erro:    {}  ", e.getMessage());
            log.error("╚══════════════════════════════════════════════════════════════╝");
            log.error("");
            throw new RuntimeException("Falha ao enviar email via SES: " + e.getMessage(), e);
        }
    }

    // ==================== TEMPLATES HTML ====================

    private String buildOrderCompletedHtml(String orderId, String customerName, 
                                           String product, String totalAmount) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #28a745, #20c997); color: white; padding: 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; }
                    .content { padding: 30px; }
                    .order-box { background: #f8f9fa; border-radius: 8px; padding: 20px; margin: 20px 0; }
                    .order-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #dee2e6; }
                    .order-row:last-child { border-bottom: none; }
                    .total { font-size: 24px; color: #28a745; font-weight: bold; }
                    .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #6c757d; font-size: 12px; }
                    .btn { display: inline-block; background: #28a745; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>✅ Pedido Confirmado!</h1>
                    </div>
                    <div class="content">
                        <p>Olá, <strong>%s</strong>!</p>
                        <p>Ótima notícia! Seu pedido foi processado com sucesso.</p>
                        
                        <div class="order-box">
                            <div class="order-row">
                                <span><strong>Número do Pedido:</strong></span>
                                <span>#%s</span>
                            </div>
                            <div class="order-row">
                                <span><strong>Produto:</strong></span>
                                <span>%s</span>
                            </div>
                            <div class="order-row">
                                <span><strong>Data:</strong></span>
                                <span>%s</span>
                            </div>
                            <div class="order-row">
                                <span><strong>Total:</strong></span>
                                <span class="total">R$ %s</span>
                            </div>
                        </div>
                        
                        <p>Você receberá mais informações sobre o envio em breve.</p>
                        <p>Obrigado por comprar conosco! 🎉</p>
                    </div>
                    <div class="footer">
                        <p>Este é um email automático. Por favor, não responda.</p>
                        <p>© 2026 Hybrid Microservices - TCC</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(customerName, orderId.substring(0, 8), product, 
                         Instant.now().toString().substring(0, 10), totalAmount);
    }

    private String buildOrderCompletedText(String orderId, String customerName, 
                                           String product, String totalAmount) {
        return """
            PEDIDO CONFIRMADO!
            
            Olá, %s!
            
            Seu pedido foi processado com sucesso.
            
            --------------------------------
            Número do Pedido: #%s
            Produto: %s
            Total: R$ %s
            --------------------------------
            
            Obrigado por comprar conosco!
            
            --
            Hybrid Microservices - TCC
            """.formatted(customerName, orderId.substring(0, 8), product, totalAmount);
    }

    private String buildOrderFailedHtml(String orderId, String customerName, String errorMessage) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #dc3545, #fd7e14); color: white; padding: 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; }
                    .content { padding: 30px; }
                    .error-box { background: #fff3cd; border: 1px solid #ffc107; border-radius: 8px; padding: 20px; margin: 20px 0; }
                    .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #6c757d; font-size: 12px; }
                    .btn { display: inline-block; background: #007bff; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>⚠️ Problema com seu Pedido</h1>
                    </div>
                    <div class="content">
                        <p>Olá, <strong>%s</strong>!</p>
                        <p>Infelizmente, encontramos um problema ao processar seu pedido.</p>
                        
                        <div class="error-box">
                            <p><strong>Pedido:</strong> #%s</p>
                            <p><strong>Motivo:</strong> %s</p>
                        </div>
                        
                        <p>Por favor, entre em contato com nosso suporte para mais informações.</p>
                        
                        <a href="#" class="btn">Falar com Suporte</a>
                    </div>
                    <div class="footer">
                        <p>Este é um email automático. Por favor, não responda.</p>
                        <p>© 2026 Hybrid Microservices - TCC</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(customerName, orderId.substring(0, 8), errorMessage);
    }

    private String buildOrderFailedText(String orderId, String customerName, String errorMessage) {
        return """
            PROBLEMA COM SEU PEDIDO
            
            Olá, %s!
            
            Infelizmente, encontramos um problema ao processar seu pedido.
            
            --------------------------------
            Pedido: #%s
            Motivo: %s
            --------------------------------
            
            Por favor, entre em contato com nosso suporte.
            
            --
            Hybrid Microservices - TCC
            """.formatted(customerName, orderId.substring(0, 8), errorMessage);
    }
}
