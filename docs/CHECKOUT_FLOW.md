# Fluxo de Checkout - ServiJá

## Visão Geral

O fluxo de checkout no ServiJá permite que clientes realizem o pagamento de serviços após a aceitação de uma proposta por um prestador de serviços.

## Fluxo Completo

### 1. Aceitação da Proposta
- Cliente ou prestador aceita uma proposta/orçamento
- Status da solicitação muda para `ACCEPTED`
- Sistema cria uma notificação para o cliente

### 2. Notificação de Pagamento
- Cliente recebe notificação: "Proposta Aceita - Pagamento Necessário"
- Notificação inclui link para página de checkout
- Cliente clica na notificação e é redirecionado para `/checkout?requestId=X`

### 3. Página de Checkout
**URL:** `/checkout?requestId={id}`

**Componentes:**
- `CheckoutPage.vue` - Componente principal de checkout
- `pages/Checkout/index.vue` - Wrapper da página

**Funcionalidades:**
- Exibe resumo do serviço e prestador
- Mostra breakdown de preços (valor do serviço + taxa da plataforma)
- Oferece 3 métodos de pagamento:
  - PIX (recomendado)
  - Cartão de Crédito
  - Carteira Digital

**Validações:**
- Verifica se requestId existe
- Confirma que proposta foi aceita (status = ACCEPTED)
- Valida permissões do usuário

### 4. Processamento do Pagamento

#### PIX
1. Sistema gera QR Code e código copia-e-cola
2. Cliente escaneia QR Code ou copia código
3. Realiza pagamento no app bancário
4. Sistema monitora webhook de confirmação
5. Após confirmação, status muda para COMPLETED

#### Cartão de Crédito
1. Cliente preenche dados do cartão
2. Sistema valida e processa com gateway de pagamento
3. Confirmação é imediata
4. Status muda para COMPLETED

#### Carteira Digital
1. Cliente é redirecionado para app da carteira
2. Completa pagamento no app
3. Retorna para plataforma
4. Status muda para COMPLETED

### 5. Confirmação de Pagamento
**URL:** `/payment-success?paymentId={id}`

**Funcionalidades:**
- Exibe confirmação de pagamento bem-sucedido
- Mostra detalhes da transação
- Oferece próximos passos:
  - Ver contratos
  - Baixar recibo
  - Voltar ao dashboard

### 6. Pós-Pagamento
- Prestador é notificado do pagamento
- Serviço aparece na lista de contratos ativos
- Cliente pode acompanhar progresso
- Sistema retém valor até conclusão do serviço

## Estrutura de Arquivos

### Frontend
```
frontend/
├── src/
│   ├── pages/
│   │   ├── Checkout/
│   │   │   └── index.vue
│   │   └── PaymentSuccess/
│   │       └── index.vue
│   ├── components/
│   │   └── payment/
│   │       └── CheckoutPage.vue
│   └── api/
│       └── services/
│           └── paymentService.ts
```

### Backend
```
backend/
├── src/main/java/com/servija/servija/
│   ├── controller/
│   │   └── PaymentController.java
│   ├── service/
│   │   └── PaymentService.java
│   ├── repository/
│   │   └── PaymentRepository.java
│   ├── model/
│   │   └── Payment.java
│   └── dto/
│       ├── PaymentRequest.java
│       └── PaymentResponse.java
```

## API Endpoints

### POST /api/payments
Cria um novo pagamento

**Request:**
```json
{
  "serviceRequestId": 1,
  "paymentMethod": "pix",
  "amount": 500.00,
  "cardData": {
    "number": "4111111111111111",
    "name": "João Silva",
    "expiry": "12/25",
    "cvv": "123"
  }
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
  "pixQrCode": "data:image/png;base64,...",
  "pixCopyPaste": "00020126580014br.gov.bcb.pix...",
  "createdAt": "2026-03-02T10:00:00"
}
```

### GET /api/payments/{id}
Obtém detalhes de um pagamento

### GET /api/payments/my
Lista todos os pagamentos do usuário

### GET /api/payments/{id}/status
Verifica status do pagamento (útil para polling de PIX)

### PATCH /api/payments/{id}/status
Atualiza status do pagamento (webhook)

## Estados do Pagamento

- `PENDING` - Pagamento criado, aguardando processamento
- `PROCESSING` - Pagamento em processamento (PIX aguardando confirmação)
- `COMPLETED` - Pagamento confirmado e finalizado
- `FAILED` - Pagamento falhou
- `REFUNDED` - Pagamento reembolsado

## Taxas da Plataforma

- Taxa padrão: 10% sobre o valor do serviço
- Calculada automaticamente no checkout
- Exibida separadamente no resumo

## Segurança

- Todas as rotas requerem autenticação JWT
- Dados de cartão nunca são armazenados em texto plano
- Apenas últimos 4 dígitos são salvos
- Comunicação via HTTPS
- Validação de permissões em todas as operações

## Notificações

O sistema envia notificações em momentos-chave:

1. **Proposta Aceita** - Cliente é notificado para realizar pagamento
2. **Pagamento Confirmado** - Cliente e prestador são notificados
3. **Pagamento Falhou** - Cliente é notificado para tentar novamente
4. **Reembolso Processado** - Cliente é notificado

## Próximas Implementações

- [ ] Integração real com gateway de pagamento (Asaas, Stripe, etc)
- [ ] Webhooks para confirmação automática de PIX
- [ ] Sistema de reembolso automatizado
- [ ] Parcelamento em cartão de crédito
- [ ] Histórico completo de transações
- [ ] Recibos em PDF
- [ ] Split de pagamento (liberação gradual para prestador)
