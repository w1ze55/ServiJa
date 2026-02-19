# 🚀 CI/CD - GitHub Actions

## 📋 Workflows Configurados

### ✅ Deploy Produção (`deploy-prod.yml`)
- **Trigger**: Push na branch `main`
- **Ambiente**: Produção (https://servija.app)
- **Docker Compose**: `docker-compose.prod.yml`
- **Portas**: 80 (frontend), 3000 (backend)

### ✅ Deploy Homologação (`deploy-homolog.yml`)
- **Trigger**: Push na branch `homolog`
- **Ambiente**: Homologação (https://homolog.servija.app)
- **Docker Compose**: `docker-compose.homolog.yml`
- **Portas**: 81 (frontend), 3001 (backend)

## 🔑 Secrets Necessários

Configure no GitHub: **Settings → Secrets and variables → Actions**

| Secret | Descrição | Valor Exemplo |
|--------|-----------|---------------|
| `EC2_SSH_KEY` | Chave privada SSH para acessar EC2 | Conteúdo completo do arquivo `.pem` |
| `EC2_HOST` | IP público da EC2 | `18.223.20.196` |
| `EC2_USER` | Usuário SSH | `ec2-user` (Amazon Linux) ou `ubuntu` |
| `PROJECT_PATH` | Caminho do projeto na EC2 | `/var/www/ServiJa/app` |

### Como adicionar os secrets:

1. Vá em: https://github.com/SEU_USUARIO/SEU_REPO/settings/secrets/actions
2. Clique em **"New repository secret"**
3. Adicione cada secret da tabela acima

### Como obter `EC2_SSH_KEY`:

```bash
# No seu computador local (onde tem a chave .pem)
cat ~/Downloads/sua-chave-ec2.pem
```

Copie **TODO** o conteúdo (incluindo as linhas BEGIN e END):
```
-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEA...
...
-----END RSA PRIVATE KEY-----
```

## 🌿 Estratégia de Branches

### Branch `main` (Produção)
```bash
# Apenas código testado e aprovado
git checkout main
git merge homolog  # Promover homolog para produção
git push origin main  # ← Trigger deploy PRODUÇÃO
```

### Branch `homolog` (Homologação)
```bash
# Para testar features antes de ir para produção
git checkout homolog
git merge feature/nova-funcionalidade
git push origin homolog  # ← Trigger deploy HOMOLOGAÇÃO
```

## 🔄 Fluxo de Trabalho Completo

```bash
# 1. Criar feature branch
git checkout -b feature/minha-feature
git add .
git commit -m "feat: minha nova feature"
git push origin feature/minha-feature

# 2. Abrir Pull Request para 'homolog'
# (no GitHub)

# 3. Merge aprovado → Deploy automático em HOMOLOG
# Testar em: https://homolog.servija.app

# 4. Se tudo OK, abrir PR de 'homolog' para 'main'
# (no GitHub)

# 5. Merge aprovado → Deploy automático em PRODUÇÃO
# Disponível em: https://servija.app
```

## 📊 O que cada workflow faz:

1. ✅ Checkout do código
2. ✅ Configura SSH
3. ✅ Conecta na EC2
4. ✅ Navega para o diretório do projeto
5. ✅ Atualiza código (git pull)
6. ✅ Para containers do ambiente específico
7. ✅ Build das imagens (--no-cache)
8. ✅ Sobe containers
9. ✅ Aguarda 10 segundos
10. ✅ Verifica status
11. ✅ Limpa imagens antigas
12. ✅ Health check (tenta acessar a URL)
13. ✅ Notifica sucesso/falha

## 🔒 Isolamento dos Ambientes

| Característica | Produção | Homologação |
|----------------|----------|-------------|
| Branch | `main` | `homolog` |
| Workflow | `deploy-prod.yml` | `deploy-homolog.yml` |
| Docker Compose | `docker-compose.prod.yml` | `docker-compose.homolog.yml` |
| Containers | `servija_*_prod` | `servija_*_homolog` |
| Rede | `prod_network` | `homolog_network` |
| Portas | 80, 3000 | 81, 3001 |
| URL | https://servija.app | https://homolog.servija.app |
| .env | `.env.prod` | `.env.homolog` |

**Deploy de um NÃO afeta o outro!** ✅

## 🧪 Testar Deploy Manualmente

### Via GitHub Actions:
1. Vá em **Actions**
2. Selecione o workflow (Production ou Homolog)
3. Clique em **Run workflow**
4. Selecione a branch
5. (Opcional) Marque `force_deploy`
6. Clique em **Run workflow**

### Via SSH direto na EC2:
```bash
# SSH na EC2
ssh -i sua-chave.pem ec2-user@18.223.20.196

# Deploy produção
cd /var/www/ServiJa/app
git pull origin main
docker-compose -f docker-compose.prod.yml down
docker-compose -f docker-compose.prod.yml up -d --build

# Deploy homolog
cd /var/www/ServiJa/app
git pull origin homolog
docker-compose -f docker-compose.homolog.yml down
docker-compose -f docker-compose.homolog.yml up -d --build
```

## 📝 Arquivos .env na EC2

**IMPORTANTE**: Os arquivos `.env` devem estar configurados na EC2!

```bash
# SSH na EC2
cd /var/www/ServiJa/app

# Verificar se existem
ls -la .env.*

# Devem existir:
# - .env.prod
# - .env.homolog
```

Se não existirem, crie-os manualmente na EC2 com as variáveis corretas.

## 🐛 Troubleshooting

### Deploy falhou no health check:
- Verifique os logs: `docker-compose -f docker-compose.prod.yml logs -f`
- Verifique se a porta está aberta no Security Group da AWS
- Verifique se a Cloudflare está com proxy ativado (nuvem laranja)

### Erro de SSH:
- Verifique se `EC2_SSH_KEY` está correto (com BEGIN e END)
- Verifique se o IP (`EC2_HOST`) está correto
- Verifique se o Security Group permite SSH (porta 22)

### Container não inicia:
- SSH na EC2 e veja os logs
- Verifique se o arquivo `.env` existe
- Verifique se as variáveis estão corretas

## ✅ Próximos Passos

1. ✅ Configure os secrets no GitHub
2. ✅ Certifique-se que `.env.prod` e `.env.homolog` existem na EC2
3. ✅ Faça um commit de teste na branch `homolog`
4. ✅ Veja o workflow executar no GitHub Actions
5. ✅ Teste o acesso: https://homolog.servija.app
6. ✅ Se estiver OK, promova para `main`
7. ✅ Deploy automático em produção!
