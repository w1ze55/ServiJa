# Testes de Carga Locais

Esta suite executa carga HTTP real contra a API local em `http://localhost:8080`, sem `k6`, `artillery` ou instalacao extra de dependencias.

## Arquivos

- `tests/load/run-load.mjs`: runner da suite.
- `tests/load/config.local.json`: configuracao padrao local.
- `tests/load/reports/`: saida gerada a cada execucao.

## Credenciais

O runner usa esta ordem para achar o login admin:

1. `LOAD_ADMIN_EMAIL` e `LOAD_ADMIN_PASSWORD`
2. `SERVIJA_ADMIN_BOOTSTRAP_EMAIL` e `SERVIJA_ADMIN_BOOTSTRAP_PASSWORD` do ambiente atual
3. fallback para os mesmos valores existentes no `.env` da raiz

Se nenhum desses valores existir, o preflight falha antes da carga.

## Como rodar

Executar a sequencia completa de todos os perfis configurados:

```powershell
node tests/load/run-load.mjs
```

Executar apenas um perfil:

```powershell
node tests/load/run-load.mjs --profile 50
node tests/load/run-load.mjs --profile 100
node tests/load/run-load.mjs --profile 1000
node tests/load/run-load.mjs --profile 10000
node tests/load/run-load.mjs --profile 50000
```

Executar dois ou mais perfis especificos:

```powershell
node tests/load/run-load.mjs --profile 10000,50000
```

Validar somente o preflight, sem disparar carga longa:

```powershell
node tests/load/run-load.mjs --profile all --preflight-only
```

## O que a suite mede

### PublicBrowse

- `GET /api/categories/public/tree`
- `GET /api/services/public/all`
- `GET /api/services/public/{serviceId}`
- `GET /api/freelancer/{freelancerId}/profile`
- `GET /api/services/public/freelancer/{freelancerId}`

### AdminRead

- `POST /api/auth/admin/login` no setup
- `GET /api/admin/dashboard`
- `GET /api/admin/users?page=0&size=20`
- `GET /api/admin/users/{id}`

### Mix

- `80%` das iteracoes usam `PublicBrowse`
- `20%` das iteracoes usam `AdminRead`

## Perfis

- `50`: warm-up `10 VUs / 30s`, rampa `30s`, sustentacao `4m`, cooldown `30s`
- `100`: warm-up `10 VUs / 30s`, rampa `60s`, sustentacao `5m`, cooldown `60s`
- `1000`: warm-up `20 VUs / 60s`, rampa `6m`, sustentacao `4m`, cooldown `90s`
- `10000`: warm-up `200 VUs / 2m`, rampa `15m`, sustentacao `2m`, cooldown `3m`
- `50000`: warm-up `500 VUs / 3m`, rampa `30m`, sustentacao `1m`, cooldown `5m`

O perfil `1000` aborta se a taxa de erro ficar acima de `25%` por `60s` continuos.
O perfil `10000` aborta acima de `40%` por `60s`.
O perfil `50000` aborta acima de `50%` por `60s`.

## Preflight

Antes de cada perfil a suite valida:

- `GET /api/categories/public/tree` retorna `200`
- `GET /api/services/public/all` retorna ao menos um servico
- o login admin funciona
- `GET /api/admin/users?page=0&size=1` retorna ao menos um usuario

Tambem valida os fixtures escolhidos no preflight:

- detalhe do primeiro servico visivel
- perfil publico do freelancer desse servico
- listagem publica de servicos do mesmo freelancer

## Relatorios

Cada execucao cria uma pasta em `tests/load/reports/<timestamp>-<label>/`.

Arquivos principais:

- `summary.json`: resultado consolidado
- `summary.md`: visao humana da execucao
- `profile-50.json`, `profile-100.json`, `profile-1000.json`: detalhe por perfil
- `preflight.json`: apenas quando usado `--preflight-only`

As metricas incluem:

- `req/s`
- `p50`, `p95`, `p99`
- media e maximo
- erros e taxa de erro
- distribuicao de status
- visao geral e por endpoint
- top 3 endpoints mais lentos
- comparacao entre `50`, `100` e `1000`

## Docker stats

Se `docker stats --no-stream` estiver disponivel no ambiente, a suite anexa snapshots antes e depois de cada perfil para:

- `servija_backend`
- `servija_mysql`
- `servija_mongo`

Se o Docker nao estiver acessivel, o relatorio marca isso como indisponivel e segue a execucao.

## Limitacoes conhecidas

- O escopo e API HTTP. Nao e browser E2E.
- O fluxo e read-only. O login admin entra apenas como setup e para leitura.
- A suite usa o admin bootstrap como credencial deterministica local.
- Endpoints fora do escopo atual, como `nearby` e `reviews`, continuam fora da carga principal por nao serem uma base estavel para SLA no ambiente local atual.
