# Notification Service

Microsserviço de notificação para arquitetura híbrida de microsserviços.

## Visão Geral

O Notification Service é um microsserviço orientado a eventos, responsável por:

- Consumir eventos Kafka (`orders.completed`, `orders.failed`)
- Simular envio de notificações externas (e-mail/SMS)
- Publicar mensagens via Amazon SNS (simulado via LocalStack)
- Permitir simulação de falhas para testes de resiliência

## Arquitetura

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Pedido Service │────▶│  Billing Service│────▶│ Notification Svc│
│ (orders.created)│     │(orders.completed│     │    (Consumer)   │
└─────────────────┘     │  orders.failed) │     └────────┬────────┘
                        └─────────────────┘              │
                                                         ▼
                                                 ┌───────────────┐
                                                 │  Amazon SNS   │
                                                 │  (LocalStack) │
                                                 └───────────────┘
```

## Tecnologias

- Java 21
- Spring Boot 4.x
- Spring Kafka
- AWS SDK Java v2 (SNS)
- LocalStack (simulação AWS)
- Docker

## Estrutura do Projeto

```
notification-service/
├── src/main/java/com/tcc/notification/
│   ├── NotificationApplication.java      # Classe principal
│   ├── config/
│   │   ├── KafkaConsumerConfig.java      # Configuração Kafka
│   │   ├── SnsConfig.java                # Configuração SNS/LocalStack
│   │   └── NotificationProperties.java   # Propriedades de simulação
│   ├── dto/
│   │   └── NotificationResult.java       # DTO de resultado
│   ├── event/
│   │   ├── OrderCompletedEvent.java      # Evento de sucesso
│   │   └── OrderFailedEvent.java         # Evento de falha
│   ├── exception/
│   │   └── NotificationException.java    # Exceção customizada
│   ├── listener/
│   │   └── OrderEventListener.java       # Kafka listeners
│   └── service/
│       ├── NotificationService.java      # Lógica de notificação
│       └── SnsPublisher.java             # Publicador SNS
└── src/main/resources/
    └── application.properties            # Configurações
```

## Configuração

### Variáveis de Ambiente

| Variável | Descrição | Default |
|----------|-----------|---------|
| `KAFKA_BOOTSTRAP_SERVERS` | Endereço do broker Kafka | `localhost:9092` |
| `AWS_SNS_ENDPOINT` | Endpoint do SNS (LocalStack) | `http://localhost:4566` |
| `AWS_SNS_ENABLED` | Habilita cliente SNS real | `true` |
| `NOTIFICATION_FAILURE_ENABLED` | Habilita simulação de falhas | `false` |
| `NOTIFICATION_FAILURE_RATE` | Taxa de falha (0.0 a 1.0) | `0.0` |
| `NOTIFICATION_LATENCY_ENABLED` | Habilita latência artificial | `true` |
| `NOTIFICATION_LATENCY_MIN_MS` | Latência mínima (ms) | `50` |
| `NOTIFICATION_LATENCY_MAX_MS` | Latência máxima (ms) | `200` |

### application.properties

```properties
# Kafka
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=notification-service-group

# AWS SNS (LocalStack)
aws.sns.endpoint=http://localhost:4566
aws.sns.region=us-east-1
aws.sns.topic.arn=arn:aws:sns:us-east-1:000000000000:order-notifications

# Simulação
notification.simulation.failure.enabled=false
notification.simulation.failure.rate=0.0
notification.simulation.latency.enabled=true
notification.simulation.latency.min-ms=50
notification.simulation.latency.max-ms=200
```

## Execução

### Local (Maven)

```bash
# Compilar
./mvnw clean package

# Executar
./mvnw spring-boot:run
```

### Docker

```bash
# Build da imagem
docker build -t notification-service .

# Executar
docker run -p 8082:8082 \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  -e AWS_SNS_ENDPOINT=http://localstack:4566 \
  notification-service
```

### Docker Compose

O serviço faz parte do `docker-compose.yml` na raiz do projeto:

```bash
docker-compose up notification-service
```

## Tópicos Kafka Consumidos

| Tópico | Descrição | Publicado por |
|--------|-----------|---------------|
| `orders.completed` | Pedido processado com sucesso | Billing Service |
| `orders.failed` | Pedido falhou no processamento | Billing Service |

## Eventos

### OrderCompletedEvent

```json
{
  "orderId": "order-123",
  "customerId": "customer-456",
  "billingId": "billing-789",
  "amount": 100.00,
  "tax": 10.00,
  "totalAmount": 110.00,
  "eventType": "OrderCompleted",
  "eventTime": "2026-02-02T10:30:00Z",
  "processedAt": "2026-02-02T10:30:01Z",
  "processingTimeMs": 150
}
```

### OrderFailedEvent

```json
{
  "orderId": "order-123",
  "customerId": "customer-456",
  "amount": 100.00,
  "errorMessage": "Falha no pagamento",
  "errorType": "PAYMENT_ERROR",
  "eventType": "OrderFailed",
  "eventTime": "2026-02-02T10:30:00Z",
  "failedAt": "2026-02-02T10:30:01Z",
  "processingTimeMs": 150
}
```

## Observabilidade

### Logs

Os logs seguem o padrão estruturado para facilitar análise:

```
[KAFKA] Evento recebido: orders.completed
[KAFKA] OrderId: order-123
[NOTIFICATION] Processando OrderCompleted - OrderId: order-123
[SNS] Mensagem publicada com sucesso - MessageId: msg-456
[NOTIFICATION] ✓ Sucesso - OrderId: order-123, ProcessingTime: 125ms
```

### Health Check

```bash
curl http://localhost:8082/actuator/health
```

### Métricas (Prometheus)

```bash
curl http://localhost:8082/actuator/prometheus
```

## Simulação de Falhas

Para testar resiliência, ative a simulação de falhas:

```properties
notification.simulation.failure.enabled=true
notification.simulation.failure.rate=0.3  # 30% de falha
```

Ou via variável de ambiente:

```bash
NOTIFICATION_FAILURE_ENABLED=true
NOTIFICATION_FAILURE_RATE=0.3
```

## Testes

```bash
# Executar todos os testes
./mvnw test

# Testes com cobertura
./mvnw test jacoco:report
```

## Fluxo de Processamento

1. **Receber evento**: Kafka listener recebe `OrderCompletedEvent` ou `OrderFailedEvent`
2. **Log de entrada**: Registra detalhes do evento recebido
3. **Simular latência**: Aplica delay artificial (se habilitado)
4. **Verificar falha**: Verifica se deve simular falha (se habilitado)
5. **Publicar SNS**: Envia notificação para o tópico SNS
6. **Log de saída**: Registra resultado do processamento

## Integração com LocalStack

O LocalStack simula o Amazon SNS localmente. Para criar o tópico SNS:

```bash
# Criar tópico no LocalStack
aws --endpoint-url=http://localhost:4566 sns create-topic --name order-notifications

# Verificar tópicos
aws --endpoint-url=http://localhost:4566 sns list-topics
```

## Licença

Projeto acadêmico - TCC sobre arquitetura de microsserviços híbrida.
