# Estrutura de Taxas do ServiJá - Correção Completa

**Data:** 2026-03-02  
**Contexto:** Ajuste das taxas do gateway (Asaas) para garantir que o valor total chegue na conta ServiJá

---

## 🔍 Problema Identificado

### Situação Original:
```
Valor do serviço: R$ 5,50
Total cobrado: R$ 6,05 (5,50 + 0,55 taxa ServiJá)

❌ Valor que chegou na conta Asaas: R$ 4,07
   - Taxa PIX Asaas: -R$ 0,99
   - Taxa notificação: -R$ 0,99
   = R$ 6,05 - R$ 1,98 = R$ 4,07
```

**Problema:** O sistema esperava R$ 6,05, mas só chegou R$ 4,07 por causa das taxas do Asaas.

---

## ✅ Solução Implementada

### 1. **Notificações Automáticas Desabilitadas**
- Modificado `AsaasService.java` para incluir `postalService: false`
- **Elimina a taxa de R$ 0,99 por notificação**

### 2. **Taxa do Gateway Incluída no Cálculo**
- Adicionado campo `gateway_fee` no banco de dados
- Taxa do Asaas agora é **repassada para o cliente**

### 3. **Novo Cálculo de Valores**

```java
// Taxas Asaas (promocionais por 3 meses)
PIX:         R$ 0,99
Boleto:      R$ 0,99
Cartão:      R$ 0,49 + 1,99%
```

#### Exemplo PIX (R$ 5,50):
```
Valor do serviço:        R$ 5,50
Taxa ServiJá (10%):      R$ 0,55
Taxa Asaas PIX:          R$ 0,99
────────────────────────────────
Total a pagar:           R$ 7,04 ✅
```

#### O que entra na conta Asaas:
```
Cliente paga:            R$ 7,04
Taxa Asaas PIX:         -R$ 0,99
────────────────────────────────
Líquido ServiJá:         R$ 6,05 ✅
```

#### Distribuição final:
```
Líquido na conta:        R$ 6,05
Taxa ServiJá (10%):     -R$ 0,55
────────────────────────────────
Para o freelancer:       R$ 5,50 ✅
```

---

## 📋 Arquivos Modificados

### Backend

1. **Migration V23**
   - `V23__add_gateway_fee_to_payments.sql`
   - Adiciona coluna `gateway_fee` à tabela `payments`

2. **Payment.java**
   - Adiciona campo `gatewayFee`

3. **PaymentService.java**
   - Define constantes de taxas do Asaas
   - Calcula `gatewayFee` baseado no método de pagamento
   - Inclui no `totalAmount`

4. **PaymentResponse.java**
   - Adiciona campo `gatewayFee` no DTO

5. **AsaasService.java**
   - Desabilita notificações: `postalService: false`

---

## 📊 Comparação: Antes vs Depois

### Antes (Incorreto):
```
Serviço: R$ 5,50 → Cliente paga R$ 6,05 → Chega R$ 4,07 ❌
```

### Depois (Correto):
```
Serviço: R$ 5,50 → Cliente paga R$ 7,04 → Chega R$ 6,05 ✅
```

---

## 🎯 Taxas por Método de Pagamento

### PIX
- Taxa Asaas: **R$ 0,99** (promocional por 3 meses, depois R$ 1,99)
- Fórmula: `Serviço + 10% + R$ 0,99`

### Boleto
- Taxa Asaas: **R$ 0,99** (promocional por 3 meses, depois R$ 1,99)
- Fórmula: `Serviço + 10% + R$ 0,99`

### Cartão de Crédito
- Taxa Asaas: **R$ 0,49 + 1,99%** (promocional por 3 meses)
- Fórmula: `Serviço + 10% + R$ 0,49 + (Serviço × 1,99%)`

#### Exemplo Cartão (R$ 5,50):
```
Valor do serviço:        R$ 5,50
Taxa ServiJá (10%):      R$ 0,55
Taxa Asaas fixa:         R$ 0,49
Taxa Asaas %:            R$ 0,11 (5,50 × 1,99%)
────────────────────────────────
Total a pagar:           R$ 6,65
```

---

## ⚠️ Importante

### Taxas Promocionais
As taxas do Asaas são **promocionais por 3 meses** após a criação da conta. Depois:
- PIX: R$ 1,99
- Boleto: R$ 1,99
- Cartão: R$ 0,49 + 2,99%

**Ação necessária:** Atualizar as constantes em `PaymentService.java` após o período promocional.

### Notificações Desabilitadas
Com `postalService: false`, o Asaas **não enviará notificações automáticas** aos clientes. 

**Alternativas:**
1. Implementar sistema próprio de notificações (e-mail/SMS)
2. Enviar apenas via WhatsApp do cliente
3. Absorver o custo de R$ 0,99 e reativar notificações

---

## 🚀 Próximos Passos

1. ✅ Deploy da migration V23
2. ✅ Rebuild do backend
3. ⏳ Testar nova cobrança PIX
4. ⏳ Verificar valor no extrato Asaas
5. ⏳ Implementar sistema próprio de notificações (opcional)
6. ⏳ Atualizar taxas após período promocional (em ~3 meses)

---

## 📱 Transparência com o Cliente

O frontend agora mostra:
- Valor do serviço
- Taxa da plataforma (10%)
- **Taxa de processamento** (gateway)
- **Total a pagar**

Exemplo de exibição:
```
Valor do serviço:         R$ 5,50
Taxa da plataforma:       R$ 0,55
Taxa de processamento:    R$ 0,99
────────────────────────────────
Total:                    R$ 7,04
```
