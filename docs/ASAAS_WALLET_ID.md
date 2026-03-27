# Configuração do Wallet ID do Asaas

## 📋 O que é o Wallet ID?

O **Wallet ID** é o identificador único da sua carteira (conta) no Asaas. Ele é usado para:

- Identificar sua conta em operações de split de pagamento
- Transferências entre contas Asaas
- Operações de marketplace

---

## 🔍 Seu Wallet ID

```
92e20694-9b8c-484d-9cd2-5d6f52288393
```

Este ID já foi configurado automaticamente no sistema.

---

## 📍 Onde Encontrar o Wallet ID

### Método 1: Via API

```bash
curl -X GET "https://api-sandbox.asaas.com/v3/wallets/" \
  -H "access_token: SUA_CHAVE_API"
```

**Response:**
```json
{
  "id": "92e20694-9b8c-484d-9cd2-5d6f52288393",
  "name": "Carteira Principal",
  ...
}
```

### Método 2: Via Painel do Asaas

1. Acesse [https://www.asaas.com](https://www.asaas.com)
2. Faça login
3. Vá em **Configurações** → **Conta** → **Dados da conta**
4. O Wallet ID estará listado

---

## ⚙️ Configuração no Sistema

### 1. Arquivo `.env`

```bash
ASAAS_WALLET_ID=92e20694-9b8c-484d-9cd2-5d6f52288393
```

### 2. Verificar Configuração

O sistema carrega automaticamente via:

```properties
asaas.wallet.id=${ASAAS_WALLET_ID:}
```

---

## 🎯 Casos de Uso

### 1. Split de Pagamento (Futuro)

Quando você quiser dividir pagamentos entre múltiplas contas:

```java
// Exemplo: 90% para a conta da empresa, 10% para afiliado
{
  "split": [
    {
      "walletId": "92e20694-9b8c-484d-9cd2-5d6f52288393",
      "percentualValue": 90
    },
    {
      "walletId": "outro-wallet-id",
      "percentualValue": 10
    }
  ]
}
```

### 2. Transferências entre Contas Asaas

```java
// Transferir para outra conta Asaas
POST /v3/transfers/
{
  "walletId": "92e20694-9b8c-484d-9cd2-5d6f52288393",
  "value": 500.00
}
```

### 3. Marketplace

Se você criar subcontas para freelancers no Asaas, pode usar o Wallet ID para:
- Identificar sua conta principal
- Fazer transferências entre contas
- Gerenciar splits automáticos

---

## ✅ Status da Configuração

| Item | Status |
|------|--------|
| Wallet ID identificado | ✅ |
| Configurado no `.env` | ✅ |
| Configurado no `application.properties` | ✅ |
| Pronto para uso | ✅ |

---

## 🔐 Segurança

- ✅ Wallet ID não é sensível (pode ser compartilhado)
- ✅ Diferente da API Key (que deve ser secreta)
- ✅ Usado apenas para identificação, não autenticação

---

## 📚 Documentação Relacionada

- [Asaas - Carteiras](https://docs.asaas.com/reference/carteiras)
- [Asaas - Split de Pagamento](https://docs.asaas.com/reference/split-de-pagamento)
- [Asaas - Transferências](https://docs.asaas.com/reference/transferencias)

---

**Configuração Completa!** ✅

Seu Wallet ID está configurado e pronto para uso no sistema ServiJá.
