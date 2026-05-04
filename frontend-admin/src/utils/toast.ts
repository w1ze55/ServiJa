// Toast notification system
// Cria toasts visuais que aparecem no canto superior direito

interface ToastOptions {
  message: string;
  type: 'success' | 'error' | 'info' | 'warning';
  duration?: number;
}

const createToastContainer = (): HTMLElement => {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    container.style.cssText = `
      position: fixed;
      top: 20px;
      right: 20px;
      z-index: 9999;
      display: flex;
      flex-direction: column;
      gap: 10px;
      pointer-events: none;
    `;
    document.body.appendChild(container);
  }
  return container;
};

const showToast = ({ message, type, duration = 4000 }: ToastOptions) => {
  const container = createToastContainer();

  const toast = document.createElement('div');
  toast.style.cssText = `
    padding: 14px 20px;
    border-radius: 8px;
    color: white;
    font-size: 14px;
    font-weight: 500;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 280px;
    max-width: 400px;
    pointer-events: auto;
    animation: slideIn 0.3s ease-out;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    position: relative;
    overflow: hidden;
  `;

  // Cores e ícones por tipo
  const styles: Record<string, { bg: string; icon: string }> = {
    success: { bg: '#22c55e', icon: '✓' },
    error: { bg: '#ef4444', icon: '✕' },
    info: { bg: '#3b82f6', icon: 'ℹ' },
    warning: { bg: '#f59e0b', icon: '⚠' },
  };

  const { bg, icon } = styles[type] || styles.info;
  toast.style.backgroundColor = bg;

  // Ícone
  const iconSpan = document.createElement('span');
  iconSpan.style.cssText = `
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.2);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    flex-shrink: 0;
  `;
  iconSpan.textContent = icon;

  // Mensagem
  const messageSpan = document.createElement('span');
  messageSpan.style.cssText = `flex: 1;`;
  messageSpan.textContent = message;

  // Botão fechar
  const closeBtn = document.createElement('button');
  closeBtn.style.cssText = `
    background: none;
    border: none;
    color: white;
    cursor: pointer;
    padding: 0;
    font-size: 16px;
    opacity: 0.7;
    transition: opacity 0.2s;
    flex-shrink: 0;
  `;
  closeBtn.textContent = '×';
  closeBtn.onmouseover = () => { closeBtn.style.opacity = '1'; };
  closeBtn.onmouseout = () => { closeBtn.style.opacity = '0.7'; };
  closeBtn.onclick = () => removeToast(toast);

  // Barra de progresso
  const progressBar = document.createElement('div');
  progressBar.style.cssText = `
    position: absolute;
    bottom: 0;
    left: 0;
    height: 3px;
    background: rgba(255, 255, 255, 0.4);
    width: 100%;
    transform-origin: left;
    animation: toastProgress ${duration}ms linear forwards;
  `;

  toast.appendChild(iconSpan);
  toast.appendChild(messageSpan);
  toast.appendChild(closeBtn);
  toast.appendChild(progressBar);

  // Adicionar estilo de animação se não existir
  if (!document.getElementById('toast-styles')) {
    const style = document.createElement('style');
    style.id = 'toast-styles';
    style.textContent = `
      @keyframes slideIn {
        from {
          transform: translateX(100%);
          opacity: 0;
        }
        to {
          transform: translateX(0);
          opacity: 1;
        }
      }
      @keyframes slideOut {
        from {
          transform: translateX(0);
          opacity: 1;
        }
        to {
          transform: translateX(100%);
          opacity: 0;
        }
      }
      @keyframes toastProgress {
        from {
          transform: scaleX(1);
        }
        to {
          transform: scaleX(0);
        }
      }
    `;
    document.head.appendChild(style);
  }

  container.appendChild(toast);

  // Auto-remover após duração
  let timeoutId = setTimeout(() => removeToast(toast), duration);

  // Pausar timer ao hover
  toast.onmouseenter = () => {
    clearTimeout(timeoutId);
    progressBar.style.animationPlayState = 'paused';
  };

  toast.onmouseleave = () => {
    // Quando sai o mouse, damos mais um segundo para fechar (manteve o comportamento original)
    progressBar.style.animationPlayState = 'running';
    // Se o bar já tava quase no fim, o comportamento de 1s ajuda.
    timeoutId = setTimeout(() => removeToast(toast), 1000);
  };
};

const removeToast = (toast: HTMLElement) => {
  toast.style.animation = 'slideOut 0.3s ease-in forwards';
  setTimeout(() => {
    toast.remove();
  }, 300);
};

export const toast = {
  success: (message: string) => {
    showToast({ message, type: 'success' });
  },
  error: (message: string) => {
    showToast({ message, type: 'error' });
  },
  info: (message: string) => {
    showToast({ message, type: 'info' });
  },
  warning: (message: string) => {
    showToast({ message, type: 'warning' });
  },
};
