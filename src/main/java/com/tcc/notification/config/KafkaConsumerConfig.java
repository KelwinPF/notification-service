package com.tcc.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.support.converter.JsonMessageConverter;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuração do consumidor Kafka para o Notification Service
 * 
 * Configura a deserialização de eventos JSON recebidos dos tópicos:
 * - orders.completed
 * - orders.failed
 * 
 * Inclui configuração de retry e DLQ via CommonErrorHandler
 */
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Configuração do ConsumerFactory
     * Usa StringDeserializer para key e value - a conversão JSON é feita pelo MessageConverter
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        
        // Configuração do broker
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        
        // Configuração de offset
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        
        // Usa StringDeserializer para ambos - o JsonMessageConverter fará a conversão
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer());
    }

    /**
     * Factory para criar listeners Kafka com configuração customizada
     * Inclui ErrorHandler para retry e DLQ
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            CommonErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        // JsonMessageConverter converte String JSON para o tipo do parâmetro do método
        factory.setRecordMessageConverter(new JsonMessageConverter(objectMapper));
        // Configura error handler com retry e DLQ
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
