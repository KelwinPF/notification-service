package com.tcc.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

import java.net.URI;

/**
 * Configuração do cliente AWS SES (Simple Email Service)
 * 
 * Suporta dois modos:
 * - LOCAL: usa LocalStack para simular (não envia email real)
 * - AWS: usa AWS SES real (envia email de verdade!)
 */
@Slf4j
@Configuration
public class SesConfig {

    @Value("${aws.ses.mode:LOCAL}")
    private String mode;

    @Value("${aws.ses.localstack.endpoint:http://localhost:4566}")
    private String localstackEndpoint;

    @Value("${aws.ses.region:us-east-1}")
    private String region;

    @Value("${aws.ses.access-key:}")
    private String accessKey;

    @Value("${aws.ses.secret-key:}")
    private String secretKey;

    @Value("${aws.ses.enabled:true}")
    private boolean sesEnabled;

    /**
     * Cliente SES configurado baseado no modo (LOCAL ou AWS)
     */
    @Bean
    public SesClient sesClient() {
        if (!sesEnabled) {
            log.warn("[SES] Cliente SES desabilitado");
            return null;
        }

        if ("AWS".equalsIgnoreCase(mode)) {
            return createAwsSesClient();
        } else {
            return createLocalStackSesClient();
        }
    }

    /**
     * Cria cliente SES para LocalStack (desenvolvimento)
     */
    private SesClient createLocalStackSesClient() {
        log.info("[SES] Configurando cliente para LocalStack: {}", localstackEndpoint);
        
        return SesClient.builder()
                .endpointOverride(URI.create(localstackEndpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("localstack", "localstack")
                ))
                .build();
    }

    /**
     * Cria cliente SES para AWS real (produção/sandbox)
     */
    private SesClient createAwsSesClient() {
        log.info("[SES] Configurando cliente para AWS SES real - Region: {}", region);
        
        if (accessKey == null || accessKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            log.error("[SES] Credenciais AWS não configuradas! Configure aws.ses.access-key e aws.ses.secret-key");
            throw new IllegalStateException("Credenciais AWS SES não configuradas");
        }

        return SesClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();
    }
}
