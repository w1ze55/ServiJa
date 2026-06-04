import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { setPendingEmailVerification } from '@/utils/pendingEmailVerification';

// VITE_API_URL deve ser a URL base sem /api no final.
// Exemplo: http://localhost:8080
const API_BASE_URL = import.meta.env.VITE_API_URL;
const cleanBaseURL = (API_BASE_URL ?? 'http://localhost:8080').replace(/\/api\/?$/, '');

if (!API_BASE_URL) {
  console.warn(
    'VITE_API_URL is not defined for frontend. Falling back to "http://localhost:8080".'
  );
}

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

api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const normalizedPath = normalizeUrlPath(config.url || '');
    const isPublicAuthEndpoint = PUBLIC_AUTH_ENDPOINTS.includes(normalizedPath);

    if (isPublicAuthEndpoint) {
      return config;
    }

    let token = localStorage.getItem('token');

    if (!token) {
      const user = localStorage.getItem('user');
      if (user) {
        try {
          const userData = JSON.parse(user);
          token = userData.token;
        } catch (e) {
          // Ignore invalid cached payloads.
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

api.interceptors.response.use(
  (response: any) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');

      if (window.location.pathname !== '/login' && window.location.pathname !== '/') {
        window.location.href = '/login?expired=true';
      }
    }

    if (error.response?.status === 403 && (error.response.data as any)?.emailVerificationRequired) {
      const email = (error.response.data as any)?.email;

      localStorage.removeItem('token');
      localStorage.removeItem('user');

      if (typeof email === 'string' && email.trim()) {
        setPendingEmailVerification({ email: email.trim() });
      }

      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }

    return Promise.reject(error);
  }
);

export default api;
