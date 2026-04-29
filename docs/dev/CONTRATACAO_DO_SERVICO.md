# Módulo Contratação do Serviço

## Visão Geral

O módulo de contratação do serviço do Servi Já controla o ciclo completo de uma solicitação feita por um cliente para um freelancer. Esse ciclo começa quando o cliente escolhe um serviço publicado, descreve sua necessidade e envia uma solicitação. Depois disso, o freelancer recebe a demanda, envia uma proposta técnica e financeira, o cliente pode aceitar, recusar ou negociar por meio de uma contraproposta, e a contratação somente avança para execução após a confirmação do pagamento.

O módulo é composto por três fases principais:

1. **Solicitação e negociação**: criação da solicitação, resposta do freelancer, contraproposta do cliente e aceite.
2. **Pagamento e liberação para execução**: criação do pagamento, integração com Asaas, confirmação e mudança da solicitação para execução.
3. **Entrega e conclusão**: acompanhamento do serviço, entrega pelo freelancer, aceite final pelo cliente e liberação do saldo para a carteira do freelancer.

Arquivos principais do módulo:

| Camada | Arquivos |
|---|---|
| Modelos JPA | `ServiceRequest.java`, `ServiceRequestHistory.java`, `Payment.java`, `PaymentStatusHistory.java`, `Service.java`, `FreelancerWallet.java` |
| Controllers | `ServiceRequestController.java`, `PaymentController.java` |
| Services | `ServiceRequestService.java`, `ServiceRequestHistoryService.java`, `PaymentService.java`, `AsaasWebhookService.java`, `WebSocketService.java`, `FreelancerWalletService.java` |
| DTOs | `ServiceRequestDTO.java`, `ServiceRequestResponse.java`, `FreelancerResponseDTO.java`, `CounterProposalDTO.java`, `PaymentRequest.java`, `PaymentResponse.java` |
| Frontend | `clientRequestStore.ts`, `proposalStore.ts`, `ServiceDetailPage.vue`, `ClienteRequestsPage.vue`, `ClienteRequestDetailPage.vue`, `FreelancerProposalsPage.vue`, `FreelancerRequestDetailPage.vue`, `CheckoutPage.vue` |

## Banco de Dados

### Tabelas Envolvidas

A entidade central do módulo é `service_requests`, pois ela representa a contratação em si. Ao redor dela existem tabelas de catálogo, usuários, histórico, pagamento e carteira financeira.

| Tabela | Responsabilidade no módulo |
|---|---|
| `users` | Armazena os usuários. Na contratação, um usuário pode ser cliente contratante ou freelancer prestador. |
| `contractors` | Especialização de `users` para clientes, com contadores de contratos. |
| `freelancers` | Especialização de `users` para prestadores, com dados profissionais, categoria, disponibilidade e localização. |
| `services` | Catálogo de serviços publicados por freelancers. É a origem mais comum de uma solicitação. |
| `service_requests` | Tabela principal. Registra solicitação, negociação, status, anexos, entrega e vínculo entre cliente e freelancer. |
| `service_request_history` | Histórico/auditoria da solicitação: abertura, proposta, contraproposta, aceite, pagamento, entrega e conclusão. |
| `payments` | Pagamento vinculado a uma solicitação aceita. Guarda valores, método, status, IDs do Asaas e dados de PIX/cartão. |
| `payment_status_history` | Histórico de mudanças de status do pagamento, incluindo eventos do Asaas. |
| `freelancer_wallets` | Carteira do freelancer. Recebe saldo pendente após pagamento confirmado e saldo disponível após conclusão. |
| `service_categories` | Catálogo normalizado de categorias. |
| `service_subcategories` | Catálogo normalizado de subcategorias. |

Migrations relevantes para a evolução do módulo:

| Migration | Papel |
|---|---|
| `V8__create_services_table.sql` | Cria `services`, tabela usada para publicar serviços contratáveis. |
| `V18__add_counter_proposal_fields.sql` | Adiciona `counter_value`, `counter_days` e `counter_notes` em `service_requests`. |
| `V19__add_details_to_history.sql` | Adiciona o campo `details` ao histórico da solicitação. |
| `V20__create_payments_table.sql` | Cria `payments`. |
| `V21__add_asaas_fields_to_payments.sql` | Adiciona identificadores do Asaas aos pagamentos. |
| `V26__add_payment_history_and_chat_enabled.sql` | Cria `payment_status_history` e adiciona `chat_enabled` em `service_requests`. |
| `V28__add_freelancer_attachments_to_service_requests.sql` | Adiciona anexos enviados pelo freelancer. |
| `V29__add_delivered_to_service_request_status.sql` | Adiciona o status `DELIVERED`. |
| `V32__add_response_attachments_to_service_requests.sql` | Adiciona anexos da proposta do freelancer. |
| `V38__replace_priority_with_work_model_in_service_requests.sql` | Substitui prioridade por modelo de trabalho (`PRESENCIAL`, `REMOTO`, `HIBRIDO`). |
| `V41__add_service_id_to_service_requests.sql` | Relaciona `service_requests` com `services`. |
| `V43__remove_budget_from_service_requests.sql` | Remove orçamento legado e centraliza o valor negociado na resposta/contraproposta. |
| `V48__create_service_catalog_and_links.sql` | Cria categorias/subcategorias e adiciona FKs de catálogo. |
| `V52__add_response_milestones_to_service_requests.sql` | Adiciona marcos de execução na proposta do freelancer. |

### Relacionamentos

Relacionamentos principais:

```text
users (cliente) 1:N service_requests
users (freelancer) 1:N service_requests
services 1:N service_requests
service_categories 1:N service_requests
service_subcategories 1:N service_requests
service_requests 1:N service_request_history
service_requests 1:1 payments
payments 1:N payment_status_history
users (freelancer) 1:1 freelancer_wallets
```

Na prática:

- `service_requests.contractor_id` referencia `users.id` e identifica o cliente que abriu a solicitação.
- `service_requests.freelancer_id` referencia `users.id` e identifica o freelancer que recebeu a demanda.
- `service_requests.service_id` referencia `services.id` quando a contratação nasce de um serviço publicado.
- `service_requests.category_id` e `service_requests.subcategory_id` referenciam o catálogo normalizado.
- `service_request_history.service_request_id` referencia a solicitação auditada.
- `service_request_history.user_id` referencia o usuário que executou a ação.
- `payments.service_request_id` referencia a solicitação paga.
- `payments.contractor_id` referencia o cliente pagador.
- `payments.freelancer_id` referencia o freelancer recebedor.
- `payment_status_history.payment_id` referencia o pagamento.
- `freelancer_wallets.user_id` referencia o freelancer e é único.

### Campos Principais

#### `service_requests`

| Campo | Descrição |
|---|---|
| `id` | Identificador da solicitação. |
| `contractor_id` | Cliente que abriu a solicitação. |
| `freelancer_id` | Freelancer que recebeu a solicitação. |
| `service_id` | Serviço publicado que originou a contratação. |
| `service_name` | Nome do serviço solicitado. |
| `category`, `subcategory` | Campos textuais de categoria/subcategoria, mantidos para exibição e compatibilidade. |
| `category_id`, `subcategory_id` | Categoria e subcategoria normalizadas. |
| `description` | Descrição da necessidade do cliente. |
| `deadline` | Prazo desejado informado pelo cliente. |
| `work_model` | Modelo de trabalho: `PRESENCIAL`, `REMOTO` ou `HIBRIDO`. |
| `status` | Estado atual da contratação. |
| `chat_enabled` | Indica se o chat está liberado. Normalmente fica `true` após pagamento confirmado. |
| `address` | Endereço de atendimento para serviços presenciais ou híbridos. |
| `desired_date` | Data desejada para realização do serviço. |
| `attachments` | URLs de anexos enviados pelo cliente. |
| `freelancer_attachments` | Anexos enviados pelo freelancer. |
| `response_attachments` | Anexos enviados na proposta do freelancer. |
| `response_milestones` | Marcos de execução propostos pelo freelancer em JSON/texto. |
| `response_value` | Valor proposto pelo freelancer. |
| `response_days` | Prazo de execução proposto em dias. |
| `response_scope` | Escopo proposto pelo freelancer. |
| `response_notes` | Observações do freelancer. |
| `responded_at` | Data/hora do envio da proposta. |
| `counter_value` | Valor da contraproposta do cliente. |
| `counter_days` | Prazo da contraproposta. |
| `counter_notes` | Observações da contraproposta. |
| `legacy_low_value_allowed` | Flag de compatibilidade para valores antigos abaixo do mínimo atual. |
| `cancel_reason` | Motivo de cancelamento. |
| `reject_reason` | Motivo de rejeição. |
| `created_at`, `updated_at` | Datas de criação e atualização. |

Status possíveis em `ServiceRequest.Status`:

| Status | Significado |
|---|---|
| `PENDING` | Solicitação criada e aguardando resposta do freelancer. |
| `SENT` | Status previsto/mantido por compatibilidade. |
| `NEGOTIATING` | Existe proposta ou contraproposta em negociação. |
| `ACCEPTED` | Proposta aceita, aguardando pagamento. |
| `REJECTED` | Solicitação/proposta rejeitada. |
| `IN_PROGRESS` | Pagamento confirmado e serviço em execução. |
| `DELIVERED` | Freelancer marcou o serviço como entregue. |
| `COMPLETED` | Cliente confirmou a conclusão. |
| `CANCELLED` | Solicitação cancelada. |

Fluxo principal de status:

```text
PENDING -> NEGOTIATING -> ACCEPTED -> IN_PROGRESS -> DELIVERED -> COMPLETED
```

Saídas alternativas:

```text
PENDING/NEGOTIATING -> REJECTED
PENDING/NEGOTIATING/ACCEPTED -> CANCELLED
```

#### `service_request_history`

| Campo | Descrição |
|---|---|
| `id` | Identificador do histórico. |
| `service_request_id` | Solicitação relacionada. |
| `operation_type` | Evento executado no ciclo da contratação. |
| `user_id` | Usuário responsável pela ação. |
| `created_at` | Data/hora do evento. |
| `details` | Informações complementares, como motivo de recusa ou resumo da ação. |

Operações relevantes:

| Operação | Quando ocorre |
|---|---|
| `REQUEST_OPENED` | Cliente abre uma nova solicitação. |
| `REQUEST_REJECTED` | Solicitação é rejeitada. |
| `BUDGET_SENT` | Freelancer envia proposta. |
| `COUNTER_PROPOSAL_SENT` | Cliente envia contraproposta. |
| `COUNTER_PROPOSAL_ACCEPTED` | Freelancer aceita a contraproposta. |
| `COUNTER_PROPOSAL_REJECTED` | Freelancer rejeita a contraproposta. |
| `BUDGET_ACCEPTED` | Cliente aceita a proposta do freelancer. |
| `CANCELLED` | Solicitação é cancelada. |
| `PAYMENT_CREATED` | Pagamento é criado. |
| `PAYMENT_CONFIRMED` | Pagamento é confirmado. |
| `PAYMENT_FAILED` | Pagamento falha. |
| `SERVICE_DELIVERED` | Freelancer entrega o serviço. |
| `SERVICE_COMPLETED` | Cliente conclui a contratação. |

#### `payments`

| Campo | Descrição |
|---|---|
| `id` | Identificador do pagamento. |
| `service_request_id` | Solicitação paga. |
| `contractor_id` | Cliente pagador. |
| `freelancer_id` | Freelancer recebedor. |
| `amount` | Valor líquido destinado ao freelancer. |
| `platform_fee` | Taxa da plataforma. |
| `gateway_fee` | Taxa estimada do gateway. |
| `total_amount` | Valor total pago pelo cliente. |
| `payment_method` | Método de pagamento. |
| `status` | Status do pagamento. |
| `transaction_id` | Identificador interno da transação. |
| `asaas_payment_id` | Identificador do pagamento no Asaas. |
| `asaas_customer_id` | Identificador do cliente no Asaas. |
| `pix_qr_code` | QR Code PIX em base64. |
| `pix_copy_paste` | Código PIX copia e cola. |
| `pix_expiration_date` | Data de expiração do PIX. |
| `card_last_digits` | Últimos dígitos do cartão, quando aplicável. |
| `refund_reason` | Motivo de reembolso/cancelamento. |
| `completed_at` | Data/hora da confirmação. |
| `created_at`, `updated_at` | Datas de criação e atualização. |

Status possíveis em `Payment.PaymentStatus`:

| Status | Significado |
|---|---|
| `PENDING` | Pagamento criado, ainda sem processamento final. |
| `PROCESSING` | Em processamento, comum para PIX aguardando confirmação. |
| `COMPLETED` | Pagamento confirmado. |
| `FAILED` | Pagamento falhou. |
| `REFUNDED` | Pagamento reembolsado. |
| `CANCELLED` | Pagamento cancelado. |
| `OVERDUE` | Pagamento vencido. |
| `CHARGEBACK` | Pagamento contestado. |

#### `payment_status_history`

| Campo | Descrição |
|---|---|
| `id` | Identificador do evento. |
| `payment_id` | Pagamento relacionado. |
| `status` | Status registrado. |
| `asaas_event` | Evento recebido do Asaas, quando houver. |
| `details` | Detalhes do evento. |
| `created_at` | Data/hora do registro. |

#### `services`

| Campo | Descrição |
|---|---|
| `id` | Identificador do serviço publicado. |
| `freelancer_id` | Freelancer dono do serviço. |
| `title` | Título do serviço. |
| `category`, `category_id`, `subcategory_id` | Classificação textual e normalizada. |
| `description` | Descrição da oferta. |
| `price` | Preço base divulgado. |
| `price_type` | Tipo de preço: fixo, por hora ou negociável. |
| `delivery_time`, `delivery_unit` | Prazo base de entrega. |
| `work_model` | Modelo de atendimento aceito pelo serviço. |
| `tags`, `images` | Dados complementares para exibição. |
| `is_active` | Indica se o serviço pode ser contratado. |

## Backend

### Arquitetura Geral

O backend usa Spring Boot com uma separação clássica entre Controllers, Services, Repositories, Models e DTOs. O controller recebe a requisição autenticada, o service aplica regras de negócio e o repository persiste ou consulta dados no banco.

Componentes principais:

- `ServiceRequestController`: expõe rotas REST de solicitação, negociação, anexos, histórico e conclusão.
- `ServiceRequestService`: concentra as regras de negócio da contratação.
- `ServiceRequestHistoryService`: registra e consulta a linha do tempo da solicitação.
- `PaymentController`: expõe rotas REST de pagamento.
- `PaymentService`: cria pagamentos, calcula taxas, integra com Asaas e atualiza estados financeiros.
- `AsaasWebhookService`: processa confirmações assíncronas do gateway de pagamento.
- `WebSocketService`: envia notificações em tempo real.
- `FreelancerWalletService`: controla saldo pendente e saldo disponível do freelancer.

### Controllers

#### `ServiceRequestController`

Base path:

```http
/api/service-requests
```

Rotas principais:

| Método | Rota | Método Java | Responsabilidade |
|---|---|---|---|
| `POST` | `/api/service-requests` | `create(...)` | Cria nova solicitação de contratação. |
| `GET` | `/api/service-requests/my` | `myRequests(...)` | Lista solicitações do cliente autenticado. |
| `GET` | `/api/service-requests/received` | `receivedRequests(...)` | Lista solicitações recebidas pelo freelancer autenticado. |
| `GET` | `/api/service-requests/{id}` | `getById(...)` | Busca detalhes de uma solicitação. |
| `PATCH` | `/api/service-requests/{id}/status` | `updateStatus(...)` | Atualiza status geral, como aceite, rejeição ou cancelamento. |
| `PATCH` | `/api/service-requests/{id}/respond` | `respondToRequest(...)` | Freelancer envia proposta. |
| `PATCH` | `/api/service-requests/{id}/milestones/{milestoneId}` | `updateMilestoneStatus(...)` | Atualiza status de um marco de execução. |
| `PATCH` | `/api/service-requests/{id}/counter-proposal` | `counterProposal(...)` | Cliente envia contraproposta. |
| `PATCH` | `/api/service-requests/{id}/accept-counter` | `acceptCounterProposal(...)` | Freelancer aceita contraproposta. |
| `PATCH` | `/api/service-requests/{id}/reject-counter` | `rejectCounterProposal(...)` | Freelancer rejeita contraproposta. |
| `POST` | `/api/service-requests/{id}/freelancer-attachments` | `addFreelancerAttachments(...)` | Adiciona anexos do freelancer. |
| `POST` | `/api/service-requests/{id}/contractor-attachments` | `addContractorAttachments(...)` | Adiciona anexos do cliente. |
| `GET` | `/api/service-requests/{id}/history` | `getHistory(...)` | Retorna histórico da contratação. |
| `PATCH` | `/api/service-requests/{id}/complete` | `completeService(...)` | Entrega pelo freelancer ou conclusão pelo cliente. |
| `DELETE` | `/api/service-requests/{id}` | `delete(...)` | Remove uma solicitação e seus anexos. |

A rota `/status` não é usada para marcar `DELIVERED` ou `COMPLETED`. Esses estados são tratados por `/complete`, porque dependem de quem está autenticado: freelancer entrega, cliente conclui.

#### `PaymentController`

Base path:

```http
/api/payments
```

Rotas principais:

| Método | Rota | Método Java | Responsabilidade |
|---|---|---|---|
| `POST` | `/api/payments` | `createPayment(...)` | Cria pagamento para uma solicitação aceita. |
| `GET` | `/api/payments/{id}` | `getPayment(...)` | Busca detalhes de um pagamento. |
| `GET` | `/api/payments/my` | `getMyPayments(...)` | Lista pagamentos do usuário autenticado. |
| `GET` | `/api/payments/{id}/status` | `checkPaymentStatus(...)` | Consulta/sincroniza status do pagamento. |
| `PATCH` | `/api/payments/{id}/status` | `updatePaymentStatus(...)` | Atualiza status manualmente. |
| `GET` | `/api/payments/{id}/pix-qrcode` | `getPixQrCode(...)` | Retorna QR Code PIX e copia e cola. |
| `GET` | `/api/payments/service-request/{serviceRequestId}` | `getPaymentByServiceRequestId(...)` | Busca pagamento associado à solicitação. |
| `GET` | `/api/payments/{id}/history` | `getPaymentHistory(...)` | Lista eventos de status do pagamento. |
| `GET` | `/api/payments/{id}/receipt-url` | `getReceiptUrl(...)` | Retorna URL de recibo/comprovante do Asaas. |
| `POST` | `/api/payments/{id}/simulate-confirmation` | `simulatePaymentConfirmation(...)` | Simula confirmação em desenvolvimento/teste. |

### Services

#### `ServiceRequestService`

É o principal serviço de regras de negócio da contratação. Ele valida dados, consulta usuários e serviços, aplica transições de status, persiste anexos, grava histórico e aciona notificações.

Constantes relevantes:

| Constante | Valor | Uso |
|---|---|---|
| `MIN_NEGOTIATED_AMOUNT` | `20.00` | Valor mínimo para proposta e contraproposta. |
| `MAX_NEGOTIATED_AMOUNT` | `1000000.00` | Valor máximo permitido na negociação. |
| `VALID_MILESTONE_STATUSES` | `pending`, `in_progress`, `completed` | Status permitidos para marcos da proposta. |

Principais métodos:

| Método | Responsabilidade |
|---|---|
| `create(String contractorEmail, ServiceRequestDTO dto)` | Cria solicitação. Resolve cliente, freelancer, serviço, categoria, modelo de trabalho, endereço, data, anexos e histórico. |
| `getByContractorEmail(String email)` | Retorna solicitações abertas pelo cliente autenticado. |
| `getByFreelancerEmail(String email)` | Retorna solicitações recebidas pelo freelancer autenticado. |
| `getById(Long id)` | Busca uma solicitação e converte para resposta. |
| `updateStatus(Long id, String status, String reason, String userEmail)` | Atualiza status como `ACCEPTED`, `REJECTED`, `NEGOTIATING` ou `CANCELLED`. |
| `respondToRequest(Long id, FreelancerResponseDTO dto, String freelancerEmail)` | Salva proposta do freelancer com valor, prazo, escopo, observações, anexos e marcos. |
| `counterProposal(Long id, CounterProposalDTO dto, String contractorEmail)` | Salva contraproposta enviada pelo cliente. |
| `acceptCounterProposal(Long id, String freelancerEmail)` | Aceita contraproposta e transforma seus dados nos dados finais da proposta. |
| `rejectCounterProposal(Long id, String reason, String freelancerEmail)` | Rejeita contraproposta, limpa campos de contraproposta e registra motivo. |
| `updateMilestoneStatus(Long id, String milestoneId, String status, String freelancerEmail)` | Atualiza o status de um marco de execução. |
| `completeService(Long id, String userEmail)` | Entrega pelo freelancer ou conclusão pelo cliente, conforme usuário autenticado. |
| `addFreelancerAttachments(Long id, FreelancerAttachmentsRequest request, String freelancerEmail)` | Adiciona anexos enviados pelo freelancer. |
| `addContractorAttachments(Long id, ProjectAttachmentsRequest request, String contractorEmail)` | Adiciona anexos enviados pelo cliente. |
| `delete(Long id)` | Remove a solicitação e exclui anexos do S3 quando existirem. |
| `toResponse(ServiceRequest request)` | Converte entidade para `ServiceRequestResponse`. |

Regras aplicadas em `create`:

- O cliente é identificado pelo e-mail autenticado recebido do `Principal`.
- Quando `serviceId` é informado, o backend carrega o serviço publicado e define automaticamente o freelancer responsável.
- Quando `serviceId` e `freelancerId` são enviados juntos, o backend valida se o serviço pertence ao freelancer informado.
- A descrição é normalizada e não pode ficar vazia.
- O modelo de trabalho é validado. Para `PRESENCIAL` e `HIBRIDO`, o endereço é obrigatório. Para `REMOTO`, o endereço é removido.
- A data desejada não pode estar no passado.
- Os anexos são serializados para persistência.
- Categoria e subcategoria são resolvidas a partir do serviço ou dos IDs informados.
- A solicitação é criada com status inicial `PENDING`.
- O histórico registra `REQUEST_OPENED`.
- O freelancer é notificado por WebSocket e pelo serviço de notificações.

Regras aplicadas em `respondToRequest`:

- O valor da proposta deve respeitar mínimo e máximo.
- O prazo deve respeitar as validações do DTO.
- O escopo é obrigatório.
- Anexos e marcos são normalizados antes da persistência.
- A solicitação passa para `NEGOTIATING`.
- O histórico registra `BUDGET_SENT`.
- O cliente recebe notificação em tempo real.

Regras aplicadas em `counterProposal`:

- O cliente envia novo valor, prazo e observações.
- O valor deve respeitar os limites de negociação.
- Os campos `counterValue`, `counterDays` e `counterNotes` são preenchidos.
- O status fica `NEGOTIATING`.
- O histórico registra `COUNTER_PROPOSAL_SENT`.
- O freelancer é notificado.

Regras aplicadas em `acceptCounterProposal`:

- A contraproposta precisa existir.
- O valor aceito é validado.
- Os dados da contraproposta são copiados para `responseValue`, `responseDays` e `responseNotes`.
- Os campos `counter*` são limpos.
- O status muda para `ACCEPTED`.
- O histórico registra `COUNTER_PROPOSAL_ACCEPTED`.

Regras aplicadas em `completeService`:

- Se o usuário é o freelancer e o status está `IN_PROGRESS`, a solicitação muda para `DELIVERED` e o histórico registra `SERVICE_DELIVERED`.
- Se o usuário é o cliente e o status está `DELIVERED`, a solicitação muda para `COMPLETED`, o histórico registra `SERVICE_COMPLETED` e o saldo pode ser liberado.
- A liberação financeira só ocorre se existir pagamento associado e se ele estiver `COMPLETED`.

#### `ServiceRequestHistoryService`

Serviço responsável pela trilha de auditoria.

| Método | Responsabilidade |
|---|---|
| `record(ServiceRequest request, User user, OperationType operationType)` | Registra um evento simples. |
| `record(ServiceRequest request, User user, OperationType operationType, String details)` | Registra um evento com detalhes. |
| `getHistory(Long serviceRequestId)` | Busca histórico da solicitação em ordem decrescente. |
| `toResponse(ServiceRequestHistory history)` | Converte histórico para DTO. |

O histórico é formatado para o fuso `America/Sao_Paulo`, no padrão `dd/MM/yyyy HH:mm`.

#### `PaymentService`

Serviço responsável por criar pagamentos, calcular taxas, integrar com Asaas e sincronizar pagamento com solicitação.

Constantes relevantes:

| Constante | Valor | Uso |
|---|---|---|
| `PLATFORM_FEE_PERCENTAGE` | `0.05` | Taxa de 5% da plataforma. |
| `PIX_FEE` | `0.99` | Taxa fixa considerada para PIX. |
| `BOLETO_FEE` | `0.99` | Taxa fixa considerada para boleto. |
| `CREDIT_CARD_FIXED_FEE` | `0.49` | Taxa fixa de cartão. |
| `CREDIT_CARD_PERCENTAGE_FEE` | `0.0199` | Taxa percentual de cartão. |
| `MIN_NEGOTIATED_AMOUNT` | `20.00` | Valor mínimo negociado. |
| `PAYMENT_RELEASE_DAYS` | `7` | Janela conceitual de liberação financeira. |

Principais métodos:

| Método | Responsabilidade |
|---|---|
| `createPayment(String contractorEmail, PaymentRequest request)` | Cria pagamento para solicitação aceita. Valida dono, status, valor e método. |
| `processPixPayment(Payment payment, String customerId, ServiceRequest serviceRequest)` | Cria cobrança PIX no Asaas e salva QR Code/copia e cola. |
| `processCreditCardPayment(Payment payment, PaymentRequest request)` | Valida dados do cartão e registra pagamento como concluído no fluxo atual. |
| `getPaymentById(Long id, String userEmail)` | Busca pagamento respeitando vínculo com o usuário. |
| `getPaymentByServiceRequestId(Long serviceRequestId, String userEmail)` | Busca pagamento de uma solicitação. |
| `getMyPayments(String userEmail)` | Lista pagamentos do usuário autenticado. |
| `updatePaymentStatus(Long id, PaymentStatus status, String userEmail)` | Atualiza status manualmente. |
| `checkAsaasPaymentStatus(Long id, String userEmail)` | Consulta o Asaas e sincroniza status local. |
| `getPixQrCode(Long id, String userEmail)` | Retorna dados PIX. |
| `getPaymentHistory(Long id, String userEmail)` | Retorna histórico de status do pagamento. |
| `getReceiptUrl(Long id, String userEmail)` | Consulta URL de comprovante/recibo no Asaas. |
| `simulatePaymentConfirmation(Long id, String userEmail)` | Simula confirmação em ambiente de desenvolvimento/teste. |
| `mapToResponse(Payment payment)` | Converte entidade para `PaymentResponse`. |

Regras principais de pagamento:

- O pagamento só pode ser criado se a solicitação estiver `ACCEPTED`.
- O cliente autenticado deve ser o dono da solicitação.
- O sistema impede pagamento duplicado para a mesma solicitação.
- O valor enviado deve corresponder ao valor negociado (`responseValue` ou `counterValue`).
- A taxa da plataforma é calculada como 5%.
- `totalAmount` representa o valor pago pelo cliente.
- `amount` representa o valor líquido destinado ao freelancer.
- Para PIX, o pagamento fica `PROCESSING` até confirmação.
- Quando o pagamento é confirmado, a solicitação muda para `IN_PROGRESS` e `chatEnabled` passa para `true`.

#### `AsaasWebhookService`

Processa eventos assíncronos do gateway Asaas. É essencial para o fluxo PIX, pois a confirmação pode ocorrer depois da criação da cobrança.

Ao receber `PAYMENT_RECEIVED`, `PAYMENT_CONFIRMED` ou evento PIX com status `DONE`, o serviço:

1. Localiza o pagamento pelo identificador do Asaas.
2. Atualiza o pagamento para `COMPLETED`.
3. Define `completedAt`.
4. Registra `PaymentStatusHistory`.
5. Adiciona o valor líquido como saldo pendente na carteira do freelancer.
6. Atualiza a solicitação para `IN_PROGRESS`.
7. Habilita o chat com `chatEnabled = true`.
8. Registra `PAYMENT_CONFIRMED` no histórico da solicitação.
9. Notifica cliente e freelancer via WebSocket/notificação persistida.

#### `FreelancerWalletService`

No contexto da contratação, controla o recebimento do freelancer.

| Método | Responsabilidade |
|---|---|
| `addPendingBalance(User freelancer, BigDecimal amount)` | Adiciona saldo pendente após pagamento confirmado. |
| `releasePendingBalance(Payment payment)` | Move saldo pendente para disponível após conclusão pelo cliente. |
| `getOrCreateWallet(String email)` | Obtém ou cria a carteira do freelancer. |
| `releasePendingToAvailable(String email, BigDecimal amount)` | Liberação alternativa usada por rotinas/schedulers. |

### DTOs

#### `ServiceRequestDTO`

Usado em `POST /api/service-requests`.

| Campo | Uso |
|---|---|
| `serviceName` | Obrigatório. Nome da solicitação. |
| `categoryId`, `subcategoryId` | Categoria e subcategoria normalizadas. |
| `category`, `subcategory` | Campos textuais para exibição/compatibilidade. |
| `description` | Obrigatório. Necessidade do cliente. |
| `budget` | Campo legado/compatibilidade. |
| `deadline` | Obrigatório. Prazo desejado. |
| `workModel` | Deve ser `PRESENCIAL`, `REMOTO` ou `HIBRIDO`. |
| `address` | Obrigatório para presencial/híbrido. |
| `desiredDate` | Data desejada para o serviço. |
| `attachments` | URLs de anexos do cliente. |
| `serviceId` | Serviço selecionado no catálogo. |
| `freelancerId` | Freelancer que receberá a solicitação. |

#### `ServiceRequestResponse`

Usado nas respostas das rotas de solicitação. Retorna dados de identificação, partes envolvidas, categoria, descrição, status, anexos, proposta, contraproposta, motivos de cancelamento/rejeição e datas.

Campos principais:

- `id`, `contractorId`, `freelancerId`, `serviceId`.
- `contractorName`, `contractorAvatar`, `freelancerName`, `freelancerAvatar`.
- `categoryId`, `category`, `categoryLabel`, `subcategoryId`, `subcategory`, `subcategoryLabel`.
- `serviceName`, `description`, `deadline`, `workModel`, `status`, `address`, `desiredDate`.
- `attachments`, `freelancerAttachments`, `freelancerAttachmentDetails`, `responseAttachments`.
- `responseValue`, `responseDays`, `responseScope`, `responseNotes`, `respondedAt`, `milestones`.
- `counterValue`, `counterDays`, `counterNotes`.
- `cancelReason`, `rejectReason`, `createdAt`, `updatedAt`.

#### `FreelancerResponseDTO`

Usado em `PATCH /api/service-requests/{id}/respond`.

| Campo | Validação/uso |
|---|---|
| `value` | Obrigatório. Mínimo `20.00`, máximo `1000000.00`. |
| `executionDays` | Obrigatório. Entre 1 e 365 dias. |
| `scope` | Obrigatório. Escopo do trabalho. |
| `notes` | Observações opcionais. |
| `milestones` | Lista de marcos de execução. |
| `attachments` | URLs de anexos da proposta. |

#### `CounterProposalDTO`

Usado em `PATCH /api/service-requests/{id}/counter-proposal`.

| Campo | Validação/uso |
|---|---|
| `value` | Mínimo `20.00`, máximo `1000000.00`. |
| `executionDays` | Entre 1 e 365 dias. |
| `notes` | Observações da contraproposta. |

#### `PaymentRequest` e `PaymentResponse`

`PaymentRequest` é usado em `POST /api/payments`.

| Campo | Uso |
|---|---|
| `serviceRequestId` | Solicitação aceita a ser paga. |
| `paymentMethod` | Método de pagamento. |
| `amount` | Valor negociado. Deve ser positivo e bater com a proposta aceita. |
| `cpf` | CPF usado para cobrança PIX no Asaas. |
| `cardData` | Dados de cartão quando aplicável. |

`PaymentResponse` retorna `id`, `serviceRequestId`, `amount`, `platformFee`, `gatewayFee`, `totalAmount`, `paymentMethod`, `status`, `transactionId`, `asaasPaymentId`, `pixQrCode`, `pixCopyPaste`, `pixExpirationDate`, `createdAt` e `completedAt`.

### Fluxo da Requisição no Backend

#### 1. Criação da solicitação

```text
Frontend -> POST /api/service-requests
-> ServiceRequestController.create()
-> ServiceRequestService.create()
-> ServiceRequestRepository.save()
-> ServiceRequestHistoryService.record(REQUEST_OPENED)
-> WebSocketService.notifyNewServiceRequest()
-> ServiceRequestResponse
```

Passos técnicos:

1. O cliente envia os dados da solicitação.
2. O controller recupera o e-mail do usuário autenticado pelo `Principal`.
3. O service busca o cliente, resolve serviço/freelancer, valida campos e normaliza dados.
4. A solicitação é persistida como `PENDING`.
5. O histórico registra `REQUEST_OPENED`.
6. O freelancer recebe notificação em tempo real.

#### 2. Envio de proposta pelo freelancer

```text
Frontend -> PATCH /api/service-requests/{id}/respond
-> ServiceRequestController.respondToRequest()
-> ServiceRequestService.respondToRequest()
-> status NEGOTIATING
-> ServiceRequestHistoryService.record(BUDGET_SENT)
-> WebSocketService.notifyNewProposal()
```

O backend grava valor, prazo, escopo, observações, marcos e anexos nos campos `response_*`. Depois disso, o cliente passa a poder aceitar, rejeitar ou negociar.

#### 3. Aceite, rejeição ou contraproposta pelo cliente

Aceite:

```http
PATCH /api/service-requests/{id}/status
Content-Type: application/json

{ "status": "ACCEPTED" }
```

Efeitos:

- Status passa para `ACCEPTED`.
- Histórico registra `BUDGET_ACCEPTED`.
- O cliente pode ir para o checkout.
- O serviço ainda não entra em execução.

Rejeição:

```http
PATCH /api/service-requests/{id}/status
Content-Type: application/json

{ "status": "REJECTED", "reason": "Motivo da rejeição" }
```

Efeitos:

- Status passa para `REJECTED`.
- Motivo é salvo em `rejectReason`.
- Histórico registra `REQUEST_REJECTED`.

Contraproposta:

```http
PATCH /api/service-requests/{id}/counter-proposal
Content-Type: application/json

{
  "value": 250.00,
  "executionDays": 5,
  "notes": "Ajuste de valor e prazo"
}
```

Efeitos:

- Campos `counter_*` são preenchidos.
- Status permanece `NEGOTIATING`.
- Histórico registra `COUNTER_PROPOSAL_SENT`.
- Freelancer é notificado.

#### 4. Resposta do freelancer à contraproposta

Aceitar contraproposta:

```http
PATCH /api/service-requests/{id}/accept-counter
```

Efeitos:

- `counterValue` e `counterDays` viram o valor e prazo finais.
- Campos de contraproposta são limpos.
- Status passa para `ACCEPTED`.
- Histórico registra `COUNTER_PROPOSAL_ACCEPTED`.

Rejeitar contraproposta:

```http
PATCH /api/service-requests/{id}/reject-counter
Content-Type: application/json

{ "reason": "Motivo" }
```

Efeitos:

- Campos de contraproposta são limpos.
- Status permanece em negociação.
- Histórico registra `COUNTER_PROPOSAL_REJECTED`.

#### 5. Criação do pagamento

```text
Frontend -> POST /api/payments
-> PaymentController.createPayment()
-> PaymentService.createPayment()
-> valida ServiceRequest ACCEPTED
-> calcula taxas
-> cria cobrança PIX/cartão
-> salva Payment
-> registra PAYMENT_CREATED
-> retorna PaymentResponse
```

Regras:

- A solicitação precisa estar `ACCEPTED`.
- O cliente autenticado precisa ser o dono da solicitação.
- O valor precisa corresponder ao valor negociado.
- Não pode existir outro pagamento para a mesma solicitação.
- Em PIX, o backend cria cobrança no Asaas e retorna dados para QR Code.

#### 6. Confirmação do pagamento

```text
Asaas -> webhook -> AsaasWebhookService
-> Payment COMPLETED
-> PaymentStatusHistory
-> FreelancerWalletService.addPendingBalance()
-> ServiceRequest IN_PROGRESS
-> chatEnabled true
-> ServiceRequestHistory PAYMENT_CONFIRMED
-> WebSocket/Notification
```

A confirmação de pagamento é o divisor de fase entre proposta aceita e serviço em execução. Antes do pagamento, a solicitação fica `ACCEPTED`; depois do pagamento confirmado, passa para `IN_PROGRESS`.

#### 7. Entrega e conclusão

Entrega pelo freelancer:

```http
PATCH /api/service-requests/{id}/complete
```

Se o usuário autenticado é o freelancer e a solicitação está `IN_PROGRESS`, o status muda para `DELIVERED`.

Conclusão pelo cliente:

```http
PATCH /api/service-requests/{id}/complete
```

Se o usuário autenticado é o cliente e a solicitação está `DELIVERED`, o status muda para `COMPLETED` e o saldo do freelancer pode ser liberado.

## Frontend

### Organização Geral

O frontend usa Vue 3, Pinia e Axios. O módulo é dividido entre telas do cliente, telas do freelancer e telas de pagamento. A comunicação HTTP usa a instância `api` de `frontend/src/api/config/axios.ts`, que adiciona automaticamente o token JWT no header `Authorization`.

Arquivos centrais:

| Arquivo | Responsabilidade |
|---|---|
| `frontend/src/api/config/axios.ts` | Configura Axios com `baseURL` em `/api`, token JWT e tratamento de `401`. |
| `frontend/src/stores/clientRequestStore.ts` | Estado do cliente: criar, listar, aceitar, rejeitar, negociar, concluir e carregar histórico. |
| `frontend/src/stores/proposalStore.ts` | Estado do freelancer: listar solicitações recebidas, responder, rejeitar, aceitar/rejeitar contraproposta e concluir entrega. |
| `frontend/src/api/services/paymentService.ts` | Chamadas de pagamento. |
| `frontend/src/composables/useServiceRequestWebSocket.ts` | WebSocket de eventos da solicitação. |
| `frontend/src/composables/usePaymentWebSocket.ts` | WebSocket de eventos de pagamento. |

### Páginas Envolvidas

#### Páginas do cliente

| Rota | Página/componente | Papel |
|---|---|---|
| `/provider-profile/:providerId/service/:serviceId` | `ServiceDetailPage.vue` | Exibe o serviço e abre o formulário de solicitação. |
| `/my-requests` | `ClienteRequestsPage.vue` | Lista solicitações do cliente, filtros e criação manual. |
| `/cliente-request-detail` | `ClienteRequestDetailPage.vue` | Detalha proposta, histórico, anexos e ações de aceite/rejeição/contraproposta. |
| `/checkout` | `pages/Checkout/index.vue` e `components/payment/CheckoutPage.vue` | Carrega proposta aceita e executa pagamento. |
| `/payment-success` | `PaymentSuccessPage.vue` | Exibe confirmação e acesso a recibo. |
| `/cliente-projects` | `ClienteProjectsPage.vue` | Lista serviços em execução/concluídos. |
| `/cliente-project-detail` | `ClienteProjectDetailPage.vue` | Acompanhamento detalhado após pagamento. |
| `/cliente-delivery-approval` | `ClienteDeliveryApproval/index.vue` | Aprovação da entrega, solicitação de ajustes e conclusão pelo cliente. |
| `/rate-freelancer` | `RateFreelancerPage.vue` | Avaliação após conclusão. |

#### Páginas do freelancer

| Rota | Página/componente | Papel |
|---|---|---|
| `/freelancer-proposals` | `FreelancerProposalsPage.vue` | Lista solicitações recebidas e negociações. |
| `/freelancer-proposal-detail` | `FreelancerRequestDetailPage.vue` | Envia proposta, responde contraproposta, atualiza marcos e marca entrega. |
| `/freelancer-deliveries` | `FreelancerProjectsPage.vue` | Lista entregas/projetos em execução. |
| `/freelancer-project-detail` | `FreelancerProjectDetailPage.vue` | Acompanhamento detalhado pelo freelancer. |
| `/freelancer-wallet` | `FreelancerWalletPage.vue` | Carteira e recebimentos do freelancer. |

### Componentes Principais

#### `ServiceDetailPage.vue`

Responsável por iniciar a contratação a partir de um serviço publicado.

Métodos principais:

| Método | Responsabilidade |
|---|---|
| `handleRequestService()` | Abre o formulário/modal de solicitação. |
| `handleSendQuote()` | Valida dados, envia anexos e cria a solicitação. |
| `uploadImages(...)` | Envia arquivos e retorna URLs para persistência. |

Regras aplicadas na interface:

- Valida descrição, prazo e data desejada.
- Impede data no passado.
- Exige endereço para `PRESENCIAL` e `HIBRIDO`.
- Usa consulta de CEP quando necessário.
- Limita anexos do cliente.
- Envia `serviceId` e `freelancerId` para o backend validar o vínculo com o serviço.

Requisição principal:

```ts
api.post('/service-requests', {
  serviceName,
  category,
  description,
  deadline,
  desiredDate,
  workModel,
  address,
  serviceId,
  freelancerId,
  attachments
})
```

#### `ClienteRequestsPage.vue`

Responsável por listar e gerenciar solicitações do cliente.

Métodos relevantes:

| Método | Responsabilidade |
|---|---|
| `handleCreateRequest()` | Valida formulário de criação manual e chama a store. |
| `handleServiceRequestNotification(...)` | Processa eventos em tempo real da contratação. |
| `handleCancelRequest(...)` | Cancela solicitação. |
| `handleViewDetails(...)` | Navega para a tela de detalhes. |

A tela usa `useClientRequestStore` para carregar solicitações e `useServiceRequestWebSocket` para receber eventos como nova proposta, aceite, cancelamento e conclusão.

#### `ClienteRequestDetailPage.vue`

Tela de decisão do cliente sobre a proposta recebida.

Métodos relevantes:

| Método | Responsabilidade |
|---|---|
| `handleAcceptResponse()` | Aceita a proposta do freelancer. |
| `handleRejectResponse()` | Rejeita a proposta com motivo. |
| `handleSendCounterProposal()` | Envia contraproposta. |
| `handleGoToCheckout()` | Redireciona para `/checkout?requestId=...`. |

A página também exibe `ServiceRequestTimeline`, anexos, dados da proposta, marcos, chat e ações condicionadas ao status.

#### `ClienteDeliveryApproval/index.vue`

Tela em que o cliente aprova a entrega feita pelo freelancer ou solicita ajustes.

| Método | Responsabilidade |
|---|---|
| `handleApprove()` | Chama `clientRequestStore.completeService` e conclui a contratação. |
| `handleRequestAdjustments(reason)` | Registra solicitação de ajustes para a entrega. |
| `handleGoToChat()` | Redireciona o cliente para o chat da solicitação. |

#### `FreelancerProposalsPage.vue`

Lista as solicitações recebidas pelo freelancer.

Métodos relevantes:

| Método | Responsabilidade |
|---|---|
| `handleServiceRequestNotification(...)` | Trata eventos de solicitação em tempo real. |
| `handlePaymentNotification(...)` | Trata eventos de pagamento. |
| `handleViewDetails(...)` | Abre detalhe da solicitação. |
| `handleRejectProposal()` | Rejeita uma solicitação. |
| `handleArchiveProposal()` | Arquiva/cancela no fluxo local. |

#### `FreelancerRequestDetailPage.vue`

Tela principal do freelancer para negociar e executar a contratação.

Métodos relevantes:

| Método | Responsabilidade |
|---|---|
| `handlePrepareProposal()` | Valida/prepara dados antes da confirmação. |
| `handleConfirmSendProposal()` | Faz upload de anexos e chama `proposalStore.sendResponse`. |
| `handleRejectProposal()` | Rejeita solicitação. |
| `handleAcceptCounterProposal()` | Aceita contraproposta do cliente. |
| `handleRejectCounterProposal()` | Rejeita contraproposta com motivo. |
| `handleUpdateMilestoneStatus(...)` | Atualiza status de marco. |
| `handleCompleteProposal()` | Marca serviço como entregue. |
| `handleSendMessage(...)` | Envia mensagem no fluxo de UI/chat. |

#### `CheckoutPage.vue`

Responsável pelo pagamento da proposta aceita.

Métodos relevantes:

| Método | Responsabilidade |
|---|---|
| `togglePaymentMethod(method)` | Seleciona/alternar método de pagamento. |
| `handleCpfInput(...)` | Formata CPF para PIX. |
| `generatePixQrCode()` | Valida CPF, cria pagamento PIX quando necessário e busca o QR Code. |
| `loadExistingPixQrCode(paymentId)` | Recarrega QR Code de pagamento existente. |
| `checkExistingPayment()` | Verifica se já existe pagamento para a solicitação. |
| `handleConfirmPayment()` | Confirma pagamento para o método selecionado. |
| `handlePaymentNotification(...)` | Processa confirmação/falha/cancelamento em tempo real. |
| `simulatePaymentConfirmation()` | Chama endpoint de simulação em desenvolvimento. |

#### `PaymentSuccessPage.vue`

Responsável por apresentar o resultado do pagamento.

| Método | Responsabilidade |
|---|---|
| `handleBackToDashboard()` | Retorna ao dashboard do cliente. |
| `handleDownloadReceipt()` | Busca URL de recibo/comprovante e abre em nova aba. |

### Stores e Métodos de Estado

#### `clientRequestStore.ts`

Store usada nas telas do cliente.

| Método | Requisição | Responsabilidade |
|---|---|---|
| `loadRequests()` | `GET /service-requests/my` | Carrega solicitações do cliente. |
| `loadRequestById(id)` | `GET /service-requests/{id}` | Carrega detalhes de uma solicitação. |
| `createRequest(requestData)` | `POST /service-requests` | Cria nova solicitação. |
| `cancelRequest(id, reason)` | `PATCH /service-requests/{id}/status` | Cancela solicitação. |
| `acceptFreelancerResponse(id)` | `PATCH /service-requests/{id}/status` | Aceita proposta do freelancer. |
| `rejectFreelancerResponse(id, reason)` | `PATCH /service-requests/{id}/status` | Rejeita proposta. |
| `sendCounterProposal(id, data)` | `PATCH /service-requests/{id}/counter-proposal` | Envia contraproposta. |
| `completeService(id)` | `PATCH /service-requests/{id}/complete` | Confirma conclusão do serviço. |
| `loadHistory(requestId)` | `GET /service-requests/{id}/history` | Carrega histórico. |
| `mapApiStatusToLocal(status)` | Local | Converte status do backend para o formato da UI. |
| `mapRequestResponse(req)` | Local | Converte resposta da API para o modelo interno. |

Mapeamento de status do backend para a interface:

| Backend | Frontend |
|---|---|
| `PENDING` | `pending` |
| `SENT` | `sent` |
| `NEGOTIATING` | `negotiating` |
| `ACCEPTED` | `accepted` |
| `IN_PROGRESS` | `in_progress` |
| `DELIVERED` | `delivered` |
| `REJECTED` | `rejected` |
| `COMPLETED` | `completed` |
| `CANCELLED` | `cancelled` |

#### `proposalStore.ts`

Store usada nas telas do freelancer.

| Método | Requisição | Responsabilidade |
|---|---|---|
| `loadProposals()` | `GET /service-requests/received` | Carrega solicitações recebidas. |
| `getProposalById(id)` | Local | Busca uma proposta na store. |
| `sendResponse(id, response)` | `PATCH /service-requests/{id}/respond` | Envia proposta do freelancer. |
| `rejectProposal(id, reason)` | `PATCH /service-requests/{id}/status` | Rejeita solicitação. |
| `completeProposal(id)` | `PATCH /service-requests/{id}/complete` | Marca serviço como entregue. |
| `archiveProposal(id)` | `PATCH /service-requests/{id}/status` | Arquiva/cancela conforme fluxo local. |
| `updateMilestoneStatus(proposalId, milestoneId, status)` | `PATCH /service-requests/{id}/milestones/{milestoneId}` | Atualiza marco de execução. |
| `loadHistory(proposalId)` | `GET /service-requests/{id}/history` | Carrega histórico. |
| `acceptCounterProposal(proposalId)` | `PATCH /service-requests/{id}/accept-counter` | Aceita contraproposta. |
| `rejectCounterProposal(proposalId, reason)` | `PATCH /service-requests/{id}/reject-counter` | Rejeita contraproposta. |

#### `paymentService.ts`

Serviço de frontend para chamadas de pagamento.

| Método | Requisição | Responsabilidade |
|---|---|---|
| `createPayment(data)` | `POST /payments` | Cria pagamento. |
| `getPayment(paymentId)` | `GET /payments/{id}` | Busca pagamento. |
| `getMyPayments()` | `GET /payments/my` | Lista pagamentos do usuário. |
| `checkPixPaymentStatus(paymentId)` | `GET /payments/{id}/status` | Consulta status do pagamento. |
| `getReceiptUrl(paymentId)` | `GET /payments/{id}/receipt-url` | Busca URL de comprovante/recibo no Asaas. |
| `requestRefund(paymentId, reason)` | `POST /payments/{id}/refund` | Método preparado no frontend; não faz parte do fluxo principal documentado. |

### Requisições Axios e Fetch

O arquivo `frontend/src/api/config/axios.ts` cria uma instância Axios com:

- `baseURL` montado como `${VITE_API_URL}/api`.
- Header padrão `Content-Type: application/json`.
- Interceptor de requisição para adicionar `Authorization: Bearer <token>`.
- Interceptor de resposta para limpar sessão e redirecionar para login em caso de `401`.

Principais chamadas do módulo:

| Origem | Chamada | Uso |
|---|---|---|
| `ServiceDetailPage.vue` | `POST /service-requests` | Criar solicitação a partir de serviço publicado. |
| `clientRequestStore.ts` | `GET /service-requests/my` | Listar solicitações do cliente. |
| `clientRequestStore.ts` | `GET /service-requests/{id}` | Detalhar solicitação. |
| `clientRequestStore.ts` | `PATCH /service-requests/{id}/status` | Aceitar, rejeitar ou cancelar. |
| `clientRequestStore.ts` | `PATCH /service-requests/{id}/counter-proposal` | Enviar contraproposta. |
| `clientRequestStore.ts` | `PATCH /service-requests/{id}/complete` | Confirmar conclusão. |
| `clientRequestStore.ts` | `GET /service-requests/{id}/history` | Carregar linha do tempo. |
| `proposalStore.ts` | `GET /service-requests/received` | Listar solicitações do freelancer. |
| `proposalStore.ts` | `PATCH /service-requests/{id}/respond` | Enviar proposta do freelancer. |
| `proposalStore.ts` | `PATCH /service-requests/{id}/accept-counter` | Aceitar contraproposta. |
| `proposalStore.ts` | `PATCH /service-requests/{id}/reject-counter` | Rejeitar contraproposta. |
| `proposalStore.ts` | `PATCH /service-requests/{id}/milestones/{milestoneId}` | Atualizar marco. |
| `CheckoutPage.vue` | `GET /payments/service-request/{serviceRequestId}` | Verificar pagamento existente. |
| `CheckoutPage.vue` | `POST /payments` | Criar pagamento. |
| `CheckoutPage.vue` | `GET /payments/{id}/pix-qrcode` | Buscar QR Code PIX. |
| `CheckoutPage.vue` | `POST /payments/{id}/simulate-confirmation` | Simular confirmação em desenvolvimento. |
| `paymentService.ts` | `GET /payments/{id}` | Buscar pagamento para tela de sucesso. |
| `paymentService.ts` | `GET /payments/{id}/receipt-url` | Buscar comprovante. |

Observação: `CheckoutPage.vue` usa `fetch` diretamente em alguns pontos do fluxo PIX, enquanto outras partes usam a instância Axios. Em ambos os casos, o objetivo é consumir as mesmas rotas REST do backend.

### WebSocket no Frontend

#### `useServiceRequestWebSocket.ts`

Esse composable usa SockJS e STOMP para conectar ao WebSocket do backend e assinar:

```text
/topic/service-request/{userEmail}
```

Ao receber uma mensagem, ele dispara um evento global no navegador:

```ts
window.dispatchEvent(
  new CustomEvent('service-request-notification', { detail: notification })
)
```

Telas como `ClienteRequestsPage.vue`, `ClienteRequestDetailPage.vue`, `FreelancerProposalsPage.vue` e `FreelancerRequestDetailPage.vue` escutam esse evento para atualizar listas e detalhes.

Eventos tratados no módulo:

- `NEW_SERVICE_REQUEST`
- `NEW_PROPOSAL`
- `COUNTER_PROPOSAL`
- `COUNTER_PROPOSAL_REJECTED`
- `PROPOSAL_ACCEPTED`
- `SERVICE_REQUEST_CANCELLED`
- `SERVICE_REQUEST_STATUS_CHANGED`
- `SERVICE_COMPLETED_REVIEW_PENDING`

#### `usePaymentWebSocket.ts`

Esse composable assina:

```text
/topic/payment/{userEmail}
```

Ao receber atualização, dispara:

```ts
window.dispatchEvent(
  new CustomEvent('payment-notification', { detail: notification })
)
```

O `CheckoutPage.vue` usa esse evento para identificar pagamento confirmado, falha ou cancelamento. O `FreelancerProposalsPage.vue` também usa notificações de pagamento para atualizar solicitações que passaram para execução.

### Fluxo do Usuário

#### Fluxo do cliente

1. O cliente acessa um serviço em `/provider-profile/:providerId/service/:serviceId`.
2. Clica para solicitar o serviço.
3. Preenche descrição, prazo, modelo de trabalho, data desejada, endereço quando necessário e anexos.
4. O frontend chama `POST /service-requests`.
5. A solicitação aparece em `/my-requests` com status `pending`.
6. O freelancer envia uma proposta.
7. O cliente recebe notificação em tempo real e abre `/cliente-request-detail`.
8. O cliente pode aceitar, rejeitar ou enviar contraproposta.
9. Ao aceitar, a solicitação muda para `accepted`.
10. O cliente é direcionado para `/checkout?requestId=...`.
11. O pagamento é criado em `/api/payments`.
12. Após confirmação, o status muda para `in_progress` e o chat é liberado.
13. Quando o freelancer entrega, o cliente valida e confirma a conclusão.
14. O status muda para `completed` e o fluxo pode seguir para avaliação.

#### Fluxo do freelancer

1. O freelancer acessa `/freelancer-proposals`.
2. O frontend chama `GET /service-requests/received`.
3. O freelancer abre uma solicitação em `/freelancer-proposal-detail`.
4. Analisa descrição, anexos, prazo, endereço e modelo de trabalho.
5. Envia proposta com valor, prazo, escopo, observações, anexos e marcos.
6. O backend salva a proposta e muda status para `NEGOTIATING`.
7. Se o cliente aceitar, o freelancer aguarda pagamento.
8. Se o cliente fizer contraproposta, o freelancer aceita ou rejeita.
9. Após pagamento confirmado, a solicitação muda para `IN_PROGRESS`.
10. O freelancer executa o serviço e pode atualizar marcos.
11. Ao finalizar, marca a entrega pela rota `/complete`.
12. A solicitação muda para `DELIVERED`.
13. Após confirmação do cliente, muda para `COMPLETED` e o saldo é liberado.

#### Fluxo de pagamento

1. O checkout carrega a solicitação pelo ID e valida se ela está `ACCEPTED`.
2. O valor negociado é obtido de `responseValue` ou `counterValue`.
3. O cliente escolhe método de pagamento.
4. Para PIX, informa CPF e o frontend chama `POST /api/payments`.
5. O backend cria cobrança no Asaas e retorna dados do pagamento.
6. O frontend busca QR Code em `GET /api/payments/{id}/pix-qrcode`.
7. O cliente paga usando QR Code ou copia e cola.
8. O Asaas confirma via webhook.
9. O backend muda pagamento para `COMPLETED` e solicitação para `IN_PROGRESS`.
10. O frontend recebe `payment-notification` e redireciona para `/payment-success`.

## Principais Métodos do Módulo

### Backend

| Classe | Métodos principais |
|---|---|
| `ServiceRequestController` | `create`, `myRequests`, `receivedRequests`, `getById`, `updateStatus`, `respondToRequest`, `counterProposal`, `acceptCounterProposal`, `rejectCounterProposal`, `updateMilestoneStatus`, `getHistory`, `completeService` |
| `ServiceRequestService` | `create`, `getByContractorEmail`, `getByFreelancerEmail`, `getById`, `updateStatus`, `respondToRequest`, `counterProposal`, `acceptCounterProposal`, `rejectCounterProposal`, `updateMilestoneStatus`, `completeService`, `addFreelancerAttachments`, `addContractorAttachments`, `toResponse` |
| `ServiceRequestHistoryService` | `record`, `getHistory`, `toResponse` |
| `PaymentController` | `createPayment`, `getPayment`, `getMyPayments`, `checkPaymentStatus`, `updatePaymentStatus`, `getPixQrCode`, `getPaymentByServiceRequestId`, `getPaymentHistory`, `getReceiptUrl`, `simulatePaymentConfirmation` |
| `PaymentService` | `createPayment`, `processPixPayment`, `processCreditCardPayment`, `getPaymentById`, `getPaymentByServiceRequestId`, `getMyPayments`, `checkAsaasPaymentStatus`, `getPixQrCode`, `getPaymentHistory`, `getReceiptUrl`, `simulatePaymentConfirmation`, `mapToResponse` |
| `AsaasWebhookService` | Processa eventos de pagamento recebido/confirmado e sincroniza `Payment` com `ServiceRequest`. |
| `FreelancerWalletService` | `addPendingBalance`, `releasePendingBalance`, `getOrCreateWallet`, `releasePendingToAvailable` |
| `WebSocketService` | `notifyNewServiceRequest`, `notifyServiceRequestStatusChange`, `notifyNewProposal`, `notifyCounterProposal`, `notifyCounterProposalRejected`, `notifyProposalAccepted`, `notifyServiceRequestCancelled`, `notifyPixPaymentConfirmed`, `notifyPaymentFailed`, `notifyReviewPending` |

### Frontend

| Arquivo | Métodos principais |
|---|---|
| `ServiceDetailPage.vue` | `handleRequestService`, `handleSendQuote`, `uploadImages` |
| `ClienteRequestsPage.vue` | `handleCreateRequest`, `handleServiceRequestNotification`, `handleCancelRequest`, `handleViewDetails` |
| `ClienteRequestDetailPage.vue` | `handleAcceptResponse`, `handleRejectResponse`, `handleSendCounterProposal`, `handleGoToCheckout` |
| `ClienteDeliveryApproval/index.vue` | `handleApprove`, `handleRequestAdjustments`, `handleGoToChat` |
| `clientRequestStore.ts` | `loadRequests`, `loadRequestById`, `createRequest`, `cancelRequest`, `acceptFreelancerResponse`, `rejectFreelancerResponse`, `sendCounterProposal`, `completeService`, `loadHistory`, `mapApiStatusToLocal`, `mapRequestResponse` |
| `FreelancerProposalsPage.vue` | `handleServiceRequestNotification`, `handlePaymentNotification`, `handleViewDetails`, `handleRejectProposal`, `handleArchiveProposal` |
| `FreelancerRequestDetailPage.vue` | `handlePrepareProposal`, `handleConfirmSendProposal`, `handleRejectProposal`, `handleAcceptCounterProposal`, `handleRejectCounterProposal`, `handleUpdateMilestoneStatus`, `handleCompleteProposal`, `handleSendMessage` |
| `proposalStore.ts` | `loadProposals`, `sendResponse`, `rejectProposal`, `completeProposal`, `updateMilestoneStatus`, `loadHistory`, `acceptCounterProposal`, `rejectCounterProposal` |
| `CheckoutPage.vue` | `togglePaymentMethod`, `handleCpfInput`, `generatePixQrCode`, `loadExistingPixQrCode`, `checkExistingPayment`, `handleConfirmPayment`, `handlePaymentNotification`, `simulatePaymentConfirmation` |
| `paymentService.ts` | `createPayment`, `getPayment`, `getMyPayments`, `checkPixPaymentStatus`, `getReceiptUrl` |

## Considerações Técnicas

### Controle de estado

O estado da contratação é controlado principalmente por `service_requests.status`. Esse campo determina quais ações são válidas em cada momento.

| Status | Ações comuns |
|---|---|
| `PENDING` | Freelancer pode responder ou rejeitar; cliente pode cancelar. |
| `NEGOTIATING` | Cliente pode aceitar, rejeitar ou enviar contraproposta; freelancer pode aceitar/rejeitar contraproposta. |
| `ACCEPTED` | Cliente pode pagar. |
| `IN_PROGRESS` | Freelancer executa, atualiza marcos e entrega; chat fica habilitado. |
| `DELIVERED` | Cliente revisa e conclui. |
| `COMPLETED` | Serviço finalizado e saldo liberado. |
| `REJECTED`/`CANCELLED` | Fluxo encerrado sem execução. |

### Auditoria

Toda mudança relevante é registrada em `service_request_history` ou `payment_status_history`. Isso permite exibir linha do tempo no frontend e fornece rastreabilidade para suporte, auditoria e análise acadêmica.

### Pagamento como divisor de fase

O aceite da proposta não inicia automaticamente o serviço. O sistema separa claramente:

```text
ACCEPTED = proposta aceita, aguardando pagamento
IN_PROGRESS = pagamento confirmado, serviço em execução
```

Essa separação reduz risco operacional, pois o freelancer só inicia formalmente após confirmação financeira.

### Liberação financeira

O pagamento confirmado adiciona o valor do freelancer como saldo pendente. A liberação para saldo disponível ocorre quando o cliente confirma a conclusão do serviço. Assim, o sistema cria uma camada de segurança entre pagamento, execução e recebimento.

### Compatibilidade com dados legados

Alguns campos e métodos indicam compatibilidade com versões anteriores, como `budget` e `legacyLowValueAllowed`. A regra atual centraliza o valor contratado em `responseValue` e `counterValue`, mas mantém mecanismos para lidar com registros antigos.

