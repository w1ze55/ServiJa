# Módulo Pesquisa do serviço

Documentação técnica do fluxo de **exploração e busca de serviços públicos** no ServiJá: filtro por texto (`q`), categoria e subcategoria, listagem ativa e ordenação no cliente. Baseada no código do repositório (Spring Boot + Vue 3).

---

## Banco de dados

### Tabelas envolvidas

| Tabela | Papel na pesquisa |
|--------|-------------------|
| `services` | Registro de cada anúncio de serviço; campos usados na busca textual e filtros. |
| `service_categories` | Catálogo de categorias (slug, nome); filtro por `category_slug` via `category_id` em `services`. |
| `service_subcategories` | Subcategorias ligadas a uma categoria; filtro por `subcategory_slug` via `subcategory_id`. |
| `freelancers` | Dono do serviço (`freelancer_id`); join implícito para carregar nome e avatar na resposta. |

Tabelas auxiliares **não** participam do critério SQL da busca por texto, mas enriquecem o resultado público: agregações de avaliações (`reviews`) e de solicitações concluídas (`service_requests`) são consultadas no backend ao montar `ServiceResponse` para listagens públicas.

### Relacionamentos (modelo JPA / integridade)

- `services.freelancer_id` → entidade `Freelancer` (obrigatório).
- `services.category_id` → `ServiceCategory` (opcional, catálogo normalizado).
- `services.subcategory_id` → `ServiceSubcategory` (opcional).
- Campo legado `services.category` (string, até 100 caracteres) permanece na entidade e é usado em fallbacks de exibição e em alguns repositórios legados.

### Campos principais para busca e filtro

- **`is_active`**: apenas serviços ativos entram em listagens e na query de busca textual.
- **`title`**, **`description`**: alvo da busca por palavra-chave (JPQL com `LIKE` case-insensitive).
- **`category_id` / `subcategory_id`**: filtros por slug de categoria/subcategoria nas listagens combinadas.
- **`created_at`**: ordenação padrão nas queries derivadas do repositório que usam `OrderByCreatedAtDesc` (listagem geral, categoria e subcategoria). A busca textual por `searchServices` não define `ORDER BY` no JPQL; por isso, na tela, a ordenação final é reforçada no cliente por `filteredServices`.
- Índices históricos em migrações incluem `idx_services_category`, `idx_services_is_active`, `idx_services_created_at`, e após o catálogo, `idx_services_category_id` e `idx_services_subcategory_id`.

---

## Backend

### Controllers (rotas principais)

Classe: `ServiceController`, prefixo `/api/services`.

| Rota | Uso na pesquisa |
|------|-----------------|
| `GET /public/all` | **Principal para a tela Explorar**: aceita `categorySlug`, `subcategorySlug` e `q` (query string) opcionais; delega a `ServiceService.getActiveServices`. |
| `GET /public/search?q=...` | Busca **somente** por texto via `searchPublicServices`; endpoint dedicado (ex.: integrações, Swagger). |
| `GET /public/category/{category}` | Lista por categoria. No service, tenta primeiro categoria por slug, depois subcategoria por slug e, por fim, categoria textual legada. Não combina com parâmetro `q` neste método. |
| `GET /public/{id}` | Detalhe de um serviço ativo (fora do escopo da “lista”, mas fluxo típico após clicar num resultado). |

Autenticação: endpoints `/public/*` são públicos; não exigem Bearer para listar/buscar.

### Services (regras de negócio)

Classe: `ServiceService`.

- **`getActiveServices(String categorySlug, String subcategorySlug, String query)`**  
  Monta a lista em quatro ramos, nesta ordem de precedência:
  1. Se há `subcategorySlug` → repositório `findByServiceSubcategory_SlugAndIsActiveOrderByCreatedAtDesc`.
  2. Senão, se há `categorySlug` → `findByServiceCategory_SlugAndIsActiveOrderByCreatedAtDesc`.
  3. Senão, se há `query` → `searchServices(query)` (texto em título/descrição).
  4. Senão → todos ativos: `findByIsActiveOrderByCreatedAtDesc(true)`.

  Se **ao mesmo tempo** existir `query` **e** (`categorySlug` ou `subcategorySlug`), após a consulta JPA a lista é **refinada em memória** com `contains` em minúsculas sobre `title` e `description` (interseção categoria + texto).

- **`searchPublicServices(String query)`**  
  Apenas `serviceRepository.searchServices(query)` + conversão para DTO público.

- **`toPublicResponse` / `toPublicResponses`**  
  Converte entidade `Service` em `ServiceResponse` e adiciona dados extras para o catálogo público: resumo de avaliações do serviço, contagem de solicitações concluídas e faixa de preço médio histórico (quando aplicável).

### DTOs (entrada / saída)

- **Saída**: `ServiceResponse` — identificação, freelancer, título, descrição, slugs/labels de categoria e subcategoria, preço, tipo de preço, prazo de entrega, modelo de trabalho, tags, imagens, flags de ativo, timestamps, foto do freelancer, métricas de avaliação e de preços médios em serviços concluídos.
- **Entrada na pesquisa**: não há body; apenas **query params** em `GET /public/all` (`categorySlug`, `subcategorySlug`, `q`).

### Repositório

Interface: `ServiceRepository`.

- Método **`searchServices(@Param("query") String query)`**: JPQL filtrando `isActive = true` e correspondência em `title` ou `description` com `LIKE '%query%'` em ambos os lados, case-insensitive.

### Fluxo da requisição (resumo)

1. Cliente chama `GET /api/services/public/all` com parâmetros opcionais.
2. `ServiceController` repassa para `getActiveServices`.
3. `ServiceRepository` executa a consulta adequada (categoria, subcategoria, texto ou lista geral).
4. Se categoria/subcategoria + `q`, aplica filtro textual adicional em Java.
5. Cada `Service` vira `ServiceResponse` enriquecido (`toPublicResponse`).
6. JSON com array de `ServiceResponse` retorna ao frontend.

---

## Frontend

### Páginas envolvidas

| Arquivo | Função |
|---------|--------|
| `frontend/src/pages/Explore/index.vue` | Rota `/explore`: usa `MainLayout`, embute **`ExplorePage`** e trata navegação para perfil e detalhe de serviço. É a rota protegida usada para clientes autenticados. |
| `frontend/src/pages/Search/index.vue` | Rota `/search`: também usa `MainLayout` e embute **`ExplorePage`**. É uma rota pública no router, mas possui diferença de navegação para detalhe de serviço em relação a `/explore`. |
| `frontend/src/router/index.ts` | Define `path: "/explore"`, `name: "explore"` e `path: "/search"`, `name: "search"`. |

### Componentes principais

- **`ExplorePage.vue`** (`frontend/src/components/pages/ExplorePage.vue`): concentra campo de busca, selects de categoria/subcategoria, alternância “Serviços” / “Profissionais”, ordenação, grid ou lista de cards e estados de carregamento/erro.
- **`ServiceCard.vue`**: exibe cada serviço retornado (modo serviços).
- **`FreelancerCard.vue`**: modo “Profissionais” — agrupa visualmente os serviços retornados por freelancer. Não existe um endpoint de busca textual próprio para profissionais nesse fluxo; porém a tela enriquece os cards buscando perfis com `GET /freelancer/{id}/profile` para obter dados como localização, avaliação e disponibilidade.
- Componentes de UI: `Card`, `Select*`, `Tabs*`, `Button`, `Badge`, ícones `lucide-vue-next`.

### Requisições (axios)

- Instância: `import api from '@/api/config/axios'` — `baseURL` já inclui `/api` (prefixo configurado a partir de `VITE_API_URL`).
- Chamada principal: **`api.get('/services/public/all', { params })`** com:
  - `categorySlug`: omitido ou slug quando categoria ≠ `all`;
  - `subcategorySlug`: omitido ou slug quando subcategoria ≠ `all`;
  - `q`: omitido se o campo de busca estiver vazio; caso contrário, texto digitado.

**Observação:** a tela **não** chama `/services/public/search` no código atual; o termo `q` é enviado junto ao endpoint unificado `/public/all`.

### Fluxo do usuário

1. Usuário acessa **Explorar** pela rota `/explore` (cliente autenticado) ou `/search` (rota pública definida no router).
2. No mount, `categoryCatalogStore.loadPublicCategories()` dispara o carregamento das categorias públicas para os filtros e `loadServices()` busca a lista inicial de serviços.
3. Digitação no campo de busca dispara **`handleSearchInput`**: debounce de **500 ms** e nova chamada a `loadServices()` (busca reativa sem precisar enviar o formulário).
4. **Enter** no input chama `handleSearch()` → `loadServices()` imediato.
5. Alteração de categoria/subcategoria dispara `watch` em `selectedCategory` / `selectedSubcategory` que chama `loadServices()` quando não há texto de busca (comportamento combinado com filtros).
6. A lista retornada é ordenada no **cliente** via `filteredServices` (`sortBy`: mais recentes, preço asc/desc, nome A-Z) usando utilitários como `getServicePriceSortValue`.
7. Ações secundárias: favoritos (`useFavoritesStore`), navegação para perfil do prestador e navegação para detalhe do serviço via `onNavigate` / `vue-router`. No wrapper de `/explore`, a navegação para `service-detail` é tratada explicitamente por rota nomeada. No wrapper de `/search`, o código atual trata `provider-profile`, mas não possui o mesmo caso específico para `service-detail`.

### Lacuna de produto (texto da interface vs. backend)

O placeholder do campo de busca sugere “serviço, profissional ou palavra-chave”, porém o backend, neste fluxo, filtra **título e descrição do serviço** (e combina com categoria/subcategoria conforme regras acima). **Busca explícita pelo nome do freelancer** não está no mesmo critério JPQL de `searchServices`; resultados por “nome do profissional” só apareceriam indiretamente se estiverem no título/descrição do anúncio.

### Observação de implementação

O componente `ExplorePage.vue` emite navegação para `service-detail` ao clicar em um serviço. A rota `/explore` trata esse caso de forma explícita em `frontend/src/pages/Explore/index.vue`. A rota `/search`, em `frontend/src/pages/Search/index.vue`, trata `provider-profile`, mas não possui o mesmo tratamento específico para `service-detail`. Caso `/search` seja mantida como ponto de entrada público para resultados clicáveis, recomenda-se alinhar o `handleNavigate` de `/search` ao comportamento já existente em `/explore`.

---

## Referências de código (rastreabilidade)

- Backend: `ServiceController.java`, `ServiceService.java`, `ServiceRepository.java`, entidade `Service.java`, DTO `ServiceResponse.java`.
- Frontend: `pages/Explore/index.vue`, `pages/Search/index.vue`, `components/pages/ExplorePage.vue`, `api/config/axios.ts`, `api/services/categoryService.ts`.
- Migrações ilustrativas de schema: `V8__create_services_table.sql`, `V48__create_service_catalog_and_links.sql`.
