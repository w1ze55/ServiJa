export interface PolSection {
  id: string;
  title: string;
  body: string;
}

export const contractMeta = {
  version: '2.2',
  updatedAt: '17 de junho de 2026',
  company: {
    name: 'Servi Já Ltda.',
    shortName: 'Servi Já',
    cnpj: '00.000.000/0001-00',
    city: 'Florianópolis/SC',
  },
  emails: {
    privacidade: 'privacidade@servija.com.br',
    juridico: 'juridico@servija.com.br',
  },
  phone: '(48) 98438-6930',
} as const;

export const privacidadeSections: PolSection[] = [
  {
    id: 'sobre',
    title: 'Quem somos e abrangência',
    body: `<p>A <strong>Servi Já</strong> é uma plataforma digital que conecta contratantes a prestadores de serviço autônomos. Esta Política de Privacidade descreve como coletamos, usamos, armazenamos e protegemos seus dados pessoais quando você utiliza nosso site, aplicativo móvel e serviços relacionados.</p><p>Esta política se aplica a todos os usuários cadastrados, incluindo contratantes e prestadores, e está alinhada à <strong>Lei Geral de Proteção de Dados (Lei nº 13.709/2018, LGPD)</strong>.</p>`,
  },
  {
    id: 'dados-coletados',
    title: 'Dados que coletamos',
    body: `<p>Coletamos apenas o necessário para oferecer um serviço seguro, personalizado e funcional. Os dados são organizados nas seguintes categorias:</p><table class="pol-table"><thead><tr><th>Categoria</th><th>Exemplos</th></tr></thead><tbody><tr><td>Cadastro</td><td>Nome completo, CPF, e-mail, telefone, data de nascimento, foto de perfil.</td></tr><tr><td>Profissional</td><td>Categorias de serviço, portfólio, área de atuação, faixa de preços.</td></tr><tr><td>Localização</td><td>Endereço informado e localização aproximada (quando autorizada) para sugerir profissionais próximos.</td></tr><tr><td>Pagamento</td><td>Dados financeiros tokenizados, e não armazenamos o número completo do cartão.</td></tr><tr><td>Uso da plataforma</td><td>Histórico de buscas, contratações, avaliações, mensagens trocadas e dispositivo utilizado.</td></tr></tbody></table>`,
  },
  {
    id: 'finalidades',
    title: 'Como usamos seus dados',
    body: `<p>Utilizamos suas informações para:</p><ul class="pol-list"><li><strong>Operar a plataforma:</strong> intermediar contatos, orçamentos e contratações entre as partes.</li><li><strong>Personalizar a experiência:</strong> sugerir profissionais e categorias relevantes para você.</li><li><strong>Garantir a segurança:</strong> verificar identidade, prevenir fraudes e moderar conteúdo.</li><li><strong>Processar pagamentos:</strong> conduzir transações através de provedores autorizados.</li><li><strong>Melhorar o produto:</strong> analisar uso agregado e anônimo para evoluir funcionalidades.</li><li><strong>Comunicar:</strong> enviar notificações transacionais, atualizações e comunicações de marketing, mediante consentimento.</li></ul>`,
  },
  {
    id: 'bases-legais',
    title: 'Bases legais do tratamento',
    body: `<p>Todo tratamento de dados pessoais realizado pela Servi Já está fundamentado em ao menos uma das bases legais previstas na LGPD (art. 7º). A base aplicável depende da finalidade:</p><table class="pol-table"><thead><tr><th>Base legal</th><th>Quando aplicamos</th></tr></thead><tbody><tr><td>Execução de contrato</td><td>Operar a plataforma, intermediar contratações e processar pagamentos solicitados por você.</td></tr><tr><td>Consentimento</td><td>Envio de comunicações de marketing e uso da sua localização precisa para sugerir profissionais próximos.</td></tr><tr><td>Cumprimento de obrigação legal</td><td>Retenção de registros financeiros e fiscais e atendimento a determinações de autoridades.</td></tr><tr><td>Legítimo interesse</td><td>Prevenção a fraudes, segurança da plataforma e melhoria de funcionalidades com dados agregados.</td></tr><tr><td>Exercício de direitos</td><td>Defesa em processos judiciais, administrativos ou arbitrais.</td></tr></tbody></table><p>Quando o tratamento se basear em consentimento, você poderá revogá-lo a qualquer momento, sem prejuízo das operações já realizadas.</p>`,
  },
  {
    id: 'compartilhamento',
    title: 'Compartilhamento com terceiros',
    body: `<p>Não vendemos seus dados pessoais. Em algumas situações, compartilhamos dados estritamente necessários com parceiros que nos ajudam a operar a plataforma:</p><ul class="pol-list"><li><strong>Provedores de pagamento</strong> regulamentados pelo Banco Central, para processar transações.</li><li><strong>Infraestrutura em nuvem</strong> (hospedagem, e-mail transacional, analytics) sob acordos de confidencialidade.</li><li><strong>Autoridades públicas</strong>, quando exigido por lei, ordem judicial ou regulação aplicável.</li></ul><p>Quando você contrata um profissional, alguns dados essenciais (nome, telefone e endereço do serviço) são compartilhados com a outra parte para a execução do trabalho.</p>`,
  },
  {
    id: 'transferencia-internacional',
    title: 'Transferência internacional de dados',
    body: `<p>Alguns dos provedores de infraestrutura, hospedagem e analytics que utilizamos podem armazenar ou processar dados em servidores localizados fora do Brasil. Nessas situações, a transferência internacional é realizada em conformidade com a LGPD (arts. 33 a 36).</p><p>Adotamos salvaguardas para garantir um nível adequado de proteção, tais como cláusulas contratuais específicas, exigência de padrões de segurança equivalentes aos previstos na legislação brasileira e seleção de fornecedores estabelecidos em países com grau de proteção reconhecido. Você pode solicitar mais informações sobre essas transferências pelos canais de contato indicados nesta política.</p>`,
  },
  {
    id: 'cookies',
    title: 'Cookies e tecnologias similares',
    body: `<p>Usamos cookies e tecnologias parecidas para lembrar suas preferências, manter sua sessão autenticada e medir a performance da plataforma. Você pode gerenciar cookies a qualquer momento pelas configurações do seu navegador.</p><h3>Tipos de cookies utilizados</h3><ul class="pol-list"><li><strong>Essenciais:</strong> necessários para o funcionamento básico (login, segurança).</li><li><strong>Funcionais:</strong> guardam preferências como idioma, tema e filtros.</li><li><strong>Analíticos:</strong> nos ajudam a entender padrões de uso de forma agregada.</li></ul>`,
  },
  {
    id: 'direitos',
    title: 'Seus direitos como titular',
    body: `<p>De acordo com a LGPD, você pode exercer os seguintes direitos sobre seus dados pessoais a qualquer momento, sem custo:</p><ul class="pol-list"><li><strong>Confirmar</strong> a existência de tratamento dos seus dados.</li><li><strong>Acessar</strong> os dados que mantemos sobre você.</li><li><strong>Corrigir</strong> dados incompletos, inexatos ou desatualizados.</li><li><strong>Solicitar a anonimização, bloqueio ou eliminação</strong> de dados desnecessários ou excessivos.</li><li><strong>Portar</strong> seus dados a outro fornecedor de serviço.</li><li><strong>Revogar o consentimento</strong> previamente fornecido.</li><li><strong>Ser informado</strong> sobre com quem compartilhamos seus dados.</li></ul><div class="pol-callout"><div class="pol-callout-ico"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10"/><path d="m9 12 2 2 4-4"/></svg></div><div class="pol-callout-body"><div class="pol-callout-title">Como exercer seus direitos</div><div class="pol-callout-text">Envie uma solicitação para <a href="mailto:privacidade@servija.com.br">privacidade@servija.com.br</a>; respondemos em até 15 dias úteis.</div></div></div>`,
  },
  {
    id: 'seguranca',
    title: 'Segurança dos dados',
    body: `<p>Adotamos medidas técnicas e administrativas para proteger seus dados contra acessos não autorizados, perda, alteração ou divulgação indevida. Entre elas: criptografia em trânsito (TLS) e em repouso, controle de acesso por perfil, monitoramento contínuo, auditorias periódicas e treinamento da equipe.</p><p>Apesar do nosso compromisso, nenhum sistema é 100% seguro. Em caso de incidente relevante, comunicaremos os titulares afetados e a ANPD nos prazos definidos pela LGPD.</p>`,
  },
  {
    id: 'menores',
    title: 'Dados de crianças e adolescentes',
    body: `<p>A Servi Já é destinada exclusivamente a maiores de 18 anos. Não coletamos intencionalmente dados de crianças e adolescentes e não permitimos o cadastro de menores de idade na plataforma.</p><p>Caso identifiquemos que uma conta foi criada por um menor de idade, eliminaremos os dados associados e encerraremos o cadastro. Se você é responsável legal e acredita que um menor sob sua tutela nos forneceu dados pessoais, entre em contato pelo e-mail <a href="mailto:privacidade@servija.com.br">privacidade@servija.com.br</a> para que possamos tomar as providências cabíveis.</p>`,
  },
  {
    id: 'retencao',
    title: 'Retenção dos dados',
    body: `<p>Mantemos seus dados pelo tempo necessário para cumprir as finalidades descritas nesta política ou conforme exigido por lei. Quando você encerra sua conta, eliminamos ou anonimizamos seus dados em até 1 ano, exceto:</p><ul class="pol-list"><li>Registros financeiros, mantidos por 5 anos por exigência fiscal.</li><li>Histórico de transações e mensagens, retidos por 5 anos para fins de auditoria e prevenção a fraudes.</li><li>Dados sob investigação ou disputa, retidos até a conclusão do caso.</li></ul>`,
  },
  {
    id: 'alteracoes',
    title: 'Alterações nesta política',
    body: `<p>Esta política pode ser atualizada para refletir mudanças em nossas práticas, novas funcionalidades ou exigências legais. Quando alterações relevantes ocorrerem, notificaremos você por e-mail e por aviso destacado dentro da plataforma com pelo menos 15 dias de antecedência.</p>`,
  },
  {
    id: 'contato',
    title: 'Contato',
    body: `<p>A Servi Já mantém um <strong>Encarregado de Proteção de Dados (DPO)</strong>, responsável por atuar como canal de comunicação entre a empresa, os titulares e a Autoridade Nacional de Proteção de Dados (ANPD).</p><p>Dúvidas, sugestões ou solicitações relacionadas a esta Política de Privacidade, bem como o contato com o Encarregado, podem ser enviadas para:</p><ul class="pol-list"><li><strong>Encarregado (DPO):</strong> <a href="mailto:privacidade@servija.com.br">privacidade@servija.com.br</a></li><li><strong>E-mail:</strong> <a href="mailto:privacidade@servija.com.br">privacidade@servija.com.br</a></li><li><strong>Telefone:</strong> (48) 98438-6930</li></ul>`,
  },
];

export const termosSections: PolSection[] = [
  {
    id: 'aceitacao',
    title: 'Aceitação dos termos',
    body: `<p>Ao se cadastrar, acessar ou utilizar a plataforma <strong>Servi Já</strong>, você declara ter lido, compreendido e concordado integralmente com estes Termos de Uso e com nossa Política de Privacidade. Se você não concorda com algum item, deve interromper o uso da plataforma.</p><p>Estes Termos formam um contrato vinculante entre você e a Servi Já Ltda., inscrita no CNPJ sob o nº 00.000.000/0001-00.</p>`,
  },
  {
    id: 'sobre-plataforma',
    title: 'Sobre a plataforma',
    body: `<p>A Servi Já é uma plataforma de intermediação que conecta <strong>contratantes</strong>, pessoas que buscam serviços, a <strong>prestadores</strong>, profissionais autônomos que oferecem serviços nas categorias disponíveis.</p><div class="pol-callout"><div class="pol-callout-ico"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg></div><div class="pol-callout-body"><div class="pol-callout-title">Nosso papel é de intermediação</div><div class="pol-callout-text">A Servi Já não é prestadora dos serviços contratados e não executa, supervisiona ou garante o resultado dos trabalhos. A relação contratual e a responsabilidade pela execução são estabelecidas diretamente entre contratante e prestador.</div></div></div>`,
  },
  {
    id: 'cadastro',
    title: 'Cadastro e conta',
    body: `<p>Para utilizar a plataforma, você deve:</p><ul class="pol-list"><li>Ser maior de 18 anos ou emancipado nos termos da lei.</li><li>Fornecer informações verdadeiras, atualizadas e completas no cadastro.</li><li>Manter a confidencialidade da sua senha e ser responsável por todas as atividades realizadas em sua conta.</li><li>Notificar a Servi Já imediatamente em caso de uso não autorizado da sua conta.</li></ul><p>Reservamo-nos o direito de suspender ou encerrar contas que violem estes Termos, forneçam informações falsas ou apresentem comportamento prejudicial à comunidade.</p>`,
  },
  {
    id: 'responsabilidades-contratante',
    title: 'Responsabilidades do contratante',
    body: `<p>Ao contratar serviços pela plataforma, você se compromete a:</p><ul class="pol-list"><li>Descrever o serviço desejado com clareza, fornecendo informações precisas.</li><li>Tratar prestadores com respeito e cordialidade, sem qualquer forma de discriminação.</li><li>Cumprir os acordos financeiros e prazos acordados com o prestador.</li><li>Avaliar o serviço de forma honesta, factual e construtiva após sua conclusão.</li><li>Garantir condições seguras para a execução do trabalho no local combinado.</li></ul>`,
  },
  {
    id: 'responsabilidades-prestador',
    title: 'Responsabilidades do prestador',
    body: `<p>Ao oferecer serviços pela plataforma, você se compromete a:</p><ul class="pol-list"><li>Possuir as habilidades, documentos e autorizações necessários para a categoria de serviço oferecida.</li><li>Manter portfólio, descrição e preços compatíveis com sua atuação real.</li><li>Cumprir prazos, orçamentos e padrão de qualidade acordados com cada contratante.</li><li>Tratar contratantes com profissionalismo e respeitar a privacidade do local de trabalho.</li><li>Emitir nota fiscal quando aplicável e arcar com seus próprios tributos e contribuições.</li></ul>`,
  },
  {
    id: 'pagamentos',
    title: 'Pagamentos e taxas',
    body: `<p>Os valores dos serviços são negociados livremente entre contratante e prestador. A plataforma oferece pagamento integrado via cartão, Pix e carteira digital com retenção dos valores até a confirmação da conclusão do serviço.</p><h3>Taxas da plataforma</h3><ul class="pol-list"><li><strong>Contratantes:</strong> uso da plataforma, busca e contratação são gratuitos.</li><li><strong>Prestadores:</strong> a Servi Já cobra uma taxa de serviço sobre cada contratação concluída, com percentual informado de forma clara antes do aceite do orçamento.</li><li><strong>Plano Pro (opcional):</strong> prestadores podem assinar planos com benefícios adicionais, como destaque na busca, selos e relatórios, sob termos específicos do plano contratado.</li></ul>`,
  },
  {
    id: 'cancelamento',
    title: 'Cancelamento e reembolso',
    body: `<p>Solicitações podem ser canceladas pelo contratante a qualquer momento antes do início do serviço, sem qualquer custo. Após o início da execução, aplicam-se as regras combinadas diretamente com o prestador.</p><p>Em casos de descumprimento, qualidade insuficiente ou disputa, a Servi Já oferece o processo de <strong>Mediação Servi Já</strong>: nossa equipe avalia evidências de ambas as partes e propõe uma solução justa em até 7 dias úteis.</p>`,
  },
  {
    id: 'propriedade-intelectual',
    title: 'Propriedade intelectual',
    body: `<p>Marca, logotipo, layout, código-fonte, textos, design e demais elementos da plataforma são de propriedade exclusiva da Servi Já e protegidos pela legislação aplicável. Conteúdos publicados pelos usuários como fotos, descrições e avaliações permanecem de propriedade dos próprios usuários, que concedem à Servi Já licença não-exclusiva para exibi-los na plataforma.</p><p>Ao enviar qualquer conteúdo à plataforma como imagens, fotos, textos, nomes, marcas, logotipos ou outros materiais, o usuário declara e garante ser o titular dos direitos autorais e demais direitos de propriedade intelectual sobre esse conteúdo, ou possuir autorização expressa para utilizá-lo.</p><div class="pol-callout"><div class="pol-callout-ico"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg></div><div class="pol-callout-body"><div class="pol-callout-title">Responsabilidade pelo conteúdo enviado</div><div class="pol-callout-text">A Servi Já não se responsabiliza por eventuais violações de direitos autorais ou de propriedade intelectual decorrentes de conteúdos enviados pelos usuários. A responsabilidade por imagens, nomes, marcas e demais materiais que possuam direitos autorais é exclusiva do usuário que realizou o upload do conteúdo, que responderá por quaisquer reivindicações de terceiros. Identificado eventual conteúdo que infrinja direitos de terceiros, a Servi Já poderá removê-lo e suspender ou encerrar a conta responsável.</div></div></div>`,
  },
  {
    id: 'limitacao',
    title: 'Limitação de responsabilidade',
    body: `<p>Por atuar como intermediadora, a Servi Já <strong>não se responsabiliza</strong> por:</p><ul class="pol-list"><li>Qualidade, segurança, legalidade ou conformidade dos serviços prestados pelos profissionais.</li><li>Condutas, declarações ou descumprimentos de qualquer das partes envolvidas em uma contratação.</li><li>Danos diretos ou indiretos decorrentes da execução ou não execução do serviço contratado.</li><li>Indisponibilidades pontuais da plataforma por motivos de força maior ou manutenção programada.</li></ul><p>Empenhamo-nos para verificar identidades e moderar conteúdo, mas a contratação é uma decisão final do contratante, baseada nas informações públicas de cada perfil.</p>`,
  },
  {
    id: 'foro',
    title: 'Legislação e foro',
    body: `<p>Estes Termos são regidos pelas leis da República Federativa do Brasil. Fica eleito o foro da Comarca de Florianópolis/SC para dirimir quaisquer controvérsias decorrentes deste instrumento, com renúncia expressa a qualquer outro, por mais privilegiado que seja.</p>`,
  },
];

export function sectionsFor(tab: 'privacidade' | 'termos'): PolSection[] {
  return tab === 'privacidade' ? privacidadeSections : termosSections;
}
