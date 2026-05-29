import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';

// VITE_API_URL deve ser a URL base SEM /api no final
// Exemplo: http://3.131.85.191 (não http://3.131.85.191/api)
// O código abaixo adiciona /api automaticamente
const API_BASE_URL = import.meta.env.VITE_API_URL;

// Remove /api ou /api/ do final se existir para evitar duplicação
const cleanBaseURL = API_BASE_URL.replace(/\/api\/?$/, '');

const api = axios.create({
  baseURL: `${cleanBaseURL}/api`,
  headers: {
    'Content-Type': 'application/json',
  },
});

const PUBLIC_AUTH_ENDPOINTS = [
  '/auth/login',
  '/auth/admin/login',
  '/auth/register',
  '/auth/validate-registration',
  '/auth/validate-address',
  '/auth/verify-email',
  '/auth/resend-verification',
  '/auth/forgot-password',
  '/auth/verify-reset-code',
  '/auth/reset-password',
  '/auth/resend-reset-code',
  '/auth/admin-reset-link/validate',
  '/auth/admin-reset-link/reset-password',
  '/auth/2fa/verify',
];

const normalizeUrlPath = (url: string) => {
  if (!url) {
    return '';
  }

  const withoutQuery = url.split('?')[0];
  return withoutQuery.startsWith('/api/')
    ? withoutQuery.substring(4)
    : withoutQuery;
};

// Interceptor para adicionar token de autenticação
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const normalizedPath = normalizeUrlPath(config.url || '');
    const isPublicAuthEndpoint = PUBLIC_AUTH_ENDPOINTS.includes(normalizedPath);

    if (isPublicAuthEndpoint) {
      return config;
    }

    // Primeiro tenta pegar o token direto
    let token = localStorage.getItem('token');

    // Se não encontrar, tenta pegar do objeto user (compatibilidade)
    if (!token) {
      const user = localStorage.getItem('user');
      if (user) {
        try {
          const userData = JSON.parse(user);
          token = userData.token;
        } catch (e) {
          // Ignora erro de parse
        }
      }
    }

    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error: AxiosError) => {
    return Promise.reject(error);
  }
);

// Interceptor para tratamento de erros
api.interceptors.response.use(
  (response: any) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      // Token inválido ou expirado
      localStorage.removeItem('token');
      localStorage.removeItem('user');

      // Se não estiver na página principal ou de login, redireciona 
      // para forçar a limpeza do estado da aplicação (ex: store do Pinia)
      if (window.location.pathname !== '/login' && window.location.pathname !== '/') {
        window.location.href = '/login?expired=true';
      }
    }
    return Promise.reject(error);
  }
);

export default api;
