package com.tcc.notification.service;

import com.tcc.notification.config.NotificationProperties;
import com.tcc.notification.dto.NotificationResult;
import com.tcc.notification.event.OrderCompletedEvent;
import com.tcc.notification.event.OrderFailedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para NotificationService
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private EmailService emailService;

    private NotificationProperties properties;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        properties = new NotificationProperties();
        properties.setFailure(new NotificationProperties.Failure());
        properties.setLatency(new NotificationProperties.Latency());
        properties.getLatency().setEnabled(false); // Desabilitar latência nos testes
        
        notificationService = new NotificationService(emailService, properties);
        // Setar o email de teste via reflection já que @Value não funciona em testes unitários
        ReflectionTestUtils.setField(notificationService, "toEmail", "test@example.com");
    }

    @Test
    @DisplayName("Deve processar OrderCompletedEvent com sucesso")
    void shouldProcessOrderCompletedSuccessfully() {
        // Arrange
        OrderCompletedEvent event = OrderCompletedEvent.builder()
                .orderId("order-123")
                .customerId("customer-456")
                .billingId("billing-789")
                .amount(new BigDecimal("100.00"))
                .tax(new BigDecimal("10.00"))
                .totalAmount(new BigDecimal("110.00"))
                .eventType("OrderCompleted")
                .eventTime(Instant.now())
                .build();

        when(emailService.sendOrderCompletedEmail(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("msg-123");

        // Act
        NotificationResult result = notificationService.processOrderCompleted(event);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrderId()).isEqualTo("order-123");
        assertThat(result.getMessageId()).isEqualTo("msg-123");
        assertThat(result.getNotificationType()).isEqualTo("EMAIL");
    }

    @Test
    @DisplayName("Deve processar OrderFailedEvent com sucesso")
    void shouldProcessOrderFailedSuccessfully() {
        // Arrange
        OrderFailedEvent event = OrderFailedEvent.builder()
                .orderId("order-456")
                .customerId("customer-789")
                .amount(new BigDecimal("200.00"))
                .errorMessage("Falha no pagamento")
                .errorType("PAYMENT_ERROR")
                .eventType("OrderFailed")
                .eventTime(Instant.now())
                .build();

        when(emailService.sendOrderFailedEmail(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("msg-456");

        // Act
        NotificationResult result = notificationService.processOrderFailed(event);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrderId()).isEqualTo("order-456");
        assertThat(result.getMessageId()).isEqualTo("msg-456");
    }

    @Test
    @DisplayName("Deve retornar falha quando simulação de falha está ativa")
    void shouldReturnFailureWhenSimulationEnabled() {
        // Arrange
        properties.getFailure().setEnabled(true);
        properties.getFailure().setRate(1.0); // 100% de falha

        OrderCompletedEvent event = OrderCompletedEvent.builder()
                .orderId("order-789")
                .customerId("customer-123")
                .totalAmount(new BigDecimal("150.00"))
                .build();

        // Act
        NotificationResult result = notificationService.processOrderCompleted(event);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Falha simulada");
    }
}
