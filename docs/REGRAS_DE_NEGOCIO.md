# Regras de Negócio do ServiJá

Documento gerado a partir do PDF `regras de negócio servija.pdf` e da revisão do código atual do backend, frontend e documentação técnica do projeto.

Observação: o PDF original contém numeração duplicada e lacunas. As regras abaixo foram renumeradas em sequência, conforme o padrão `RN1`, `RN2`, `RN3...`.

Status usados:

- `(implementado)`: regra encontrada no código atual.
- `(parcialmente implementado)`: existe suporte parcial ou comportamento equivalente com lacunas.
- `(não implementado)`: não foi encontrada implementação correspondente.

## Regras

| RN | Regra de negócio | Origem | Observação |
| --- | --- | --- | --- |
| RN1 | O sistema deve permitir o cadastro de usuários mediante fornecimento de nome completo, e-mail, CPF e senha. **(implementado)** | Documento | Também exige telefone, tipo de usuário e aceite dos termos. |
| RN2 | Cada e-mail cadastrado deve ser único e não pode estar vinculado a mais de um perfil ativo. **(implementado)** | Documento | A unicidade atual é global, inclusive para contas inativas ou soft-deleted. |
| RN3 | O acesso às funcionalidades restritas exige autenticação válida com e-mail e senha criptografada. **(implementado)** | Documento | Rotas protegidas exigem JWT Bearer. |
| RN4 | Senhas devem ser armazenadas em formato seguro usando BCrypt. **(implementado)** | Documento | `SecurityConfig` registra `BCryptPasswordEncoder`. |
| RN5 | A redefinição de senha deve ocorrer por link enviado ao e-mail cadastrado, com validade limitada a 1 hora. **(parcialmente implementado)** | Documento | O sistema usa código de 6 dígitos enviado por e-mail, com validade de 30 minutos, não link de 1 hora. |
| RN6 | O sistema deve possuir três tipos de usuário: Cliente, Freelancer e Administrador, cada um com permissões específicas. **(implementado)** | Documento | Implementado como `CONTRACTOR`, `FREELANCER` e `ADMIN`. |
| RN7 | O Cliente pode solicitar orçamentos, negociar propostas, realizar pagamentos e avaliar freelancers após a conclusão dos serviços. **(implementado)** | Documento | Avaliação só é permitida para solicitação concluída. |
| RN8 | O Freelancer deve cadastrar informações profissionais completas, incluindo especialidades, faixa de preço, portfólio e descrição. **(parcialmente implementado)** | Documento | Existem perfil profissional, serviços e portfólio, mas o preenchimento completo não é obrigatório como regra de bloqueio. |
| RN9 | O Administrador deve ter acesso à gestão da plataforma, podendo editar, suspender ou excluir contas e serviços conforme necessidade. **(parcialmente implementado)** | Documento | Há gestão de usuários, status, KYC e categorias; não há endpoint administrativo direto para editar ou excluir serviços. |
| RN10 | Cada serviço cadastrado por um freelancer deve estar vinculado a uma categoria existente. **(implementado)** | Documento | Criação e atualização resolvem categoria/subcategoria ativas pelo catálogo. |
| RN11 | O Administrador é o único autorizado a criar, editar ou excluir categorias de serviço. **(parcialmente implementado)** | Documento | Apenas admin cria/edita/arquiva/restaura categorias; exclusão definitiva não foi implementada. |
| RN12 | O Cliente pode enviar solicitações de orçamento a múltiplos freelancers simultaneamente. **(parcialmente implementado)** | Documento | A API cria uma solicitação por serviço/freelancer; não há endpoint de envio em lote para vários freelancers. |
| RN13 | O Freelancer pode aceitar, recusar ou apresentar contraproposta a uma solicitação recebida. **(parcialmente implementado)** | Documento | Freelancer pode recusar, enviar orçamento e aceitar/recusar contraproposta do cliente; não há aceite direto da solicitação original como ação separada. |
| RN14 | Cada solicitação de serviço deve conter descrição detalhada, prazo de entrega e demais condições específicas do serviço. **(implementado)** | Documento | Descrição e prazo são obrigatórios; proposta do freelancer inclui valor, prazo, escopo, anexos e marcos. |
| RN15 | O sistema deve registrar e manter o histórico de todas as interações realizadas entre cliente e freelancer durante a negociação. **(implementado)** | Documento | Há histórico para abertura, proposta, contraproposta, aceite, rejeição, pagamento, entrega, conclusão e ajustes. |
| RN16 | Propostas recusadas ou expiradas não podem ser reabertas, devendo ser criada uma nova solicitação. **(não implementado)** | Documento | Não há status `EXPIRED` nem bloqueio geral impedindo transições posteriores de propostas recusadas. |
| RN17 | As avaliações devem utilizar notas e comentários descritivos sobre o serviço prestado. **(implementado)** | Documento | Nota de 1 a 5 é obrigatória; comentário e tags são aceitos. |
| RN18 | O sistema deve gerar automaticamente resumos das avaliações com auxílio de inteligência artificial. **(implementado)** | Documento | Resumo via Gemini é gerado quando o freelancer possui ao menos 3 avaliações. |
| RN19 | Freelancers não podem alterar ou excluir avaliações recebidas. **(implementado)** | Documento | Não há endpoints para freelancer editar ou excluir avaliações recebidas. |
| RN20 | Avaliações com conteúdo inadequado ou ofensivo devem ser analisadas e, se necessário, removidas pelo administrador. **(não implementado)** | Documento | Não foram encontrados fluxo de moderação, denúncia ou remoção administrativa de avaliações. |
| RN21 | A média de avaliações deve influenciar a ordenação dos resultados de busca de freelancers. **(parcialmente implementado)** | Documento | Avaliações influenciam destaques da home; busca pública principal lista serviços e não ordena freelancers por média no backend. |
| RN22 | O Administrador é responsável por gerenciar denúncias, avaliações suspeitas e atividades irregulares. **(não implementado)** | Documento | A interface de chat menciona denúncia, mas não foi encontrado backend de denúncias/moderação. |
| RN23 | O sistema deve gerar logs de todas as ações administrativas, incluindo data, hora e identificador do responsável. **(não implementado)** | Documento | Existem logs de aplicação, mas não há auditoria administrativa persistida e consultável. |
| RN24 | Todo o tráfego de dados deve ocorrer sob protocolo seguro HTTPS. **(parcialmente implementado)** | Documento | O backend aceita origens HTTPS e cabeçalhos de proxy, mas não força HTTPS por configuração própria. |
| RN25 | O sistema deve estar em conformidade com a LGPD. **(parcialmente implementado)** | Documento | Há exclusão de conta e soft delete administrativo, mas não há fluxo completo de consentimento, exportação, retenção e anonimização. |
| RN26 | A IA deve operar de forma imparcial, sem alterar o conteúdo original das avaliações. **(implementado)** | Documento | Resumos são derivados das avaliações e o comentário original salvo não é alterado pelo resumo. |
| RN27 | O administrador pode ajustar os parâmetros do modelo de IA conforme a evolução do sistema. **(não implementado)** | Documento | Modelo e chave são configurados por ambiente; não há ajuste por painel administrativo. |
| RN28 | O sistema deve supervisionar periodicamente o funcionamento da IA para garantir coerência e ausência de vieses. **(não implementado)** | Documento | Não foi encontrado job, revisão periódica ou monitoramento de viés da IA. |
| RN29 | Logs devem ser armazenados por tempo determinado e acessíveis apenas pelo administrador. **(não implementado)** | Documento | Não há política de retenção nem tela/API administrativa de logs. |
| RN30 | O sistema deve manter disponibilidade mínima de 99%. **(não implementado)** | Documento | Não há configuração de SLA, health check operacional ou monitoramento de disponibilidade no repositório. |
| RN31 | O cadastro público de usuários administradores deve ser bloqueado. **(implementado)** | Código | `AuthService.register` rejeita `UserType.ADMIN`. |
| RN32 | CPF deve ser obrigatório, conter 11 dígitos e ser único. **(implementado)** | Código | Há validação no DTO, endpoint de disponibilidade e checagem de duplicidade. |
| RN33 | Telefone deve ser obrigatório no cadastro e conter 10 ou 11 dígitos. **(implementado)** | Código | Regra validada em `RegisterRequest`. |
| RN34 | Senhas devem ter de 8 a 256 caracteres e conter maiúscula, minúscula, número e caractere especial. **(implementado)** | Código | Regra aplicada no cadastro, redefinição e alteração autenticada. |
| RN35 | O usuário deve aceitar os termos e condições para concluir o cadastro. **(implementado)** | Código | `RegisterRequest.acceptTerms` exige `true`. |
| RN36 | Após o cadastro, o sistema deve enviar código de verificação por e-mail. **(implementado)** | Código | Código de 6 dígitos expira em 10 minutos. |
| RN37 | Tentativas inválidas de verificação de e-mail devem ser limitadas. **(implementado)** | Código | Máximo de 5 tentativas e bloqueio de 30 minutos. |
| RN38 | Tentativas inválidas de recuperação de senha devem ser limitadas. **(implementado)** | Código | Máximo de 5 tentativas e bloqueio de 30 minutos. |
| RN39 | A recuperação de senha não deve revelar se o e-mail existe. **(implementado)** | Código | `forgotPassword` retorna sem erro quando o e-mail não existe. |
| RN40 | Login comum não deve autenticar administradores e login administrativo não deve autenticar usuários comuns. **(implementado)** | Código | `login` rejeita `ADMIN`; `loginAdmin` exige `ADMIN`. |
| RN41 | Contas inativas ou suspensas não podem realizar login. **(implementado)** | Código | Login rejeita usuários com `active=false`. |
| RN42 | Alteração de senha autenticada deve exigir a senha atual correta. **(implementado)** | Código | `UserService.changePassword` valida a senha atual com BCrypt. |
| RN43 | Exclusão de conta pelo próprio usuário deve exigir confirmação por senha. **(implementado)** | Código | `UserService.deleteAccount` valida senha antes da exclusão. |
| RN44 | Administradores podem redefinir a senha de usuários por envio de código ao e-mail. **(implementado)** | Código | Código expira em 30 minutos. |
| RN45 | Administradores podem suspender, reativar, editar e excluir usuários não administradores. **(implementado)** | Código | Suporta soft delete e hard delete para usuários não admin. |
| RN46 | Administradores não podem bloquear ou excluir outros administradores. **(implementado)** | Código | `AdminUserService` bloqueia essas operações. |
| RN47 | Administradores não podem bloquear a própria conta. **(implementado)** | Código | `updateStatus` valida o admin autenticado. |
| RN48 | Administradores podem alterar o status de KYC de freelancers. **(implementado)** | Código | Endpoint `/api/admin/users/{id}/kyc`. |
| RN49 | Administradores podem impersonar usuários não administradores por tempo limitado. **(implementado)** | Código | Token de impersonação expira em 30 minutos. |
| RN50 | Um administrador inicial pode ser criado por bootstrap controlado por variáveis de ambiente. **(implementado)** | Código | `AdminBootstrapService` cria admin com senha BCrypt quando habilitado. |
| RN51 | Perfil de freelancer deve suportar categoria, subcategoria principal e múltiplas subcategorias. **(implementado)** | Código | Há entidade de relacionamento `freelancer_subcategories`. |
| RN52 | Cidades de atendimento do freelancer devem pertencer ao estado informado e não podem ser duplicadas. **(implementado)** | Código | Validação usa IBGE e normalização de cidade/UF. |
| RN53 | Cidades de atendimento podem ser geocodificadas para busca por proximidade. **(implementado)** | Código | `GeocodingService` e coordenadas em `service_cities`. |
| RN54 | Busca de freelancers próximos deve respeitar raio, coordenadas e filtro opcional de categoria. **(implementado)** | Código | Resultados próximos são ordenados por distância. |
| RN55 | Apenas freelancers podem acessar e alterar o próprio perfil profissional. **(implementado)** | Código | `FreelancerService.getCurrentFreelancer` valida o tipo de usuário. |
| RN56 | Biografia refinada por IA deve preservar informações originais e respeitar limite de tamanho. **(implementado)** | Código | Prompt orienta preservação e limita resposta a 1000 caracteres. |
| RN57 | O freelancer só pode criar, editar, ativar, desativar ou excluir serviços próprios. **(implementado)** | Código | `ServiceService` verifica propriedade antes de alterar. |
| RN58 | Serviços públicos devem listar apenas anúncios ativos. **(implementado)** | Código | Endpoints públicos usam filtros `isActive=true`. |
| RN59 | A busca pública de serviços deve filtrar por texto, categoria e subcategoria. **(implementado)** | Código | Texto é aplicado sobre título/descrição; categoria/subcategoria usam slugs. |
| RN60 | Resultados públicos de serviços devem trazer métricas de avaliação e faixa média histórica quando houver serviços concluídos. **(implementado)** | Código | `ServiceService.toPublicResponse` calcula média de reviews e faixa de preço concluído. |
| RN61 | Projetos de portfólio devem pertencer ao freelancer autenticado e podem ser exibidos publicamente por freelancer. **(implementado)** | Código | Criação/edição/exclusão validam propriedade; listagem pública usa `freelancerId`. |
| RN62 | Projetos de portfólio devem estar vinculados ao catálogo de categorias/subcategorias. **(implementado)** | Código | `PortfolioService` resolve seleção obrigatória no catálogo. |
| RN63 | Solicitações de serviço podem ser vinculadas a um serviço específico ou a um freelancer específico. **(implementado)** | Código | `ServiceRequestDTO` aceita `serviceId` e `freelancerId`. |
| RN64 | Se uma solicitação informa serviço e freelancer, o serviço deve pertencer ao freelancer informado. **(implementado)** | Código | A criação rejeita divergência entre `serviceId` e `freelancerId`. |
| RN65 | Solicitações presenciais ou híbridas devem ter endereço; solicitações remotas não devem manter endereço. **(implementado)** | Código | `resolveRequestWorkModel` e validação de endereço aplicam a regra. |
| RN66 | Orçamentos e valores negociados devem respeitar mínimo de R$ 20,00 e máximo de R$ 1.000.000,00. **(implementado)** | Código | Há exceção controlada para registros legados de baixo valor. |
| RN67 | Prazo estimado de solicitação deve estar entre 1 e 365 dias quando expresso em dias. **(implementado)** | Código | Validação ocorre na criação da solicitação. |
| RN68 | Data desejada da solicitação não pode estar no passado. **(implementado)** | Código | Datas no formato `AAAA-MM-DD` são validadas. |
| RN69 | Resposta do freelancer deve conter valor, prazo de execução e escopo. **(implementado)** | Código | `FreelancerResponseDTO` exige esses campos. |
| RN70 | Propostas podem conter anexos e marcos de entrega. **(implementado)** | Código | Marcos aceitam status `pending`, `in_progress` e `completed`. |
| RN71 | Apenas o freelancer responsável pode atualizar marcos da proposta. **(implementado)** | Código | `updateMilestoneStatus` valida o freelancer da solicitação. |
| RN72 | Cliente e freelancer podem adicionar anexos a uma solicitação somente quando são partes dela. **(implementado)** | Código | Métodos de anexo validam contractor/freelancer da solicitação. |
| RN73 | Alterações para `DELIVERED` e `COMPLETED` devem ocorrer apenas pelo endpoint de conclusão. **(implementado)** | Código | `updateStatus` bloqueia esses status. |
| RN74 | Freelancer só pode marcar entrega quando a solicitação está em andamento. **(implementado)** | Código | `completeService` exige `IN_PROGRESS` para virar `DELIVERED`. |
| RN75 | Cliente só pode concluir o serviço após entrega pelo freelancer. **(implementado)** | Código | `completeService` exige `DELIVERED` para virar `COMPLETED`. |
| RN76 | Ao concluir o serviço, o sistema deve liberar saldo do freelancer quando houver pagamento concluído. **(implementado)** | Código | `releaseFreelancerBalanceIfNeeded` move saldo pendente para disponível. |
| RN77 | Se o cliente não aprovar entrega entregue em até 7 dias, saldo pendente deve ser liberado automaticamente. **(implementado)** | Código | `WalletScheduler` roda a cada hora e conclui solicitações `DELIVERED` antigas. |
| RN78 | Cliente pode solicitar ajustes apenas quando o serviço está entregue. **(implementado)** | Código | `DeliveryAdjustmentService` exige status `DELIVERED`. |
| RN79 | Cada solicitação de serviço pode ter apenas um pedido de ajuste. **(implementado)** | Código | Criação rejeita ajuste duplicado para a mesma solicitação. |
| RN80 | Apenas o cliente criador pode remover pedido de ajuste pendente. **(implementado)** | Código | Exclusão valida criador e status `PENDING`. |
| RN81 | Apenas o freelancer responsável pode atualizar o status do ajuste. **(implementado)** | Código | Validação usa o e-mail do freelancer da solicitação. |
| RN82 | Pagamento só pode ser criado pelo cliente dono da solicitação aceita. **(implementado)** | Código | `PaymentService.createPayment` valida contractor e status `ACCEPTED`. |
| RN83 | Deve existir no máximo um pagamento por solicitação. **(implementado)** | Código | Criação rejeita pagamento duplicado por `serviceRequest`. |
| RN84 | O valor pago deve ser exatamente o valor aceito na negociação. **(implementado)** | Código | `validatePaymentAmount` compara valor solicitado e valor acordado. |
| RN85 | Pagamento deve separar valor total, taxa da plataforma e valor líquido do freelancer. **(implementado)** | Código | A taxa da plataforma atual é 5%; o valor líquido é creditado ao freelancer. |
| RN86 | Taxas de gateway devem ser calculadas conforme o método de pagamento. **(implementado)** | Código | PIX e boleto usam taxa fixa configurada; cartão usa taxa fixa mais percentual. |
| RN87 | Pagamento PIX deve criar cobrança no Asaas e retornar QR Code e cópia e cola. **(implementado)** | Código | PIX entra como `PROCESSING` até confirmação. |
| RN88 | Pagamento por cartão de crédito deve validar dados mínimos, salvar os últimos quatro dígitos e concluir o pagamento no fluxo atual. **(implementado)** | Código | Fluxo atual é simulado e aprova instantaneamente. |
| RN89 | Pagamento por carteira digital entra em processamento no fluxo atual. **(parcialmente implementado)** | Código | Método existe no backend, mas não há fluxo completo de liquidação observado. |
| RN90 | Confirmação de pagamento deve mudar a solicitação para `IN_PROGRESS` e habilitar o chat. **(implementado)** | Código | Fluxo completo ocorre no cartão simulado, webhooks e simulação de PIX. |
| RN91 | O histórico de status de pagamento deve guardar status, evento Asaas, detalhes e data. **(implementado)** | Código | Entidade `PaymentStatusHistory`. |
| RN92 | Webhooks do Asaas devem validar token de acesso quando configurado. **(implementado)** | Código | Header `asaas-access-token` é comparado ao token configurado. |
| RN93 | Webhooks de pagamento Asaas devem sincronizar status e notificar as partes. **(implementado)** | Código | Processa confirmação, atraso, falha, estorno, chargeback e outros eventos. |
| RN94 | Transações PIX recebidas por webhook devem ser vinculadas a pagamentos pendentes quando possível. **(implementado)** | Código | Busca por transactionId, valor/status ou externalReference. |
| RN95 | Comprovante de pagamento deve ser obtido pelo Asaas apenas para pagamento integrado. **(implementado)** | Código | `getReceiptUrl` exige `asaasPaymentId`. |
| RN96 | Pagamentos concluídos devem adicionar saldo pendente na carteira do freelancer. **(implementado)** | Código | O valor líquido é creditado em `pendingBalance`. |
| RN97 | Carteira do freelancer deve controlar saldo pendente, disponível, total recebido e total sacado. **(implementado)** | Código | Entidade `FreelancerWallet`. |
| RN98 | Saque deve exigir saldo disponível suficiente. **(implementado)** | Código | `WithdrawalService` chama `hasSufficientBalance`. |
| RN99 | Saque deve respeitar mínimo de R$ 10,00 e máximo de R$ 50.000,00. **(implementado)** | Código | Validação em `WithdrawalService`. |
| RN100 | Saque por PIX deve exigir chave PIX e tipo de chave. **(parcialmente implementado)** | Código | Chave PIX é exigida; tipo da chave é repassado, mas não validado como obrigatório. |
| RN101 | Saque por TED deve exigir conta bancária própria e completa. **(implementado)** | Código | Valida propriedade e dados bancários obrigatórios. |
| RN102 | Usuário pode cadastrar no máximo 5 contas bancárias ou chaves PIX. **(implementado)** | Código | `BankAccountService.MAX_ACCOUNTS = 5`. |
| RN103 | A primeira conta bancária/chave PIX cadastrada deve virar padrão automaticamente. **(implementado)** | Código | Também permite definir outra como padrão. |
| RN104 | Dados sensíveis de conta, CPF/CNPJ e chave PIX devem ser mascarados em respostas. **(implementado)** | Código | `BankAccountService` mascara campos na resposta. |
| RN105 | Apenas o dono pode remover conta bancária/chave PIX. **(implementado)** | Código | Remoção valida o usuário autenticado. |
| RN106 | Apenas saques pendentes podem ser cancelados pelo dono. **(parcialmente implementado)** | Código | Regra existe, mas o saque é processado imediatamente após a criação; cancelamento tende a ter pouca janela de uso. |
| RN107 | Favoritos podem apontar para um serviço ou para um freelancer. **(implementado)** | Código | `FavoriteRequest` aceita `serviceId` ou `freelancerId`. |
| RN108 | O sistema não deve duplicar favorito já existente para o mesmo usuário e alvo. **(implementado)** | Código | `addFavorite` retorna favorito existente quando encontrado. |
| RN109 | Apenas o dono pode remover um favorito. **(implementado)** | Código | `removeFavorite` valida `userId`. |
| RN110 | Usuário pode cadastrar, editar, listar, excluir e definir endereço padrão. **(implementado)** | Código | `AddressService` implementa CRUD e default. |
| RN111 | O primeiro endereço cadastrado deve virar padrão automaticamente. **(implementado)** | Código | Regra aplicada em `createAddress`. |
| RN112 | Deve existir no máximo um endereço padrão por usuário. **(implementado)** | Código | Ao definir padrão, os demais são limpos. |
| RN113 | Ao excluir endereço padrão, outro endereço restante deve assumir o padrão. **(implementado)** | Código | `deleteAddress` promove o primeiro restante. |
| RN114 | Notificações persistidas devem pertencer a um destinatário e possuir categoria, tipo, título, descrição e metadados. **(implementado)** | Código | Entidade `Notification` e `NotificationService`. |
| RN115 | Usuário deve poder listar, filtrar, marcar como lida, marcar todas como lidas, excluir e limpar suas notificações. **(implementado)** | Código | APIs de notificação validam destinatário. |
| RN116 | Eventos importantes devem gerar notificações em tempo real por WebSocket. **(implementado)** | Código | Solicitações, propostas, pagamentos, entrega, ajustes, anexos e avaliações disparam eventos. |
| RN117 | Conexões WebSocket devem aceitar autenticação por JWT. **(implementado)** | Código | `WebSocketAuthChannelInterceptor` valida token no CONNECT. |
| RN118 | Resumo de IA das avaliações deve ficar indisponível para freelancers com menos de 3 avaliações. **(implementado)** | Código | `MIN_REVIEWS_FOR_SUMMARY = 3`. |
| RN119 | Resumo de IA das avaliações deve usar cache temporário e ser recalculado quando houver novas avaliações. **(implementado)** | Código | Cache de 24 horas e invalidação ao criar review. |
| RN120 | Sugestão de comentário por IA deve gerar ou refinar texto curto preservando opinião original quando existir comentário do cliente. **(implementado)** | Código | Prompt preserva tom/ideias e limita a 480 caracteres. |
| RN121 | Apenas clientes podem avaliar freelancers em solicitações concluídas. **(implementado)** | Código | `ReviewService` rejeita avaliador que não seja o contractor. |
| RN122 | Um cliente só pode avaliar uma solicitação uma vez. **(implementado)** | Código | Repositório verifica `existsByServiceRequestIdAndReviewerId`. |
| RN123 | Ao criar uma avaliação, a média e o total de avaliações do freelancer devem ser recalculados. **(implementado)** | Código | `updateFreelancerRating` atualiza `rating` e `totalReviews`. |
| RN124 | Home do cliente deve priorizar interesses, região, favoritos, profissionais e serviços em destaque. **(implementado)** | Código | `ClientHomeService` calcula blocos de home. |
| RN125 | Destaques da home devem considerar avaliações recentes antes das avaliações históricas. **(implementado)** | Código | Ranking usa janela de 90 dias e depois média histórica. |
| RN126 | Categorias e subcategorias arquivadas não devem aparecer no catálogo público nem ser usadas em novas seleções obrigatórias. **(implementado)** | Código | Catálogo público filtra ativas e `resolveSelection` pode exigir ativo. |
| RN127 | Slugs de categorias e subcategorias devem ser únicos. **(implementado)** | Código | Criação/edição verifica duplicidade de slug. |
| RN128 | O catálogo de categorias deve poder ser semeado a partir de arquivo JSON. **(implementado)** | Código | `CatalogBootstrapService`/`CategoryCatalogService.seedCatalogIfNeeded`. |
