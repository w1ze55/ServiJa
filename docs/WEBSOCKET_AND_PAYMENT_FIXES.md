# Correções de WebSocket e Pagamento Duplicado

**Data:** 2026-03-02  
**Contexto:** Correção de erros 403 no WebSocket e erro 400 de pagamento duplicado

---

## Problemas Identificados

### 1. Erro 403 - WebSocket Bloqueado
**Erro:**
```json
{
    "timestamp": "2026-03-02T18:26:45.053Z",
    "status": 403,
    "error": "Forbidden",
    "path": "/ws/info"
}
```

**Causa:** O endpoint `/ws/**` não estava na lista de URLs permitidas no `SecurityConfig.java`.

**Solução:** Adicionado `/ws/**` aos endpoints públicos:
```java
.requestMatchers(
    "/api/auth/**",
    "/api/freelancer/nearby",
    "/api/freelancer/*/profile",
    "/api/portfolio/freelancer/**",
    "/api/services/public/**",
    "/api/files/**",
    "/error",
    "/swagger-ui/**",
    "/v3/api-docs/**",
    "/swagger-ui.html",
    "/ws/**")  // <-- ADICIONADO
.permitAll()
```

### 2. Erro 400 - Pagamento Duplicado
**Erro:**
```json
{
    "timestamp": "2026-03-02T18:26:41.968166013",
    "status": 400,
    "error": "Bad Request",
    "message": "A solicitação deve estar aceita para realizar o pagamento",
    "remainingAttempts": null,
    "lockUntil": null
}
```

**Causa:** Sistema permitia criar múltiplos pagamentos para a mesma solicitação de serviço.

**Solução:** Adicionada verificação no `PaymentService.java`:
```java
// Verificar se já existe um pagamento para esta solicitação
boolean paymentExists = paymentRepository.existsByServiceRequest(serviceRequest);
if (paymentExists) {
    throw new IllegalStateException("Já existe um pagamento para esta solicitação");
}
```

E método no `PaymentRepository.java`:
```java
boolean existsByServiceRequest(ServiceRequest serviceRequest);
```

---

## Arquivos Alterados

### Backend
1. **`SecurityConfig.java`**
   - Adicionado `/ws/**` aos endpoints públicos

2. **`PaymentService.java`**
   - Adicionada verificação de pagamento duplicado

3. **`PaymentRepository.java`**
   - Adicionado método `existsByServiceRequest`

---

## Resultado

✅ **WebSocket**: Endpoint `/ws/**` agora está acessível sem autenticação  
✅ **Pagamento Duplicado**: Sistema bloqueia tentativas de criar múltiplos pagamentos para a mesma solicitação  
✅ **Mensagem de Erro**: Usuário recebe feedback claro quando tenta pagar novamente

---

## Testes Realizados

1. ✅ Acesso ao WebSocket endpoint (`/ws/info`) sem erro 403
2. ✅ Tentativa de criar pagamento duplicado retorna erro específico
3. ✅ Console do navegador sem erros de conexão WebSocket
4. ✅ Página de checkout carregando corretamente

---

## Próximos Passos

1. Implementar status `PAID` ou `IN_PROGRESS` para solicitações com pagamento confirmado
2. Atualizar frontend para mostrar mensagem clara quando pagamento já existe
3. Adicionar botão para visualizar pagamento existente ao invés de tentar criar novo
