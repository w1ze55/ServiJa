import React from 'react'
import { Search, Star, MessageSquare, Shield, TrendingUp, Users, CheckCircle, Sparkles, ListChecks, ShieldCheck } from 'lucide-react'
import './App.css'
import logo from '/favicon-32x32.png'

function App() {
  const handleLogin = () => {
    window.location.href = '/app/login'
  }

  const handleSignup = () => {
    window.location.href = '/app/cadastro'
  }

  return (
    <div className="landing-page">
      {/* Header */}
      <header className="header">
        <div className="header-content">
          <div className="logo-container">
            <div className="logo-icon">
              <img src={logo} alt="Servi Já" />
            </div>
            <span className="logo-title">Servi Já</span>
          </div>

          <nav className="nav-links">
            <a href="#como-funciona" className="nav-link">Como funciona</a>
            <a href="#diferenciais" className="nav-link">Diferenciais</a>
            <a href="#contratantes" className="nav-link">Para Contratantes</a>
          </nav>

          <div className="header-actions">
            <button className="btn-ghost" onClick={handleLogin}>Entrar</button>
            <button className="btn-signup" onClick={handleSignup}>Cadastrar</button>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <section className="hero-section">
        <div className="hero-background"></div>
        <div className="hero-content">
          <div className="hero-badge">
            <Sparkles size={16} />
            <span>Plataforma de conexão entre prestadores e contratantes</span>
          </div>

          <h1 className="hero-title">
            Encontre os melhores{' '}
            <span className="hero-highlight">profissionais</span>
            {' '}para qualquer serviço
          </h1>

          <p className="hero-description">
            O lugar onde você encontra os melhores prestadores de serviço.
            Compare perfis, avaliações e escolha com confiança.
          </p>

          <div className="hero-actions">
            <button className="btn-primary" onClick={handleSignup}>
              Começar agora - É grátis
              <CheckCircle size={20} />
            </button>
          </div>
        </div>
      </section>

      {/* Features */}
      <section id="como-funciona" className="features-section">
        <div className="features-content">
          <div className="section-header">
            <span className="section-badge">Como Funciona</span>
            <h2 className="section-title">Simples, rápido e confiável</h2>
            <p className="section-description">Uma plataforma completa para conectar prestadores e contratantes</p>
          </div>

          <div className="features-grid">
            <div className="feature-card">
              <div className="feature-icon feature-icon-blue">
                <Search size={32} />
              </div>
              <div className="feature-step">Passo 1</div>
              <h3 className="feature-title">Busque o serviço</h3>
              <p className="feature-description">
                Pesquise por categoria, localização ou palavra-chave e encontre os melhores profissionais
              </p>
            </div>

            <div className="feature-card">
              <div className="feature-icon feature-icon-purple">
                <Users size={32} />
              </div>
              <div className="feature-step">Passo 2</div>
              <h3 className="feature-title">Compare perfis</h3>
              <p className="feature-description">
                Veja portfólios, avaliações e histórico de cada prestador antes de decidir
              </p>
            </div>

            <div className="feature-card">
              <div className="feature-icon feature-icon-green">
                <MessageSquare size={32} />
              </div>
              <div className="feature-step">Passo 3</div>
              <h3 className="feature-title">Solicite e contrate</h3>
              <p className="feature-description">
                Peça orçamentos, negocie e contrate de forma direta e segura
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Benefits */}
      <section id="diferenciais" className="benefits-section">
        <div className="benefits-content">
          <div className="benefits-grid">
            <div className="benefits-list">
              <span className="section-badge">Diferenciais</span>
              <h2 className="benefits-title">Por que escolher o Servi Já?</h2>

              <div className="benefit-items">
                <div className="benefit-item">
                  <div className="benefit-icon benefit-icon-blue">
                    <Star size={24} />
                  </div>
                  <div className="benefit-content">
                    <h3 className="benefit-title">Sistema de Avaliações Inteligente</h3>
                    <p className="benefit-description">
                      Resumos automáticos com IA organizam feedbacks de forma clara para facilitar sua escolha
                    </p>
                  </div>
                </div>

                <div className="benefit-item">
                  <div className="benefit-icon benefit-icon-blue">
                    <ListChecks size={24} />
                  </div>
                  <div className="benefit-content">
                    <h3 className="benefit-title">Sistema de Avaliações por Serviço</h3>
                    <p className="benefit-description">
                      Cada serviço possui seu próprio sistema de avaliações, permitindo que você veja as avaliações de cada serviço antes de contratar.
                    </p>
                  </div>
                </div>

                <div className="benefit-item">
                  <div className="benefit-icon benefit-icon-purple">
                    <Shield size={24} />
                  </div>
                  <div className="benefit-content">
                    <h3 className="benefit-title">Transparência Total</h3>
                    <p className="benefit-description">
                      Perfis públicos completos com histórico, portfólio e reputação verificada
                    </p>
                  </div>
                </div>

                <div className="benefit-item">
                  <div className="benefit-icon benefit-icon-green">
                    <TrendingUp size={24} />
                  </div>
                  <div className="benefit-content">
                    <h3 className="benefit-title">Fortaleça sua marca</h3>
                    <p className="benefit-description">
                      Prestadores constroem sua reputação no mercado digital com perfil profissional
                    </p>
                  </div>
                </div>
              </div>
            </div>

            <div id="prestadores" className="cta-card">
              <div className="cta-card-glow"></div>
              <div className="cta-card-content">
                <div className="cta-badge">
                  <Sparkles size={16} />
                  <span>Seja um Prestador</span>
                </div>

                <h3 className="cta-title">Cresça seu negócio conosco</h3>
                <p className="cta-description">
                  Crie seu perfil profissional, receba solicitações de clientes e construa sua reputação
                </p>

                <ul className="cta-benefits">
                  <li className="cta-benefit">
                    <CheckCircle size={20} />
                    <span>Perfil profissional completo</span>
                  </li>
                  <li className="cta-benefit">
                    <CheckCircle size={20} />
                    <span>Receba solicitações diretamente</span>
                  </li>
                  <li className="cta-benefit">
                    <CheckCircle size={20} />
                    <span>Construa sua reputação online</span>
                  </li>
                </ul>

                <button className="cta-button" onClick={handleSignup}>
                  Cadastre-se como Prestador
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Contratantes */}
      <section id="contratantes" className="benefits-section">
        <div className="benefits-content">
          <div className="benefits-grid">
            <div className="benefits-list">
              <span className="section-badge">Para Contratantes</span>
              <div className="cta-card">
                <div className="cta-card-glow"></div>
                <div className="cta-card-content">
                  <div className="cta-badge">
                    <Sparkles size={16} />
                    <span>Seja um Contratante</span>
                  </div>

                  <h3 className="cta-title">Encontre o profissional ideal</h3>
                  <p className="cta-description">
                    Encontre o melhor profissional para o seu serviço antes de contratar.
                  </p>

                  <ul className="cta-benefits">
                    <li className="cta-benefit">
                      <CheckCircle size={20} />
                      <span>Busque por categoria, localização ou palavra-chave</span>
                    </li>
                    <li className="cta-benefit">
                      <CheckCircle size={20} />
                      <span>Compare perfis, avaliações e histórico de cada prestador</span>
                    </li>
                    <li className="cta-benefit">
                      <CheckCircle size={20} />
                      <span>Peça orçamentos, negocie e contrate de forma direta e segura</span>
                    </li>
                  </ul>

                  <button className="cta-button" onClick={handleSignup}>
                    Cadastre-se como Contratante
                  </button>
                </div>
              </div>
            </div>

            <div className="benefits-list">
              <h2 className="benefits-title">Por que escolher o Servi Já?</h2>

              <div className="benefit-items">
                <div className="benefit-item">
                  <div className="benefit-icon benefit-icon-blue">
                    <Search size={24} />
                  </div>
                  <div className="benefit-content">
                    <h3 className="benefit-title">Busque por categoria, localização ou palavra-chave</h3>
                    <p className="benefit-description">
                      Encontre o melhor profissional para o seu serviço antes de contratar.
                    </p>
                  </div>
                </div>

                <div className="benefit-item">
                  <div className="benefit-icon benefit-icon-blue">
                    <ListChecks size={24} />
                  </div>
                  <div className="benefit-content">
                    <h3 className="benefit-title">Compare perfis, avaliações e histórico de cada prestador</h3>
                    <p className="benefit-description">
                      Encontre o melhor profissional para o seu serviço antes de contratar.
                    </p>
                  </div>
                </div>

                <div className="benefit-item">
                  <div className="benefit-icon benefit-icon-purple">
                    <MessageSquare size={24} />
                  </div>
                  <div className="benefit-content">
                    <h3 className="benefit-title">Peça orçamentos, negocie e contrate de forma direta e segura</h3>
                    <p className="benefit-description">
                      Encontre o melhor profissional para o seu serviço antes de contratar.
                    </p>
                  </div>
                </div>

                <div className="benefit-item">
                  <div className="benefit-icon benefit-icon-green">
                    <ShieldCheck size={24} />
                  </div>
                  <div className="benefit-content">
                    <h3 className="benefit-title">Encontre o melhor profissional para o seu serviço antes de contratar.</h3>
                    <p className="benefit-description">
                      Encontre o melhor profissional para o seu serviço antes de contratar.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="final-cta-section">
        <div className="final-cta-content">
          <div className="final-cta-background"></div>
          <div className="final-cta-text">
            <h2 className="final-cta-title">Pronto para encontrar o profissional ideal?</h2>
            <p className="final-cta-description">
              Junte-se a milhares de pessoas que já encontraram os melhores serviços no Servi Já
            </p>
            <button className="final-cta-button" onClick={handleSignup}>
              Começar agora
              <CheckCircle size={20} />
            </button>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="footer">
        <div className="footer-content">
          <div className="footer-grid">
            <div className="footer-brand">
              <div className="footer-logo">
                <div className="footer-logo-icon">
                  <img src={logo} alt="Servi Já" />
                </div>
                <span className="footer-logo-text">Servi Já</span>
              </div>
              <p className="footer-description">
                O lugar onde você encontra os melhores prestadores de serviço.
              </p>
            </div>

            <div className="footer-links">
              <h4 className="footer-links-title">Para Contratantes</h4>
              <ul className="footer-links-list">
                <li><a href="#como-funciona" className="footer-link">Como funciona</a></li>
                <li><a href="#diferenciais" className="footer-link">Diferenciais</a></li>
                <li><a href="#contratantes" className="footer-link">Para Contratantes</a></li>
              </ul>
            </div>

            <div className="footer-links">
              <h4 className="footer-links-title">Para Prestadores</h4>
              <ul className="footer-links-list">
                <li><a href="#prestadores" className="footer-link">Seja um prestador</a></li>
                <li><a href="#diferenciais" className="footer-link">Diferenciais</a></li>
                <li><a href="#" className="footer-link">Central de ajuda</a></li>
              </ul>
            </div>

            <div className="footer-links">
              <h4 className="footer-links-title">Institucional</h4>
              <ul className="footer-links-list">
                <li><a href="#" className="footer-link">Sobre nós</a></li>
                <li><a href="#" className="footer-link">Termos de uso</a></li>
                <li><a href="#" className="footer-link">Privacidade</a></li>
              </ul>
            </div>
          </div>

          <div className="footer-bottom">
            <p>&copy; {new Date().getFullYear()} Servi Já. Todos os direitos reservados.</p>
          </div>
        </div>
      </footer>
    </div>
  )
}

export default App
