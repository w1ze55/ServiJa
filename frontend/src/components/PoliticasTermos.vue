<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import {
  Shield, ShieldCheck, CheckCircle, List,
  ArrowRight, Mail,
} from 'lucide-vue-next';
import { useRouter, useRoute } from 'vue-router';
import logo from '@/assets/favicon-32x32.png';

const router = useRouter();
const route = useRoute();
const tab = ref<'privacidade' | 'termos'>(
  route.query.tab === 'termos' ? 'termos' : 'privacidade'
);
const activeId = ref<string | null>(null);

// ── Data ─────────────────────────────────────────────────────────────
const privacidadeSections = [
  {
    id: 'sobre',
    title: 'Quem somos e abrangência',
    body: `<p>A <strong>Servi Já</strong> é uma plataforma digital que conecta contratantes a prestadores de serviço autônomos. Esta Política de Privacidade descreve como coletamos, usamos, armazenamos e protegemos seus dados pessoais quando você utiliza nosso site, aplicativo móvel e serviços relacionados.</p><p>Esta política se aplica a todos os usuários cadastrados — contratantes e prestadores — e está alinhada à <strong>Lei Geral de Proteção de Dados (Lei nº 13.709/2018 — LGPD)</strong>.</p>`,
  },
  {
    id: 'dados-coletados',
    title: 'Dados que coletamos',
    body: `<p>Coletamos apenas o necessário para oferecer um serviço seguro, personalizado e funcional. Os dados são organizados nas seguintes categorias:</p><table class="pol-table"><thead><tr><th>Categoria</th><th>Exemplos</th></tr></thead><tbody><tr><td>Cadastro</td><td>Nome completo, CPF, e-mail, telefone, data de nascimento, foto de perfil.</td></tr><tr><td>Profissional</td><td>Categorias de serviço, portfólio, certificações, área de atuação, faixa de preços.</td></tr><tr><td>Localização</td><td>Endereço informado e localização aproximada (quando autorizada) para sugerir profissionais próximos.</td></tr><tr><td>Pagamento</td><td>Dados financeiros tokenizados — não armazenamos o número completo do cartão.</td></tr><tr><td>Uso da plataforma</td><td>Histórico de buscas, contratações, avaliações, mensagens trocadas e dispositivo utilizado.</td></tr></tbody></table>`,
  },
  {
    id: 'finalidades',
    title: 'Como usamos seus dados',
    body: `<p>Utilizamos suas informações para:</p><ul class="pol-list"><li><strong>Operar a plataforma:</strong> intermediar contatos, orçamentos e contratações entre as partes.</li><li><strong>Personalizar a experiência:</strong> sugerir profissionais e categorias relevantes para você.</li><li><strong>Garantir a segurança:</strong> verificar identidade, prevenir fraudes e moderar conteúdo.</li><li><strong>Processar pagamentos:</strong> conduzir transações através de provedores autorizados.</li><li><strong>Melhorar o produto:</strong> analisar uso agregado e anônimo para evoluir funcionalidades.</li><li><strong>Comunicar:</strong> enviar notificações transacionais, atualizações e — mediante consentimento — comunicações de marketing.</li></ul>`,
  },
  {
    id: 'compartilhamento',
    title: 'Compartilhamento com terceiros',
    body: `<p>Não vendemos seus dados pessoais. Em algumas situações, compartilhamos dados estritamente necessários com parceiros que nos ajudam a operar a plataforma:</p><ul class="pol-list"><li><strong>Provedores de pagamento</strong> regulamentados pelo Banco Central, para processar transações.</li><li><strong>Serviços de verificação de identidade</strong>, para validar documentos e prevenir fraudes.</li><li><strong>Infraestrutura em nuvem</strong> (hospedagem, e-mail transacional, analytics) sob acordos de confidencialidade.</li><li><strong>Autoridades públicas</strong>, quando exigido por lei, ordem judicial ou regulação aplicável.</li></ul><p>Quando você contrata um profissional, alguns dados essenciais (nome, telefone e endereço do serviço) são compartilhados com a outra parte para a execução do trabalho.</p>`,
  },
  {
    id: 'cookies',
    title: 'Cookies e tecnologias similares',
    body: `<p>Usamos cookies e tecnologias parecidas para lembrar suas preferências, manter sua sessão autenticada e medir a performance da plataforma. Você pode gerenciar cookies a qualquer momento pelas configurações do seu navegador.</p><h3>Tipos de cookies utilizados</h3><ul class="pol-list"><li><strong>Essenciais:</strong> necessários para o funcionamento básico (login, segurança).</li><li><strong>Funcionais:</strong> guardam preferências como idioma, tema e filtros.</li><li><strong>Analíticos:</strong> nos ajudam a entender padrões de uso de forma agregada.</li></ul>`,
  },
  {
    id: 'direitos',
    title: 'Seus direitos como titular',
    body: `<p>De acordo com a LGPD, você pode exercer os seguintes direitos sobre seus dados pessoais a qualquer momento — sem custo:</p><ul class="pol-list"><li><strong>Confirmar</strong> a existência de tratamento dos seus dados.</li><li><strong>Acessar</strong> os dados que mantemos sobre você.</li><li><strong>Corrigir</strong> dados incompletos, inexatos ou desatualizados.</li><li><strong>Solicitar a anonimização, bloqueio ou eliminação</strong> de dados desnecessários ou excessivos.</li><li><strong>Portar</strong> seus dados a outro fornecedor de serviço.</li><li><strong>Revogar o consentimento</strong> previamente fornecido.</li><li><strong>Ser informado</strong> sobre com quem compartilhamos seus dados.</li></ul><div class="pol-callout"><div class="pol-callout-ico"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10"/><path d="m9 12 2 2 4-4"/></svg></div><div class="pol-callout-body"><div class="pol-callout-title">Como exercer seus direitos</div><div class="pol-callout-text">Envie uma solicitação para <a href="mailto:privacidade@servija.com.br">privacidade@servija.com.br</a> — respondemos em até 15 dias úteis. Nossa Encarregada de Proteção de Dados (DPO) é a Sra. Ana Ribeiro.</div></div></div>`,
  },
  {
    id: 'seguranca',
    title: 'Segurança dos dados',
    body: `<p>Adotamos medidas técnicas e administrativas para proteger seus dados contra acessos não autorizados, perda, alteração ou divulgação indevida. Entre elas: criptografia em trânsito (TLS) e em repouso, controle de acesso por perfil, monitoramento contínuo, auditorias periódicas e treinamento da equipe.</p><p>Apesar do nosso compromisso, nenhum sistema é 100% seguro. Em caso de incidente relevante, comunicaremos os titulares afetados e a ANPD nos prazos definidos pela LGPD.</p>`,
  },
  {
    id: 'retencao',
    title: 'Retenção dos dados',
    body: `<p>Mantemos seus dados pelo tempo necessário para cumprir as finalidades descritas nesta política ou conforme exigido por lei. Quando você encerra sua conta, eliminamos ou anonimizamos seus dados em até 90 dias — exceto:</p><ul class="pol-list"><li>Registros financeiros, mantidos por 5 anos por exigência fiscal.</li><li>Histórico de transações e mensagens, retidos por 5 anos para fins de auditoria e prevenção a fraudes.</li><li>Dados sob investigação ou disputa, retidos até a conclusão do caso.</li></ul>`,
  },
  {
    id: 'alteracoes',
    title: 'Alterações nesta política',
    body: `<p>Esta política pode ser atualizada para refletir mudanças em nossas práticas, novas funcionalidades ou exigências legais. Quando alterações relevantes ocorrerem, notificaremos você por e-mail e por aviso destacado dentro da plataforma com pelo menos 15 dias de antecedência.</p>`,
  },
  {
    id: 'contato',
    title: 'Contato',
    body: `<p>Dúvidas, sugestões ou solicitações relacionadas a esta Política de Privacidade podem ser enviadas para:</p><ul class="pol-list"><li><strong>E-mail:</strong> <a href="mailto:privacidade@servija.com.br">privacidade@servija.com.br</a></li><li><strong>DPO (Encarregada):</strong> Ana Ribeiro</li><li><strong>Endereço:</strong> Av. Paulista, 1000 — 10º andar, Bela Vista, São Paulo/SP, 01310-100</li></ul>`,
  },
];

const termosSections = [
  {
    id: 'aceitacao',
    title: 'Aceitação dos termos',
    body: `<p>Ao se cadastrar, acessar ou utilizar a plataforma <strong>Servi Já</strong>, você declara ter lido, compreendido e concordado integralmente com estes Termos de Uso e com nossa Política de Privacidade. Se você não concorda com algum item, deve interromper o uso da plataforma.</p><p>Estes Termos formam um contrato vinculante entre você e a Servi Já Tecnologia Ltda., inscrita no CNPJ sob o nº 00.000.000/0001-00.</p>`,
  },
  {
    id: 'sobre-plataforma',
    title: 'Sobre a plataforma',
    body: `<p>A Servi Já é uma plataforma de intermediação que conecta <strong>contratantes</strong> — pessoas que buscam serviços — a <strong>prestadores</strong> — profissionais autônomos que oferecem serviços nas categorias disponíveis.</p><div class="pol-callout"><div class="pol-callout-ico"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg></div><div class="pol-callout-body"><div class="pol-callout-title">Nosso papel é de intermediação</div><div class="pol-callout-text">A Servi Já não é prestadora dos serviços contratados — não executa, supervisiona ou garante o resultado dos trabalhos. A relação contratual e a responsabilidade pela execução são estabelecidas diretamente entre contratante e prestador.</div></div></div>`,
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
    body: `<p>Os valores dos serviços são negociados livremente entre contratante e prestador. A plataforma oferece pagamento integrado via cartão, Pix e carteira digital — com retenção dos valores até a confirmação da conclusão do serviço.</p><h3>Taxas da plataforma</h3><ul class="pol-list"><li><strong>Contratantes:</strong> uso da plataforma, busca e contratação são gratuitos.</li><li><strong>Prestadores:</strong> a Servi Já cobra uma taxa de serviço sobre cada contratação concluída, com percentual informado de forma clara antes do aceite do orçamento.</li><li><strong>Plano Pro (opcional):</strong> prestadores podem assinar planos com benefícios adicionais — destaque na busca, selos e relatórios — sob termos específicos do plano contratado.</li></ul>`,
  },
  {
    id: 'cancelamento',
    title: 'Cancelamento e reembolso',
    body: `<p>Solicitações podem ser canceladas pelo contratante a qualquer momento antes do início do serviço, sem qualquer custo. Após o início da execução, aplicam-se as regras combinadas diretamente com o prestador.</p><p>Em casos de descumprimento, qualidade insuficiente ou disputa, a Servi Já oferece o processo de <strong>Mediação Servi Já</strong>: nossa equipe avalia evidências de ambas as partes e propõe uma solução justa em até 7 dias úteis.</p>`,
  },
  {
    id: 'propriedade-intelectual',
    title: 'Propriedade intelectual',
    body: `<p>Marca, logotipo, layout, código-fonte, textos, design e demais elementos da plataforma são de propriedade exclusiva da Servi Já e protegidos pela legislação aplicável. Conteúdos publicados pelos usuários — fotos, descrições e avaliações — permanecem de propriedade dos próprios usuários, que concedem à Servi Já licença não-exclusiva para exibi-los na plataforma.</p>`,
  },
  {
    id: 'limitacao',
    title: 'Limitação de responsabilidade',
    body: `<p>Por atuar como intermediadora, a Servi Já <strong>não se responsabiliza</strong> por:</p><ul class="pol-list"><li>Qualidade, segurança, legalidade ou conformidade dos serviços prestados pelos profissionais.</li><li>Condutas, declarações ou descumprimentos de qualquer das partes envolvidas em uma contratação.</li><li>Danos diretos ou indiretos decorrentes da execução ou não execução do serviço contratado.</li><li>Indisponibilidades pontuais da plataforma por motivos de força maior ou manutenção programada.</li></ul><p>Empenhamo-nos para verificar identidades e moderar conteúdo — mas a contratação é uma decisão final do contratante, baseada nas informações públicas de cada perfil.</p>`,
  },
  {
    id: 'foro',
    title: 'Legislação e foro',
    body: `<p>Estes Termos são regidos pelas leis da República Federativa do Brasil. Fica eleito o foro da Comarca de São Paulo/SP para dirimir quaisquer controvérsias decorrentes deste instrumento, com renúncia expressa a qualquer outro, por mais privilegiado que seja.</p>`,
  },
];

const sections = computed(() =>
  tab.value === 'privacidade' ? privacidadeSections : termosSections
);

const heroTitle = computed(() =>
  tab.value === 'privacidade' ? 'Política de Privacidade' : 'Termos de Uso'
);

const heroDesc = computed(() =>
  tab.value === 'privacidade'
    ? 'Transparência sobre quais dados coletamos, como os utilizamos e quais são os seus direitos como titular — em linguagem clara, sem juridiquês desnecessário.'
    : 'Regras que organizam a relação entre a Servi Já, contratantes e prestadores — para que todos saibam o que esperar da plataforma e uns dos outros.'
);

const introText = computed(() =>
  tab.value === 'privacidade'
    ? 'coletamos somente o necessário para conectar você aos melhores profissionais, nunca vendemos seus dados, e você pode pedir acesso, correção ou exclusão deles a qualquer momento.'
    : 'a Servi Já é um espaço de intermediação. Contratantes e prestadores combinam o serviço, o preço e a data; nós oferecemos a tecnologia, a segurança do pagamento e a mediação em caso de problemas.'
);

const endEmail = computed(() =>
  tab.value === 'privacidade' ? 'privacidade@servija.com.br' : 'juridico@servija.com.br'
);

// ── Scrollspy ────────────────────────────────────────────────────────
let observer: IntersectionObserver | null = null;

function setupObserver() {
  if (observer) observer.disconnect();
  activeId.value = sections.value[0]?.id ?? null;

  observer = new IntersectionObserver(
    (entries) => {
      const visible = entries
        .filter((e) => e.isIntersecting)
        .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);
      if (visible[0]) {
        activeId.value = visible[0].target.id.replace(/^sec-/, '');
      }
    },
    { rootMargin: '-180px 0px -65% 0px', threshold: 0 }
  );

  setTimeout(() => {
    sections.value.forEach((s) => {
      const el = document.getElementById('sec-' + s.id);
      if (el) observer!.observe(el);
    });
  }, 50);
}

function scrollToSection(id: string) {
  const el = document.getElementById('sec-' + id);
  if (el) {
    const top = el.getBoundingClientRect().top + window.scrollY - 170;
    window.scrollTo({ top, behavior: 'smooth' });
  }
}

function print() { window.print(); }

function switchTab(t: 'privacidade' | 'termos') {
  tab.value = t;
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

watch(tab, () => {
  setupObserver();
});

onMounted(() => {
  setupObserver();
});

onUnmounted(() => {
  observer?.disconnect();
});
</script>

<template>
  <div class="pol-page">
    <!-- Hero -->
    <section class="pol-hero">
      <div class="pol-hero-bg" />
      <div class="pol-hero-inner">
        <button class="pol-breadcrumb" @click="router.push('/')">
          <ArrowRight :size="14" class="pol-breadcrumb-arrow" />
          Voltar para a página inicial
        </button>
        <h1 class="pol-hero-title">{{ heroTitle }}</h1>
        <p class="pol-hero-desc">{{ heroDesc }}</p>
        <div class="pol-hero-meta">
          <div class="pol-meta-item">
            <CheckCircle :size="16" class="pol-meta-ico" />
            <span>Última atualização: <strong>12 de maio de 2026</strong></span>
          </div>
          <div class="pol-meta-item">
            <ShieldCheck :size="16" class="pol-meta-ico" />
            <span>Em conformidade com a <strong>LGPD</strong></span>
          </div>
          <div class="pol-meta-item">
            <List :size="16" class="pol-meta-ico" />
            <span>Versão <strong>2.1</strong></span>
          </div>
        </div>
      </div>
    </section>

    <!-- Tabs -->
    <div class="pol-tabs-wrap">
      <div class="pol-tabs">
        <button
          class="pol-tab"
          :class="{ active: tab === 'privacidade' }"
          @click="switchTab('privacidade')"
        >
          <Shield :size="16" />
          Política de Privacidade
        </button>
        <button
          class="pol-tab"
          :class="{ active: tab === 'termos' }"
          @click="switchTab('termos')"
        >
          <List :size="16" />
          Termos de Uso
        </button>
      </div>
    </div>

    <!-- Body -->
    <div class="pol-body">
      <!-- TOC Sidebar -->
      <aside class="pol-toc">
        <div class="pol-toc-label">Nesta página</div>
        <nav class="pol-toc-list">
          <a
            v-for="(s, i) in sections"
            :key="s.id"
            :href="'#sec-' + s.id"
            class="pol-toc-link"
            :class="{ active: activeId === s.id }"
            @click.prevent="scrollToSection(s.id)"
          >
            <span class="pol-toc-num">{{ String(i + 1).padStart(2, '0') }}</span>
            <span>{{ s.title }}</span>
          </a>
        </nav>
      </aside>

      <!-- Content -->
      <article class="pol-content">
        <div class="pol-intro">
          <strong>Resumo:</strong> {{ introText }}
        </div>

        <section
          v-for="(s, i) in sections"
          :key="s.id"
          :id="'sec-' + s.id"
          class="pol-section"
        >
          <div class="pol-section-head">
            <span class="pol-section-num">{{ String(i + 1).padStart(2, '0') }}</span>
            <h2>{{ s.title }}</h2>
          </div>
          <div v-html="s.body" />
        </section>

        <!-- End block -->
        <div class="pol-end">
          <div class="pol-end-eyebrow">Precisa de ajuda?</div>
          <h3 class="pol-end-title">Ficou com alguma dúvida?</h3>
          <p class="pol-end-desc">
            Nossa equipe está pronta para esclarecer qualquer ponto sobre
            {{ tab === 'privacidade' ? 'esta Política de Privacidade' : 'estes Termos de Uso' }}.
            Entre em contato — costumamos responder em até 24 horas úteis.
          </p>
          <div class="pol-end-actions">
            <a :href="'mailto:' + endEmail" class="pol-end-btn primary">
              <Mail :size="16" />
              Falar com o time
            </a>
            <button class="pol-end-btn ghost" @click="print()">
              <List :size="16" />
              Baixar em PDF
            </button>
          </div>
        </div>
      </article>
    </div>

    <!-- Footer -->
    <footer class="pol-footer">
      <div class="pol-footer-content">
        <div class="pol-footer-grid">
          <div class="pol-footer-brand">
            <div class="pol-footer-logo">
              <div class="pol-footer-logo-icon">
                <img :src="logo" alt="Servi Já" />
              </div>
              <span class="pol-footer-logo-text">Servi Já</span>
            </div>
            <p class="pol-footer-desc">O lugar onde você encontra os melhores prestadores de serviço.</p>
          </div>
          <div class="pol-footer-links">
            <h4 class="pol-footer-links-title">Para Contratantes</h4>
            <ul>
              <li><a href="/#como-funciona">Como funciona</a></li>
              <li><a href="/#diferenciais">Diferenciais</a></li>
              <li><a href="/#contratantes">Para Contratantes</a></li>
            </ul>
          </div>
          <div class="pol-footer-links">
            <h4 class="pol-footer-links-title">Para Prestadores</h4>
            <ul>
              <li><a href="/#prestadores">Seja um prestador</a></li>
              <li><a href="/#diferenciais">Diferenciais</a></li>
              <li><a href="#">Central de ajuda</a></li>
            </ul>
          </div>
          <div class="pol-footer-links">
            <h4 class="pol-footer-links-title">Institucional</h4>
            <ul>
              <li><a href="#">Sobre nós</a></li>
              <li><router-link to="/politicas?tab=termos">Termos de uso</router-link></li>
              <li><router-link to="/politicas">Privacidade</router-link></li>
            </ul>
          </div>
        </div>
        <div class="pol-footer-bottom">
          <p>© 2026 Servi Já. Todos os direitos reservados.</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
/* ── Tokens ──────────────────────────────────────────────────────── */
.pol-page {
  --primary: #286ad8;
  --primary-dark: #143774;
  --primary-soft: rgba(40, 106, 216, 0.08);
  --primary-soft-2: rgba(40, 106, 216, 0.15);
  --bg: #ffffff;
  --bg-subtle: #f9fafb;
  --bg-muted: #f3f4f6;
  --card: #ffffff;
  --text: #0f172a;
  --text-2: #1f2937;
  --text-muted: #475569;
  --border: rgba(0, 0, 0, 0.07);
  --border-2: rgba(0, 0, 0, 0.04);
  --shadow-sm: 0 1px 3px rgba(0, 0, 0, 0.05);
  --shadow-md: 0 8px 24px rgba(0, 0, 0, 0.06);
  --shadow-lg: 0 20px 40px rgba(0, 0, 0, 0.1);

  background: var(--bg);
  color: var(--text);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', system-ui, sans-serif;
  min-height: 100vh;
  -webkit-font-smoothing: antialiased;
  text-rendering: optimizeLegibility;
}
.pol-page * { box-sizing: border-box; }
.pol-page a { color: inherit; text-decoration: none; }
.pol-page button { font: inherit; border: 0; background: transparent; cursor: pointer; color: inherit; }
.pol-page ul { list-style: none; padding: 0; margin: 0; }

/* ── Hero ────────────────────────────────────────────────────────── */
.pol-hero {
  position: relative;
  padding: 5rem 1.5rem 3.5rem;
  background: linear-gradient(
    180deg,
    rgba(40, 106, 216, 0.05) 0%,
    var(--bg) 100%
  );
  border-bottom: 1px solid var(--border);
  overflow: hidden;
}
.pol-hero-bg {
  position: absolute;
  top: -30%;
  right: -10%;
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(40, 106, 216, 0.1) 0%, transparent 60%);
  pointer-events: none;
}
.pol-hero-inner {
  max-width: 1100px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}
.pol-breadcrumb {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  color: var(--text-muted);
  font-size: 0.875rem;
  font-weight: 500;
  margin-bottom: 1.25rem;
  transition: color 0.2s;
}
.pol-breadcrumb:hover { color: var(--primary); }
.pol-breadcrumb-arrow { transform: rotate(180deg); }
.pol-hero-title {
  margin-top: 0.5rem;
  font-size: clamp(2.25rem, 4vw, 3.25rem);
  font-weight: 800;
  line-height: 1.08;
  letter-spacing: -0.025em;
  color: var(--text);
  margin-bottom: 1rem;
  max-width: 36rem;
}
.pol-hero-desc {
  font-size: 1.1rem;
  color: var(--text-muted);
  line-height: 1.6;
  max-width: 38rem;
}
.pol-hero-meta {
  margin-top: 2rem;
  display: flex;
  flex-wrap: wrap;
  gap: 1.25rem 2rem;
  align-items: center;
}
.pol-meta-item {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: var(--text-muted);
  font-weight: 500;
}
.pol-meta-ico { color: var(--primary); }
.pol-meta-item strong { color: var(--text); font-weight: 600; }

/* ── Tabs ────────────────────────────────────────────────────────── */
.pol-tabs-wrap {
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  z-index: 30;
  backdrop-filter: blur(12px);
  background: rgba(255, 255, 255, 0.92);
}
.pol-tabs {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 1.5rem;
  display: flex;
}
.pol-tab {
  padding: 1.15rem 0.25rem;
  margin-right: 2rem;
  font-size: 0.98rem;
  font-weight: 600;
  color: var(--text-muted);
  border-bottom: 2px solid transparent;
  transition: color 0.2s, border-color 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
}
.pol-tab:hover { color: var(--text); }
.pol-tab.active {
  color: var(--primary);
  border-bottom-color: var(--primary);
}

/* ── Body 2-col ──────────────────────────────────────────────────── */
.pol-body {
  max-width: 1100px;
  margin: 0 auto;
  padding: 3.5rem 1.5rem 6rem;
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 4rem;
  align-items: flex-start;
}
@media (max-width: 900px) {
  .pol-body {
    grid-template-columns: 1fr;
    gap: 2rem;
  }
  .pol-toc { position: static; }
  .pol-toc-list { display: grid; grid-template-columns: 1fr 1fr; gap: 0.25rem; }
}
@media (max-width: 600px) {
  .pol-toc-list { grid-template-columns: 1fr; }
  .pol-hero { padding: 3rem 1.25rem 2.5rem; }
  .pol-body { padding: 2.5rem 1.25rem 4rem; }
}

/* ── TOC ─────────────────────────────────────────────────────────── */
.pol-toc {
  position: sticky;
  top: 170px;
  align-self: flex-start;
}
.pol-toc-label {
  font-size: 0.7rem;
  font-weight: 700;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.12em;
  margin-bottom: 1rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--border);
}
.pol-toc-list { display: flex; flex-direction: column; gap: 0.15rem; }
.pol-toc-link {
  padding: 0.5rem 0.75rem;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-muted);
  line-height: 1.4;
  display: flex;
  gap: 0.55rem;
  transition: all 0.15s;
  cursor: pointer;
  border-left: 2px solid transparent;
  margin-left: -2px;
}
.pol-toc-link:hover {
  color: var(--text);
  background: var(--bg-muted);
}
.pol-toc-link.active {
  color: var(--primary);
  background: var(--primary-soft);
  border-left-color: var(--primary);
  font-weight: 600;
}
.pol-toc-num {
  font-variant-numeric: tabular-nums;
  font-size: 0.78rem;
  font-weight: 600;
  flex-shrink: 0;
  padding-top: 0.05rem;
  opacity: 0.7;
}

/* ── Content ─────────────────────────────────────────────────────── */
.pol-content {
  min-width: 0;
  max-width: 720px;
}
.pol-intro {
  padding: 1.25rem 1.5rem;
  background: var(--primary-soft);
  border-left: 3px solid var(--primary);
  border-radius: 0 0.75rem 0.75rem 0;
  color: var(--text-2);
  font-size: 0.95rem;
  line-height: 1.65;
  margin-bottom: 3rem;
}
.pol-intro strong { color: var(--text); font-weight: 600; }

.pol-section {
  margin-bottom: 3rem;
  scroll-margin-top: 180px;
}
.pol-section:last-child { margin-bottom: 0; }
.pol-section-head {
  display: flex;
  align-items: baseline;
  gap: 0.75rem;
  margin-bottom: 1.25rem;
}
.pol-section-num {
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--primary);
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.02em;
}
.pol-section h2 {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text);
  letter-spacing: -0.015em;
  line-height: 1.25;
}

/* Content v-html styles (deep) */
.pol-content :deep(h3) {
  font-size: 1.05rem;
  font-weight: 600;
  color: var(--text);
  margin-top: 1.75rem;
  margin-bottom: 0.75rem;
}
.pol-content :deep(p) {
  font-size: 1rem;
  color: var(--text-muted);
  line-height: 1.75;
  margin-bottom: 1rem;
  text-wrap: pretty;
}
.pol-content :deep(p strong) { color: var(--text-2); font-weight: 600; }
.pol-content :deep(p:last-child) { margin-bottom: 0; }
.pol-content :deep(a) {
  color: var(--primary);
  font-weight: 500;
  text-decoration: underline;
  text-decoration-color: rgba(40, 106, 216, 0.35);
  text-underline-offset: 3px;
  transition: text-decoration-color 0.2s;
}
.pol-content :deep(a:hover) { text-decoration-color: var(--primary); }

.pol-content :deep(.pol-list) {
  list-style: none;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
  margin: 1rem 0 1.25rem;
}
.pol-content :deep(.pol-list li) {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
  font-size: 1rem;
  color: var(--text-muted);
  line-height: 1.65;
}
.pol-content :deep(.pol-list li::before) {
  content: '';
  flex-shrink: 0;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--primary);
  margin-top: 0.65rem;
}
.pol-content :deep(.pol-list li strong) { color: var(--text-2); font-weight: 600; }

.pol-content :deep(.pol-callout) {
  margin: 1.5rem 0;
  padding: 1.25rem 1.5rem;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 0.85rem;
  display: flex;
  gap: 1rem;
  align-items: flex-start;
}
.pol-content :deep(.pol-callout-ico) {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: 0.55rem;
  background: var(--primary-soft);
  color: var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
}
.pol-content :deep(.pol-callout-body) { flex: 1; min-width: 0; }
.pol-content :deep(.pol-callout-title) {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 0.35rem;
}
.pol-content :deep(.pol-callout-text) {
  font-size: 0.9rem;
  color: var(--text-muted);
  line-height: 1.6;
}

.pol-content :deep(.pol-table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1rem 0 1.25rem;
  font-size: 0.92rem;
  border: 1px solid var(--border);
  border-radius: 0.75rem;
  overflow: hidden;
}
.pol-content :deep(.pol-table thead) { background: var(--bg-subtle); }
.pol-content :deep(.pol-table th) {
  text-align: left;
  padding: 0.85rem 1rem;
  font-weight: 600;
  font-size: 0.78rem;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  border-bottom: 1px solid var(--border);
}
.pol-content :deep(.pol-table td) {
  padding: 0.85rem 1rem;
  border-bottom: 1px solid var(--border-2);
  color: var(--text-muted);
  line-height: 1.5;
  vertical-align: top;
}
.pol-content :deep(.pol-table tr:last-child td) { border-bottom: 0; }
.pol-content :deep(.pol-table td:first-child) {
  color: var(--text-2);
  font-weight: 600;
  width: 30%;
}

/* ── End block ───────────────────────────────────────────────────── */
.pol-end {
  margin-top: 4rem;
  padding: 2rem;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%);
  color: #fff;
  border-radius: 1.25rem;
  position: relative;
  overflow: hidden;
  box-shadow: var(--shadow-md);
}
.pol-end::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.14) 0%, transparent 60%);
  pointer-events: none;
}
.pol-end-eyebrow {
  font-size: 0.75rem;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.75);
  text-transform: uppercase;
  letter-spacing: 0.12em;
  margin-bottom: 0.75rem;
  position: relative;
}
.pol-end-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #fff !important;
  margin-bottom: 0.75rem;
  letter-spacing: -0.015em;
  position: relative;
}
.pol-end-desc {
  font-size: 0.98rem;
  color: rgba(255, 255, 255, 0.85) !important;
  line-height: 1.65;
  margin-bottom: 1.5rem;
  max-width: 32rem;
  position: relative;
}
.pol-end-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  position: relative;
}
.pol-end-btn {
  padding: 0.75rem 1.25rem;
  border-radius: 0.6rem;
  font-weight: 600;
  font-size: 0.95rem;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.2s;
  cursor: pointer;
}
.pol-end-btn.primary {
  background: #fff;
  color: var(--primary);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  text-decoration: none;
}
.pol-end-btn.primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.22);
}
.pol-end-btn.ghost {
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(6px);
  font: inherit;
}
.pol-end-btn.ghost:hover { background: rgba(255, 255, 255, 0.22); }

/* ── Footer ──────────────────────────────────────────────────────── */
.pol-footer {
  background: #111827;
  color: #fff;
  padding: 4rem 1.5rem 2rem;
}
.pol-footer-content { max-width: 1280px; margin: 0 auto; }
.pol-footer-grid {
  display: grid;
  gap: 3rem;
  margin-bottom: 3rem;
  grid-template-columns: 2fr 1fr 1fr 1fr;
}
@media (max-width: 900px) {
  .pol-footer-grid { grid-template-columns: 1fr 1fr; }
}
@media (max-width: 600px) {
  .pol-footer-grid { grid-template-columns: 1fr; }
  .pol-end { padding: 1.5rem; }
}

.pol-footer-logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
}
.pol-footer-logo-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.pol-footer-logo-icon img { width: 100%; height: 100%; object-fit: contain; }
.pol-footer-logo-text { font-weight: 700; font-size: 1.1rem; color: #fff; }
.pol-footer-desc { color: rgba(255, 255, 255, 0.6); font-size: 0.9rem; line-height: 1.6; }

.pol-footer-links-title {
  font-weight: 600;
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.5);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin-bottom: 1rem;
}
.pol-footer-links ul { display: flex; flex-direction: column; gap: 0.6rem; }
.pol-footer-links a {
  color: rgba(255, 255, 255, 0.7);
  font-size: 0.9rem;
  transition: color 0.2s;
  text-decoration: none;
}
.pol-footer-links a:hover { color: #fff; }
.pol-footer-bottom {
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  padding-top: 1.5rem;
}
.pol-footer-bottom p { color: rgba(255, 255, 255, 0.4); font-size: 0.875rem; }

/* ── Print ───────────────────────────────────────────────────────── */
@media print {
  .pol-header,
  .pol-footer,
  .pol-tabs-wrap,
  .pol-toc,
  .pol-end { display: none !important; }
  .pol-body { display: block; padding: 0; }
  .pol-content { max-width: 100%; }
  .pol-hero { padding: 1rem 0; background: none; }
  .pol-hero-bg { display: none; }
}
</style>
