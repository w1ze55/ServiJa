# Sistema de Histórico de Pagamento e Liberação de Chat

**Data:** 2026-03-02  
**Contexto:** Implementação de linha do tempo de pagamento com base nos status do Asaas e liberação de chat mediante pagamento aprovado

---

## 📋 Resumo das Mudanças

### Objetivo
Criar um sistema completo de rastreamento de pagamentos que:
1. Registra todos os eventos de mudança de status
2. Exibe uma linha do tempo visual para o usuário
3. Libera o chat automaticamente quando o pagamento for aprovado (`status: "RECEIVED"` ou `"CONFIRMED"`)

---

## 🎯 Status do Pagamento

### Mapeamento Asaas → ServiJá

| Status Asaas | Status Sistema | Descrição | Chat Liberado? |
|--------------|---------------|-----------|----------------|
| `PAYMENT_CREATED` | `PENDING` | Pagamento criado | ❌ Não |
| `PENDING` | `PROCESSING` | Aguardando confirmação | ❌ Não |
| `AWAITING_RISK_ANALYSIS` | `PROCESSING` | Em análise de risco | ❌ Não |
| `PAYMENT_RECEIVED` | `COMPLETED` | **Pagamento confirmado** | ✅ **SIM** |
| `PAYMENT_CONFIRMED` | `COMPLETED` | **Pagamento aprovado** | ✅ **SIM** |
| `PAYMENT_OVERDUE` | `OVERDUE` | Pagamento vencido | ❌ Não |
| `PAYMENT_CREDIT_CARD_CAPTURE_REFUSED` | `FAILED` | Cartão recusado | ❌ Não |
| `PAYMENT_REPROVED_BY_RISK_ANALYSIS` | `FAILED` | Reprovado por risco | ❌ Não |
| `PAYMENT_REFUNDED` | `REFUNDED` | Estornado | ❌ Não |
| `PAYMENT_DELETED` | `CANCELLED` | Cancelado | ❌ Não |
| `PAYMENT_CHARGEBACK_REQUESTED` | `CHARGEBACK` | Contestado | ❌ Não |

---

## 🗄️ Banco de Dados

### Nova Tabela: `payment_status_history`

```sql
CREATE TABLE payment_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    asaas_event VARCHAR(100),
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE,
    INDEX idx_payment_id (payment_id),
    INDEX idx_created_at (created_at)
);
```

### Alteração em `service_requests`

```sql
ALTER TABLE service_requests
ADD COLUMN chat_enabled BOOLEAN DEFAULT FALSE AFTER status;
```

---

## 🔧 Backend

### Arquivos Criados

#### 1. `PaymentStatusHistory.java` (Model)
```java
@Entity
@Table(name = "payment_status_history")
public class PaymentStatusHistory {
    private Long id;
    private Payment payment;
    private Payment.PaymentStatus status;
    private String asaasEvent;  // Ex: "PAYMENT_RECEIVED"
    private String details;      // Descrição do evento
    private LocalDateTime createdAt;
}
```

#### 2. `PaymentStatusHistoryRepository.java`
```java
public interface PaymentStatusHistoryRepository extends JpaRepository<PaymentStatusHistory, Long> {
    List<PaymentStatusHistory> findByPaymentOrderByCreatedAtAsc(Payment payment);
    List<PaymentStatusHistory> findByPaymentIdOrderByCreatedAtAsc(Long paymentId);
}
```

### Arquivos Modificados

#### 1. `ServiceRequest.java`
- ✅ Adicionado campo `chatEnabled` (Boolean)

#### 2. `AsaasWebhookService.java`
- ✅ Injeta `PaymentStatusHistoryRepository`
- ✅ Método `saveStatusHistory()` para registrar mudanças
- ✅ Atualiza `chatEnabled = true` quando `PAYMENT_RECEIVED` ou `PAYMENT_CONFIRMED`
- ✅ Registra histórico para todos os eventos importantes:
  - `PAYMENT_RECEIVED` → `COMPLETED`
  - `PAYMENT_OVERDUE` → `OVERDUE`
  - `PAYMENT_REPROVED_BY_RISK_ANALYSIS` → `FAILED`
  - `PAYMENT_CREDIT_CARD_CAPTURE_REFUSED` → `FAILED`
  - `PAYMENT_CHARGEBACK_REQUESTED` → `CHARGEBACK`
  - `PAYMENT_DELETED` → `CANCELLED`
  - `PAYMENT_REFUNDED` → `REFUNDED`

#### 3. `PaymentService.java`
- ✅ Injeta `PaymentStatusHistoryRepository`
- ✅ Registra histórico inicial quando pagamento é criado
- ✅ Novo método `getPaymentHistory(Long paymentId)` que retorna timeline

#### 4. `PaymentController.java`
- ✅ Novo endpoint `GET /api/payments/{id}/history`
- Retorna lista de eventos ordenados cronologicamente

### Exemplo de Resposta da API

```json
GET /api/payments/123/history
[
  {
    "id": 1,
    "status": "PENDING",
    "asaasEvent": "PAYMENT_CREATED",
    "details": "Pagamento criado via pix",
    "createdAt": "2026-03-02T14:30:00"
  },
  {
    "id": 2,
    "status": "PROCESSING",
    "asaasEvent": "PAYMENT_AWAITING_RISK_ANALYSIS",
    "details": "Pagamento em análise de risco",
    "createdAt": "2026-03-02T14:30:15"
  },
  {
    "id": 3,
    "status": "COMPLETED",
    "asaasEvent": "PAYMENT_RECEIVED",
    "details": "Pagamento recebido/confirmado via webhook Asaas",
    "createdAt": "2026-03-02T14:35:42"
  }
]
```

---

## 🎨 Frontend

### Arquivo Criado

#### `PaymentTimeline.vue`

Componente visual que exibe a linha do tempo do pagamento:

**Características:**
- 🎯 Ícones específicos para cada status
- 🎨 Cores diferentes para cada estado (verde = sucesso, vermelho = falha, etc.)
- 📅 Formatação de datas em PT-BR
- ⏱️ Atualização via API
- 📱 Responsivo

**Exemplo Visual:**

```
┌─────────────────────────────────────────┐
│ ✅ Pagamento Recebido                   │
│ Pagamento confirmado e aprovado ✅      │
└─────────────────────────────────────────┘

Timeline:
● PENDING          (02/03/2026 14:30)
  Pagamento criado via pix

● PROCESSING       (02/03/2026 14:30)
  Pagamento em análise

● COMPLETED        (02/03/2026 14:35)
  Pagamento recebido e confirmado ✅
```

### Arquivo Modificado

#### `CheckoutPage.vue`
- ✅ Importa `PaymentTimeline.vue`
- ✅ Exibe timeline abaixo do QR Code PIX quando `currentPaymentId` existe
- ✅ Timeline aparece automaticamente após gerar QR Code

---

## 🔄 Fluxo Completo

### 1. Cliente Cria Pagamento PIX
```
Cliente → Frontend → Backend → Asaas
                     ↓
            PaymentStatusHistory
            status: PENDING
            event: PAYMENT_CREATED
```

### 2. Cliente Escaneia QR Code
```
Cliente paga → Asaas detecta
               ↓
          Webhook para Backend
```

### 3. Webhook Recebe `PAYMENT_RECEIVED`
```
AsaasWebhookService.handlePaymentReceived()
  ├─ Payment.status = COMPLETED
  ├─ ServiceRequest.chatEnabled = TRUE  ← 🔓 CHAT LIBERADO
  ├─ PaymentStatusHistory registrado
  ├─ Saldo adicionado à carteira do freelancer
  └─ WebSocket notifica cliente
```

### 4. Frontend Atualiza
```
WebSocket recebe notificação
  ├─ CheckoutPage marca paymentConfirmed = true
  ├─ PaymentTimeline busca novo histórico
  └─ Redireciona para página de sucesso
```

---

## 🔐 Liberação do Chat

### Condição para Liberar

```java
// AsaasWebhookService.java - Linha ~220
if (serviceRequest.getStatus() != ServiceRequest.Status.COMPLETED) {
    serviceRequest.setStatus(ServiceRequest.Status.COMPLETED);
    serviceRequest.setChatEnabled(true); // 🔓 HABILITAR CHAT
    serviceRequestRepository.save(serviceRequest);
}
```

### Como Verificar no Frontend

```typescript
// Verificar se o chat está liberado
const response = await fetch(`/api/service-requests/${requestId}`);
const serviceRequest = await response.json();

if (serviceRequest.chatEnabled) {
  // Mostrar chat
} else {
  // Mostrar mensagem: "Aguardando confirmação de pagamento"
}
```

---

## ✅ Testes

### 1. Testar Webhook Localmente (Asaas Sandbox)

```bash
# Configurar webhook no painel Asaas para:
https://homolog.servija.app/api/webhooks/asaas
```

### 2. Simular Pagamento Recebido

```bash
# Payload do webhook
POST /api/webhooks/asaas
{
  "event": "PAYMENT_RECEIVED",
  "payment": {
    "id": "pay_123456789",
    "status": "RECEIVED",
    "value": 100.00
  }
}
```

### 3. Verificar Chat Liberado

```sql
-- Verificar no banco
SELECT id, status, chat_enabled 
FROM service_requests 
WHERE id = 123;

-- Deve retornar:
-- id | status    | chat_enabled
-- 123| COMPLETED | 1 (true)
```

---

## 📝 Migration

**Arquivo:** `V25__create_payment_status_history.sql`

```sql
-- Criar tabela de histórico
CREATE TABLE payment_status_history (...);

-- Adicionar campo chat_enabled
ALTER TABLE service_requests
ADD COLUMN chat_enabled BOOLEAN DEFAULT FALSE AFTER status;

-- Atualizar solicitações já pagas
UPDATE service_requests sr
SET sr.chat_enabled = TRUE
WHERE EXISTS (
    SELECT 1 FROM payments p 
    WHERE p.service_request_id = sr.id 
    AND p.status = 'COMPLETED'
);
```

---

## 🚀 Deploy

### Checklist

- [ ] Aplicar migration V25
- [ ] Build backend
- [ ] Build frontend
- [ ] Reiniciar containers
- [ ] Configurar webhook no Asaas
- [ ] Testar pagamento em homolog
- [ ] Verificar chat liberado

### Comandos

```bash
# Backend
cd backend
mvn clean package

# Frontend
cd frontend
npm run build

# Docker
docker-compose -f docker-compose.homolog.yml down
docker-compose -f docker-compose.homolog.yml up -d --build

# Verificar logs
docker logs -f servija_backend_homolog
```

---

## 🎉 Resultado Final

### ✅ Funcionalidades Implementadas

1. ✅ Histórico completo de mudanças de status
2. ✅ Timeline visual na página de checkout
3. ✅ Chat liberado automaticamente quando `PAYMENT_RECEIVED`
4. ✅ Registro no banco de todos os eventos
5. ✅ Endpoint REST para buscar histórico
6. ✅ Webhook atualiza status em tempo real
7. ✅ WebSocket notifica cliente instantaneamente

### 📊 Exemplo de Uso

1. Cliente cria pagamento → Status `PENDING`
2. Cliente paga PIX → Asaas envia webhook `PAYMENT_RECEIVED`
3. Backend atualiza → Status `COMPLETED` + `chatEnabled = true`
4. Frontend exibe → Timeline atualizada + Chat liberado
5. Cliente e Freelancer → Podem conversar! 🎉

---

## 📚 Referências

- [Documentação Asaas Webhooks](https://docs.asaas.com/reference/webhooks)
- [Eventos de Pagamento Asaas](https://docs.asaas.com/reference/eventos-de-cobranca)
- Arquivo: `WEBSOCKET_AND_PAYMENT_FIXES.md`
- Arquivo: `WEBHOOK_ASAAS_CONFIG.md`
