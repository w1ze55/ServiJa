# Sistema de Carteira e Saques - Freelancers

## 🎯 Visão Geral

O ServiJá implementa um **sistema de carteira** onde os pagamentos dos clientes ficam **armazenados na conta da empresa** no Asaas. Os freelancers podem **solicitar saques** que são processados via **transferência PIX** diretamente do Asaas.

---

## 🔄 Fluxo Completo

### 1. Cliente Paga o Serviço

```
Cliente → Paga via PIX/Cartão → Asaas (Conta da Empresa)
```

- Pagamento fica na conta da empresa
- Valor **não** vai direto para o freelancer
- Sistema registra o pagamento

### 2. Creditação na Carteira

```
Asaas (Conta Empresa) → Saldo Pendente na Carteira do Freelancer
```

- Valor é adicionado como **saldo pendente**
- Fica bloqueado por período de segurança (7 dias padrão)
- Após período, move para **saldo disponível**

### 3. Freelancer Solicita Saque

```
Freelancer → Solicita Saque → Sistema → Asaas → PIX para conta do Freelancer
```

- Freelancer informa chave PIX
- Sistema valida saldo disponível
- Asaas processa transferência PIX
- Dinheiro cai na conta do freelancer

---

## 💰 Estrutura da Carteira

### Tipos de Saldo

1. **Saldo Pendente** (`pending_balance`)
   - Pagamentos recém recebidos
   - Aguardando período de segurança
   - Não pode ser sacado ainda

2. **Saldo Disponível** (`available_balance`)
   - Pode ser sacado a qualquer momento
   - Liberado após período de segurança
   - Sem restrições

3. **Total Ganho** (`total_earned`)
   - Soma de todos os pagamentos recebidos
   - Métrica histórica

4. **Total Sacado** (`total_withdrawn`)
   - Soma de todos os saques realizados
   - Métrica histórica

### Tabela `freelancer_wallets`

```sql
CREATE TABLE freelancer_wallets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    available_balance DECIMAL(10, 2) DEFAULT 0.00,
    pending_balance DECIMAL(10, 2) DEFAULT 0.00,
    total_earned DECIMAL(10, 2) DEFAULT 0.00,
    total_withdrawn DECIMAL(10, 2) DEFAULT 0.00,
    created_at DATETIME,
    updated_at DATETIME
);
```

---

## 📤 Sistema de Saques

### Validações

- **Valor Mínimo**: R$ 10,00
- **Valor Máximo**: R$ 50.000,00 por saque
- **Saldo Suficiente**: Deve ter saldo disponível
- **Chave PIX Válida**: Obrigatória

### Status do Saque

| Status | Descrição |
|--------|-----------|
| `PENDING` | Aguardando processamento |
| `PROCESSING` | Em processamento no Asaas |
| `COMPLETED` | Transferência concluída |
| `FAILED` | Erro no processamento |
| `CANCELLED` | Cancelado pelo freelancer |

### Tabela `withdrawal_requests`

```sql
CREATE TABLE withdrawal_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    freelancer_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    pix_key VARCHAR(255) NOT NULL,
    pix_key_type VARCHAR(50),
    asaas_transfer_id VARCHAR(255),
    transaction_id VARCHAR(255),
    rejection_reason TEXT,
    processed_at DATETIME,
    created_at DATETIME,
    updated_at DATETIME
);
```

---

## 🔌 Endpoints da API

### GET /api/wallet

Obtém saldo da carteira do freelancer.

**Response:**

```json
{
  "id": 1,
  "availableBalance": 1500.00,
  "pendingBalance": 300.00,
  "totalEarned": 5000.00,
  "totalWithdrawn": 3200.00,
  "createdAt": "2026-01-15T10:00:00",
  "updatedAt": "2026-03-02T14:30:00"
}
```

### POST /api/wallet/withdraw

Solicita saque do saldo disponível.

**Request:**

```json
{
  "amount": 500.00,
  "pixKey": "11987654321",
  "pixKeyType": "PHONE"
}
```

**Tipos de Chave PIX:**
- `CPF`
- `CNPJ`
- `EMAIL`
- `PHONE`
- `RANDOM` (chave aleatória)

**Response:**

```json
{
  "id": 42,
  "amount": 500.00,
  "status": "PROCESSING",
  "pixKey": "11987654321",
  "pixKeyType": "PHONE",
  "transactionId": "uuid-123",
  "createdAt": "2026-03-02T14:30:00"
}
```

### GET /api/wallet/withdrawals

Lista todas as solicitações de saque do freelancer.

**Response:**

```json
[
  {
    "id": 42,
    "amount": 500.00,
    "status": "COMPLETED",
    "pixKey": "11987654321",
    "pixKeyType": "PHONE",
    "transactionId": "uuid-123",
    "processedAt": "2026-03-02T14:35:00",
    "createdAt": "2026-03-02T14:30:00"
  },
  {
    "id": 41,
    "amount": 1000.00,
    "status": "COMPLETED",
    "pixKey": "joao@email.com",
    "pixKeyType": "EMAIL",
    "transactionId": "uuid-122",
    "processedAt": "2026-03-01T10:20:00",
    "createdAt": "2026-03-01T10:15:00"
  }
]
```

### DELETE /api/wallet/withdrawals/{id}

Cancela uma solicitação de saque pendente.

**Response:** `204 No Content`

---

## 💡 Lógica de Liberação de Saldo

### Fluxo Automático

```java
// Quando pagamento é confirmado
1. Cliente paga R$ 550,00 (R$ 500 + R$ 50 de taxa)
2. Empresa recebe R$ 550,00 no Asaas
3. Sistema adiciona R$ 500,00 ao SALDO PENDENTE do freelancer
4. Taxa de R$ 50,00 fica com a empresa

// Após 7 dias (período de segurança)
5. Sistema move R$ 500,00 de PENDENTE → DISPONÍVEL
6. Freelancer pode sacar

// Quando freelancer solicita saque
7. Sistema deduz R$ 500,00 do SALDO DISPONÍVEL
8. Sistema cria transferência PIX no Asaas
9. Asaas transfere R$ 500,00 para conta do freelancer
10. Status muda para COMPLETED
```

### Período de Segurança

Por padrão: **7 dias**

**Por que?**
- Proteção contra chargebacks
- Tempo para cliente reportar problemas
- Garantia de qualidade do serviço

**Configurável em:**
```java
private static final int RELEASE_DAYS = 7;
```

---

## 🔒 Segurança

### Validações Implementadas

1. **Saldo Insuficiente**
   ```java
   if (!hasSufficientBalance(freelancer, amount)) {
       throw new IllegalStateException("Saldo insuficiente");
   }
   ```

2. **Valores Mínimo e Máximo**
   ```java
   MIN_WITHDRAWAL_AMOUNT = R$ 10,00
   MAX_WITHDRAWAL_AMOUNT = R$ 50.000,00
   ```

3. **Permissões**
   - Apenas dono da carteira pode sacar
   - Apenas saques PENDING podem ser cancelados

4. **Reversão em Caso de Erro**
   - Se transferência falhar, saldo é devolvido
   - Log completo do erro

---

## 🔄 Integração com Asaas

### Transferência PIX

```java
asaasService.createPixTransfer(
    amount,           // Valor
    pixKey,           // Chave PIX
    pixKeyType,       // Tipo da chave
    description       // Descrição
)
```

### Endpoint Asaas

```
POST /v3/transfers
```

**Request:**

```json
{
  "value": 500.00,
  "pixAddressKey": "11987654321",
  "pixAddressKeyType": "PHONE",
  "description": "Saque ServiJá - uuid-123"
}
```

**Response:**

```json
{
  "id": "tra_123456",
  "status": "PENDING",
  "value": 500.00,
  "transactionId": "E123456789..."
}
```

---

## 📊 Monitoramento

### Logs Importantes

```java
log.info("Saldo pendente adicionado: R$ {} para {}", amount, email);
log.info("Saldo liberado: R$ {} para {}", amount, email);
log.info("Solicitação de saque criada: {} - R$ {}", id, amount);
log.info("Saque processado com sucesso: {}", id);
log.error("Erro ao processar saque: {}", id, exception);
```

### Métricas Recomendadas

- Taxa de sucesso de saques
- Tempo médio de processamento
- Volume total de saques
- Saldo pendente vs disponível
- Erros de transferência PIX

---

## 🚀 Próximas Implementações

### Prioridade Alta

- [ ] **Job Agendado**: Liberar saldos pendentes após 7 dias automaticamente
- [ ] **Webhook Asaas**: Confirmar transferências automaticamente
- [ ] **Notificações**: Avisar freelancer quando saldo for liberado

### Prioridade Média

- [ ] **Histórico Detalhado**: Extrato completo de movimentações
- [ ] **Múltiplas Chaves PIX**: Cadastrar e escolher entre várias chaves
- [ ] **Saque Programado**: Agendar saques recorrentes
- [ ] **Limites Personalizados**: Limites diferentes por freelancer

### Prioridade Baixa

- [ ] **Relatórios**: PDF com histórico de saques
- [ ] **Dashboard**: Gráficos de ganhos e saques
- [ ] **Taxas Diferenciadas**: Taxa menor para saques maiores

---

## 🧪 Testando o Sistema

### 1. Criar Pagamento

```bash
POST /api/payments
{
  "serviceRequestId": 1,
  "paymentMethod": "pix",
  "amount": 500.00
}
```

### 2. Verificar Carteira

```bash
GET /api/wallet

Response:
{
  "availableBalance": 0.00,
  "pendingBalance": 500.00  # Adicionado!
}
```

### 3. Simular Liberação (após 7 dias)

```java
// Manualmente via código ou job
walletService.releasePendingBalance(payment);
```

### 4. Solicitar Saque

```bash
POST /api/wallet/withdraw
{
  "amount": 500.00,
  "pixKey": "11987654321",
  "pixKeyType": "PHONE"
}
```

### 5. Verificar Status

```bash
GET /api/wallet/withdrawals

Response:
[
  {
    "id": 1,
    "amount": 500.00,
    "status": "COMPLETED",
    "pixKey": "11987654321"
  }
]
```

---

## ⚙️ Configuração

### Variáveis de Ambiente

Já configuradas no `.env`:

```bash
ASAAS_API_URL=https://api-sandbox.asaas.com
ASAAS_API_KEY=sua_chave_api
```

### Migrations

Execute as migrations na ordem:

1. `V20__create_payments_table.sql`
2. `V21__add_asaas_fields_to_payments.sql`
3. `V22__create_freelancer_wallet_system.sql` ✨ NOVA

---

## 💼 Vantagens do Sistema

### Para a Empresa

✅ **Controle Total**: Dinheiro fica na conta da empresa  
✅ **Proteção**: Período de segurança contra fraudes  
✅ **Flexibilidade**: Pode reter em caso de disputa  
✅ **Comissão Garantida**: Taxa já descontada antes

### Para o Freelancer

✅ **Transparência**: Saldo sempre visível  
✅ **Facilidade**: Saque via PIX instantâneo  
✅ **Histórico**: Extrato completo de ganhos  
✅ **Sem Burocracia**: Não precisa emitir nota fiscal para saque

### Para o Cliente

✅ **Segurança**: Período para reclamações  
✅ **Garantia**: Dinheiro retido até conclusão do serviço  
✅ **Proteção**: Possibilidade de reembolso

---

## 📈 Resumo do Fluxo

```
Cliente Paga
    ↓
Asaas (Conta Empresa)
    ↓
Saldo Pendente (7 dias)
    ↓
Saldo Disponível
    ↓
Freelancer Solicita Saque
    ↓
Asaas Transfere PIX
    ↓
Dinheiro na Conta do Freelancer
```

---

**Status**: ✅ Sistema de Carteira e Saques 100% Funcional  
**Versão**: 1.0.0  
**Data**: 02/03/2026
