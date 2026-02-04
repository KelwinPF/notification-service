# 📧 Notification Service

Microsserviço de notificação **100% event-driven** para arquitetura híbrida de microsserviços - TCC de MBA.

## 📋 Descrição

O **Notification Service** é o serviço final da cadeia de processamento da arquitetura híbrida, responsável por notificar os clientes sobre o resultado do processamento de seus pedidos. Este microsserviço é **100% orientado a eventos**, consumindo mensagens do Apache Kafka e enviando emails via **AWS SES** (Simple Email Service).

O serviço representa o último estágio do fluxo, onde o cliente recebe feedback sobre seu pedido - seja uma confirmação de sucesso ou uma notificação de falha.

## 🎯 Responsabilidades

- ✅ Consumir eventos `OrderCompletedEvent` do Kafka (topic: `orders.completed`)
- ✅ Consumir eventos `OrderFailedEvent` do Kafka (topic: `orders.failed`)
- ✅ Preparar templates de email (HTML e texto plano)
- ✅ Enviar emails transacionais via AWS SES
- ✅ Suportar modo local via LocalStack (simulação AWS)
- ✅ Permitir simulação de **falhas** e **latência** para testes experimentais

## 🏗️ Arquitetura

```
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                                   NOTIFICATION SERVICE                                      │
├─────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                             │
│   ┌────────────────────────────────────────────────────────────────────────────────────┐   │
│   │                           Kafka Consumers                                          │   │
│   │   ┌─────────────────────────────┐    ┌─────────────────────────────┐              │   │
│   │   │     orders.completed        │    │      orders.failed          │              │   │
│   │   │  (OrderCompletedEvent)      │    │   (OrderFailedEvent)        │              │   │
│   │   └──────────────┬──────────────┘    └──────────────┬──────────────┘              │   │
│   └──────────────────┼──────────────────────────────────┼──────────────────────────────┘   │
│                      │                                  │                                   │
│                      └────────────────┬─────────────────┘                                   │
│                                       ▼                                                     │
│                      ┌────────────────────────────────────┐                                │
│                      │      OrderEventListener            │                                │
│                      │   (Delega para NotificationService)│                                │
│                      └────────────────┬───────────────────┘                                │
│                                       │                                                     │
│                                       ▼                                                     │
│                      ┌────────────────────────────────────┐                                │
│                      │       NotificationService          │                                │
│                      │                                    │                                │
│                      │  ┌──────────────────────────────┐  │                                │
│                      │  │ 1. Simular latência          │  │                                │
│                      │  │ 2. Verificar falha simulada  │  │                                │
│                      │  │ 3. Gerar email do cliente    │  │                                │
│                      │  │ 4. Preparar template         │  │                                │
│                      │  │ 5. Enviar via EmailService   │  │                                │
│                      │  └──────────────────────────────┘  │                                │
│                      └────────────────┬───────────────────┘                                │
│                                       │                                                     │
│                                       ▼                                                     │
│                      ┌────────────────────────────────────┐                                │
│                      │          EmailService              │                                │
│                      │    (Integração com AWS SES)        │                                │
│                      └────────────────┬───────────────────┘                                │
│                                       │                                                     │
│                                       ▼                                                     │
│                      ┌────────────────────────────────────┐                                │
│                      │    AWS SES / LocalStack            │                                │
│                      │   (Simple Email Service)           │                                │
│                      └────────────────────────────────────┘                                │
│                                                                                             │
└─────────────────────────────────────────────────────────────────────────────────────────────┘
```

> **⚠️ Importante**: Este serviço é **100% event-driven** e não expõe endpoints HTTP de negócio.

## 🔄 Fluxo de Processamento

### Fluxo de Sucesso (OrderCompleted)

```
1️⃣  Evento OrderCompletedEvent recebido via Kafka
         │
         ▼
2️⃣  Simulação de latência (se habilitada)
         │
         ▼
3️⃣  Verificação de falha simulada (se habilitada)
         │
         ▼
4️⃣  Geração do email do cliente (baseado no customerId)
         │
         ▼
5️⃣  Preparação do template HTML de confirmação:
         ├── Assunto: "✅ Pedido #XXX confirmado!"
         ├── Dados do pedido
         ├── Valor + Imposto + Total
         └── Mensagem de agradecimento
         │
         ▼
6️⃣  Envio via AWS SES
         │
         ▼
7️⃣  Log do MessageId retornado
```

### Fluxo de Falha (OrderFailed)

```
1️⃣  Evento OrderFailedEvent recebido via Kafka
         │
         ▼
2️⃣  Simulação de latência (se habilitada)
         │
         ▼
3️⃣  Preparação do template HTML de erro:
         ├── Assunto: "⚠️ Problema com seu pedido #XXX"
         ├── Descrição do erro
         └── Instruções de suporte
         │
         ▼
4️⃣  Envio via AWS SES
         │
         ▼
5️⃣  Log do resultado
```

## 📊 Eventos Consumidos

### OrderCompletedEvent (Topic: `orders.completed`)

```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST123",
  "billingId": "660e9500-f39c-52e5-b827-557766551111",
  "amount": 8999.90,
  "tax": 899.99,
  "totalAmount": 9899.89,
  "eventType": "OrderCompleted",
  "eventTime": "2024-01-15T10:30:05.456Z",
  "processedAt": "2024-01-15T10:30:05.456Z",
  "processingTimeMs": 150
}
```

### OrderFailedEvent (Topic: `orders.failed`)

```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST123",
  "amount": 8999.90,
  "errorMessage": "SIMULATED_FAILURE: Billing processing failed intentionally",
  "errorType": "PROCESSING_ERROR",
  "eventType": "OrderFailed",
  "eventTime": "2024-01-15T10:30:05.456Z",
  "failedAt": "2024-01-15T10:30:05.456Z",
  "processingTimeMs": 150
}
```

## 🚀 Stack Técnica

| Tecnologia | Versão | Descrição |
|------------|--------|-----------|
| Java | 21 | Linguagem de programação |
| Spring Boot | 4.0.1 | Framework principal |
| Spring Kafka | - | Consumer de eventos |
| AWS SDK v2 | - | Integração com SES |
| LocalStack | 3.0 | Simulação AWS local |
| Lombok | - | Redução de boilerplate |
| Micrometer | - | Métricas |

## 📦 Estrutura do Projeto

```
notification-service/
├── src/main/java/com/tcc/notification/
│   ├── NotificationApplication.java      # Classe principal
│   ├── config/
│   │   ├── JacksonConfig.java            # Configuração JSON
│   │   ├── KafkaConsumerConfig.java      # Configuração Kafka
│   │   ├── NotificationProperties.java   # Propriedades de simulação
│   │   └── SesConfig.java                # Configuração AWS SES
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
│       ├── EmailService.java             # Envio de emails (SES)
│       └── NotificationService.java      # Lógica de notificação
└── src/main/resources/
    ├── application.properties            # Configurações
    └── logback-spring.xml                # Configuração de logs
```

## 🌐 Endpoints (Apenas Monitoramento)

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `GET` | `/actuator/health` | Health check detalhado |
| `GET` | `/actuator/prometheus` | Métricas Prometheus |

## ⚙️ Configuração

### application.properties

```properties
# Servidor
server.port=8082

# Kafka Consumer
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=notification-service-group

# AWS SES
aws.ses.mode=LOCAL                              # LOCAL ou AWS
aws.ses.localstack.endpoint=http://localhost:4566
aws.ses.region=sa-east-1
aws.ses.from-email=noreply@exemplo.com
aws.ses.enabled=true

# Simulação (Experimental)
notification.simulation.failure.enabled=false
notification.simulation.failure.rate=0.0
notification.simulation.latency.enabled=true
notification.simulation.latency.min-ms=50
notification.simulation.latency.max-ms=200
```

### Variáveis de Ambiente

| Variável | Descrição | Default |
|----------|-----------|---------|
| `SERVER_PORT` | Porta do servidor | `8082` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Broker Kafka | `localhost:9092` |
| `AWS_SES_MODE` | Modo SES (LOCAL/AWS) | `LOCAL` |
| `AWS_SES_FROM_EMAIL` | Email de origem | `noreply@exemplo.com` |
| `NOTIFICATION_SIMULATION_FAILURE_ENABLED` | Habilita falhas | `false` |
| `NOTIFICATION_SIMULATION_FAILURE_RATE` | Taxa de falha (0.0-1.0) | `0.0` |
| `NOTIFICATION_SIMULATION_LATENCY_ENABLED` | Habilita latência | `true` |

### Modos de Operação do SES

| Modo | Descrição | Uso |
|------|-----------|-----|
| `LOCAL` | Usa LocalStack (não envia email real) | Desenvolvimento |
| `AWS` | Usa AWS SES real (envia email de verdade) | Produção |

## 🐳 Como Executar

### Pré-requisitos
- Java 21
- Maven 3.9+
- Docker e Docker Compose

### Via Maven

```powershell
# 1. Subir infraestrutura
docker-compose up -d

# 2. Compilar
./mvnw clean package -DskipTests

# 3. Executar
./mvnw spring-boot:run
```

### Via Docker

```powershell
# Build da imagem
docker build -t notification-service .

# Executar
docker run -p 8082:8082 `
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092 `
  -e AWS_SES_LOCALSTACK_ENDPOINT=http://localstack:4566 `
  notification-service
```

## 🧪 Simulação Experimental

### 1. Simulação de Latência

```properties
notification.simulation.latency.enabled=true
notification.simulation.latency.min-ms=100
notification.simulation.latency.max-ms=500
```

**Casos de uso:**
- Testar timeouts
- Analisar impacto de latência variável
- Simular condições de rede adversas

### 2. Simulação de Falha

```properties
notification.simulation.failure.enabled=true
notification.simulation.failure.rate=0.3  # 30% de falha
```

**Casos de uso:**
- Testar resiliência do sistema
- Verificar tratamento de erros
- Analisar retentativas do Kafka

## 📈 Observabilidade

### Logs Estruturados

```
╔══════════════════════════════════════════════════════════════╗
║  [NOTIFICATION-SERVICE] EVENTO KAFKA RECEBIDO              ║
╠══════════════════════════════════════════════════════════════╣
║  Topic:     orders.completed
║  OrderId:   550e8400-e29b-41d4-a716-446655440000
║  Customer:  CUST123
║  BillingId: 660e9500-f39c-52e5-b827-557766551111
║  Total:     R$ 9899.89
╚══════════════════════════════════════════════════════════════╝

[NOTIFICATION-SERVICE] 📧 ETAPA 1: Preparando notificação de SUCESSO...
[NOTIFICATION-SERVICE]    └── Cliente: CUST123 | Pedido: 550e8400... | Total: R$ 9899.89
[NOTIFICATION-SERVICE] 📤 ETAPA 2: Enviando email via AWS SES...

╔══════════════════════════════════════════════════════════════╗
║  [EMAIL-SERVICE] 📧 ENVIANDO EMAIL                          ║
╠══════════════════════════════════════════════════════════════╣
║  Para:    cust123@email.com
║  Assunto: ✅ Pedido #550e8400 confirmado!
║  Modo:    LOCAL
╚══════════════════════════════════════════════════════════════╝

[NOTIFICATION-SERVICE] ✅ Email enviado com sucesso via AWS SES
[NOTIFICATION-SERVICE]    └── MessageId: 0102018d-1234-5678-90ab-cdef01234567
```

### Métricas Prometheus

Disponíveis em `/actuator/prometheus`:
- `kafka_consumer_records_consumed_total`
- `notification_emails_sent_total`
- `notification_processing_time_ms`

## 🔗 Integração com Outros Serviços

| Serviço | Integração | Direção |
|---------|------------|---------|
| **Billing Service** | Kafka (`orders.completed`, `orders.failed`) | Billing → Notification |
| **AWS SES** | HTTPS | Notification → SES |
| **LocalStack** | HTTP (desenvolvimento) | Notification → LocalStack |
| **Prometheus** | HTTP Pull | Métricas |

## 🏛️ Decisões Arquiteturais

| Decisão | Justificativa |
|---------|---------------|
| **100% Event-Driven** | Desacoplamento total; não depende de HTTP |
| **AWS SES** | Serviço gerenciado para emails transacionais |
| **LocalStack** | Permite desenvolvimento offline sem custos |
| **Dual Mode (LOCAL/AWS)** | Flexibilidade para dev e produção |
| **Templates HTML** | Emails profissionais com formatação |

## 📚 Documentação Adicional

- [Guia de Testes](TESTING.md)

---

**Projeto de TCC - MBA** | Arquitetura Híbrida de Microsserviços
