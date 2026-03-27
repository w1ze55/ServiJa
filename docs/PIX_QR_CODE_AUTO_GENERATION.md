# Geração Automática de QR Code PIX

## 📋 Resumo

Implementado sistema de **geração automática de QR Code PIX** no momento em que o cliente seleciona PIX como método de pagamento no checkout.

## ✅ Implementações

### 1. Frontend - CheckoutPage.vue

**Mudanças:**
- Adicionado ícone `Loader2` para loading state
- Adicionado estado `isLoadingPix` para controlar carregamento
- Atualizado tipo de `pixData` para incluir `qrCodeImage`, `payload` e `expirationDate`
- Adicionado `watch` no `paymentMethod` para detectar quando PIX é selecionado
- Criada função `generatePixQrCode()` que:
  - Cria um pagamento temporário via API
  - Busca o QR Code do Asaas
  - Exibe o QR Code na tela
- Atualizada interface do PIX com:
  - Estado de loading com spinner
  - QR Code em formato de imagem (base64)
  - Código PIX Copia e Cola truncado
  - Botão para copiar código
  - Informações de validade e valor
  - Estado de erro com botão de retry

**Fluxo:**
1. Cliente seleciona PIX → `watch` detecta
2. Chama `generatePixQrCode()` automaticamente
3. Cria pagamento no backend
4. Backend cria cobrança no Asaas
5. Frontend busca QR Code via endpoint `/api/payments/{id}/pix-qrcode`
6. Exibe QR Code + código Copia e Cola

### 2. Backend

#### PaymentController.java
```java
@GetMapping("/{id}/pix-qrcode")
public ResponseEntity<Map<String, Object>> getPixQrCode(@PathVariable Long id)
```
Novo endpoint para retornar dados do QR Code PIX.

#### PaymentService.java
- **Adicionado validação de CPF/CNPJ obrigatório** (linha 60-63):
  - Verifica se o contratante tem CPF cadastrado
  - Lança exceção se não tiver
  - Passa CPF para `asaasService.getOrCreateCustomer()`
- **Novo método `getPixQrCode()`**:
  - Busca pagamento por ID
  - Valida se é PIX
  - Chama `asaasService.getPixQrCode()`
  - Retorna dados formatados

#### PaymentResponse.java
- Adicionado campo `asaasPaymentId` para identificar pagamento no Asaas

### 3. Docker & Configuração

#### .env
- **Corrigido escape da chave API do Asaas**:
  ```bash
  # Antes (não funcionava):
  ASAAS_API_KEY=$aact_prod_...
  
  # Depois (funcionando):
  ASAAS_API_KEY=$$aact_prod_...
  ```
- Docker Compose interpreta `$` como variável de ambiente
- Usar `$$` faz o escape correto

## 🚀 Como Usar

### Pré-requisitos

1. **Usuário deve ter CPF cadastrado**:
   - Endpoint: `PUT /api/users/me`
   - Body: `{ "name": "...", "phone": "...", "cpf": "12345678901" }`

2. **Backend configurado** com:
   - `ASAAS_API_KEY` corretamente escapada no `.env`
   - `ASAAS_WALLET_ID` configurado
   - Docker Compose rodando

### Fluxo do Cliente

1. Acessa proposta aceita
2. Clica em "Realizar Pagamento"
3. **Seleciona PIX** → QR Code é gerado automaticamente
4. Opções:
   - Escanear QR Code com app do banco
   - Copiar código PIX Copia e Cola
5. Realizar pagamento
6. Sistema confirma automaticamente via webhook

## 📊 Estrutura de Dados

### QR Code Response
```json
{
  "encodedImage": "iVBORw0KGgoAAAANSUhEUgAA...",
  "payload": "00020126580014br.gov.bcb.pix...",
  "expirationDate": "2026-03-03 23:59:59",
  "description": "Pagamento para..."
}
```

## ⚠️ Tratamento de Erros

### 1. CPF não cadastrado
**Erro:** "CPF/CNPJ do contratante não cadastrado"
**Solução:** Cliente deve atualizar perfil com CPF

### 2. Erro ao gerar QR Code
**UI:** Mostra mensagem de erro + botão "Tentar Novamente"

### 3. Token Asaas inválido (401)
**Causa:** Chave API não configurada corretamente
**Solução:** Verificar escape `$$` no `.env`

## 🔐 Segurança

- ✅ Validação JWT em todos os endpoints
- ✅ Verificação de ownership do pagamento
- ✅ CPF/CNPJ obrigatório para PIX (exigência Asaas)
- ✅ API Key do Asaas em variável de ambiente (não commitada)

## 🧪 Teste Manual

### Criar Pagamento de Teste via API

```bash
# 1. Criar cliente
curl -X POST https://api.asaas.com/v3/customers \
  -H "access_token: $ASAAS_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Cliente Teste",
    "cpfCnpj": "24971563792",
    "email": "teste@example.com"
  }'

# 2. Criar cobrança PIX
curl -X POST https://api.asaas.com/v3/payments \
  -H "access_token: $ASAAS_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "customer": "cus_000164003041",
    "billingType": "PIX",
    "value": 5.00,
    "dueDate": "2026-03-03",
    "description": "Teste ServiJá"
  }'

# 3. Obter QR Code
curl -X GET https://api.asaas.com/v3/payments/pay_xxx/pixQrCode \
  -H "access_token: $ASAAS_API_KEY"
```

## 📝 Próximos Passos

- [ ] Implementar polling automático para verificar status do pagamento
- [ ] Adicionar notificação quando pagamento for confirmado
- [ ] Implementar timeout/expiração visual do QR Code
- [ ] Adicionar validação de CPF no frontend antes de criar pagamento
- [ ] Criar modal para solicitar CPF caso não esteja cadastrado

## 🐛 Problemas Conhecidos

1. **Status não atualiza automaticamente**: Cliente precisa verificar manualmente
   - **Solução temporária**: Polling manual
   - **Solução ideal**: Webhooks do Asaas (já implementado no backend)

2. **Múltiplos pagamentos para mesma proposta**: Não há bloqueio
   - **Solução**: Adicionar verificação se já existe pagamento pendente

## 📚 Documentação Relacionada

- [ASAAS_INTEGRATION.md](./ASAAS_INTEGRATION.md) - Integração completa com Asaas
- [WALLET_AND_WITHDRAWALS.md](./WALLET_AND_WITHDRAWALS.md) - Sistema de carteira
- [CHECKOUT_FLOW.md](./CHECKOUT_FLOW.md) - Fluxo completo de checkout
