# Módulo Autenticação

Este documento descreve a implementação do módulo de autenticação do sistema Servi Já. O módulo cobre cadastro, login, emissão e validação de JWT, verificação de e-mail, recuperação de senha, alteração de senha e controle de sessão no frontend.

A implementação usa Spring Boot, Spring Security, JWT, BCrypt, MySQL, Flyway, Vue 3, Vue Router, Pinia e Axios.

## Banco de Dados

### Visão Geral

A autenticação é baseada na tabela `users`, que armazena a identidade comum de todos os usuários: dados pessoais, credenciais, tipo de usuário, status da conta, códigos de verificação e códigos de recuperação de senha.

No backend, a entidade `User` usa herança JPA com estratégia `JOINED`. Assim, os dados comuns ficam em `users` e os dados específicos ficam em tabelas filhas:

| Tabela | Responsabilidade |
|---|---|
| `users` | Identidade, credenciais, tipo de usuário, status, verificação de e-mail e recuperação de senha. |
| `contractors` | Dados específicos do contratante. |
| `freelancers` | Dados específicos do prestador/freelancer. |

Migrations principais:

| Migration | Finalidade |
|---|---|
| `V1__create_users_table.sql` | Cria a tabela base `users`. |
| `V2__create_contractors_table.sql` | Cria a tabela `contractors`. |
| `V3__create_freelancers_table.sql` | Cria a tabela `freelancers`. |
| `V13__add_verification_attempts_columns.sql` | Adiciona tentativas e bloqueio para verificação de e-mail. |
| `V51__allow_admin_user_type.sql` | Permite `ADMIN` em `users.user_type`. |
| `V53__add_reset_password_attempt_tracking.sql` | Adiciona tentativas e bloqueio para recuperação de senha. |

### Tabelas Envolvidas

#### `users`

A tabela `users` é mapeada por `backend/src/main/java/com/servija/servija/model/User.java`. Ela também implementa `UserDetails`, permitindo que o próprio usuário seja usado pelo Spring Security.

Campos principais:

| Campo | Descrição |
|---|---|
| `id` | Chave primária autoincremental. |
| `name` | Nome completo do usuário. |
| `email` | E-mail de login, único e obrigatório. |
| `cpf` | CPF do usuário, único e obrigatório. |
| `phone` | Telefone do usuário. |
| `password` | Hash BCrypt da senha. A senha em texto puro não é armazenada. |
| `user_type` | Tipo do usuário: `CONTRACTOR`, `FREELANCER` ou `ADMIN`. |
| `email_verified` | Indica se o e-mail foi confirmado. |
| `verification_code` | Código de 6 dígitos enviado para verificar e-mail. |
| `verification_code_expiry` | Expiração do código de verificação. |
| `verification_attempts` | Tentativas incorretas de verificação de e-mail. |
| `verification_lock_until` | Bloqueio temporário por excesso de tentativas. |
| `reset_password_token` | Código de recuperação de senha. Apesar do nome, a implementação usa código numérico de 6 dígitos. |
| `reset_password_token_expiry` | Expiração do código de recuperação. |
| `reset_password_attempts` | Tentativas incorretas no fluxo de recuperação. |
| `reset_password_lock_until` | Bloqueio temporário no fluxo de recuperação. |
| `avatar_url` | URL ou base64 do avatar. |
| `active` | Define se a conta pode autenticar. |
| `created_at` | Data de criação. |
| `updated_at` | Data de atualização. |

Índices importantes: `idx_users_email`, `idx_users_cpf`, `idx_users_user_type` e `idx_users_active`.

#### `contractors`

A tabela `contractors` é mapeada por `Contractor.java` e complementa `users` para usuários contratantes.

| Campo | Descrição |
|---|---|
| `user_id` | Chave primária e chave estrangeira para `users.id`. |
| `preferred_location` | Localização preferida do contratante. |
| `total_contracts` | Total de contratos. |
| `active_contracts` | Contratos ativos. |
| `completed_contracts` | Contratos concluídos. |

Relacionamento:

```sql
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
```

#### `freelancers`

A tabela `freelancers` é mapeada por `Freelancer.java` e complementa `users` para prestadores de serviço.

| Campo | Descrição |
|---|---|
| `user_id` | Chave primária e chave estrangeira para `users.id`. |
| `bio` | Biografia profissional. |
| `professional_title` | Título profissional. |
| `location` | Localização textual. |
| `rating` | Nota média, com constraint de 0 a 5. |
| `total_reviews` | Total de avaliações. |
| `completed_jobs` | Trabalhos concluídos. |
| `active_jobs` | Trabalhos ativos. |
| `kyc_completed` | Indica se o KYC foi concluído. |
| `profile_completed` | Indica se o perfil profissional foi completado. |
| `available` | Indica disponibilidade. |
| `category`, `service_type` | Campos legados de classificação profissional. |
| `category_id`, `subcategory_id` | Relações com catálogo de categorias. |
| `cities`, `social_links` | Dados textuais/serializados do perfil. |
| `latitude`, `longitude` | Coordenadas para geolocalização. |

Relacionamento:

```sql
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
```

### Relacionamentos

| Relação | Cardinalidade | Implementação |
|---|---:|---|
| `users` para `contractors` | 1:0..1 | `contractors.user_id` é PK e FK para `users.id`. |
| `users` para `freelancers` | 1:0..1 | `freelancers.user_id` é PK e FK para `users.id`. |

Um usuário `CONTRACTOR` possui registro em `users` e `contractors`. Um usuário `FREELANCER` possui registro em `users` e `freelancers`. Um usuário `ADMIN` usa apenas `users`; o cadastro público de admin é bloqueado no serviço de autenticação.

A tabela `users` também é referenciada por outros módulos, como endereços, solicitações de serviço, pagamentos, favoritos, avaliações, notificações e carteira. Por isso, ela funciona como a raiz de identidade do sistema.

### Campos de Segurança

A senha é protegida por BCrypt, configurado em `SecurityConfig`:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

O tipo de usuário define as permissões no Spring Security. A entidade `User` converte `user_type` para role:

```java
new SimpleGrantedAuthority("ROLE_" + userType.name())
```

Mapeamento de tipos:

| Banco/backend | Role Spring Security | Frontend |
|---|---|---|
| `CONTRACTOR` | `ROLE_CONTRACTOR` | `contractor` |
| `FREELANCER` | `ROLE_FREELANCER` | `provider` |
| `ADMIN` | `ROLE_ADMIN` | `admin` |

A verificação de e-mail usa código de 6 dígitos, expiração de 10 minutos, máximo de 5 tentativas e bloqueio de 30 minutos. A recuperação de senha usa código de 6 dígitos, expiração de 30 minutos, máximo de 5 tentativas e bloqueio de 30 minutos.

## Backend

### Visão Geral da Arquitetura

O backend é dividido em controllers, services, repositories, models, DTOs e configuração de segurança.

| Camada | Responsabilidade | Arquivos principais |
|---|---|---|
| Controller | Expõe rotas REST e recebe DTOs. | `AuthController.java`, `UserController.java` |
| Service | Aplica regras de negócio. | `AuthService.java`, `UserService.java`, `JwtService.java`, `EmailService.java` |
| Repository | Acessa o banco via Spring Data JPA. | `UserRepository.java`, `ContractorRepository.java`, `FreelancerRepository.java` |
| Model | Define entidades JPA e integração com Spring Security. | `User.java`, `Contractor.java`, `Freelancer.java` |
| Security | Configura autenticação stateless e filtro JWT. | `SecurityConfig.java`, `JwtAuthenticationFilter.java` |
| DTO | Define contratos de entrada e saída. | Pacote `backend/src/main/java/com/servija/servija/dto` |

A autenticação é stateless. O servidor não mantém sessão HTTP. Após login ou verificação de e-mail, o backend emite um JWT. Requisições protegidas enviam:

```http
Authorization: Bearer <token>
```

### Controllers

#### `AuthController`

Arquivo: `backend/src/main/java/com/servija/servija/controller/AuthController.java`

Base path:

```http
/api/auth
```

Rotas principais:

| Método | Rota | Autenticação | Entrada | Saída | Finalidade |
|---|---|---|---|---|---|
| `POST` | `/api/auth/register` | Pública | `RegisterRequest` | `AuthResponse` | Cadastra usuário e envia código de verificação. |
| `GET` | `/api/auth/check-cpf?cpf=...` | Pública | Query `cpf` | `MessageResponse` | Verifica se CPF já existe. |
| `POST` | `/api/auth/login` | Pública | `LoginRequest` | `AuthResponse` | Autentica e retorna JWT. |
| `POST` | `/api/auth/verify-email` | Pública | `VerifyEmailRequest` | `AuthResponse` | Confirma e-mail e retorna JWT. |
| `POST` | `/api/auth/resend-verification?email=...` | Pública | Query `email` | `MessageResponse` | Reenvia código de e-mail. |
| `POST` | `/api/auth/forgot-password` | Pública | `ForgotPasswordRequest` | `MessageResponse` | Inicia recuperação de senha. |
| `POST` | `/api/auth/verify-reset-code` | Pública | `VerifyResetPasswordCodeRequest` | `MessageResponse` | Valida código de recuperação. |
| `POST` | `/api/auth/reset-password` | Pública | `ResetPasswordRequest` | `MessageResponse` | Redefine senha. |
| `POST` | `/api/auth/resend-reset-code?email=...` | Pública | Query `email` | `MessageResponse` | Reenvia código de recuperação. |
| `POST` | `/api/auth/test-email?email=...` | Pública | Query `email` | `MessageResponse` | Testa envio de e-mail. |

Como `SecurityConfig` libera `/api/auth/**`, todos os endpoints desse controller ficam públicos pela configuração atual.

#### `UserController`

Arquivo: `backend/src/main/java/com/servija/servija/controller/UserController.java`

Base path:

```http
/api/users
```

Essas rotas exigem JWT válido.

| Método | Rota | Entrada | Saída | Finalidade |
|---|---|---|---|---|
| `GET` | `/api/users/me` | Nenhuma | `AuthResponse.UserDto` | Retorna dados do usuário autenticado. |
| `GET` | `/api/users/me/profile` | Nenhuma | `ProfileResponse` | Retorna perfil completo. |
| `PUT` | `/api/users/me` | `UpdateUserRequest` | `AuthResponse.UserDto` | Atualiza dados básicos. |
| `PUT` | `/api/users/me/password` | `ChangePasswordRequest` | `MessageResponse` | Altera senha autenticada. |
| `PUT` | `/api/users/me/avatar/upload` | Multipart file | `AuthResponse.UserDto` | Faz upload de avatar. |
| `PUT` | `/api/users/me/avatar` | `UpdateAvatarRequest` | `AuthResponse.UserDto` | Atualiza avatar por URL ou base64. |
| `DELETE` | `/api/users/me` | `DeleteAccountRequest` | `MessageResponse` | Exclui conta após confirmação de senha. |

O controller obtém o usuário autenticado pelo contexto de segurança:

```java
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
String email = authentication.getName();
```

Esse e-mail vem do claim `sub` do JWT.

### Services

#### `AuthService`

Arquivo: `backend/src/main/java/com/servija/servija/service/AuthService.java`

É o principal serviço de regras de autenticação.

| Método | Responsabilidade |
|---|---|
| `register(...)` | Valida duplicidade, bloqueia cadastro público de admin, cria `Contractor` ou `Freelancer`, criptografa senha, gera código de verificação, salva usuário, envia e-mail e gera JWT. |
| `login(...)` | Autentica e-mail/senha via `AuthenticationManager`, valida conta ativa e gera JWT. |
| `verifyEmail(...)` | Valida código de e-mail, controla tentativas, aplica bloqueio, marca e-mail como verificado e gera JWT. |
| `resendVerificationCode(...)` | Gera novo código, reinicia tentativas e envia e-mail. |
| `forgotPassword(...)` | Gera código de recuperação e envia e-mail. Se o e-mail não existir, retorna sem erro. |
| `verifyResetPasswordCode(...)` | Valida código de recuperação e controla tentativas. |
| `resetPassword(...)` | Valida código, valida senha forte, criptografa e salva nova senha. |

Regras de negócio importantes:

| Regra | Implementação |
|---|---|
| Admin não pode se cadastrar publicamente | `request.getUserType() == User.UserType.ADMIN` gera erro. |
| E-mail único | `userRepository.existsByEmail(...)`. |
| CPF único | `userRepository.existsByCpf(...)`. |
| Senha segura | Mínimo 8, máximo 256, maiúscula, minúscula, número e caractere especial. |
| Código seguro | Gerado com `SecureRandom`. |
| Verificação de e-mail | Código de 6 dígitos, 10 minutos, 5 tentativas, bloqueio de 30 minutos. |
| Recuperação de senha | Código de 6 dígitos, 30 minutos, 5 tentativas, bloqueio de 30 minutos. |

#### `JwtService`

Arquivo: `backend/src/main/java/com/servija/servija/service/JwtService.java`

Responsável por gerar, assinar, extrair e validar JWTs.

| Método | Finalidade |
|---|---|
| `generateToken(UserDetails userDetails)` | Gera token com `subject` igual ao e-mail do usuário. |
| `extractUsername(String token)` | Extrai o e-mail do claim `sub`. |
| `extractClaim(...)` | Extrai claims específicas. |
| `isTokenValid(...)` | Verifica se o token pertence ao usuário e não expirou. |

Configurações em `application.properties`:

| Propriedade | Finalidade |
|---|---|
| `jwt.secret` | Chave secreta em Base64 usada na assinatura. |
| `jwt.expiration` | Expiração do token. Padrão: `86400000` ms, ou 24 horas. |

Claims principais:

| Claim | Valor |
|---|---|
| `sub` | E-mail do usuário. |
| `iat` | Emissão. |
| `exp` | Expiração. |

#### `CustomUserDetailsService`

Arquivo: `backend/src/main/java/com/servija/servija/service/CustomUserDetailsService.java`

Carrega o usuário por e-mail usando `UserRepository.findByEmail(email)`. Como `User` implementa `UserDetails`, o Spring Security consegue acessar senha, username e authorities diretamente na entidade.

#### `EmailService`

Arquivo: `backend/src/main/java/com/servija/servija/service/EmailService.java`

Envia e-mails com `JavaMailSender` de forma assíncrona por `@Async`.

| Método | Finalidade |
|---|---|
| `sendVerificationEmail(...)` | Envia código de verificação de e-mail. |
| `sendPasswordResetEmail(...)` | Envia código de recuperação de senha. |

Em caso de erro de SMTP, o serviço registra log e imprime o código no console. Isso ajuda em desenvolvimento, mas exige cuidado em produção.

#### `UserService`

Arquivo: `backend/src/main/java/com/servija/servija/service/UserService.java`

Compõe o módulo por executar operações sensíveis sobre a conta autenticada.

| Método | Responsabilidade |
|---|---|
| `getCurrentUser(...)` | Retorna DTO básico do usuário autenticado. |
| `getCurrentUserProfile(...)` | Retorna perfil completo, com dados de contractor ou freelancer. |
| `updateUser(...)` | Atualiza nome, telefone e CPF. |
| `changePassword(...)` | Confere senha atual, valida nova senha, criptografa e salva. |
| `updateAvatar(...)` | Atualiza avatar. |
| `deleteAccount(...)` | Confirma senha e remove a conta. |

#### `AdminBootstrapService`

Arquivo: `backend/src/main/java/com/servija/servija/service/AdminBootstrapService.java`

Cria usuário admin na inicialização quando `servija.admin.bootstrap.enabled=true`. O admin é salvo em `users` com `userType=ADMIN`, `emailVerified=true`, `active=true` e senha BCrypt. Antes de criar, o serviço verifica duplicidade de e-mail e CPF.

### Segurança HTTP

#### `SecurityConfig`

Arquivo: `backend/src/main/java/com/servija/servija/config/SecurityConfig.java`

Configurações principais:

| Configuração | Descrição |
|---|---|
| CSRF | Desabilitado, pois a API usa autenticação stateless com JWT. |
| CORS | Configurado por `cors.allowed-origins`. |
| Rotas públicas | `/api/auth/**`, Swagger, webhooks, arquivos públicos e alguns endpoints públicos de catálogo/perfil. |
| Rotas protegidas | Qualquer rota fora da lista pública exige autenticação. |
| Sessão | `SessionCreationPolicy.STATELESS`. |
| Provider | `DaoAuthenticationProvider` com `UserDetailsService` e `BCryptPasswordEncoder`. |
| Filtro JWT | `JwtAuthenticationFilter` antes de `UsernamePasswordAuthenticationFilter`. |

#### `JwtAuthenticationFilter`

Arquivo: `backend/src/main/java/com/servija/servija/config/JwtAuthenticationFilter.java`

Fluxo do filtro:

1. Lê o header `Authorization`.
2. Se não existir ou não começar com `Bearer `, segue sem autenticar.
3. Remove o prefixo `Bearer ` e extrai o token.
4. Extrai o e-mail do claim `sub`.
5. Carrega `UserDetails` pelo e-mail.
6. Valida assinatura, dono do token e expiração.
7. Cria `UsernamePasswordAuthenticationToken`.
8. Registra a autenticação no `SecurityContextHolder`.
9. Se o token expirou, retorna HTTP 401 com `expired=true`.
10. Se o token é inválido, retorna HTTP 401 com `expired=false`.

### DTOs

#### Entrada

| DTO | Campos | Validações principais |
|---|---|---|
| `RegisterRequest` | `name`, `email`, `cpf`, `phone`, `password`, `userType`, `acceptTerms` | Nome 3-255; e-mail válido; CPF 11 dígitos; telefone 10-11 dígitos; senha forte; aceite obrigatório. |
| `LoginRequest` | `email`, `password` | E-mail válido e senha obrigatória. |
| `VerifyEmailRequest` | `email`, `code` | E-mail válido e código com 6 dígitos. |
| `ForgotPasswordRequest` | `email` | E-mail válido. |
| `VerifyResetPasswordCodeRequest` | `email`, `code` | E-mail válido e código com 6 dígitos. |
| `ResetPasswordRequest` | `email`, `code`, `newPassword`, `confirmPassword` | E-mail válido; código com 6 dígitos; senha 8-256; confirmação obrigatória. |
| `ChangePasswordRequest` | `currentPassword`, `newPassword`, `confirmPassword` | Senha atual obrigatória; nova senha 8-256; confirmação obrigatória. |
| `UpdateUserRequest` | `name`, `phone`, `cpf` | Nome e telefone obrigatórios; CPF opcional. |
| `DeleteAccountRequest` | `password`, `reason` | Senha obrigatória. |

#### Saída

| DTO | Campos | Finalidade |
|---|---|---|
| `AuthResponse` | `token`, `tokenType`, `user` | Retorno de cadastro, login e verificação de e-mail. |
| `AuthResponse.UserDto` | `id`, `name`, `email`, `cpf`, `phone`, `userType`, `emailVerified`, `avatarUrl`, `active`, `createdAt` | Dados básicos do usuário. |
| `ProfileResponse` | Dados básicos e perfil específico | Perfil completo para telas de conta. |
| `MessageResponse` | `message` | Mensagens simples de sucesso. |

### Fluxo da Requisição

#### Cadastro

1. O frontend envia `POST /api/auth/register` com dados pessoais, senha, tipo de usuário e aceite dos termos.
2. `AuthController.register()` recebe `RegisterRequest` validado por Bean Validation.
3. `AuthService.register()` bloqueia `ADMIN` em cadastro público.
4. O service verifica duplicidade de e-mail e CPF.
5. O service cria `Contractor` ou `Freelancer` conforme `userType`.
6. A senha é criptografada com BCrypt.
7. O service gera código de verificação de 6 dígitos.
8. O usuário é salvo no repositório da entidade filha correta.
9. `EmailService` envia o código de verificação.
10. O backend gera JWT e retorna `AuthResponse`.
11. O fluxo visual do frontend avança para verificação de e-mail.

#### Verificação de e-mail

1. O usuário informa o código recebido.
2. O frontend envia `POST /api/auth/verify-email` com `email` e `code`.
3. O backend busca o usuário por e-mail.
4. Se houver bloqueio ativo, retorna HTTP 429.
5. Se o código estiver incorreto, incrementa `verification_attempts`.
6. Ao atingir 5 tentativas, define `verification_lock_until` para 30 minutos no futuro.
7. Se o código estiver correto e não expirado, define `email_verified=true`.
8. O backend limpa código, expiração, tentativas e bloqueio.
9. O backend gera JWT e retorna `AuthResponse`.
10. O frontend salva token e usuário no `localStorage`.

#### Login

1. O frontend envia `POST /api/auth/login` com e-mail e senha.
2. `AuthService.login()` chama `AuthenticationManager.authenticate(...)`.
3. O `DaoAuthenticationProvider` carrega o usuário por e-mail.
4. O `BCryptPasswordEncoder` compara a senha informada com o hash salvo.
5. Se a senha estiver errada, o backend retorna HTTP 401.
6. Se `active=false`, o backend rejeita a autenticação.
7. Se estiver correto, `JwtService` gera JWT.
8. O backend retorna token, tipo `Bearer` e dados do usuário.
9. O frontend salva `localStorage.token` e `localStorage.user`.
10. A página redireciona para `/dashboard`.

#### Requisição protegida

1. A tela chama um endpoint protegido, como `GET /api/users/me/profile`.
2. O interceptor Axios lê `localStorage.token`.
3. O interceptor adiciona `Authorization: Bearer <token>`.
4. `JwtAuthenticationFilter` valida o token.
5. O filtro grava a autenticação no `SecurityContextHolder`.
6. O controller lê o e-mail autenticado pelo contexto.
7. O service executa a regra usando o usuário localizado por e-mail.

#### Recuperação de senha

1. O usuário solicita recuperação em `ForgotPassword.vue`.
2. O frontend envia `POST /api/auth/forgot-password` com e-mail.
3. Se o e-mail não existir, o backend retorna sucesso sem revelar inexistência da conta.
4. Se existir, o backend gera código, salva expiração e envia e-mail.
5. O usuário informa o código em `ResetPassword.vue`.
6. O frontend envia `POST /api/auth/verify-reset-code`.
7. O backend valida código, expiração e bloqueio.
8. O frontend libera a etapa de nova senha.
9. O frontend envia `POST /api/auth/reset-password`.
10. O backend valida novamente o código, valida senha forte e confirmação.
11. O backend salva o novo hash BCrypt e limpa o desafio de recuperação.
12. O usuário volta para a tela de login.

#### Alteração de senha autenticada

1. O usuário acessa configurações de conta.
2. O frontend envia `PUT /api/users/me/password` com senha atual, nova senha e confirmação.
3. O JWT autentica a requisição.
4. `UserService.changePassword()` valida a senha atual com `passwordEncoder.matches(...)`.
5. O service valida se nova senha e confirmação coincidem.
6. O service valida força da senha.
7. O novo hash BCrypt é salvo.
8. O frontend limpa o formulário e mostra sucesso.

### Tratamento de Erros

Arquivo: `backend/src/main/java/com/servija/servija/exception/GlobalExceptionHandler.java`

| Exceção | HTTP | Uso |
|---|---:|---|
| `DuplicateResourceException` | 409 | E-mail ou CPF duplicado. |
| `ResourceNotFoundException` | 404 | Usuário não encontrado. |
| `InvalidCredentialsException` | 401 | Credenciais inválidas. |
| `BadCredentialsException` | 401 | Falha de autenticação do Spring Security. |
| `TooManyAttemptsException` | 429 | Excesso de tentativas. |
| `IllegalArgumentException` | 400 | Regra de negócio inválida. |
| `IllegalStateException` | 400 | Estado inválido, como e-mail já verificado. |
| `MethodArgumentNotValidException` | 400 | Erros de validação dos DTOs. |

Em bloqueios por tentativas, a resposta inclui `remainingAttempts` e `lockUntil`. Em validações Bean Validation, a resposta inclui mapa de campos inválidos.

## Frontend

### Visão Geral

O frontend do módulo de autenticação é implementado em Vue 3. Ele usa Vue Router para navegação, Pinia para estado global do usuário e Axios para comunicação com a API.

Os principais arquivos ficam em `frontend/src/components/auth`, `frontend/src/pages/Login`, `frontend/src/pages/Register`, `frontend/src/stores/userStore.ts`, `frontend/src/router/index.ts` e `frontend/src/api/config/axios.ts`.

### Páginas Envolvidas

| Rota | Arquivo | Finalidade |
|---|---|---|
| `/login` | `frontend/src/pages/Login/index.vue` | Abre o fluxo de autenticação na aba de login. |
| `/cadastro` | `frontend/src/pages/Register/index.vue` | Abre o fluxo de autenticação na aba de cadastro. |
| `/dashboard` | `frontend/src/pages/Dashboard/index.vue` | Redireciona conforme o tipo do usuário. |
| `/perfil` | `frontend/src/pages/Perfil/index.vue` | Redireciona para configurações/perfil conforme tipo. |
| `/freelancer-dashboard` | `frontend/src/pages/FreelancerDashboard/index.vue` | Área protegida do freelancer. |
| `/cliente-dashboard` | `frontend/src/pages/ClienteDashboard/index.vue` | Área protegida do contratante. |
| `/admin/categories` | `frontend/src/pages/AdminCategories/index.vue` | Área protegida de admin. |
| `/freelancer-profile-settings` | `frontend/src/pages/FreelancerProfileSettings/index.vue` | Conta e senha do freelancer. |
| `/cliente-profile-settings` | `frontend/src/pages/ClienteProfileSettings/index.vue` | Conta e senha do contratante. |

### Componentes Principais

#### `AuthFlow.vue`

Arquivo: `frontend/src/components/auth/AuthFlow.vue`

É o orquestrador do fluxo. Ele renderiza componentes diferentes conforme o estado interno:

| Estado | Componente |
|---|---|
| `auth-card` | `AuthCard.vue` |
| `email-verification` | `EmailVerification.vue` |
| `forgot-password` | `ForgotPassword.vue` |
| `reset-password` | `ResetPassword.vue` |

Responsabilidades:

| Responsabilidade | Descrição |
|---|---|
| Login | Envia `POST /auth/login`, salva token, normaliza tipo do usuário e atualiza Pinia. |
| Cadastro | Recebe tipo de conta e dados pessoais antes de abrir a verificação de e-mail. |
| Verificação | Recebe token e usuário após confirmação do e-mail e salva sessão. |
| Recuperação | Controla navegação entre esqueci senha e redefinir senha. |

Mapeamento de tipo:

| Backend | Frontend |
|---|---|
| `ADMIN` | `admin` |
| `FREELANCER` | `provider` |
| `CONTRACTOR` | `contractor` |

#### `AuthCard.vue`

Arquivo: `frontend/src/components/auth/AuthCard.vue`

Contém a interface visual de login e cadastro. No login, coleta e-mail e senha. No cadastro, divide o processo em três etapas: seleção de tipo de conta, dados pessoais e confirmação.

#### `SignupPersonalData.vue`

Arquivo: `frontend/src/components/auth/SignupPersonalData.vue`

Coleta nome, e-mail, telefone, CPF, senha e aceite dos termos. Aplica validações locais e consulta disponibilidade de CPF com debounce de 600 ms:

```http
GET /api/auth/check-cpf?cpf=<cpf_limpo>
```

Validações principais:

| Campo | Validação |
|---|---|
| Nome | Obrigatório, mínimo 3 caracteres. |
| E-mail | Obrigatório, formato válido, máximo 256 caracteres. |
| Telefone | Obrigatório e validado por máscara. |
| CPF | Obrigatório, validado por máscara e checado no backend. |
| Senha | Mínimo 8, máximo 256, maiúscula, minúscula, número e caractere especial. |
| Termos | Aceite obrigatório. |

#### `SignupConfirmation.vue`

Arquivo: `frontend/src/components/auth/SignupConfirmation.vue`

Exibe revisão dos dados e executa o cadastro:

```http
POST /api/auth/register
```

Payload enviado:

```json
{
  "name": "Nome do usuário",
  "email": "usuario@email.com",
  "cpf": "00000000000",
  "phone": "11999999999",
  "password": "SenhaForte@123",
  "userType": "CONTRACTOR ou FREELANCER",
  "acceptTerms": true
}
```

Após sucesso, o componente chama o callback e o fluxo avança para `EmailVerification.vue`.

#### `EmailVerification.vue`

Arquivo: `frontend/src/components/auth/EmailVerification.vue`

Valida o código de e-mail.

| Ação | Endpoint |
|---|---|
| Validar código | `POST /api/auth/verify-email` |
| Reenviar código | `POST /api/auth/resend-verification?email=...` |

Comportamentos:

| Comportamento | Descrição |
|---|---|
| Código em 6 inputs | Cada dígito fica em um campo. |
| Colagem | Permite colar os 6 dígitos de uma vez. |
| Validação automática | Ao preencher o último dígito, tenta validar. |
| Bloqueio | Se receber HTTP 429, exibe contador até `lockUntil`. |
| Sucesso | Recebe token e dados do usuário e devolve ao `AuthFlow`. |

#### `ForgotPassword.vue`

Arquivo: `frontend/src/components/auth/ForgotPassword.vue`

Solicita o e-mail para recuperação:

```http
POST /api/auth/forgot-password
```

O componente mostra mensagem genérica de sucesso mesmo em caso de erro, seguindo a estratégia de não revelar se o e-mail existe.

#### `ResetPassword.vue`

Arquivo: `frontend/src/components/auth/ResetPassword.vue`

Executa validação do código e redefinição de senha.

| Etapa | Endpoint |
|---|---|
| Validar código | `POST /api/auth/verify-reset-code` |
| Reenviar código | `POST /api/auth/resend-reset-code?email=...` |
| Redefinir senha | `POST /api/auth/reset-password` |

A tela só libera a nova senha após validar o código. A senha é validada no frontend com os mesmos requisitos aplicados no backend.

#### `userStore.ts`

Arquivo: `frontend/src/stores/userStore.ts`

Store Pinia responsável pelo estado do usuário.

| Função | Descrição |
|---|---|
| `init()` | Carrega usuário salvo em `localStorage.user`. |
| `setUser(...)` | Atualiza estado e salva usuário no `localStorage`. |
| `updateUser(...)` | Atualiza parcialmente estado e `localStorage`. |
| `clearUser()` | Remove usuário e token. |

Chaves usadas no `localStorage`:

| Chave | Conteúdo |
|---|---|
| `token` | JWT retornado pelo backend. |
| `user` | Dados mínimos do usuário usados pelo frontend. |

### Requisições Axios

#### Configuração

Arquivo: `frontend/src/api/config/axios.ts`

A URL base é montada a partir de `VITE_API_URL`. O código remove `/api` do final caso exista e adiciona `/api` automaticamente:

```ts
const cleanBaseURL = API_BASE_URL.replace(/\/api\/?$/, '');
baseURL: `${cleanBaseURL}/api`
```

#### Interceptor de requisição

Antes de cada chamada, o interceptor tenta buscar o token em `localStorage.token`. Se não encontrar, tenta compatibilidade com `localStorage.user.token`. Quando encontra, adiciona:

```http
Authorization: Bearer <token>
```

#### Interceptor de resposta

Quando recebe HTTP 401:

1. Remove `localStorage.token`.
2. Remove `localStorage.user`.
3. Redireciona para `/login?expired=true`, exceto quando já está em `/login` ou `/`.

A página de login lê esse query param e exibe aviso de sessão expirada.

#### Endpoints Consumidos

| Fluxo | Método | Endpoint relativo | Arquivo |
|---|---|---|---|
| Login | `POST` | `/auth/login` | `AuthFlow.vue` |
| Cadastro | `POST` | `/auth/register` | `SignupConfirmation.vue` |
| Verificar CPF | `GET` | `/auth/check-cpf?cpf=...` | `SignupPersonalData.vue` |
| Verificar e-mail | `POST` | `/auth/verify-email` | `EmailVerification.vue` |
| Reenviar verificação | `POST` | `/auth/resend-verification?email=...` | `EmailVerification.vue` |
| Solicitar recuperação | `POST` | `/auth/forgot-password` | `ForgotPassword.vue` |
| Validar código de recuperação | `POST` | `/auth/verify-reset-code` | `ResetPassword.vue` |
| Reenviar código de recuperação | `POST` | `/auth/resend-reset-code?email=...` | `ResetPassword.vue` |
| Redefinir senha | `POST` | `/auth/reset-password` | `ResetPassword.vue` |
| Obter perfil | `GET` | `/users/me/profile` | `ClienteAccountSettings.vue`, `FreelancerProfileSettings.vue` |
| Atualizar dados | `PUT` | `/users/me` | `ClienteAccountSettings.vue`, `FreelancerProfileSettings.vue` |
| Alterar senha | `PUT` | `/users/me/password` | `ClienteAccountSettings.vue`, `FreelancerProfileSettings.vue` |
| Excluir conta | `DELETE` | `/users/me` | `ClienteAccountSettings.vue`, `FreelancerProfileSettings.vue` |

Observação: existe `frontend/src/api/services/authService.ts` com wrappers auxiliares. O fluxo principal atual usa chamadas diretas via `api.post(...)` dentro dos componentes. Algumas funções desse arquivo apontam para rotas como `/auth/me` e `/auth/logout`, que não aparecem implementadas no `AuthController`; o logout efetivo é feito no frontend pela limpeza do `localStorage`.

### Vue Router e Controle de Acesso

Arquivo: `frontend/src/router/index.ts`

As rotas usam `meta.requiresAuth` e, quando necessário, `meta.userType`:

```ts
meta: { requiresAuth: true, userType: "provider" }
```

O guard global `beforeEach` executa:

1. Lê `localStorage.user`.
2. Se a rota exige autenticação e não há usuário, redireciona para `/login`.
3. Se o usuário autenticado tenta acessar `/login`, redireciona para `/dashboard`.
4. Se a rota exige tipo específico e o usuário não corresponde, redireciona para `/dashboard`.
5. Caso contrário, permite a navegação.

A rota `/dashboard` redireciona conforme o tipo salvo no frontend:

| Tipo | Destino |
|---|---|
| `provider` | `/freelancer-dashboard` |
| `contractor` | `/cliente-dashboard` |
| `admin` | `/admin/categories` |

### Fluxo do Usuário

#### Cadastro

1. Usuário acessa `/cadastro` ou alterna para cadastro em `/login`.
2. Escolhe tipo de conta: contratante ou prestador.
3. Preenche dados pessoais em `SignupPersonalData.vue`.
4. O CPF é validado localmente e checado em `/auth/check-cpf`.
5. Confirma os dados em `SignupConfirmation.vue`.
6. O frontend envia `POST /auth/register`.
7. O backend cria o usuário, gera código e envia e-mail.
8. O frontend exibe `EmailVerification.vue`.
9. Usuário informa o código de 6 dígitos.
10. O frontend envia `POST /auth/verify-email`.
11. O backend marca e-mail como verificado e retorna JWT.
12. O frontend salva `token` e `user` no `localStorage`.
13. O usuário é redirecionado para `/dashboard`.
14. `/dashboard` envia o usuário para a área correspondente.

#### Login

1. Usuário acessa `/login`.
2. Informa e-mail e senha em `AuthCard.vue`.
3. `AuthFlow.vue` envia `POST /auth/login`.
4. O backend valida credenciais e retorna JWT.
5. O frontend salva o token e os dados mínimos do usuário.
6. A store Pinia é atualizada.
7. O usuário é redirecionado para `/dashboard`.

#### Uso autenticado

1. Usuário navega para uma rota protegida.
2. Vue Router verifica se existe `localStorage.user`.
3. Axios adiciona o JWT no header da requisição.
4. O backend valida o JWT em `JwtAuthenticationFilter`.
5. O controller recupera o e-mail autenticado do `SecurityContextHolder`.
6. O service executa a regra para o usuário autenticado.

#### Sessão expirada

1. O JWT expira conforme `jwt.expiration`, com padrão de 24 horas.
2. Uma requisição protegida retorna HTTP 401.
3. O interceptor Axios remove `token` e `user`.
4. O frontend redireciona para `/login?expired=true`.
5. A página de login mostra mensagem de sessão expirada.

#### Logout

O logout é client-side:

1. O usuário aciona logout no layout/menu.
2. `useNavigation().logout()` chama `userStore.clearUser()`.
3. A store remove `localStorage.user` e `localStorage.token`.
4. O router redireciona para `/login`.

Como não há blacklist ou endpoint backend de logout, o JWT continua válido até expirar, mas deixa de ser usado pelo navegador após a remoção local.

#### Recuperação de senha

1. Usuário clica em “Esqueci minha senha”.
2. `ForgotPassword.vue` solicita e-mail.
3. O frontend envia `POST /auth/forgot-password`.
4. O backend envia código se o e-mail existir e retorna mensagem genérica.
5. `ResetPassword.vue` solicita o código.
6. O frontend envia `POST /auth/verify-reset-code`.
7. Se válido, a tela libera nova senha e confirmação.
8. O frontend envia `POST /auth/reset-password`.
9. O backend valida código e senha, salva novo hash BCrypt e limpa os campos de recuperação.
10. O usuário volta ao login.

### Observações Técnicas para o TCC

#### Pontos fortes

| Ponto | Justificativa |
|---|---|
| BCrypt | Evita senha em texto puro no banco. |
| JWT stateless | Facilita escalabilidade por não depender de sessão no servidor. |
| Verificação por e-mail | Confirma posse do e-mail cadastrado. |
| Controle de tentativas | Reduz risco de brute force nos códigos. |
| Resposta genérica em recuperação | Evita enumeração de contas por e-mail. |
| Camadas separadas | Controllers, services, repositories e DTOs têm responsabilidades definidas. |
| Bean Validation | Rejeita entradas inválidas antes das regras de negócio. |
| Roles por tipo de usuário | Permite autorização por papel no Spring Security. |

#### Limitações e cuidados

| Ponto | Impacto |
|---|---|
| JWT sem refresh token | Ao expirar, o usuário precisa fazer login novamente. |
| Logout apenas client-side | O token não é invalidado no servidor antes da expiração. |
| `email_verified` não bloqueia login explicitamente | O cadastro força o fluxo de verificação, mas o login não impede diretamente conta não verificada. |
| Código salvo em texto puro | Funciona, mas uma alternativa mais robusta seria armazenar hash do código. |
| `/api/auth/test-email` público | Por estar sob `/api/auth/**`, deve ser restringido ou removido em produção. |
| Sem rate limit por IP no código analisado | O bloqueio é por usuário/código, não por origem da requisição. |
| Templates de e-mail devem acompanhar as constantes | A regra efetiva de expiração está em `AuthService`. |

### Arquivos de Referência

| Área | Arquivo |
|---|---|
| Entidade base | `backend/src/main/java/com/servija/servija/model/User.java` |
| Entidade contratante | `backend/src/main/java/com/servija/servija/model/Contractor.java` |
| Entidade freelancer | `backend/src/main/java/com/servija/servija/model/Freelancer.java` |
| Controller de autenticação | `backend/src/main/java/com/servija/servija/controller/AuthController.java` |
| Controller de usuário | `backend/src/main/java/com/servija/servija/controller/UserController.java` |
| Serviço de autenticação | `backend/src/main/java/com/servija/servija/service/AuthService.java` |
| Serviço de usuário | `backend/src/main/java/com/servija/servija/service/UserService.java` |
| Serviço JWT | `backend/src/main/java/com/servija/servija/service/JwtService.java` |
| Serviço de e-mail | `backend/src/main/java/com/servija/servija/service/EmailService.java` |
| Filtro JWT | `backend/src/main/java/com/servija/servija/config/JwtAuthenticationFilter.java` |
| Configuração de segurança | `backend/src/main/java/com/servija/servija/config/SecurityConfig.java` |
| DTOs | `backend/src/main/java/com/servija/servija/dto` |
| Migrations | `backend/src/main/resources/db/migration` |
| Fluxo de autenticação | `frontend/src/components/auth/AuthFlow.vue` |
| Login/cadastro | `frontend/src/components/auth/AuthCard.vue` |
| Dados pessoais | `frontend/src/components/auth/SignupPersonalData.vue` |
| Confirmação | `frontend/src/components/auth/SignupConfirmation.vue` |
| Verificação de e-mail | `frontend/src/components/auth/EmailVerification.vue` |
| Esqueci senha | `frontend/src/components/auth/ForgotPassword.vue` |
| Redefinir senha | `frontend/src/components/auth/ResetPassword.vue` |
| Axios | `frontend/src/api/config/axios.ts` |
| Store de usuário | `frontend/src/stores/userStore.ts` |
| Rotas e guards | `frontend/src/router/index.ts` |
