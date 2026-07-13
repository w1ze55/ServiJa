<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import {
  Shield, ShieldCheck, CheckCircle, List,
  ArrowRight, Mail,
} from 'lucide-vue-next';
import { useRouter, useRoute } from 'vue-router';
import logo from '@/assets/favicon-32x32.png';
import { privacidadeSections, termosSections, contractMeta } from '@/data/politicasTermos';
import { downloadContractPdf } from '@/utils/contractPdf';

const router = useRouter();
const route = useRoute();
const tab = ref<'privacidade' | 'termos'>(
  route.query.tab === 'termos' ? 'termos' : 'privacidade'
);
const activeId = ref<string | null>(null);

// ── Data ─────────────────────────────────────────────────────────────
const sections = computed(() =>
  tab.value === 'privacidade' ? privacidadeSections : termosSections
);

const heroTitle = computed(() =>
  tab.value === 'privacidade' ? 'Política de Privacidade' : 'Termos de Uso'
);

const heroDesc = computed(() =>
  tab.value === 'privacidade'
    ? 'Transparência sobre quais dados coletamos, como os utilizamos e quais são os seus direitos como titular em linguagem clara, sem juridiquês desnecessário.'
    : 'Regras que organizam a relação entre a Servi Já, contratantes e prestadores para que todos saibam o que esperar da plataforma e uns dos outros.'
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

const downloadingPdf = ref(false);
async function downloadPdf() {
  if (downloadingPdf.value) return;
  downloadingPdf.value = true;
  try {
    await downloadContractPdf(tab.value, logo);
  } catch (err) {
    console.error('Falha ao gerar o PDF do contrato:', err);
    window.print();
  } finally {
    downloadingPdf.value = false;
  }
}

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
            <span>Última atualização: <strong>{{ contractMeta.updatedAt }}</strong></span>
          </div>
          <div class="pol-meta-item">
            <ShieldCheck :size="16" class="pol-meta-ico" />
            <span>Em conformidade com a <strong>LGPD</strong></span>
          </div>
          <div class="pol-meta-item">
            <List :size="16" class="pol-meta-ico" />
            <span>Versão <strong>{{ contractMeta.version }}</strong></span>
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
            Entre em contato, costumamos responder em até 24 horas úteis.
          </p>
          <div class="pol-end-actions">
            <a :href="'mailto:' + endEmail" class="pol-end-btn primary">
              <Mail :size="16" />
              Falar com o time
            </a>
            <button class="pol-end-btn ghost" :disabled="downloadingPdf" @click="downloadPdf()">
              <List :size="16" />
              {{ downloadingPdf ? 'Gerando PDF…' : 'Baixar em PDF' }}
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
  overflow-wrap: anywhere;
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
