# 🚨 SEGURANÇA - CHAVE DE API VAZADA

## ⚠️ SITUAÇÃO CRÍTICA

Uma chave de API de **PRODUÇÃO** do Asaas foi exposta publicamente.

---

## 🔒 **AÇÕES OBRIGATÓRIAS (URGENTE)**

### 1. Revogar Chave Comprometida

A chave que começa com `$aact_prod_000MzkwODA2...` foi exposta e **DEVE** ser revogada imediatamente.

**Como revogar:**

1. Acesse: [https://www.asaas.com](https://www.asaas.com)
2. Faça login na sua conta
3. Vá em **Configurações** → **Integrações** → **Chaves de API**
4. Localize a chave comprometida
5. Clique em **"Revogar"** ou **"Deletar"**

### 2. Gerar Nova Chave

1. No mesmo painel, clique em **"Gerar Nova Chave"**
2. Escolha um nome descritivo: "ServiJá - Produção"
3. **Copie** a nova chave
4. **Salve** em local seguro (gerenciador de senhas)
5. Configure no arquivo `.env` (que está no `.gitignore`)

### 3. Atualizar `.env`

```bash
ASAAS_API_KEY=nova_chave_aqui
```

### 4. Gerar Token de Webhook

```bash
# Linux/Mac
openssl rand -base64 32

# Windows PowerShell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

Adicione no `.env`:
```bash
ASAAS_WEBHOOK_TOKEN=token_gerado_aqui
```

---

## ⚡ **POR QUE ISSO É PERIGOSO?**

Com sua chave de API, alguém mal-intencionado pode:

- ❌ Criar cobranças falsas em nome da sua empresa
- ❌ Fazer transferências de dinheiro
- ❌ Acessar dados de clientes
- ❌ Cancelar pagamentos legítimos
- ❌ Modificar configurações da conta
- ❌ Visualizar informações financeiras sensíveis

---

## ✅ **BOAS PRÁTICAS DE SEGURANÇA**

### 1. Variáveis de Ambiente

✅ **SEMPRE** use arquivos `.env` para chaves secretas
✅ **NUNCA** commite `.env` no Git (deve estar no `.gitignore`)
✅ **NUNCA** compartilhe chaves em chats, emails ou fóruns

### 2. Diferentes Chaves por Ambiente

```bash
# Desenvolvimento/Testes
ASAAS_API_URL=https://api-sandbox.asaas.com
ASAAS_API_KEY=chave_de_sandbox

# Produção
ASAAS_API_URL=https://api.asaas.com
ASAAS_API_KEY=chave_de_producao_diferente
```

### 3. Rotação de Chaves

- 🔄 Troque as chaves periodicamente (a cada 3-6 meses)
- 🔄 Revogue chaves antigas após trocar
- 🔄 Mantenha registro de quando foram geradas

### 4. Monitoramento

- 📊 Monitore logs de acesso à API
- 📊 Configure alertas de atividades suspeitas
- 📊 Revise transações regularmente

---

## 🔍 **VERIFICAR SE HOUVE USO INDEVIDO**

1. Acesse o painel do Asaas
2. Vá em **Relatórios** → **Histórico de Transações**
3. Verifique se há:
   - Cobranças não reconhecidas
   - Transferências suspeitas
   - Acessos em horários estranhos

Se encontrar algo suspeito:
- 🚨 Entre em contato com o suporte do Asaas imediatamente
- 🚨 Relate o incidente
- 🚨 Solicite auditoria da conta

---

## 📋 **CHECKLIST DE SEGURANÇA**

- [ ] Chave antiga revogada no painel do Asaas
- [ ] Nova chave gerada
- [ ] Nova chave salva no `.env`
- [ ] Token de webhook gerado
- [ ] Arquivo `.env` no `.gitignore`
- [ ] Histórico de transações verificado
- [ ] Senha do Asaas alterada (recomendado)
- [ ] 2FA ativado no Asaas (recomendado)

---

## 🛡️ **ARQUIVO `.gitignore` ATUALIZADO**

Certifique-se que seu `.gitignore` contém:

```gitignore
# Environment Variables
.env
.env.local
.env.production
.env.development
*.env

# Secrets
*secret*
*password*
*credentials*
*key*
```

---

## 📞 **SUPORTE ASAAS**

Se precisar de ajuda:

- 📧 Email: suporte@asaas.com
- 💬 Chat: No painel do Asaas
- 📞 Telefone: (48) 3033-3290

---

## ⏰ **LINHA DO TEMPO**

```
AGORA     → Revogar chave comprometida
↓
+5 min    → Gerar nova chave
↓
+10 min   → Atualizar .env
↓
+15 min   → Reiniciar aplicação
↓
+30 min   → Verificar se tudo funciona
↓
+1 hora   → Monitorar logs
↓
+24 horas → Verificar transações
```

---

## 🎓 **APRENDIZADO**

**Lembre-se:**

> "Chaves de API são como senhas bancárias. Nunca compartilhe, nunca exponha, sempre proteja."

---

**STATUS**: 🔴 AÇÃO URGENTE NECESSÁRIA

Revogue a chave comprometida AGORA antes de continuar qualquer desenvolvimento.
