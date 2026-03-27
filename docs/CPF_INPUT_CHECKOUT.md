# Campo de CPF Obrigatório no Checkout PIX

## 📋 Resumo

Adicionado campo de **CPF obrigatório** no formulário de checkout PIX para garantir que o cliente sempre forneça o CPF antes de gerar o QR Code, evitando erros de validação do Asaas.

## ✅ Melhorias Implementadas

### 1. Frontend - CheckoutPage.vue

**Novo Estado:**
```typescript
const pixCpf = ref<string>('');
```

**Funções de Formatação:**
- `formatCpf()` - Formata CPF com pontos e traço (000.000.000-00)
- `handleCpfInput()` - Aplica formatação em tempo real
- `validateCpf()` - Valida se o CPF tem 11 dígitos

**Novo Formulário PIX:**
- Campo de input para CPF com formatação automática
- Validação em tempo real (11 dígitos)
- Botão "Gerar QR Code PIX" habilitado apenas com CPF válido
- Design destacado com fundo azul claro

**QR Code Display Separado:**
- Card independente que aparece após gerar o QR Code
- Botão para "Gerar Novo QR Code" que limpa os dados
- Visual melhorado com emojis e cores

**Watch Modificado:**
- Não gera mais automaticamente ao selecionar PIX
- Aguarda o cliente digitar o CPF e clicar no botão

### 2. Backend

#### PaymentRequest.java
```java
private String cpf; // CPF do pagador (opcional, mas recomendado para PIX)
```

#### PaymentService.java
**Lógica de Prioridade de CPF:**
1. Tenta usar o CPF fornecido no request (do formulário)
2. Se não fornecido, busca do perfil do usuário
3. Se nenhum dos dois, retorna erro

```java
String cpfCnpj = request.getCpf() != null && !request.getCpf().isEmpty() 
    ? request.getCpf() 
    : contractor.getCpf();
    
if (cpfCnpj == null || cpfCnpj.isEmpty()) {
    throw new IllegalStateException("CPF/CNPJ não encontrado...");
}
```

## 🎯 Benefícios

1. **✅ Sem erros de CPF**: Cliente sempre fornece CPF antes de gerar QR Code
2. **✅ UX Melhorada**: Cliente vê exatamente o que precisa fazer
3. **✅ Validação instantânea**: Feedback em tempo real sobre CPF válido
4. **✅ Formatação automática**: CPF formatado automaticamente (000.000.000-00)
5. **✅ Fallback inteligente**: Usa CPF do perfil se disponível
6. **✅ Botão desabilitado**: Não permite gerar QR Code sem CPF válido

## 🖼️ UI/UX

### Fluxo do Cliente

1. **Seleciona PIX** → Formulário azul aparece
2. **Digita CPF** → Formatação automática enquanto digita
3. **Botão habilita** → Quando CPF tiver 11 dígitos
4. **Clica "Gerar QR Code PIX"** → Loading spinner
5. **QR Code aparece** → Card separado abaixo com QR Code
6. **Pode gerar novo** → Botão para limpar e gerar novamente

### Visual

```
┌─────────────────────────────────────┐
│ Método de Pagamento                 │
│                                     │
│ [x] PIX - Aprovação imediata        │
│                                     │
│ ┌─────────────────────────────────┐ │
│ │ 🛡️ Informe seu CPF              │ │
│ │ para gerar o QR Code            │ │
│ │                                 │ │
│ │ CPF do pagador *                │ │
│ │ [000.000.000-00]                │ │
│ │                                 │ │
│ │ [📲 Gerar QR Code PIX]          │ │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘

(Após clicar)

┌─────────────────────────────────────┐
│ ✅ QR Code PIX Gerado               │
│                                     │
│     ┌─────────────────┐            │
│     │                 │            │
│     │   [QR CODE]     │            │
│     │                 │            │
│     └─────────────────┘            │
│                                     │
│ 📱 Escaneie com seu app             │
│                                     │
│ Código: [00020126...] [📋 Copiar]  │
│                                     │
│ ⏱️ Válido até: 02/03/2026 23:59    │
│ 💰 Valor: R$ 110,00                 │
│                                     │
│ [🔄 Gerar Novo QR Code]             │
└─────────────────────────────────────┘
```

## 🔧 Validações

### Frontend
- ✅ CPF deve ter exatamente 11 dígitos
- ✅ Remove caracteres não numéricos
- ✅ Formata automaticamente enquanto digita
- ✅ Botão desabilitado se CPF inválido

### Backend
- ✅ Usa CPF do request se fornecido
- ✅ Fallback para CPF do perfil do usuário
- ✅ Erro claro se nenhum CPF disponível
- ✅ CPF sem formatação enviado para Asaas

## 📦 Payload da Request

```json
{
  "serviceRequestId": 123,
  "paymentMethod": "PIX",
  "amount": 100.00,
  "cpf": "12345678901"  // ← Novo campo
}
```

## 🚨 Tratamento de Erros

### Erro: CPF não fornecido
```
Por favor, digite um CPF válido (11 dígitos)
```

### Erro: CPF inválido no backend
```
CPF/CNPJ não encontrado. Por favor, informe o CPF para realizar o pagamento.
```

## 🎨 Melhorias Visuais

- ✅ Campo CPF centralizado e em destaque
- ✅ Placeholder com exemplo de formato
- ✅ Fonte mono para melhor legibilidade
- ✅ Emojis para tornar interface mais amigável
- ✅ Cores consistentes com design system
- ✅ Botão com ícone e texto descritivo
- ✅ Loading state com spinner
- ✅ QR Code em card separado e destacado

## 🔄 Comparação: Antes vs Depois

### ❌ Antes
1. Cliente seleciona PIX
2. Sistema tenta gerar QR Code automaticamente
3. **Erro**: "CPF não cadastrado"
4. Cliente fica perdido, não sabe o que fazer

### ✅ Depois
1. Cliente seleciona PIX
2. **Formulário pede CPF**
3. Cliente digita CPF
4. Clica em "Gerar QR Code"
5. QR Code aparece com sucesso
6. Cliente faz o pagamento

## 📝 Próximos Passos (Opcional)

- [ ] Adicionar validação de CPF mais robusta (dígitos verificadores)
- [ ] Permitir salvar CPF no perfil a partir do checkout
- [ ] Lembrar último CPF usado (localStorage)
- [ ] Adicionar suporte para CNPJ também
- [ ] Máscara condicional: CPF (11 dígitos) ou CNPJ (14 dígitos)

## 🧪 Testado

- ✅ Formatação de CPF em tempo real
- ✅ Validação de 11 dígitos
- ✅ Botão habilita/desabilita corretamente
- ✅ Geração de QR Code com CPF
- ✅ Card separado aparece após gerar
- ✅ Botão "Gerar Novo QR Code" limpa dados
- ✅ Backend aceita CPF do request
- ✅ Fallback para CPF do perfil funciona
