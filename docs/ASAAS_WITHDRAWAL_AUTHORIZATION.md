# Autorizacao de Saques no Asaas

## Objetivo

Eliminar a confirmacao manual por SMS/app do Asaas nos saques do ServiJa sem abrir mao da validacao de seguranca.

## O que a documentacao oficial do Asaas diz

1. Toda retirada via API gera um evento critico por padrao.
2. Para automatizar saques sem aprovacao manual, o Asaas permite desativar o evento critico para IPs autorizados na Whitelist.
3. O Asaas recomenda combinar isso com o mecanismo de autorizacao de transferencias via webhook.
4. Nesse mecanismo, o Asaas faz um `POST` para o nosso sistema alguns segundos apos criar a transferencia.
5. O nosso endpoint precisa responder com `{"status":"APPROVED"}` ou `{"status":"REFUSED"}`.

## Implementacao no backend

O backend agora expõe um endpoint dedicado para esse mecanismo:

```text
POST /api/webhooks/asaas/transfer-authorization
GET  /api/webhooks/asaas/transfer-authorization
```

Comportamento:

- Valida o token enviado no header `asaas-access-token`.
- Procura o saque pelo `asaasTransferId`.
- Confere o valor da transferencia e o tipo de operacao.
- Responde `APPROVED` quando o saque pertence ao ServiJa.
- Responde `REFUSED` quando o token ou os dados da transferencia nao batem.

## Configuracao recomendada no painel do Asaas

### 1. Descobrir o IP de saida do backend

Se a aplicacao roda em nuvem, o IP de saida precisa ser fixo. Se hoje ele for dinamico, o ideal e usar NAT Gateway ou outro mecanismo equivalente para fixar o egress IP.

### 2. Configurar Whitelist de IPs

No Asaas:

```text
Menu do usuario > Integracoes > Mecanismos de seguranca > Whitelist de IPs
```

Adicionar o IP de saida do backend.

Na mesma tela, ajustar:

```text
Evento critico em requisicoes de saque = Desativado
```

Isso faz com que requisicoes de saque vindas desse IP nao precisem mais da aprovacao manual por SMS/app.

### 3. Configurar webhook de autorizacao de transferencias

No Asaas:

```text
Menu do usuario > Integracoes > Mecanismos de seguranca
```

Preencher:

- URL: `https://seu-dominio.com/api/webhooks/asaas/transfer-authorization`
- E-mail de notificacao de erros: o e-mail operacional da equipe
- Token: usar o mesmo valor configurado em `ASAAS_WEBHOOK_TOKEN`

Observacoes:

- Nao confundir esse endpoint com o webhook comum `/api/webhooks/asaas`.
- Marque `Validar tambem saques via interface` apenas se quiser que saques criados manualmente no painel passem pelo mesmo fluxo.
- A opcao de autorizacao para estornos Pix nao e necessaria para o fluxo atual de saque do marketplace.

## Variavel de ambiente

Continuamos usando:

```bash
ASAAS_WEBHOOK_TOKEN=seu_token_seguro
```

O mesmo token pode proteger:

- webhook comum de eventos
- webhook de transacoes PIX
- webhook de autorizacao de saques

## Fluxo esperado apos a configuracao

1. Freelancer solicita saque no ServiJa.
2. O backend cria a transferencia via API do Asaas.
3. O Asaas chama `/api/webhooks/asaas/transfer-authorization`.
4. O ServiJa responde `APPROVED`.
5. O saque segue sem exigir codigo SMS no painel.
6. O webhook comum de transferencias continua informando `TRANSFER_DONE` ou `TRANSFER_FAILED`.

## Links oficiais

- Additional Security Mechanisms: https://docs.asaas.com/docs/additional-security-mechanisms
- Mecanismo para validacao de saque via webhooks: https://docs.asaas.com/docs/mecanismo-para-validacao-de-saque-via-webhooks
- Whitelist de IPs: https://docs.asaas.com/docs/whitelist-de-ips
- Duvias frequentes sobre transferencias: https://docs.asaas.com/docs/duvidas-frequentes-transferencias
