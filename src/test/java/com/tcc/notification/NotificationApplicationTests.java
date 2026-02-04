package com.tcc.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

/**
 * Testes de integração básicos para o Notification Service
 */
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"orders.completed", "orders.failed"})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "aws.sns.enabled=false"
})
class NotificationApplicationTests {

    @Test
    void contextLoads() {
        // Verifica se o contexto Spring carrega corretamente
    }
}
