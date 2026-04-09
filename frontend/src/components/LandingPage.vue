<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { Search, Star, MessageSquare, Shield, TrendingUp, Users, CheckCircle, Sparkles, ListChecks, ShieldCheck } from 'lucide-vue-next';
import Button from './ui/button.vue';
import logo from '@/assets/favicon-32x32.png';

interface Props {
  onLogin: () => void;
  onSignup: () => void;
}

defineProps<Props>();

const isScrolled = ref(false);

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

onMounted(() => {
  handleScroll();
  window.addEventListener('scroll', handleScroll, { passive: true });
  window.addEventListener('resize', handleScroll, { passive: true });
});

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll);
  window.removeEventListener('resize', handleScroll);
});
</script>

<template>
  <div class="landing-page">
    <!-- Header -->
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

    <!-- Hero Section -->
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

    <!-- Features -->
    <section id="como-funciona" class="features-section">
      <div class="features-content">
        <div class="section-header">
          <span class="section-badge">Como Funciona</span>
          <h2 class="section-title">Simples, rápido e confiável</h2>
          <p class="section-description">Uma plataforma completa para conectar prestadores e contratantes</p>
        </div>

        <div class="features-grid">
          <div class="feature-card">
            <div class="feature-icon feature-icon-blue">
              <Search :size="32" />
            </div>
            <div class="feature-step">Passo 1</div>
            <h3 class="feature-title">Busque o serviço</h3>
            <p class="feature-description">
              Pesquise por categoria, localização ou palavra-chave e encontre os melhores
              profissionais
            </p>
          </div>

          <div class="feature-card">
            <div class="feature-icon feature-icon-purple">
              <Users :size="32" />
            </div>
            <div class="feature-step">Passo 2</div>
            <h3 class="feature-title">Compare perfis</h3>
            <p class="feature-description">
              Veja portfólios, avaliações e histórico de cada prestador antes de decidir
            </p>
          </div>

          <div class="feature-card">
            <div class="feature-icon feature-icon-green">
              <MessageSquare :size="32" />
            </div>
            <div class="feature-step">Passo 3</div>
            <h3 class="feature-title">Solicite e contrate</h3>
            <p class="feature-description">
              Peça orçamentos, negocie e contrate de forma direta e segura
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- Benefits -->
    <section id="diferenciais" class="benefits-section">
      <div class="benefits-content">
        <div class="benefits-grid">
          <div class="benefits-list">
            <span class="section-badge">Diferenciais</span>
            <h2 class="benefits-title">Por que escolher o Servi Já?</h2>

            <div class="benefit-items">
              <div class="benefit-item">
                <div class="benefit-icon benefit-icon-blue">
                  <Star :size="24" />
                </div>
                <div class="benefit-content">
                  <h3 class="benefit-title">Sistema de Avaliações Inteligente</h3>
                  <p class="benefit-description">
                    Resumos automáticos com IA organizam feedbacks de forma clara para facilitar sua escolha
                  </p>
                </div>
              </div>

              <div class="benefit-item">
                <div class="benefit-icon benefit-icon-blue">
                  <ListChecks :size="24" />
                </div>
                <div class="benefit-content">
                  <h3 class="benefit-title">Sistema de Avaliações por Serviço</h3>
                  <p class="benefit-description">
                    Cada serviço possui seu próprio sistema de avaliações, permitindo que você veja as avaliações de
                    cada serviço antes de contratar.
                  </p>
                </div>
              </div>

              <div class="benefit-item">
                <div class="benefit-icon benefit-icon-purple">
                  <Shield :size="24" />
                </div>
                <div class="benefit-content">
                  <h3 class="benefit-title">Transparência Total</h3>
                  <p class="benefit-description">
                    Perfis públicos completos com histórico, portfólio e reputação verificada
                  </p>
                </div>
              </div>

              <div class="benefit-item">
                <div class="benefit-icon benefit-icon-green">
                  <TrendingUp :size="24" />
                </div>
                <div class="benefit-content">
                  <h3 class="benefit-title">Fortaleça sua marca</h3>
                  <p class="benefit-description">
                    Prestadores constroem sua reputação no mercado digital com perfil profissional
                  </p>
                </div>
              </div>
            </div>
          </div>

          <div id="prestadores" class="cta-card">
            <div class="cta-card-glow"></div>
            <div class="cta-card-content">
              <div class="cta-badge">
                <Sparkles :size="16" />
                <span>Seja um Prestador</span>
              </div>

              <h3 class="cta-title">Cresça seu negócio conosco</h3>
              <p class="cta-description">
                Crie seu perfil profissional, receba solicitações de clientes e construa sua reputação
              </p>

              <ul class="cta-benefits">
                <li class="cta-benefit">
                  <CheckCircle :size="20" />
                  <span>Perfil profissional completo</span>
                </li>
                <li class="cta-benefit">
                  <CheckCircle :size="20" />
                  <span>Receba solicitações diretamente</span>
                </li>
                <li class="cta-benefit">
                  <CheckCircle :size="20" />
                  <span>Construa sua reputação online</span>
                </li>
              </ul>

              <Button variant="secondary" class="cta-button" @click="$props.onSignup">
                Cadastre-se como Prestador
              </Button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Contratantes -->
    <section id="contratantes" class="benefits-section">
      <div class="benefits-content">
        <div class="benefits-grid">
          <div class="benefits-list">
            <span class="section-badge">Para Contratantes</span>
            <h2 class="benefits-title">Facilidade para quem contrata</h2>
            <div class="benefit-items">
              <div class="benefit-item">
                <div class="benefit-icon benefit-icon-blue">
                  <Search :size="24" />
                </div>
                <div class="benefit-content">
                  <h3 class="benefit-title">Busque por categoria e localização</h3>
                  <p class="benefit-description">
                    Encontre o melhor profissional para o seu serviço com filtros inteligentes.
                  </p>
                </div>
              </div>

              <div class="benefit-item">
                <div class="benefit-icon benefit-icon-blue">
                  <ListChecks :size="24" />
                </div>
                <div class="benefit-content">
                  <h3 class="benefit-title">Compare perfis e avaliações</h3>
                  <p class="benefit-description">
                    Veja o histórico e a reputação de cada prestador antes de decidir.
                  </p>
                </div>
              </div>

              <div class="benefit-item">
                <div class="benefit-icon benefit-icon-purple">
                  <MessageSquare :size="24" />
                </div>
                <div class="benefit-content">
                  <h3 class="benefit-title">Negocie com segurança</h3>
                  <p class="benefit-description">
                    Peça orçamentos e contrate de forma direta pela plataforma.
                  </p>
                </div>
              </div>
            </div>
          </div>

          <div id="contratantes-cta" class="cta-card">
            <div class="cta-card-glow"></div>
            <div class="cta-card-content">
              <div class="cta-badge">
                <Sparkles :size="16" />
                <span>Seja um Contratante</span>
              </div>

              <h3 class="cta-title">Encontre o profissional ideal</h3>
              <p class="cta-description">
                O melhor profissional para o seu serviço a apenas alguns cliques de distância.
              </p>

              <ul class="cta-benefits">
                <li class="cta-benefit">
                  <CheckCircle :size="20" />
                  <span>Busca simplificada por categoria</span>
                </li>
                <li class="cta-benefit">
                  <CheckCircle :size="20" />
                  <span>Histórico e avaliações reais</span>
                </li>
                <li class="cta-benefit">
                  <CheckCircle :size="20" />
                  <span>Contratação direta e segura</span>
                </li>
              </ul>

              <Button variant="secondary" class="cta-button" @click="$props.onSignup">
                Cadastre-se como Contratante
              </Button>
            </div>
          </div>


        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="final-cta-section">
      <div class="final-cta-content">
        <div class="final-cta-background"></div>
        <div class="final-cta-text">
          <h2 class="final-cta-title">Pronto para encontrar o profissional ideal?</h2>
          <p class="final-cta-description">
            Junte-se a milhares de pessoas que já encontraram os melhores serviços no Servi Já
          </p>
          <Button size="lg" @click="$props.onSignup" class="final-cta-button">
            Começar agora
            <CheckCircle :size="20" />
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
              <li><a href="#" class="footer-link">Termos de uso</a></li>
              <li><a href="#" class="footer-link">Privacidade</a></li>
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
  background: linear-gradient(to bottom, #ffffff 0%, #f9fafb 100%);
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
    background 0.4s ease,
    box-shadow 0.4s ease,
    border-color 0.4s ease,
    margin 0.4s ease,
    border-radius 0.4s ease,
    top 0.4s ease,
    width 0.4s ease;
}

.header-content {
  max-width: 1280px;
  margin: 0 auto;
  padding: 1rem 1.5rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: padding 0.4s ease;
}

.logo {
  width: 5rem;
  height: 5rem;
}

.logo-container {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.logo-icon {
  width: 5rem;
  height: 5rem;
  display: flex;
  margin: -10px;
  align-items: center;
  justify-content: center;
  transition: transform 0.3s ease, width 0.3s ease, height 0.3s ease;
}

.logo-text {
  color: white;
  font-weight: 700;
  font-size: 1.125rem;
}

.logo-title {
  font-weight: 700;
  font-size: 1.25rem;
  background: #286ad8;
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.nav-links {
  display: none;
  gap: 2rem;
}

.nav-link {
  color: #6b7280;
  font-weight: 500;
  transition: color 0.2s ease;
  position: relative;
}

.nav-link::after {
  content: '';
  position: absolute;
  bottom: -4px;
  left: 0;
  width: 0;
  height: 2px;
  background: linear-gradient(135deg, #286ad8 0%, #143774 100%);
  transition: width 0.3s ease;
}

.nav-link:hover {
  color: #286ad8;
}

.nav-link:hover::after {
  width: 100%;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.btn-login {
  font-weight: 500;
}

.btn-signup {
  background: linear-gradient(135deg, #286ad8 0%, #143774 100%);
  color: white;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
  transition: all 0.3s ease;
}

.btn-signup:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(40, 106, 216, 0.4);
}

/* ===== Hero Section V2 ===== */
.hero-section {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  overflow: hidden;
  background: white;
}

.hero-bg-overlay {
  position: absolute;
  top: -20rem;
  left: 0;
  width: 100%;
  height: 120%;
  z-index: 1;
  pointer-events: none;
}

.hero-bg-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.9;
}

.hero-container {
  max-width: 1280px;
  margin: 0 auto;
  width: 100%;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 4rem;
  align-items: center;
  position: relative;
  z-index: 10;
}

.hero-content-left {
  animation: slideInLeft 1s ease-out;
}

.hero-badge-v2 {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
  background: rgba(40, 106, 216, 0.08);
  border: 1px solid rgba(40, 106, 216, 0.15);
  padding: 0.6rem 1.25rem;
  border-radius: 99px;
  color: #286ad8;
  font-weight: 600;
  font-size: 0.9rem;
  margin-bottom: 2rem;
}

.hero-title-v2 {
  font-size: clamp(2.5rem, 5vw, 4rem);
  line-height: 1.1;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 1.5rem;
  letter-spacing: -0.02em;
}

.hero-highlight-v2 {
  color: #286ad8;
  position: relative;
}

.hero-description-v2 {
  font-size: clamp(1.1rem, 2vw, 1.25rem);
  color: #475569;
  line-height: 1.6;
  max-width: 540px;
  margin-bottom: 2.5rem;
}

.hero-actions-v2 {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.btn-primary-v2 {
  background: #286ad8;
  color: white;
  padding: 1.25rem 2.5rem;
  border-radius: 1rem;
  font-size: 1.125rem;
  font-weight: 700;
  width: fit-content;
  box-shadow: 0 10px 25px rgba(40, 106, 216, 0.25);
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.btn-primary-v2:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 15px 35px rgba(40, 106, 216, 0.35);
  background: #1e58bc;
}

.hero-trust-badges {
  display: flex;
  gap: 2rem;
  align-items: center;
}

.trust-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #64748b;
  font-size: 0.875rem;
  font-weight: 500;
}

.trust-item svg {
  color: #286ad8;
}

.hero-visual-right {
  position: relative;
  animation: slideInRight 1.2s ease-out;
}

.visual-wrapper {
  position: relative;
  width: 100%;
  max-width: 600px;
}

.hero-people-img {
  width: 100%;
  height: auto;
  filter: drop-shadow(0 20px 50px rgba(0, 0, 0, 0.1));
  position: relative;
  z-index: 5;
}

.hero-line-img-fixed {
  position: absolute;
  top: 80.5%;
  right: 0;
  width: 20%;
  max-width: 45vw;
  z-index: 1;
  opacity: 0.8;
  pointer-events: none;
  transform: translateY(-50%);
}

.hero-line-img-inverted {
  position: absolute;
  top: 10%;
  left: 0;
  width: 20%;
  max-width: 45vw;
  z-index: 1;
  opacity: 0.8;
  pointer-events: none;
  transform: scaleX(-1) scaleY(-1);
}

.floating-card {
  position: absolute;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  padding: 1rem;
  border-radius: 1rem;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  gap: 0.8rem;
  z-index: 20;
}

.card-1 {
  top: 15%;
  right: -5%;
}

.card-2 {
  bottom: 20%;
  left: -5%;
}

.card-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-icon.blue { background: rgba(40, 106, 216, 0.1); color: #286ad8; }
.card-icon.yellow { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }

.card-info {
  display: flex;
  flex-direction: column;
}

.card-label {
  font-size: 0.75rem;
  color: #64748b;
  font-weight: 500;
}

.card-val {
  font-size: 0.9rem;
  color: #0f172a;
  font-weight: 700;
}

/* Animations */
@keyframes slideInLeft {
  from { opacity: 0; transform: translateX(-40px); }
  to { opacity: 1; transform: translateX(0); }
}

@keyframes slideInRight {
  from { opacity: 0; transform: translateX(40px); }
  to { opacity: 1; transform: translateX(0); }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}

/* Responsive */
@media (max-width: 1024px) {
  .hero-container {
    grid-template-columns: 1fr;
    text-align: center;
    gap: 3rem;
  }

  .hero-content-left {
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .hero-description-v2 {
    margin-left: auto;
    margin-right: auto;
  }

  .hero-actions-v2 {
    align-items: center;
  }

  .hero-visual-right {
    order: -1;
    max-width: 500px;
    margin: 0 auto;
  }
}

@media (max-width: 640px) {
  .hero-section {
    padding-top: 60px;
  }
  
  .hero-badge-v2 {
    font-size: 0.8rem;
  }

  .floating-card {
    display: none;
  }
}

/* ===== Features Section ===== */
.features-section {
  padding: 6rem 1.5rem;
  min-height: 96vh;
  background: white;
  position: relative;
}

.features-content {
  max-width: 1280px;
  margin: 0 auto;
}

.section-header {
  text-align: center;
  margin-bottom: 4rem;
}

.section-badge {
  display: inline-block;
  padding: 0.5rem 1rem;
  background: linear-gradient(135deg, #abcaff 0%, #eef4ff 100%);
  color: #3b82f6;
  font-size: 0.875rem;
  font-weight: 700;
  border-radius: 9999px;
  margin-bottom: 1rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.section-title {
  font-size: 2.5rem;
  font-weight: 800;
  color: #111827;
  margin-bottom: 1rem;
}

.section-description {
  font-size: 1.125rem;
  color: #6b7280;
  max-width: 42rem;
  margin: 0 auto;
}

.features-grid {
  display: grid;
  gap: 2rem;
  grid-template-columns: 1fr;
}

.feature-card {
  background: white;
  border-radius: 1.5rem;
  padding: 2.5rem;
  text-align: center;
  border: 2px solid #f3f4f6;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.feature-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, transparent 0%, rgba(59, 130, 246, 0.05) 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.feature-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  border-color: #3b82f6;
}

.feature-card:hover::before {
  opacity: 1;
}

.feature-icon {
  width: 5rem;
  height: 5rem;
  border-radius: 1.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1.5rem;
  position: relative;
  transition: transform 0.3s ease;
}

.feature-icon-blue {
  background: linear-gradient(135deg, #286ad8 0%, #1c3b70 100%);
  color: #ffffff;
}

.feature-icon-purple {
  background: linear-gradient(135deg, #286ad8 0%, #1c3b70 100%);
  color: white;
}

.feature-icon-green {
  background: linear-gradient(135deg, #286ad8 0%, #1c3b70 100%);
  color: #ffffff;
}

.feature-step {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  background: #f3f4f6;
  color: #6b7280;
  font-size: 0.75rem;
  font-weight: 700;
  border-radius: 9999px;
  margin-bottom: 1rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.feature-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #111827;
  margin-bottom: 0.75rem;
}

.feature-description {
  color: #6b7280;
  line-height: 1.75;
}

/* ===== Benefits Section ===== */
.benefits-section {
  padding: 6rem 1.5rem;
  background: linear-gradient(to bottom, #f9fafb 0%, white 100%);
  min-height: 96vh;
  position: relative;
}

.benefits-content {
  max-width: 1280px;
  margin: 0 auto;
}

.benefits-grid {
  display: grid;
  gap: 3rem;
  align-items: center;
  grid-template-columns: 1fr;
}

.benefits-list {
  position: sticky;
  top: 6rem;
}

.benefits-title {
  font-size: 2.5rem;
  font-weight: 800;
  color: #111827;
  margin-bottom: 2rem;
  margin-top: 1rem;
}

.benefit-items {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.benefit-item {
  display: flex;
  gap: 1.5rem;
  align-items: flex-start;
}

.benefit-icon {
  width: 3.5rem;
  height: 3.5rem;
  border-radius: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.3s ease;
}

.benefit-item:hover .benefit-icon {
  transform: scale(1.1) rotate(5deg);
}

.benefit-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #111827;
  margin-bottom: 0.5rem;
}

.benefit-description {
  color: #6b7280;
  line-height: 1.75;
}

/* ===== CTA Card ===== */
.cta-card {
  position: relative;
  border-radius: 2rem;
  overflow: hidden;
  background: linear-gradient(135deg, #286ad8 0%, #143774 100%);
  padding: 2rem;
  display: flex;
  justify-content: center;
  align-items: center;
  text-align: center;
  position: sticky;
  top: 7rem;
}

.cta-cardC {
  position: relative;
  border-radius: 2rem;
  overflow: hidden;
  background: linear-gradient(135deg, #286ad8 0%, #143774 100%);
  padding: 2rem;
  display: flex;
  justify-content: center;
  align-items: center;
  text-align: center;
  grid-column: 2;
  margin-top: 30rem;
}

@keyframes glow {

  0%,
  100% {
    opacity: 0.3;
  }

  50% {
    opacity: 0.6;
  }
}

.cta-card:hover .cta-card-glow {
  opacity: 1;
}

.cta-card-content {
  position: relative;
  z-index: 10;
}

.cta-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 9999px;
  color: white;
  font-size: 0.875rem;
  font-weight: 600;
  margin-bottom: 1.5rem;
}

.cta-title {
  font-size: 2rem;
  font-weight: 800;
  color: white;
  margin-bottom: 1rem;
}

.cta-description {
  color: rgba(255, 255, 255, 0.9);
  line-height: 1.75;
  margin-bottom: 2rem;
  font-size: 1.125rem;
}

.cta-benefits {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 2rem;
  align-items: center;
}

.cta-benefit {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  color: white;
  font-weight: 500;
}

.cta-button {
  width: 100%;
  background: white;
  color: #3b82f6;
  font-weight: 700;
  padding: 1rem 2rem;
  border-radius: 0.75rem;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.cta-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.3);
  background: #f9fafb;
}

/* ===== Final CTA Section ===== */
.final-cta-section {
  padding: 6rem 1.5rem;
  background: white;
  position: relative;
  overflow: hidden;
}

.final-cta-content {
  max-width: 1280px;
  margin: 0 auto;
  position: relative;
}

.final-cta-background {
  position: absolute;
  inset: -10rem;
  background: radial-gradient(circle at center, rgba(59, 130, 246, 0.1) 0%, transparent 70%);
  animation: pulse 4s ease-in-out infinite;
}

.final-cta-text {
  text-align: center;
  max-width: 48rem;
  margin: 0 auto;
  position: relative;
  z-index: 10;
}

.final-cta-title {
  font-size: 2.5rem;
  font-weight: 800;
  color: #111827;
  margin-bottom: 1rem;
}

.final-cta-description {
  font-size: 1.125rem;
  color: #6b7280;
  margin-bottom: 2.5rem;
}

.final-cta-button {
  background: linear-gradient(135deg, #286ad8 0%, #143774 100%);
  color: white;
  font-weight: 700;
  font-size: 1.125rem;
  padding: 1rem 2.5rem;
  border-radius: 0.75rem;
  box-shadow: 0 10px 30px rgba(59, 130, 246, 0.3);
  transition: all 0.3s ease;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.final-cta-button:hover {
  transform: translateY(-3px);
  box-shadow: 0 15px 40px rgba(38, 97, 193, 0.4);
}

/* ===== Footer ===== */
.footer {
  background: #111827;
  color: white;
  padding: 4rem 1.5rem 2rem;
  position: relative;
}

.footer-content {
  max-width: 1280px;
  margin: 0 auto;
}

.footer-grid {
  display: grid;
  gap: 3rem;
  margin-bottom: 3rem;
  grid-template-columns: 1fr;
}

.footer-brand {
  max-width: 20rem;
}

.footer-logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.footer-logo-icon {
  width: 3rem;
  height: 3rem;
  background: #111827;
  box-shadow: 0 0 10px 0 #1a397c;
  border-radius: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.footer-logo-text {
  font-weight: 700;
  font-size: 1.125rem;
}

.footer-description {
  color: #9ca3af;
  line-height: 1.75;
}

.footer-links-title {
  font-weight: 700;
  color: white;
  margin-bottom: 1.25rem;
}

.footer-links-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.footer-link {
  color: #9ca3af;
  transition: color 0.2s ease;
}

.footer-link:hover {
  color: white;
}

.footer-bottom {
  padding-top: 2rem;
  border-top: 1px solid #374151;
  text-align: center;
  color: #9ca3af;
  font-size: 0.875rem;
}

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
}

@media (min-width: 836px) {
  .header.scrolled .header-content {
    padding: 0.5rem 1.25rem;
  }
}

@media (min-width: 836px) {
  .header.scrolled .logo-icon {
    width: 2.25rem;
    height: 2.25rem;
    margin: 0px;
  }
}

@media (min-width: 768px) {
  .nav-links {
    display: flex;
  }
}

@media (min-width: 768px) {
  .hero-section {
    padding: 8rem 1.5rem;
  }
}

@media (min-width: 768px) {
  .hero-title {
    font-size: 3.75rem;
  }
}

@media (min-width: 768px) {
  .section-title {
    font-size: 3rem;
  }
}

@media (min-width: 768px) {
  .features-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (min-width: 1024px) {
  .benefits-grid {
    grid-template-columns: 1fr 1fr;
    gap: 4rem;
  }
}

@media (min-width: 768px) {
  .cta-card {
    padding: 3rem;
    text-align: left;
  }
}

@media (min-width: 768px) {
  .cta-benefits {
    align-items: flex-start;
  }
}

@media (min-width: 768px) {
  .final-cta-title {
    font-size: 3rem;
  }
}

@media (min-width: 768px) {
  .footer-grid {
    grid-template-columns: 2fr 1fr 1fr 1fr;
  }
}
</style>
