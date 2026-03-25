# Documentação Swagger/OpenAPI - ServiJa API

Esta documentação descreve como acessar e utilizar o Swagger/OpenAPI para interagir com a API do projeto ServiJa.

## 📚 Sobre

O projeto utiliza o **SpringDoc OpenAPI 3** para gerar automaticamente a documentação técnica da API. Isso permite que desenvolvedores frontend e backend testem os endpoints de forma interativa, sem a necessidade de ferramentas externas como Postman ou Insomnia.

## 🚀 Como Acessar

### Ambientes Disponíveis

*   **Desenvolvimento Local**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
*   **Servidor de Homologação**: [https://homolog.servija.app/swagger-ui/index.html](https://homolog.servija.app/swagger-ui/index.html)
*   **API Docs (Formato JSON)**: `/v3/api-docs`
*   **API Docs (Formato YAML)**: `/v3/api-docs.yaml`

## 🔐 Autenticação JWT

A maioria dos endpoints da API é protegida e requer um token JWT (Bearer Token).

### Passo a passo para testar no Swagger:

1.  **Obter o Token**:
    *   Acesse o grupo **Autenticação** (`/api/auth`).
    *   Use o endpoint `POST /api/auth/login` com suas credenciais.
    *   Copie o valor do campo `token` retornado na resposta.

2.  **Autorizar**:
    *   No topo da página do Swagger UI, clique no botão **"Authorize"** (ícone de cadeado).
    *   Cole o token no campo de valor (obs: não é necessário escrever "Bearer", apenas o código).
    *   Clique em **Authorize** e depois em **Close**.

3.  **Executar Requisições**:
    *   Agora, ao clicar em **"Try it out"** e **"Execute"** em qualquer endpoint protegido, o Swagger enviará o token automaticamente no cabeçalho `Authorization`.

## 📋 Grupos de Endpoints (Tags)

Os endpoints estão organizados nos seguintes grupos:

*   **Autenticação (`/api/auth`)**: Registro, login, verificação de e-mail e recuperação de senha.
*   **Usuários (`/api/users`)**: Gerenciamento de perfil, alteração de senha e exclusão de conta.
*   **Freelancers (`/api/freelancer`)**: Perfil profissional, busca por geolocalização e disponibilidade.
*   **Serviços (`/api/services`)**: Catálogo de serviços, criação, edição, ativação/desativação e busca pública.
*   **Endereços (`/api/addresses`)**: Gerenciamento de endereços dos usuários para prestação de serviço.
*   **Pagamentos (`/api/payments`)**: Integração com Asaas, geração de faturas e histórico.
*   **Portfólio (`/api/portfolio`)**: Galeria de projetos e trabalhos realizados pelos freelancers.

## ⚙️ Configurações Técnicas

A configuração do Swagger no backend está centralizada no arquivo:
`backend/src/main/java/com/servija/servija/config/OpenApiConfig.java`

*   **OpenAPI Specification**: 3.0
*   **Segurança**: HTTP Bearer (JWT)
*   **Ordenação**: Os métodos são ordenados por tipo (GET, POST, etc) e as tags alfabeticamente.

## 💡 Dicas de Uso

*   **Schemas**: No final da página do Swagger, você encontrará a seção "Schemas", que detalha a estrutura de todos os objetos enviados e recebidos pela API.
*   **Try it out**: Sempre use esta funcionalidade para validar se o corpo (body) da sua requisição está no formato correto antes de implementar no código frontend.
*   **Filtros**: Use a barra de busca no topo para encontrar rapidamente um endpoint específico.

---
*Para mais detalhes sobre a implementação técnica, consulte `backend/SWAGGER_IMPLEMENTATION.md`.*
