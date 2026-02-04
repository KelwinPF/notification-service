# Testando o Notification Service

Este documento descreve como testar o Notification Service de forma isolada e integrada.

## Pré-requisitos

- Docker e Docker Compose
- Java 21
- Maven 3.9+
- (Opcional) AWS CLI para interagir com LocalStack

## 1. Teste Unitário

Execute os testes unitários com Maven:

```bash
cd notification-service
./mvnw test
```

## 2. Teste Local com LocalStack

### 2.1 Iniciar LocalStack e Kafka

```bash
# Na raiz do projeto (hybrid-microservices-architecture)
docker-compose up -d kafka zookeeper localstack
```

### 2.2 Criar tópico SNS no LocalStack

```bash
# Criar tópico SNS
aws --endpoint-url=http://localhost:4566 \
    --region us-east-1 \
    sns create-topic --name order-notifications

# Verificar criação
aws --endpoint-url=http://localhost:4566 \
    --region us-east-1 \
    sns list-topics
```

### 2.3 Criar tópicos Kafka

```bash
# Criar tópicos (se não existirem)
docker exec -it kafka kafka-topics --create \
    --topic orders.completed \
    --bootstrap-server localhost:9092 \
    --partitions 1 \
    --replication-factor 1

docker exec -it kafka kafka-topics --create \
    --topic orders.failed \
    --bootstrap-server localhost:9092 \
    --partitions 1 \
    --replication-factor 1
```

### 2.4 Iniciar o Notification Service

```bash
./mvnw spring-boot:run
```

### 2.5 Publicar evento de teste

```bash
# Publicar OrderCompletedEvent
echo '{"orderId":"test-001","customerId":"cust-001","billingId":"bill-001","amount":100.00,"tax":10.00,"totalAmount":110.00,"eventType":"OrderCompleted","eventTime":"2026-02-02T10:00:00Z"}' | \
docker exec -i kafka kafka-console-producer \
    --topic orders.completed \
    --bootstrap-server localhost:9092

# Publicar OrderFailedEvent
echo '{"orderId":"test-002","customerId":"cust-002","amount":200.00,"errorMessage":"Pagamento recusado","errorType":"PAYMENT_ERROR","eventType":"OrderFailed","eventTime":"2026-02-02T10:00:00Z"}' | \
docker exec -i kafka kafka-console-producer \
    --topic orders.failed \
    --bootstrap-server localhost:9092
```

### 2.6 Verificar logs

Observe os logs do Notification Service para ver o processamento:

```
[KAFKA] Evento recebido: orders.completed
[KAFKA] OrderId: test-001
[NOTIFICATION] Processando OrderCompleted - OrderId: test-001
[SNS] Mensagem publicada com sucesso - MessageId: xxx
[NOTIFICATION] ✓ Sucesso - OrderId: test-001, ProcessingTime: 125ms
```

## 3. Teste de Simulação de Falhas

### 3.1 Ativar simulação de falhas

Edite `application.properties`:

```properties
notification.simulation.failure.enabled=true
notification.simulation.failure.rate=0.5
```

Ou use variável de ambiente:

```bash
NOTIFICATION_FAILURE_ENABLED=true NOTIFICATION_FAILURE_RATE=0.5 ./mvnw spring-boot:run
```

### 3.2 Enviar múltiplos eventos

```bash
# Script para enviar 10 eventos
for i in {1..10}; do
    echo "{\"orderId\":\"test-$i\",\"customerId\":\"cust-$i\",\"billingId\":\"bill-$i\",\"totalAmount\":$((i*100)).00,\"eventType\":\"OrderCompleted\"}" | \
    docker exec -i kafka kafka-console-producer \
        --topic orders.completed \
        --bootstrap-server localhost:9092
    sleep 1
done
```

### 3.3 Analisar resultados

Com taxa de falha de 50%, aproximadamente metade dos eventos deve falhar.

## 4. Teste de Latência

### 4.1 Configurar latência

```properties
notification.simulation.latency.enabled=true
notification.simulation.latency.min-ms=100
notification.simulation.latency.max-ms=500
```

### 4.2 Medir tempo de processamento

Os logs incluem `ProcessingTime` em milissegundos:

```
[NOTIFICATION] ✓ Sucesso - OrderId: test-001, ProcessingTime: 350ms
```

## 5. Teste de Health Check

```bash
# Verificar health
curl http://localhost:8082/actuator/health

# Resposta esperada
{
  "status": "UP",
  "components": {
    "kafka": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}
```

## 6. Teste Integrado (Full Stack)

### 6.1 Iniciar toda a infraestrutura

```bash
# Na raiz do projeto
docker-compose up -d
```

### 6.2 Criar um pedido via Pedido Service

```bash
curl -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d '{
        "customerId": "customer-123",
        "items": [
            {"productId": "prod-1", "quantity": 2, "price": 50.00}
        ]
    }'
```

### 6.3 Verificar fluxo completo

1. **Pedido Service**: Cria pedido e publica `orders.created`
2. **Billing Service**: Processa e publica `orders.completed` ou `orders.failed`
3. **Notification Service**: Recebe evento e envia notificação

## 7. Métricas Prometheus

```bash
# Ver métricas
curl http://localhost:8082/actuator/prometheus

# Métricas importantes
# - kafka_consumer_records_consumed_total
# - process_cpu_usage
# - jvm_memory_used_bytes
```

## 8. Troubleshooting

### Kafka não conecta

```bash
# Verificar se Kafka está rodando
docker ps | grep kafka

# Verificar logs do Kafka
docker logs kafka
```

### SNS não publica

```bash
# Verificar LocalStack
docker logs localstack

# Testar SNS manualmente
aws --endpoint-url=http://localhost:4566 \
    sns publish \
    --topic-arn arn:aws:sns:us-east-1:000000000000:order-notifications \
    --message "Teste"
```

### Eventos não são consumidos

```bash
# Verificar consumer groups
docker exec kafka kafka-consumer-groups \
    --bootstrap-server localhost:9092 \
    --describe \
    --group notification-service-group
```
