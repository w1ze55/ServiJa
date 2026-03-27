# Configuração de Webhooks do Asaas

## 📋 Pré-requisitos

1. Conta ativa no Asaas
2. API Key configurada
3. Aplicação em produção com HTTPS
4. URL pública acessível

---

## 🔧 Passo a Passo

### 1. Gerar Token de Autenticação

Crie um token seguro para autenticar os webhooks:

```bash
# Gerar um token aleatório (use em produção)
openssl rand -base64 32
# Exemplo: 7K9mN2pQ5rS8tU1vW3xY6zB4cD7eF0gH2jK4lM6nP8qR
```

### 2. Configurar Variáveis de Ambiente

Adicione no arquivo `.env` ou configure no servidor:

```bash
ASAAS_WEBHOOK_TOKEN=7K9mN2pQ5rS8tU1vW3xY6zB4cD7eF0gH2jK4lM6nP8qR
```

### 3. URL do Webhook

Sua URL de webhook será:

```
https://seu-dominio.com/api/webhooks/asaas
```

**Importante:**
- ✅ Deve ser HTTPS (obrigatório)
- ✅ Deve estar publicamente acessível
- ✅ Não pode ter autenticação externa (JWT etc)
- ✅ Deve retornar status 200

### 4. Configurar no Painel do Asaas

1. Acesse [https://www.asaas.com](https://www.asaas.com)
2. Faça login na sua conta
3. Vá em **Configurações** → **Integrações** → **Webhooks**
4. Clique em **"Adicionar Webhook"** ou **"Habilitar validação"**

### 5. Preencher Formulário

Conforme a imagem que você compartilhou:

**URL do Webhook:**
```
https://seu-dominio.com/api/webhooks/asaas
```

**E-mail para notificação de erros:**
```
seu-email@empresa.com
```

**Token de autenticação (Opcional):**
```
7K9mN2pQ5rS8tU1vW3xY6zB4cD7eF0gH2jK4lM6nP8qR
```

**Opções:**
- ✅ Validar também saques via interface
- ✅ Ativar autorização de saque para usuários Plus

### 6. Habilitar Validação

Clique em **"Habilitar validação"**

O Asaas vai fazer uma requisição de teste para sua URL.

---

## 🎯 Eventos Configurados

O sistema está preparado para receber os seguintes eventos:

### Pagamentos

| Evento | Descrição | Ação do Sistema |
|--------|-----------|-----------------|
| `PAYMENT_RECEIVED` | Pagamento recebido | Confirma pagamento, adiciona saldo pendente |
| `PAYMENT_CONFIRMED` | Pagamento confirmado | Confirma pagamento, adiciona saldo pendente |
| `PAYMENT_OVERDUE` | Pagamento vencido | Registra vencimento |
| `PAYMENT_REFUNDED` | Pagamento reembolsado | Marca como reembolsado |

### Transferências (Saques)

| Evento | Descrição | Ação do Sistema |
|--------|-----------|-----------------|
| `TRANSFER_CREATED` | Transferência criada | Registra transferência |
| `TRANSFER_DONE` | Transferência concluída | Confirma saque do freelancer |
| `TRANSFER_FAILED` | Transferência falhou | Reverte saldo, marca como falha |

---

## 🔒 Segurança

### Validação de Token

O sistema valida o token em cada requisição:

```java
@RequestHeader(value = "asaas-access-token", required = false) String accessToken

if (!accessToken.equals(webhookToken)) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED);
}
```

O Asaas envia o token no header `asaas-access-token`.

### Headers Recebidos

```
POST /api/webhooks/asaas
Content-Type: application/json
asaas-access-token: 7K9mN2pQ5rS8tU1vW3xY6zB4cD7eF0gH2jK4lM6nP8qR
```

---

## 📨 Formato dos Webhooks

### Exemplo: Pagamento Confirmado

```json
{
  "event": "PAYMENT_CONFIRMED",
  "payment": {
    "id": "pay_123456789",
    "customer": "cus_987654321",
    "value": 500.00,
    "netValue": 485.00,
    "status": "RECEIVED",
    "billingType": "PIX",
    "confirmedDate": "2026-03-02",
    "paymentDate": "2026-03-02T14:30:00"
  }
}
```

### Exemplo: Transferência Concluída

```json
{
  "event": "TRANSFER_DONE",
  "transfer": {
    "id": "tra_987654321",
    "value": 500.00,
    "status": "DONE",
    "type": "PIX",
    "effectiveDate": "2026-03-02",
    "endToEndIdentifier": "E123456789..."
  }
}
```

### Exemplo: Transferência Falhou

```json
{
  "event": "TRANSFER_FAILED",
  "transfer": {
    "id": "tra_987654321",
    "value": 500.00,
    "status": "FAILED",
    "failReason": "Chave PIX inválida",
    "type": "PIX"
  }
}
```

---

## 🧪 Testando o Webhook

### 1. Teste Local (desenvolvimento)

Use **ngrok** para expor sua aplicação local:

```bash
# Instalar ngrok
npm install -g ngrok
# ou
brew install ngrok

# Expor porta 8080
ngrok http 8080
```

Você receberá uma URL pública:
```
https://abc123.ngrok.io
```

Use no Asaas:
```
https://abc123.ngrok.io/api/webhooks/asaas
```

### 2. Teste Manual

Simule um webhook usando curl:

```bash
curl -X POST https://seu-dominio.com/api/webhooks/asaas \
  -H "Content-Type: application/json" \
  -H "asaas-access-token: seu_token_aqui" \
  -d '{
    "event": "PAYMENT_CONFIRMED",
    "payment": {
      "id": "pay_test123",
      "value": 100.00,
      "status": "RECEIVED"
    }
  }'
```

### 3. Verificar Logs

Acompanhe os logs da aplicação:

```bash
# Logs do Spring Boot
tail -f logs/application.log

# Você verá:
# INFO - Webhook recebido do Asaas: {event=PAYMENT_CONFIRMED, ...}
# INFO - Processando evento do webhook: PAYMENT_CONFIRMED
# INFO - Pagamento confirmado via webhook: pay_test123 - R$ 100.00
```

### 4. Teste no Sandbox

No ambiente sandbox do Asaas, você pode forçar eventos:

```bash
# Confirmar pagamento manualmente
POST https://api-sandbox.asaas.com/v3/sandbox/payment/{id}/confirm

# Forçar vencimento
POST https://api-sandbox.asaas.com/v3/sandbox/payment/{id}/overdue
```

---

## 🔍 Monitoramento

### Logs Importantes

```java
log.info("Webhook recebido do Asaas: {}", payload);
log.info("Processando evento do webhook: {}", event);
log.info("Pagamento confirmado via webhook: {} - R$ {}", id, amount);
log.info("Saque confirmado via webhook: {} - R$ {}", id, amount);
log.error("Saque falhou via webhook: {} - Motivo: {}", id, reason);
log.warn("Webhook sem evento especificado");
log.warn("Webhook recebido com token inválido");
```

### Métricas Recomendadas

- Taxa de sucesso dos webhooks (200 vs erros)
- Tempo de processamento
- Eventos por tipo
- Falhas de autenticação
- Webhooks não processados

---

## ⚠️ Troubleshooting

### Webhook não está sendo recebido

1. **Verificar URL:**
   - Está acessível publicamente?
   - HTTPS está funcionando?
   - Certificado SSL válido?

2. **Verificar Logs:**
   ```bash
   grep "Webhook" logs/application.log
   ```

3. **Testar manualmente:**
   ```bash
   curl -I https://seu-dominio.com/api/webhooks/asaas
   # Deve retornar 200 OK
   ```

### Token inválido

1. Verificar `.env`:
   ```bash
   echo $ASAAS_WEBHOOK_TOKEN
   ```

2. Verificar no código:
   ```bash
   grep "webhookToken" logs/application.log
   ```

3. Garantir que token no Asaas é o mesmo do `.env`

### Evento não está sendo processado

1. Verificar se evento está implementado:
   ```java
   case "PAYMENT_RECEIVED": // ✅ Implementado
   case "CUSTOM_EVENT":     // ❌ Não implementado
   ```

2. Adicionar novo evento se necessário

---

## 🚀 Deploy em Produção

### Checklist

- [ ] HTTPS configurado e funcionando
- [ ] Token de webhook gerado e seguro
- [ ] Variável `ASAAS_WEBHOOK_TOKEN` configurada
- [ ] URL pública acessível
- [ ] Webhook configurado no painel do Asaas
- [ ] Teste realizado com sucesso
- [ ] Logs sendo monitorados
- [ ] E-mail de notificação configurado

### Configuração Nginx (exemplo)

```nginx
location /api/webhooks/asaas {
    proxy_pass http://localhost:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

---

## 📊 Resumo do Fluxo

### Pagamento PIX

```
1. Cliente paga no app
2. Asaas recebe pagamento
3. Asaas envia webhook PAYMENT_CONFIRMED
4. Sistema confirma pagamento
5. Sistema adiciona saldo pendente ao freelancer
6. Cliente vê status: COMPLETED
```

### Saque do Freelancer

```
1. Freelancer solicita saque
2. Sistema cria transferência no Asaas
3. Asaas processa PIX
4. Asaas envia webhook TRANSFER_DONE
5. Sistema confirma saque
6. Freelancer vê status: COMPLETED
7. Dinheiro está na conta do freelancer
```

---

## 📚 Links Úteis

- [Documentação Oficial dos Webhooks](https://docs.asaas.com/reference/webhooks)
- [Lista de Eventos](https://docs.asaas.com/reference/eventos-de-webhook)
- [Teste de Webhooks](https://webhook.site) - Para testar payload

---

**Status**: ✅ Implementação Completa  
**Versão**: 1.0.0  
**Data**: 02/03/2026
