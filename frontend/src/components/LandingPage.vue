<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import {
  Search, Star, MessageSquare, Shield, TrendingUp, Users,
  CheckCircle, Sparkles, ListChecks, ShieldCheck,
  Wrench, Zap, Paintbrush, Hammer, Leaf, Package, Snowflake,
  ArrowRight, Mail, Plus, Minus,
} from 'lucide-vue-next';
import Button from './ui/button.vue';
import logo from '@/assets/favicon-32x32.png';

interface Props {
  onLogin: () => void;
  onSignup: () => void;
}

defineProps<Props>();

const isScrolled = ref(false);
const openFaq = ref(0);

const DESKTOP_BREAKPOINT = 836;
const COLLAPSE_SCROLL_Y = 80;
const EXPAND_SCROLL_Y = 32;

function handleScroll() {
  if (window.innerWidth < DESKTOP_BREAKPOINT) {
    isScrolled.value = false;
    return;
  }
  const currentScrollY = window.scrollY;
  if (!isScrolled.value && currentScrollY >= COLLAPSE_SCROLL_Y) {
    isScrolled.value = true;
    return;
  }
  if (isScrolled.value && currentScrollY <= EXPAND_SCROLL_Y) {
    isScrolled.value = false;
  }
}

function toggleFaq(i: number) {
  openFaq.value = openFaq.value === i ? -1 : i;
}

onMounted(() => {
  handleScroll();
  window.addEventListener('scroll', handleScroll, { passive: true });
  window.addEventListener('resize', handleScroll, { passive: true });
});

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll);
  window.removeEventListener('resize', handleScroll);
});

const categorias = [
  { name: 'Encanador',     icon: Wrench,    count: '+1.2k' },
  { name: 'Eletricista',   icon: Zap,       count: '+980'  },
  { name: 'Diarista',      icon: Sparkles,  count: '+2.1k' },
  { name: 'Pintor',        icon: Paintbrush,count: '+640'  },
  { name: 'Pedreiro',      icon: Hammer,    count: '+540'  },
  { name: 'Jardineiro',    icon: Leaf,      count: '+320'  },
  { name: 'Marceneiro',    icon: Package,   count: '+280'  },
  { name: 'Técnico em Ar', icon: Snowflake, count: '+410'  },
];

const comoFunciona = [
  { title: 'Busque o serviço',
    body: 'Pesquise por categoria, localização ou palavra-chave e encontre os melhores profissionais.' },
  { title: 'Compare perfis',
    body: 'Veja portfólios, avaliações e histórico de cada prestador antes de decidir.' },
  { title: 'Solicite e contrate',
    body: 'Peça orçamentos, negocie e contrate de forma direta e segura pela plataforma.' },
];

const beneficiosContratantes = [
  { icon: Search,      title: 'Busque por categoria e localização',
    body: 'Encontre o melhor profissional para o seu serviço com filtros inteligentes.' },
  { icon: ListChecks,  title: 'Compare perfis e avaliações',
    body: 'Veja o histórico e a reputação de cada prestador antes de decidir.' },
  { icon: MessageSquare, title: 'Negocie com segurança',
    body: 'Peça orçamentos e contrate de forma direta pela plataforma.' },
];

const beneficiosPrestadores = [
  { icon: Star,        title: 'Sistema de Avaliações Inteligente',
    body: 'Resumos automáticos com IA organizam feedbacks de forma clara para facilitar sua escolha.' },
  { icon: Shield,      title: 'Transparência Total',
    body: 'Perfis públicos completos com histórico, portfólio e reputação verificada.' },
  { icon: TrendingUp,  title: 'Fortaleça sua marca',
    body: 'Prestadores constroem sua reputação no mercado digital com perfil profissional.' },
];

const faqItems = [
  { q: 'Como funciona o Servi Já?',
    a: 'Você busca o serviço, compara perfis de prestadores próximos e contrata pelo aplicativo. Toda comunicação, orçamento e avaliação acontece dentro da plataforma.' },
  { q: 'É grátis para contratantes?',
    a: 'Sim. Criar conta, buscar prestadores e pedir orçamentos é totalmente gratuito. Você só paga pelo serviço contratado, direto com o profissional.' },
  { q: 'Como sei que o profissional é confiável?',
    a: 'Cada perfil mostra avaliações verificadas de outros clientes, histórico de serviços concluídos e documentos validados pela nossa equipe.' },
  { q: 'Como funciona o pagamento?',
    a: 'Você combina o valor com o prestador no chat e paga pelo app — com cartão, Pix ou na carteira Servi Já. O valor só é liberado após a conclusão do serviço.' },
  { q: 'Posso cancelar uma solicitação?',
    a: 'Sim, a qualquer momento antes da execução do serviço, sem custo. Após o início, valem as regras combinadas com o profissional.' },
];
</script>

<template>
  <div class="landing-page">
    <!-- Header (mantido do original) -->
    <header class="header" :class="{ scrolled: isScrolled }">
      <div class="header-content">
        <div class="logo-container">
          <div class="logo-icon">
            <img :src="`${logo}`" alt="Servi Já" />
          </div>
          <span class="logo-title">Servi Já</span>
        </div>

        <nav class="nav-links">
          <a href="#como-funciona" class="nav-link">Como funciona</a>
          <a href="#diferenciais" class="nav-link">Diferenciais</a>
          <a href="#contratantes" class="nav-link">Para Contratantes</a>
        </nav>

        <div class="header-actions">
          <Button variant="ghost" @click="$props.onLogin" class="btn-login">Entrar</Button>
          <Button @click="$props.onSignup" class="btn-signup">Cadastrar</Button>
        </div>
      </div>
    </header>

    <!-- Hero (mantido do original) -->
    <section class="hero-section">
      <div class="hero-bg-overlay">
        <img src="/BG.svg" alt="Background" class="hero-bg-img" />
      </div>

      <div class="hero-container">
        <div class="hero-content-left">
          <h1 class="hero-title-v2">
            Encontre os melhores
            <span class="hero-highlight-v2">profissionais</span>
            para qualquer serviço
          </h1>
          <p class="hero-description-v2">
            A plataforma mais completa para conectar você aos melhores prestadores.
            Segurança, qualidade e agilidade em um só lugar.
          </p>
          <div class="hero-actions-v2">
            <Button size="lg" @click="$props.onSignup" class="btn-primary-v2">
              Começar agora - É grátis
              <CheckCircle :size="20" />
            </Button>
            <div class="hero-trust-badges">
              <div class="trust-item">
                <ShieldCheck :size="18" />
                <span>100% Seguro</span>
              </div>
              <div class="trust-item">
                <Star :size="18" />
                <span>Avaliações aprimoradas</span>
              </div>
            </div>
          </div>
        </div>

        <div class="hero-visual-right">
          <div class="visual-wrapper">
            <img src="/peopleblue.svg" alt="Profissionais" class="hero-people-img" />
            <div class="floating-card card-1">
              <div class="card-icon blue"><CheckCircle :size="16" /></div>
              <div class="card-info">
                <span class="card-label">Qualidade</span>
                <span class="card-val">Verificada</span>
              </div>
            </div>
            <div class="floating-card card-2">
              <div class="card-icon yellow"><Star :size="16" /></div>
              <div class="card-info">
                <span class="card-label">Avaliação</span>
                <span class="card-val">4.9/5.0</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <img src="/line.svg" alt="Decorative Line Top" class="hero-line-img-inverted" />
      <img src="/line.svg" alt="Decorative Line Bottom" class="hero-line-img-fixed" />
    </section>

    <!-- Categorias — rail horizontal -->
    <section class="b-cat" id="categorias">
      <div class="b-cat-head">
        <div class="b-eyebrow">
          <span class="b-eyebrow-num">01</span>
          <span class="b-eyebrow-label">Categorias</span>
        </div>
        <h2 class="b-section-title">
          Encontre profissionais<br />
          <em>em qualquer área.</em>
        </h2>
      </div>
      <div class="b-cat-rail">
        <div class="b-cat-track">
          <div v-for="_ in 2" class="b-cat-set" aria-hidden="true">
            <div v-for="c in categorias" :key="c.name" class="b-cat-pill">
              <div class="b-cat-pill-ico">
                <component :is="c.icon" :size="26" />
              </div>
              <div class="b-cat-pill-info">
                <span class="b-cat-pill-name">{{ c.name }}</span>
                <span class="b-cat-pill-count">{{ c.count }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Como funciona — timeline -->
    <section class="b-how" id="como-funciona">
      <div class="b-section-inner">
        <div class="b-how-head">
          <div class="b-eyebrow">
            <span class="b-eyebrow-num">02</span>
            <span class="b-eyebrow-label">Como funciona</span>
          </div>
          <h2 class="b-section-title">
            Três passos.<br /><em>Sem fricção.</em>
          </h2>
        </div>
        <div class="b-timeline">
          <div class="b-timeline-line"></div>
          <div v-for="(f, i) in comoFunciona" :key="f.title" class="b-step">
            <div class="b-step-num">{{ String(i + 1).padStart(2, '0') }}</div>
            <div class="b-step-body">
              <h3 class="b-step-title">{{ f.title }}</h3>
              <p class="b-step-desc">{{ f.body }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Diferenciais — Contratantes + Prestadores (painéis contrastados) -->
    <section class="b-duo" id="diferenciais">
      <div class="b-duo-grid">
        <article class="b-panel b-panel-light" id="contratantes">
          <div class="b-panel-eyebrow">
            <span class="b-eyebrow-num">03</span>
            <span>Para Contratantes</span>
          </div>
          <h3 class="b-panel-title">
            Quem contrata, contrata <em>com confiança.</em>
          </h3>
          <div class="b-panel-img">
            <div class="b-placeholder">
              <div class="b-placeholder-grid"></div>
              <span>screenshot · tela de busca</span>
            </div>
          </div>
          <ul class="b-panel-list">
            <li v-for="b in beneficiosContratantes" :key="b.title">
              <CheckCircle :size="18" />
              <div>
                <strong>{{ b.title }}</strong>
                <span>{{ b.body }}</span>
              </div>
            </li>
          </ul>
          <Button variant="secondary" class="b-panel-btn" @click="$props.onSignup">
            Cadastre-se como Contratante <ArrowRight :size="18" />
          </Button>
        </article>

        <article class="b-panel b-panel-dark" id="prestadores">
          <div class="b-panel-eyebrow">
            <span class="b-eyebrow-num light">04</span>
            <span>Para Prestadores</span>
          </div>
          <h3 class="b-panel-title">
            Quem presta, prospera <em>com reputação.</em>
          </h3>
          <div class="b-panel-img">
            <div class="b-placeholder dark">
              <div class="b-placeholder-grid"></div>
              <span>screenshot · perfil do prestador</span>
            </div>
          </div>
          <ul class="b-panel-list">
            <li v-for="b in beneficiosPrestadores" :key="b.title">
              <CheckCircle :size="18" />
              <div>
                <strong>{{ b.title }}</strong>
                <span>{{ b.body }}</span>
              </div>
            </li>
          </ul>
          <Button variant="secondary" class="b-panel-btn invert" @click="$props.onSignup">
            Cadastre-se como Prestador <ArrowRight :size="18" />
          </Button>
        </article>
      </div>
    </section>

    <!-- FAQ -->
    <section class="b-faq" id="faq">
      <div class="b-section-inner">
        <div class="b-faq-grid">
          <div class="b-faq-side">
            <div class="b-eyebrow">
              <span class="b-eyebrow-num">05</span>
              <span class="b-eyebrow-label">Dúvidas</span>
            </div>
            <h2 class="b-section-title">
              Perguntas<br /><em>frequentes.</em>
            </h2>
            <p class="b-faq-side-desc">Não achou o que procura? Fale com a gente.</p>
            <a href="mailto:contato@servija.com.br" class="b-faq-side-link">
              <Mail :size="16" /> contato@servija.com.br
            </a>
          </div>
          <div class="b-faq-list">
            <div
              v-for="(it, i) in faqItems"
              :key="it.q"
              class="b-faq-item"
              :class="{ open: openFaq === i }"
            >
              <button class="b-faq-q" @click="toggleFaq(i)">
                <span class="b-faq-q-text">{{ it.q }}</span>
                <span class="b-faq-q-ico">
                  <Minus v-if="openFaq === i" :size="22" />
                  <Plus v-else :size="22" />
                </span>
              </button>
              <div v-if="openFaq === i" class="b-faq-a">{{ it.a }}</div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA Final -->
    <section class="b-final">
      <div class="b-final-inner">
        <h2 class="b-final-title">
          Comece agora.<br />
          <em>É grátis.</em>
        </h2>
        <p class="b-final-desc">
          Milhares de profissionais e contratantes já confiam no Servi Já.
          Junte-se em menos de 2 minutos.
        </p>
        <div class="b-final-actions">
          <Button size="lg" class="b-final-btn primary" @click="$props.onSignup">
            Quero contratar <ArrowRight :size="18" />
          </Button>
          <Button size="lg" variant="ghost" class="b-final-btn ghost" @click="$props.onSignup">
            Sou prestador <ArrowRight :size="18" />
          </Button>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer class="footer">
      <div class="footer-content">
        <div class="footer-grid">
          <div class="footer-brand">
            <div class="footer-logo">
              <div class="footer-logo-icon">
                <img :src="`${logo}`" alt="Servi Já" />
              </div>
              <span class="footer-logo-text">Servi Já</span>
            </div>
            <p class="footer-description">
              O lugar onde você encontra os melhores prestadores de serviço.
            </p>
          </div>

          <div class="footer-links">
            <h4 class="footer-links-title">Para Contratantes</h4>
            <ul class="footer-links-list">
              <li><a href="#como-funciona" class="footer-link">Como funciona</a></li>
              <li><a href="#diferenciais" class="footer-link">Diferenciais</a></li>
              <li><a href="#contratantes" class="footer-link">Para Contratantes</a></li>
            </ul>
          </div>

          <div class="footer-links">
            <h4 class="footer-links-title">Para Prestadores</h4>
            <ul class="footer-links-list">
              <li><a href="#prestadores" class="footer-link">Seja um prestador</a></li>
              <li><a href="#diferenciais" class="footer-link">Diferenciais</a></li>
              <li><a href="#" class="footer-link">Central de ajuda</a></li>
            </ul>
          </div>

          <div class="footer-links">
            <h4 class="footer-links-title">Institucional</h4>
            <ul class="footer-links-list">
              <li><a href="#" class="footer-link">Sobre nós</a></li>
              <li><router-link to="/politicas?tab=termos" class="footer-link">Termos de uso</router-link></li>
              <li><router-link to="/politicas" class="footer-link">Privacidade</router-link></li>
            </ul>
          </div>
        </div>

        <div class="footer-bottom">
          <p>&copy; 2026 Servi Já. Todos os direitos reservados.</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
/* ===== Base ===== */
.landing-page {
  min-height: 100vh;
  background: #ffffff;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  color: #0f172a;
  --primary: #286ad8;
  --primary-dark: #143774;
  --primary-soft: rgba(40, 106, 216, 0.08);
}

/* ===== Header ===== */
.header {
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 50;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  transition:
    background 0.4s ease, box-shadow 0.4s ease, border-color 0.4s ease,
    margin 0.4s ease, border-radius 0.4s ease, top 0.4s ease, width 0.4s ease;
}
.header-content {
  max-width: 1280px; margin: 0 auto;
  padding: 1rem 1.5rem;
  display: flex; align-items: center; justify-content: space-between;
  transition: padding 0.4s ease;
}
.logo-container { display: flex; align-items: center; gap: 0.75rem; }
.logo-icon {
  width: 5rem; height: 5rem;
  display: flex; margin: -10px;
  align-items: center; justify-content: center;
  transition: transform 0.3s ease, width 0.3s ease, height 0.3s ease;
}
.logo-title {
  font-weight: 700; font-size: 1.25rem;
  background: #286ad8;
  -webkit-background-clip: text; background-clip: text;
  -webkit-text-fill-color: transparent;
}
.nav-links { display: none; gap: 2rem; }
.nav-link { color: #6b7280; font-weight: 500; transition: color 0.2s ease; position: relative; }
.nav-link::after {
  content: ''; position: absolute; bottom: -4px; left: 0; width: 0; height: 2px;
  background: linear-gradient(135deg, #286ad8 0%, #143774 100%);
  transition: width 0.3s ease;
}
.nav-link:hover { color: #286ad8; }
.nav-link:hover::after { width: 100%; }
.header-actions { display: flex; align-items: center; gap: 0.75rem; }
.btn-login { font-weight: 500; }
.btn-signup {
  background: linear-gradient(135deg, #286ad8 0%, #143774 100%);
  color: white; font-weight: 600;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
  transition: all 0.3s ease;
}
.btn-signup:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(40, 106, 216, 0.4);
}

/* ===== Hero ===== */
.hero-section {
  position: relative; min-height: 100vh;
  display: flex; align-items: center;
  background: white;
}
.hero-bg-overlay {
  position: absolute; top: -20rem; left: 0; width: 100%; height: 120%;
  z-index: 1; pointer-events: none;
}
.hero-bg-img { width: 100%; height: 100%; object-fit: cover; opacity: 0.9; }
.hero-container {
  max-width: 1280px; margin: 0 auto; width: 100%;
  display: grid; grid-template-columns: 1.1fr 0.9fr;
  gap: 4rem; align-items: center; position: relative; z-index: 10;
}
.hero-content-left { animation: slideInLeft 1s ease-out; }
.hero-title-v2 {
  font-size: clamp(2.5rem, 5vw, 4rem); line-height: 1.1; font-weight: 800;
  color: #0f172a; margin-bottom: 1.5rem; letter-spacing: -0.02em;
}
.hero-highlight-v2 { color: #286ad8; position: relative; }
.hero-description-v2 {
  font-size: clamp(1.1rem, 2vw, 1.25rem); color: #475569;
  line-height: 1.6; max-width: 540px; margin-bottom: 2.5rem;
}
.hero-actions-v2 { display: flex; flex-direction: column; gap: 1.5rem; }
.btn-primary-v2 {
  background: #286ad8; color: white;
  padding: 1.25rem 2.5rem; border-radius: 1rem;
  font-size: 1.125rem; font-weight: 700; width: fit-content;
  box-shadow: 0 10px 25px rgba(40, 106, 216, 0.25);
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}
.btn-primary-v2:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 15px 35px rgba(40, 106, 216, 0.35);
  background: #1e58bc;
}
.hero-trust-badges { display: flex; gap: 2rem; align-items: center; }
.trust-item {
  display: flex; align-items: center; gap: 0.5rem;
  color: #64748b; font-size: 0.875rem; font-weight: 500;
}
.trust-item svg { color: #286ad8; }
.hero-visual-right { position: relative; animation: slideInRight 1.2s ease-out; }
.visual-wrapper { position: relative; width: 100%; max-width: 600px; }
.hero-people-img {
  width: 100%; height: auto;
  filter: drop-shadow(0 20px 50px rgba(0, 0, 0, 0.1));
  position: relative; z-index: 5;
}
.hero-line-img-fixed {
  position: absolute; top: 80.5%; right: 0; width: 20%; max-width: 45vw;
  z-index: 1; opacity: 0.8; pointer-events: none; transform: translateY(-50%);
}
.hero-line-img-inverted {
  position: absolute; top: 10%; left: 0; width: 20%; max-width: 45vw;
  z-index: 1; opacity: 0.8; pointer-events: none; transform: scaleX(-1) scaleY(-1);
}
.floating-card {
  position: absolute; background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px); padding: 1rem; border-radius: 1rem;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05);
  display: flex; align-items: center; gap: 0.8rem; z-index: 20;
}
.card-1 { top: 15%; right: -5%; }
.card-2 { bottom: 20%; left: -5%; }
.card-icon {
  width: 32px; height: 32px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
}
.card-icon.blue { background: rgba(40, 106, 216, 0.1); color: #286ad8; }
.card-icon.yellow { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }
.card-info { display: flex; flex-direction: column; }
.card-label { font-size: 0.75rem; color: #64748b; font-weight: 500; }
.card-val { font-size: 0.9rem; color: #0f172a; font-weight: 700; }

@keyframes slideInLeft {
  from { opacity: 0; transform: translateX(-40px); }
  to   { opacity: 1; transform: translateX(0); }
}
@keyframes slideInRight {
  from { opacity: 0; transform: translateX(40px); }
  to   { opacity: 1; transform: translateX(0); }
}

@media (max-width: 1024px) {
  .hero-container { grid-template-columns: 1fr; text-align: center; gap: 3rem; }
  .hero-content-left { display: flex; flex-direction: column; align-items: center; }
  .hero-description-v2 { margin-left: auto; margin-right: auto; }
  .hero-actions-v2 { align-items: center; }
  .hero-visual-right { order: -1; max-width: 500px; margin: 0 auto; }
}
@media (max-width: 640px) {
  .hero-section { padding-top: 60px; }
  .floating-card { display: none; }
}

/* ===== Variant B — base ===== */
.b-section-inner { max-width: 1240px; margin: 0 auto; padding: 6rem 2rem; }

.b-eyebrow {
  display: inline-flex; align-items: baseline; gap: 0.8rem;
  margin-bottom: 1.5rem;
}
.b-eyebrow-num {
  font-family: 'Fraunces', Georgia, serif;
  font-size: 0.95rem; font-weight: 600;
  color: #286ad8; letter-spacing: 0.02em;
}
.b-eyebrow-num.light { color: rgba(255,255,255,0.7); }
.b-eyebrow-label {
  font-size: 0.75rem; font-weight: 600;
  color: #475569; text-transform: uppercase;
  letter-spacing: 0.15em;
}
.b-section-title {
  font-size: clamp(2.5rem, 5vw, 4rem); font-weight: 700;
  line-height: 1.04; letter-spacing: -0.03em; color: #0f172a;
}
.b-section-title em,
.b-panel-title em,
.b-final-title em {
  font-style: italic;
  font-family: 'Fraunces', Georgia, serif;
  font-weight: 500; color: #286ad8;
  letter-spacing: -0.01em;
}

/* ===== Categorias — rail ===== */
.b-cat {
  padding: 6rem 0 5rem;
  background: #ffffff;
}
.b-cat-head { max-width: 1240px; margin: 0 auto; padding: 0 2rem 2.5rem; }
.b-cat-rail {
  overflow: hidden;
  padding: 0.5rem 0 1.5rem;
  mask-image: linear-gradient(to right, transparent 0%, black 8%, black 92%, transparent 100%);
  -webkit-mask-image: linear-gradient(to right, transparent 0%, black 8%, black 92%, transparent 100%);
}
.b-cat-track {
  display: flex; gap: 0;
  width: max-content;
  animation: marquee 32s linear infinite;
}
.b-cat-set {
  display: flex; gap: 0.9rem;
  padding-right: 0.9rem;
  flex-shrink: 0;
}
@keyframes marquee {
  from { transform: translateX(0); }
  to   { transform: translateX(-50%); }
}
.b-cat-pill {
  flex: 0 0 auto;
  background: #ffffff; border: 1px solid rgba(0,0,0,0.07);
  border-radius: 1.25rem; padding: 1.1rem 1.5rem 1.1rem 1.1rem;
  display: flex; align-items: center; gap: 0.9rem;
  min-width: 230px; pointer-events: none; user-select: none;
}
.b-cat-pill-ico {
  width: 44px; height: 44px; border-radius: 12px;
  background: var(--primary-soft); color: #286ad8;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.b-cat-pill-info { display: flex; flex-direction: column; }
.b-cat-pill-name { font-weight: 600; color: #0f172a; }
.b-cat-pill-count { font-size: 0.82rem; color: #6b7280; }
.b-cat-footer {
  max-width: 1240px; margin: 0 auto; padding: 1.5rem 2rem 0;
  display: flex; justify-content: space-between; align-items: center;
  color: #6b7280; font-size: 0.95rem;
}
.b-cat-footer a {
  display: inline-flex; align-items: center; gap: 0.4rem;
  color: #286ad8; font-weight: 600; text-decoration: none;
}

/* ===== Como funciona — timeline ===== */
.b-how {
  background: linear-gradient(to bottom, #f9fafb 0%, #ffffff 100%);
}
.b-how-head { margin-bottom: 4rem; max-width: 720px; }
.b-timeline {
  position: relative; padding-left: 5rem;
  display: flex; flex-direction: column; gap: 3rem;
}
.b-timeline-line {
  position: absolute; left: 2.2rem; top: 1rem; bottom: 1rem; width: 1px;
  background: linear-gradient(to bottom, transparent 0%, #286ad8 15%, #286ad8 85%, transparent 100%);
}
.b-step { position: relative; }
.b-step-num {
  position: absolute; left: -5rem; top: -0.5rem;
  width: 4.4rem; height: 4.4rem; border-radius: 50%;
  background: #ffffff; border: 1px solid rgba(0,0,0,0.07);
  display: flex; align-items: center; justify-content: center;
  font-family: 'Fraunces', Georgia, serif;
  font-size: 1.4rem; font-weight: 600;
  color: #286ad8; letter-spacing: -0.02em;
}
.b-step-body { max-width: 640px; }
.b-step-ico {
  width: 40px; height: 40px; border-radius: 10px;
  background: var(--primary-soft); color: #286ad8;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 1rem;
}
.b-step-title {
  font-size: 1.75rem; font-weight: 700;
  color: #0f172a; margin-bottom: 0.6rem;
  letter-spacing: -0.02em;
}
.b-step-desc { color: #475569; font-size: 1.05rem; line-height: 1.65; }

/* ===== Duo — painéis claro + escuro ===== */
.b-duo { padding: 6rem 0; background: #ffffff; }
.b-duo-grid {
  max-width: 1320px; margin: 0 auto; padding: 0 2rem;
  display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem;
}
.b-panel {
  border-radius: 2rem; padding: 2.75rem;
  display: flex; flex-direction: column;
}
.b-panel-light {
  background: #f9fafb;
  border: 1px solid rgba(0,0,0,0.06);
  color: #0f172a;
}
.b-panel-dark {
  background: #0a0f1c; color: #f1f5f9;
  position: relative; overflow: hidden;
}
.b-panel-dark::after {
  content: ''; position: absolute; top: -50%; right: -50%;
  width: 600px; height: 600px;
  background: radial-gradient(circle, rgba(40, 106, 216, 0.30) 0%, transparent 60%);
  pointer-events: none;
}
.b-panel-eyebrow {
  display: flex; align-items: baseline; gap: 0.8rem;
  font-size: 0.75rem; font-weight: 600;
  text-transform: uppercase; letter-spacing: 0.15em;
  margin-bottom: 1rem; position: relative;
}
.b-panel-light .b-panel-eyebrow { color: #475569; }
.b-panel-dark  .b-panel-eyebrow { color: rgba(255,255,255,0.6); }
.b-panel-title {
  font-size: clamp(1.75rem, 3vw, 2.4rem); font-weight: 700;
  line-height: 1.1; letter-spacing: -0.02em;
  margin-bottom: 2rem; position: relative;
}
.b-panel-dark .b-panel-title em { color: #92b6f0; }
.b-panel-img { margin-bottom: 2rem; position: relative; border-radius: 1.25rem; overflow: hidden; }
.b-placeholder {
  aspect-ratio: 16/10; position: relative;
  background: #e8eaf0;
  display: flex; align-items: center; justify-content: center;
  font-family: ui-monospace, 'SF Mono', Menlo, monospace; font-size: 0.8rem;
  color: rgba(0,0,0,0.45); letter-spacing: 0.02em;
}
.b-placeholder.dark { background: #1a2236; color: rgba(255,255,255,0.45); }
.b-placeholder-grid {
  position: absolute; inset: 0;
  background-image:
    linear-gradient(to right, rgba(0,0,0,0.04) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(0,0,0,0.04) 1px, transparent 1px);
  background-size: 24px 24px;
}
.b-placeholder.dark .b-placeholder-grid {
  background-image:
    linear-gradient(to right, rgba(255,255,255,0.04) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(255,255,255,0.04) 1px, transparent 1px);
}
.b-placeholder span {
  position: relative; z-index: 1;
  padding: 0.4rem 0.75rem; background: rgba(255,255,255,0.5); border-radius: 4px;
}
.b-placeholder.dark span { background: rgba(0,0,0,0.4); }
.b-panel-list {
  list-style: none; padding: 0; margin: 0 0 2rem;
  display: flex; flex-direction: column; gap: 1rem;
  position: relative; flex: 1;
}
.b-panel-list li {
  display: flex; align-items: flex-start; gap: 0.75rem;
  font-size: 0.95rem; line-height: 1.5;
}
.b-panel-light .b-panel-list svg { color: #286ad8; flex-shrink: 0; margin-top: 0.15rem; }
.b-panel-dark  .b-panel-list svg { color: #92b6f0; flex-shrink: 0; margin-top: 0.15rem; }
.b-panel-list strong { display: block; font-weight: 600; margin-bottom: 0.15rem; }
.b-panel-light .b-panel-list span { color: #475569; }
.b-panel-dark  .b-panel-list span { color: rgba(255,255,255,0.65); }
.b-panel-btn {
  align-self: flex-start;
  background: #0f172a !important; color: #ffffff !important;
  font-weight: 600; padding: 0.95rem 1.5rem; border-radius: 0.75rem;
  display: inline-flex; align-items: center; gap: 0.5rem;
  transition: all .2s; position: relative;
  border: 0; cursor: pointer;
}
.b-panel-btn:hover { transform: translateX(3px); }
.b-panel-btn.invert { background: #ffffff !important; color: #0a0f1c !important; }

/* ===== FAQ ===== */
.b-faq {
  background: #ffffff;
}
.b-faq-grid {
  display: grid; grid-template-columns: 1fr 1.4fr; gap: 4rem;
  align-items: flex-start;
}
.b-faq-side { position: sticky; top: 6rem; }
.b-faq-side .b-section-title { margin-bottom: 1.5rem; }
.b-faq-side-desc { color: #475569; margin-bottom: 1rem; font-size: 1.05rem; }
.b-faq-side-link {
  display: inline-flex; align-items: center; gap: 0.5rem;
  color: #286ad8; font-weight: 600; text-decoration: none;
}
.b-faq-list { display: flex; flex-direction: column; }
.b-faq-item { border-bottom: 1px solid rgba(0,0,0,0.07); }
.b-faq-item:first-child { border-top: 1px solid rgba(0,0,0,0.07); }
.b-faq-q {
  width: 100%; padding: 1.5rem 0;
  display: flex; justify-content: space-between; align-items: center; gap: 1rem;
  text-align: left; background: transparent; border: 0; cursor: pointer;
  font: inherit;
}
.b-faq-q-text { font-size: 1.15rem; font-weight: 600; color: #0f172a; }
.b-faq-q-ico {
  width: 32px; height: 32px; border-radius: 50%;
  background: #f3f4f6;
  display: flex; align-items: center; justify-content: center;
  color: #286ad8; flex-shrink: 0;
  transition: background .2s, color .2s;
}
.b-faq-item.open .b-faq-q-ico { background: #286ad8; color: #ffffff; }
.b-faq-a {
  padding: 0 0 1.5rem; color: #475569;
  font-size: 1rem; line-height: 1.7; max-width: 90%;
}

/* ===== CTA Final ===== */
.b-final {
  background: #0a0f1c; color: #ffffff;
  padding: 7rem 2rem; position: relative; overflow: hidden;
}
.b-final::before {
  content: ''; position: absolute; top: -200px; right: -200px;
  width: 700px; height: 700px;
  background: radial-gradient(circle, rgba(40,106,216,0.35) 0%, transparent 60%);
}
.b-final::after {
  content: ''; position: absolute; bottom: -300px; left: -200px;
  width: 700px; height: 700px;
  background: radial-gradient(circle, rgba(20,55,116,0.60) 0%, transparent 60%);
}
.b-final-inner { max-width: 1240px; margin: 0 auto; position: relative; text-align: center; }
.b-final-title {
  font-size: clamp(3rem, 6vw, 5rem); font-weight: 700;
  line-height: 1.02; letter-spacing: -0.03em;
  margin-bottom: 1.5rem; color: #ffffff;
}
.b-final-title em {
  font-family: 'Fraunces', Georgia, serif;
  font-style: italic; font-weight: 500; color: #92b6f0;
}
.b-final-desc {
  font-size: 1.2rem; color: rgba(255,255,255,0.75);
  max-width: 48rem; margin: 0 auto 2.5rem; line-height: 1.6;
}
.b-final-actions {
  display: flex; justify-content: center; gap: 1rem; flex-wrap: wrap;
}
.b-final-btn {
  padding: 1.1rem 2rem; border-radius: 0.85rem;
  font-weight: 700; font-size: 1.05rem;
  display: inline-flex; align-items: center; gap: 0.5rem;
  transition: all .25s;
}
.b-final-btn.primary {
  background: #286ad8 !important; color: #ffffff !important;
  box-shadow: 0 10px 30px rgba(40,106,216,0.35);
  border: 0;
}
.b-final-btn.primary:hover {
  transform: translateY(-3px);
  box-shadow: 0 15px 40px rgba(40,106,216,0.45);
}
.b-final-btn.ghost {
  background: rgba(255,255,255,0.08) !important; color: #ffffff !important;
  border: 1px solid rgba(255,255,255,0.18); backdrop-filter: blur(10px);
}
.b-final-btn.ghost:hover { background: rgba(255,255,255,0.14) !important; }

/* ===== Footer ===== */
.footer {
  background: #ffffff; color: #0f172a;
  padding: 4rem 1.5rem 2rem;
  position: relative; overflow: hidden;
}
.footer-content { max-width: 1280px; margin: 0 auto; position: relative; z-index: 1; }
.footer-grid {
  display: grid; gap: 2rem; margin-bottom: 3rem;
  grid-template-columns: 2fr 1fr 1fr 1fr;
}
.footer-brand { max-width: 20rem; }
.footer-logo { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 1rem; }
.footer-logo-icon {
  width: 3rem; height: 3rem; background: #f1f5f9;
  border-radius: 0.5rem;
  display: flex; align-items: center; justify-content: center;
}
.footer-logo-text { font-weight: 700; font-size: 1.125rem; color: #0f172a; }
.footer-description { color: #6b7280; line-height: 1.75; }
.footer-links-title { font-weight: 700; color: #0f172a; margin-bottom: 1.25rem; }
.footer-links-list {
  display: flex; flex-direction: column; gap: 0.75rem;
  list-style: none; padding: 0; margin: 0;
}
.footer-link { color: #6b7280; transition: color 0.2s ease; text-decoration: none; }
.footer-link:hover { color: #286ad8; }
.footer-bottom {
  padding-top: 2rem; border-top: 1px solid rgba(0,0,0,0.07);
  text-align: center; color: #9ca3af; font-size: 0.875rem;
}

/* ===== Header — sticky scrolled ===== */
@media (min-width: 836px) {
  .header.scrolled {
    background: rgba(255, 255, 255, 0.92);
    backdrop-filter: blur(20px);
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
    border-bottom: none;
    margin: 12px auto 0;
    border-radius: 100px;
    top: 1.5rem;
    width: 50%;
    min-width: 800px;
    max-width: 95%;
  }
  .header.scrolled .header-content { padding: 0.5rem 1.25rem; }
  .header.scrolled .logo-icon { width: 2.25rem; height: 2.25rem; margin: 0px; }
}
@media (min-width: 768px) {
  .nav-links { display: flex; }
  .hero-section { padding: 8rem 1.5rem; }
}

/* ===== Responsivo Variant B ===== */
@media (max-width: 1024px) {
  .b-duo-grid { grid-template-columns: 1fr; }
  .b-faq-grid { grid-template-columns: 1fr; gap: 2.5rem; }
  .b-faq-side { position: static; }
  .footer-grid { grid-template-columns: 2fr 1fr 1fr 1fr; }
}
@media (max-width: 768px) {
  .b-section-inner { padding: 4rem 1.25rem; }
  .b-cat-head { padding: 0 1.25rem 2rem; }
  .b-cat-rail { padding: 0.5rem 1.25rem 1.5rem; }
  .b-cat-footer { padding: 1.5rem 1.25rem 0; flex-direction: column; gap: 0.75rem; }
  .b-timeline { padding-left: 4rem; }
  .b-step-num { left: -4rem; width: 3.5rem; height: 3.5rem; font-size: 1.1rem; }
  .b-timeline-line { left: 1.75rem; }
  .b-panel { padding: 2rem 1.5rem; }
  .b-final { padding: 5rem 1.5rem; }
  .footer-grid { grid-template-columns: 1fr 1fr; }
}
</style>
