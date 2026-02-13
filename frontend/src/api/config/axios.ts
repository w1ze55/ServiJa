import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';

// VITE_API_URL deve ser a URL base SEM /api no final
// Exemplo: http://3.131.85.191 (não http://3.131.85.191/api)
// O código abaixo adiciona /api automaticamente
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

// Remove /api ou /api/ do final se existir para evitar duplicação
const cleanBaseURL = API_BASE_URL.replace(/\/api\/?$/, '');

const api = axios.create({
  baseURL: `${cleanBaseURL}/api`,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor para adicionar token de autenticação
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
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
      // Não redireciona automaticamente para evitar loops
    }
    return Promise.reject(error);
  }
);

export default api;

