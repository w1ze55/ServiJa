# ✅ Configuração Completa do Webhook Asaas - ServiJa

## 📋 Resumo da Implementação

Data: 02/03/2026

---

## 🎯 O Que Foi Implementado

### 1. ✅ Webhook Asaas Configurado

**Eventos Configurados (30 eventos):**
- Todos os eventos de pagamento, chargeback, estorno, split, e visualização
- Ver lista completa em: `docs/WEBHOOK_ASAAS_CONFIG.md`

**Backend preparado:**
- `AsaasWebhookController.java` - Recebe webhooks
- `AsaasWebhookService.java` - Processa todos os 30 eventos
- `WebSocketService.java` - Notifica frontend em tempo real
- Token de segurança configurado no `.env`

### 2. ✅ Sistema de Verificação Automática de Pagamento

Implementamos **duas camadas** de verificação para garantir que o pagamento seja detectado:

#### **Camada 1: WebSocket (Tempo Real)**
- Quando o Asaas confirma o pagamento, envia webhook
- Backend processa e notifica frontend via WebSocket
- Frontend recebe notificação instantânea

#### **Camada 2: Polling Automático (Fallback)**
- A cada 3 segundos, consulta o status do pagamento no Asaas
- Funciona mesmo se o webhook falhar
- Para automaticamente quando pagamento é confirmado
- Timeout após 5 minutos (100 tentativas)

---

## 🔧 Arquivos Modificados

### Backend

| Arquivo | O Que Foi Feito |
|---------|-----------------|
| `AsaasWebhookService.java` | ✅ Adicionados 30 métodos para processar todos os eventos do Asaas |
| `Payment.java` | ✅ Adicionados novos status: CANCELLED, OVERDUE, CHARGEBACK |
| `WebSocketService.java` | ✅ Adicionado método `notifyPaymentFailed()` |
| `V24__add_new_payment_statuses.sql` | ✅ Migration para MySQL com novos status |

### Frontend

| Arquivo | O Que Foi Feito |
|---------|-----------------|
| `usePaymentWebSocket.ts` | ✅ Corrigido tópico para `/user/queue/payment` |
| `usePaymentStatusPolling.ts` | ✅ **NOVO** - Composable para verificação automática |
| `CheckoutPage.vue` | ✅ Integrado polling + WebSocket com fallback |

### Configuração

| Arquivo | O Que Foi Feito |
|---------|-----------------|
| `.env` | ✅ Token de webhook configurado: `a1/VlaJoShmhsOeGXHJBQOHpwdReybpDM+imiAnv0t8=` |
| `WEBHOOK_ASAAS_CONFIG.md` | ✅ Documentação completa dos eventos |

---

## 🚀 Como Funciona Agora

### Fluxo Completo de Pagamento PIX:

```mermaid
sequenceDiagram
    participant Cliente
    participant Frontend
    participant Backend
    participant Asaas
    participant WebSocket
    
    Cliente->>Frontend: Gera QR Code PIX
    Frontend->>Backend: POST /api/payments
    Backend->>Asaas: Cria cobrança
    Asaas-->>Backend: Retorna QR Code
    Backend-->>Frontend: QR Code + Payment ID
    
    Frontend->>Frontend: Inicia Polling (3s)
    Frontend->>WebSocket: Conecta /user/queue/payment
    
    Cliente->>Asaas: Paga via PIX no banco
    
    Asaas->>Backend: Webhook: PAYMENT_CONFIRMED
    Backend->>Backend: Atualiza status COMPLETED
    Backend->>WebSocket: Notifica cliente
    WebSocket-->>Frontend: PIX_CONFIRMED
    
    Frontend->>Frontend: Para Polling
    Frontend->>Cliente: Redireciona para sucesso
    
    Note over Frontend,Backend: Polling (Fallback)
    loop A cada 3s (se webhook falhar)
        Frontend->>Backend: GET /api/payments/{id}/status
        Backend->>Asaas: GET /v3/payments/{id}
        Asaas-->>Backend: Status atual
        Backend-->>Frontend: Status atualizado
        Frontend->>Frontend: Se COMPLETED, redireciona
    end
```

---

## 🧪 Como Testar

### 1. Teste Local

```bash
# 1. Iniciar backend
cd backend
mvn spring-boot:run

# 2. Iniciar frontend
cd frontend
npm run dev

# 3. Fazer um pagamento PIX de teste
```

### 2. Monitorar Logs

**Backend:**
```bash
# Procurar por:
- "Webhook recebido do Asaas"
- "Pagamento confirmado via webhook"
- "Status do pagamento atualizado"
```

**Frontend (Console do navegador):**
```javascript
// Procurar por:
- 🔄 Iniciando verificação automática de pagamento
- 🔍 Verificando status do pagamento... (tentativa X/100)
- 📊 Status do pagamento: COMPLETED
- ✅ Pagamento confirmado!
```

### 3. Testar Webhook Manualmente

Você pode simular um webhook do Asaas:

```bash
curl -X POST http://localhost:8080/api/webhooks/asaas \
  -H "Content-Type: application/json" \
  -H "asaas-access-token: a1/VlaJoShmhsOeGXHJBQOHpwdReybpDM+imiAnv0t8=" \
  -d '{
    "event": "PAYMENT_CONFIRMED",
    "payment": {
      "id": "pay_123456789",
      "status": "CONFIRMED",
      "value": 100.00
    }
  }'
```

---

## ⚠️ Pendências - Ação Manual Necessária

### ❗ Configurar Webhook no Painel do Asaas

A API key atual está retornando erro 401, então você precisa:

1. **Acessar:** https://www.asaas.com/login
2. **Ir em:** Configurações → Integrações → Webhooks
3. **Criar novo webhook:**
   - Nome: `ServiJa Payment Webhook - Production`
   - URL: `https://servija.app/api/webhooks/asaas`
   - Token: `a1/VlaJoShmhsOeGXHJBQOHpwdReybpDM+imiAnv0t8=`
   - Versão da API: `3`
   - Tipo: `SEQUENCIAL`
   - Marcar todos os eventos de pagamento

4. **Gerar nova API key:**
   - Revogar a API key antiga
   - Gerar uma nova com permissões completas
   - Atualizar no `.env`

---

## 📊 Status Atual do Sistema

| Componente | Status | Observação |
|------------|--------|------------|
| **Webhook Backend** | ✅ Pronto | 30 eventos implementados |
| **WebSocket** | ✅ Pronto | Notificação em tempo real |
| **Polling Automático** | ✅ Pronto | Verifica a cada 3s |
| **Frontend** | ✅ Pronto | Dupla camada de verificação |
| **Webhook no Asaas** | ⚠️ Pendente | Precisa configurar manualmente |
| **API Key** | ⚠️ Atualizar | Gerar nova chave |

---

## 🔐 Segurança

- ✅ Token de webhook único e seguro
- ✅ Validação de token no backend
- ✅ HTTPS obrigatório em produção
- ⚠️ API key precisa ser renovada

---

## 📚 Documentação

- `docs/WEBHOOK_ASAAS_CONFIG.md` - Guia completo dos webhooks
- `docs/ASAAS_INTEGRATION.md` - Integração com Asaas
- `frontend/src/composables/usePaymentStatusPolling.ts` - Código do polling
- `frontend/src/composables/usePaymentWebSocket.ts` - Código do WebSocket

---

## 🎉 Resultado Esperado

Agora, quando um cliente pagar via PIX:

1. ⚡ **Webhook detecta em 1-2 segundos** (ideal)
2. 🔄 **Polling detecta em até 3 segundos** (fallback)
3. ✅ **Cliente vê confirmação automática**
4. 💰 **Saldo é creditado na carteira do freelancer**
5. ✉️ **Ambas as partes recebem notificação**

**O sistema é resiliente:** Mesmo se o webhook falhar, o polling garante que o pagamento será detectado!

---

## 🆘 Solução de Problemas

### Pagamento não é detectado

1. Verificar logs do backend
2. Verificar se o webhook está configurado no Asaas
3. Verificar se o polling está ativo (console do navegador)
4. Testar endpoint manualmente: `GET /api/payments/{id}/status`

### WebSocket não conecta

1. Verificar se o backend está rodando
2. Verificar URL do WebSocket no console
3. O polling deve funcionar como fallback

### Webhook retorna 401

1. Verificar se o token no `.env` está correto
2. Verificar se o token no Asaas é o mesmo

---

**🎯 Sistema de pagamento completo e resiliente implementado com sucesso!**
