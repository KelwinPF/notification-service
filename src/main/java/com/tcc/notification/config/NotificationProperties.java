package com.tcc.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propriedades de configuração para simulação de notificações
 * 
 * Permite configurar:
 * - Simulação de falhas (para testes de resiliência)
 * - Simulação de latência (para testes de performance)
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "notification.simulation")
public class NotificationProperties {

    private Failure failure = new Failure();
    private Latency latency = new Latency();

    @Data
    public static class Failure {
        /**
         * Habilita simulação de falhas no envio de notificação
         */
        private boolean enabled = false;
        
        /**
         * Taxa de falha (0.0 a 1.0)
         * Exemplo: 0.2 = 20% de chance de falha
         */
        private double rate = 0.0;
    }

    @Data
    public static class Latency {
        /**
         * Habilita simulação de latência no processamento
         */
        private boolean enabled = true;
        
        /**
         * Latência mínima em milissegundos
         */
        private long minMs = 50;
        
        /**
         * Latência máxima em milissegundos
         */
        private long maxMs = 200;
    }
}
