# Integração com Asaas - Pagamentos Reais

## 🚀 Visão Geral

O ServiJá agora está integrado com o **Asaas**, plataforma de pagamentos brasileira, para processar transações reais de PIX, Boleto e Cartão de Crédito.

## 🔧 Configuração

### Variáveis de Ambiente

Adicione as seguintes variáveis ao arquivo `.env` ou configure no servidor:

```bash
# Asaas Configuration
ASAAS_API_URL=https://api-sandbox.asaas.com  # Sandbox para testes
# ASAAS_API_URL=https://api.asaas.com  # Produção
ASAAS_API_KEY=sua_chave_api_aqui
```

### Obter Credenciais

1. Acesse [https://www.asaas.com](https://www.asaas.com)
2. Crie uma conta
3. Vá em **Configurações** → **Integrações** → **API**
4. Gere uma nova chave de API
5. Use a chave na variável `ASAAS_API_KEY`

## 📦 Componentes Criados

### 1. AsaasService (`AsaasService.java`)

Serviço principal de integração com a API do Asaas.

**Métodos:**

- `getOrCreateCustomer(email, name, cpfCnpj)` - Cria ou recupera cliente
- `createPayment(customerId, value, dueDate, billingType, description)` - Cria cobrança
- `getPixQrCode(paymentId)` - Obtém QR Code PIX
- `getPaymentStatus(paymentId)` - Consulta status do pagamento

### 2. Modelo Payment Atualizado

Novos campos adicionados:

```java
private String asaasPaymentId;    // ID do pagamento no Asaas
private String asaasCustomerId;   // ID do cliente no Asaas
```

### 3. PaymentService Aprimorado

Agora integra com Asaas para:

- Criar clientes automaticamente
- Gerar cobranças reais
- Obter QR Codes PIX válidos
- Verificar status em tempo real

## 🔄 Fluxo de Pagamento

### PIX (Totalmente Integrado)

```
1. Cliente aceita proposta
2. Cliente vai para checkout
3. Sistema cria cliente no Asaas (se não existir)
4. Sistema cria cobrança PIX no Asaas
5. Asaas retorna QR Code e payload
6. Cliente escaneia QR Code
7. Sistema verifica status no Asaas
8. Pagamento confirmado automaticamente
```

**Código:**

```java
// Criar cliente
String customerId = asaasService.getOrCreateCustomer(
    "cliente@email.com",
    "João Silva",
    "12345678900"
);

// Criar cobrança PIX
Map<String, Object> payment = asaasService.createPayment(
    customerId,
    new BigDecimal("500.00"),
    LocalDate.now().plusDays(1),
    "PIX",
    "Serviço de instalação elétrica"
);

// Obter QR Code
Map<String, String> pixData = asaasService.getPixQrCode(paymentId);
String qrCodeBase64 = pixData.get("encodedImage");
String pixCopyPaste = pixData.get("payload");
```

### Cartão de Crédito (Simulado)

Por enquanto, pagamentos com cartão são aprovados instantaneamente para fins de testes.

**TODO:** Implementar integração completa com tokenização de cartão do Asaas.

### Boleto (Disponível via Asaas)

Para ativar boleto:

```java
Map<String, Object> payment = asaasService.createPayment(
    customerId,
    value,
    dueDate,
    "BOLETO",  // Tipo de cobrança
    description
);

String boletoUrl = (String) payment.get("bankSlipUrl");
```

## 📡 Endpoints da API

### POST /api/payments

Cria um novo pagamento (integrado com Asaas).

**Request:**

```json
{
  "serviceRequestId": 1,
  "paymentMethod": "pix",
  "amount": 500.00
}
```

**Response:**

```json
{
  "id": 1,
  "serviceRequestId": 1,
  "amount": 500.00,
  "platformFee": 50.00,
  "totalAmount": 550.00,
  "paymentMethod": "pix",
  "status": "PROCESSING",
  "transactionId": "uuid-123",
  "pixQrCode": "data:image/png;base64,iVBORw...",
  "pixCopyPaste": "00020126580014br.gov.bcb.pix...",
  "createdAt": "2026-03-02T10:00:00"
}
```

### GET /api/payments/{id}/status

Verifica status do pagamento no Asaas e atualiza localmente.

**Response:**

```json
{
  "id": 1,
  "status": "COMPLETED",
  "completedAt": "2026-03-02T10:05:00"
}
```

## 🔍 Verificação de Status

O sistema verifica automaticamente o status do pagamento no Asaas através do endpoint:

```
GET /api/payments/{id}/status
```

### Mapeamento de Status

| Status Asaas | Status ServiJá |
|--------------|----------------|
| PENDING | PROCESSING |
| AWAITING_RISK_ANALYSIS | PROCESSING |
| RECEIVED | COMPLETED |
| CONFIRMED | COMPLETED |
| REFUNDED | REFUNDED |
| OVERDUE | PENDING |

## 🔔 Webhooks (Próxima Implementação)

Para receber notificações automáticas do Asaas quando um pagamento é confirmado:

1. Configure o webhook no painel do Asaas
2. URL: `https://seu-dominio.com/api/webhooks/asaas`
3. Eventos: `PAYMENT_RECEIVED`, `PAYMENT_CONFIRMED`

**TODO:** Implementar controller de webhooks.

## 🧪 Testando no Sandbox

### Ambiente Sandbox

O Asaas possui um ambiente sandbox para testes:

- URL: `https://api-sandbox.asaas.com`
- Não processa pagamentos reais
- Simula aprovações e rejeições

### Simular Pagamento PIX (Sandbox)

```bash
POST /v3/sandbox/payment/{id}/confirm
```

Confirma um pagamento manualmente no sandbox.

### Forçar Vencimento (Sandbox)

```bash
POST /v3/sandbox/payment/{id}/overdue
```

Marca um pagamento como vencido.

## 📊 Dados Armazenados

### Tabela `payments`

Novos campos:

```sql
asaas_payment_id VARCHAR(255)   -- ID do pagamento no Asaas
asaas_customer_id VARCHAR(255)  -- ID do cliente no Asaas
```

Índices criados para performance.

## 🛡️ Segurança

### Chave de API

- **NUNCA** commite a chave de API no repositório
- Use variáveis de ambiente
- Mantenha a chave secreta

### Validações

- Cliente só pode pagar suas próprias solicitações
- Proposta deve estar aceita (status = ACCEPTED)
- Valores são validados antes de enviar ao Asaas

## 🐛 Tratamento de Erros

### Erros Comuns

1. **Cliente inválido**: Email ou dados incompletos
2. **Pagamento duplicado**: Proposta já foi paga
3. **Erro no Asaas**: API fora do ar ou chave inválida
4. **QR Code expirado**: PIX tem validade de 24h

### Logs

Todos os erros são registrados com `@Slf4j`:

```java
log.error("Erro ao criar pagamento PIX no Asaas", e);
```

Consulte os logs para diagnóstico.

## 📈 Monitoramento

### Métricas Importantes

- Taxa de conversão (checkout → pagamento confirmado)
- Tempo médio de confirmação de PIX
- Taxa de erro na API do Asaas
- Pagamentos pendentes

### Dashboard Asaas

Acesse o painel do Asaas para:

- Visualizar transações
- Exportar relatórios
- Configurar webhooks
- Gerenciar reembolsos

## 🚀 Próximas Implementações

### Prioridade Alta

- [ ] Implementar webhooks do Asaas
- [ ] Integração completa com cartão de crédito
- [ ] Sistema de parcelamento
- [ ] Reembolsos automatizados

### Prioridade Média

- [ ] Split de pagamento (liberar valor para prestador)
- [ ] Antecipação de recebíveis
- [ ] Boleto bancário
- [ ] Assinaturas recorrentes

### Prioridade Baixa

- [ ] Link de pagamento personalizável
- [ ] Checkout transparente
- [ ] Múltiplos métodos de pagamento
- [ ] Carteira digital nativa

## 📚 Documentação Asaas

Links úteis:

- [Documentação Oficial](https://docs.asaas.com)
- [API Reference](https://asaasv3.docs.apiary.io)
- [Sandbox](https://sandbox.asaas.com)
- [Status da API](https://status.asaas.com)

## ⚙️ Configurações Recomendadas

### Produção

```properties
asaas.api.url=https://api.asaas.com
asaas.api.key=${ASAAS_API_KEY_PROD}
```

### Sandbox

```properties
asaas.api.url=https://api-sandbox.asaas.com
asaas.api.key=${ASAAS_API_KEY_SANDBOX}
```

## 🎯 Checklist de Deploy

- [ ] Variável `ASAAS_API_KEY` configurada
- [ ] URL correta (sandbox vs produção)
- [ ] Migrations executadas (V20, V21)
- [ ] Testes de pagamento PIX realizados
- [ ] Logs monitorados
- [ ] Webhooks configurados (quando implementados)
- [ ] Certificado SSL ativo (HTTPS obrigatório)

## 💡 Dicas

1. **Sandbox primeiro**: Sempre teste no sandbox antes de produção
2. **Monitore logs**: Erros do Asaas são detalhados nos logs
3. **Cache de clientes**: O sistema já faz cache de clientes do Asaas
4. **Timeouts**: Configure timeouts adequados nas requisições HTTP
5. **Retry logic**: Implemente retentativas para erros temporários

---

**Status**: ✅ Integração PIX 100% funcional  
**Versão**: 1.0.0  
**Data**: 02/03/2026
