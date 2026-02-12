# Documentação Swagger/OpenAPI - ServiJa API

## 📚 Sobre

Este projeto utiliza **SpringDoc OpenAPI 3** para gerar documentação interativa da API através do Swagger UI.

## 🚀 Como Acessar

### Desenvolvimento Local

Após iniciar o backend, acesse:

- **Swagger UI (Interface Interativa)**: http://localhost:8080/swagger-ui.html
- **API Docs (JSON)**: http://localhost:8080/v3/api-docs
- **API Docs (YAML)**: http://localhost:8080/v3/api-docs.yaml

### Com Docker

Se estiver rodando via Docker Compose:

```bash
docker-compose up backend
```

Acesse: http://localhost:8080/swagger-ui.html

## 🔐 Autenticação

A maioria dos endpoints requer autenticação JWT. Para testar no Swagger UI:

1. **Faça Login ou Registre-se**:
   - Use o endpoint `POST /api/auth/login` ou `POST /api/auth/register`
   - Copie o `token` retornado no response

2. **Autorize no Swagger**:
   - Clique no botão **"Authorize"** (cadeado verde) no topo da página
   - Cole o token no campo (não é necessário adicionar "Bearer", apenas o token)
   - Clique em **"Authorize"**
   - Feche o modal

3. **Teste os Endpoints Protegidos**:
   - Agora você pode testar qualquer endpoint que requer autenticação
   - O token será enviado automaticamente no header `Authorization: Bearer {token}`

## 📋 Grupos de Endpoints

### 🔑 Autenticação (`/api/auth`)
- Registro de usuários
- Login
- Verificação de e-mail
- Recuperação de senha
- Testes de e-mail

**Endpoints públicos** - não requerem autenticação.

### 👤 Usuários (`/api/users`)
- Obter perfil do usuário atual
- Atualizar dados do usuário
- Alterar senha
- Atualizar avatar
- Excluir conta

**Requer autenticação.**

### 💼 Freelancers (`/api/freelancer`)
- Gerenciar perfil de freelancer
- Buscar freelancers próximos (público)
- Atualizar localização

**Público**: `/api/freelancer/nearby`  
**Protegido**: outros endpoints

### 🛠️ Serviços (`/api/services`)
- Criar, editar e excluir serviços
- Listar serviços do freelancer
- Obter estatísticas
- Ativar/desativar serviços
- Buscar serviços públicos (por categoria ou texto)

**Público**: `/api/services/public/**`  
**Protegido**: outros endpoints

### 📍 Endereços (`/api/addresses`)
- Listar endereços do usuário
- Criar, editar e excluir endereços
- Definir endereço padrão

**Requer autenticação.**

## 🎯 Funcionalidades do Swagger UI

### Try it out
Clique em **"Try it out"** em qualquer endpoint para:
- Preencher os parâmetros necessários
- Editar o body da requisição
- Executar a requisição diretamente
- Ver a resposta em tempo real

### Schemas
Role até o final da página para ver todos os **schemas (modelos de dados)** utilizados pela API:
- `RegisterRequest`
- `LoginRequest`
- `AuthResponse`
- `ServiceRequest`
- `FreelancerProfileRequest`
- E muitos outros...

### Filtros
Use a barra de busca no topo para filtrar endpoints por nome ou tag.

### Ordenação
- **Tags** são ordenadas alfabeticamente
- **Operações** são ordenadas por método HTTP (GET, POST, PUT, DELETE, PATCH)

## ⚙️ Configuração

As configurações do Swagger estão em:

### `application.properties`
```properties
# SpringDoc OpenAPI (Swagger) Configuration
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.operationsSorter=method
springdoc.swagger-ui.tagsSorter=alpha
springdoc.swagger-ui.tryItOutEnabled=true
springdoc.swagger-ui.filter=true
springdoc.swagger-ui.syntaxHighlight.activated=true
```

### `OpenApiConfig.java`
Define as informações gerais da API:
- Título e descrição
- Versão
- Informações de contato
- Licença
- Servidores disponíveis
- Esquema de segurança (JWT)

### `SecurityConfig.java`
Permite acesso público aos endpoints do Swagger:
```java
.requestMatchers(
    "/swagger-ui/**",
    "/v3/api-docs/**",
    "/swagger-ui.html"
).permitAll()
```

## 📝 Exemplo de Uso

### 1. Registrar um novo usuário

```bash
POST /api/auth/register
```

Body:
```json
{
  "name": "João Silva",
  "email": "joao@example.com",
  "password": "senha123",
  "confirmPassword": "senha123",
  "role": "FREELANCER",
  "phoneNumber": "11999999999"
}
```

### 2. Fazer login

```bash
POST /api/auth/login
```

Body:
```json
{
  "email": "joao@example.com",
  "password": "senha123"
}
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "name": "João Silva",
    "email": "joao@example.com",
    "role": "FREELANCER"
  }
}
```

### 3. Usar o token para acessar endpoints protegidos

Autorize no Swagger UI com o token recebido, depois teste:

```bash
GET /api/users/me
```

## 🔧 Personalização

Para personalizar a documentação dos seus endpoints, use as annotations:

```java
@Tag(name = "Nome do Grupo", description = "Descrição do grupo")
@Operation(summary = "Resumo", description = "Descrição detalhada")
@Parameter(description = "Descrição do parâmetro")
@ApiResponse(responseCode = "200", description = "Sucesso")
@SecurityRequirement(name = "bearerAuth")
```

## 📚 Documentação Oficial

- [SpringDoc OpenAPI](https://springdoc.org/)
- [Swagger UI](https://swagger.io/tools/swagger-ui/)
- [OpenAPI Specification](https://swagger.io/specification/)

## 🐛 Troubleshooting

### Swagger UI não carrega
- Verifique se o backend está rodando
- Confirme que a porta 8080 está acessível
- Limpe o cache do navegador

### Token não funciona
- Certifique-se de copiar apenas o token, sem aspas ou espaços
- Verifique se o token não expirou (validade: 24 horas)
- Faça logout e login novamente

### Endpoints protegidos retornam 401
- Clique em "Authorize" e insira o token válido
- Verifique se o token foi copiado corretamente
- Certifique-se de que o usuário está verificado (se aplicável)

## 💡 Dicas

1. **Use o Swagger para testar rapidamente** novos endpoints durante o desenvolvimento
2. **Compartilhe a URL do Swagger** com a equipe de frontend para facilitar a integração
3. **Mantenha as annotations atualizadas** quando modificar os endpoints
4. **Use exemplos** nas annotations para melhorar a documentação
5. **Documente erros comuns** com `@ApiResponse` para cada código de status

---

**Desenvolvido com ❤️ para o projeto ServiJa**
