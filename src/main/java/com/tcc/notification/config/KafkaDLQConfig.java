package com.tcc.notification.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuração de Dead Letter Queue (DLQ) e Retry para Kafka no Notification Service
 * 
 * Tópicos DLQ:
 * - orders.completed.dlq
 * - orders.failed.dlq
 */
@Slf4j
@Configuration
public class KafkaDLQConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${kafka.retry.backoff-ms:1000}")
    private long backoffMs;

    private Counter retryCounter;
    private Counter dlqCounter;

    @Bean
    public Counter notificationRetryCounter(MeterRegistry meterRegistry) {
        this.retryCounter = Counter.builder("kafka.consumer.retry.total")
                .description("Total de retries no consumer Kafka")
                .tag("service", "notification-service")
                .register(meterRegistry);
        return retryCounter;
    }

    @Bean
    public Counter notificationDlqCounter(MeterRegistry meterRegistry) {
        this.dlqCounter = Counter.builder("kafka.consumer.dlq.total")
                .description("Total de mensagens enviadas para DLQ")
                .tag("service", "notification-service")
                .register(meterRegistry);
        return dlqCounter;
    }

    @Bean
    public ProducerFactory<String, Object> dlqProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> dlqKafkaTemplate() {
        return new KafkaTemplate<>(dlqProducerFactory());
    }

    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaTemplate<String, Object> dlqKafkaTemplate) {
        return new DeadLetterPublishingRecoverer(
                (KafkaOperations<?, ?>) dlqKafkaTemplate,
                (record, exception) -> {
                    if (dlqCounter != null) {
                        dlqCounter.increment();
                    }
                    
                    String dlqTopic = record.topic() + ".dlq";
                    
                    log.error("");
                    log.error("╔══════════════════════════════════════════════════════════════╗");
                    log.error("║  [NOTIFICATION-SERVICE] ☠️ MENSAGEM ENVIADA PARA DLQ       ║");
                    log.error("╠══════════════════════════════════════════════════════════════╣");
                    log.error("║  Topic Original: {}  ", record.topic());
                    log.error("║  DLQ Topic:      {}  ", dlqTopic);
                    log.error("║  Key:            {}  ", record.key());
                    log.error("║  Erro:           {}  ", exception.getMessage());
                    log.error("╚══════════════════════════════════════════════════════════════╝");
                    log.error("");
                    
                    return new TopicPartition(dlqTopic, record.partition() % 3);
                }
        );
    }

    @Bean
    public CommonErrorHandler errorHandler(DeadLetterPublishingRecoverer recoverer) {
        FixedBackOff backOff = new FixedBackOff(backoffMs, maxAttempts - 1);
        
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            if (retryCounter != null) {
                retryCounter.increment();
            }
            
            log.warn("");
            log.warn("╔══════════════════════════════════════════════════════════════╗");
            log.warn("║  [NOTIFICATION-SERVICE] 🔄 RETRY #{} de {}                 ║", deliveryAttempt, maxAttempts);
            log.warn("╠══════════════════════════════════════════════════════════════╣");
            log.warn("║  Topic:  {}  ", record.topic());
            log.warn("║  Key:    {}  ", record.key());
            log.warn("║  Erro:   {}  ", ex.getMessage());
            log.warn("╚══════════════════════════════════════════════════════════════╝");
            log.warn("");
        });
        
        return errorHandler;
    }
}
