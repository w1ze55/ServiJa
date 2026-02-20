import { toast } from './toast'

// Configuração da API
// VITE_API_URL deve ser a URL base SEM /api no final
// Exemplo: http://3.131.85.191 (não http://3.131.85.191/api)
// O código abaixo adiciona /api automaticamente
const rawAPIBaseURL = import.meta.env.VITE_API_URL;

// Remove /api do final se existir para evitar duplicação (tanto /api quanto /api/)
export const API_BASE_URL = rawAPIBaseURL.replace(/\/api\/?$/, '')

// Endpoints da API
export const API_ENDPOINTS = {
  // Autenticação
  LOGIN: `${API_BASE_URL}/api/auth/login`,
  REGISTER: `${API_BASE_URL}/api/auth/register`,
  VERIFY_EMAIL: `${API_BASE_URL}/api/auth/verify-email`,
  RESEND_VERIFICATION: `${API_BASE_URL}/api/auth/resend-verification`,
  FORGOT_PASSWORD: `${API_BASE_URL}/api/auth/forgot-password`,
  RESET_PASSWORD: `${API_BASE_URL}/api/auth/reset-password`,
  TEST_EMAIL: `${API_BASE_URL}/api/auth/test-email`,
  
  // Usuário
  GET_CURRENT_USER: `${API_BASE_URL}/api/users/me`,
  GET_CURRENT_USER_PROFILE: `${API_BASE_URL}/api/users/me/profile`,
  UPDATE_USER: `${API_BASE_URL}/api/users/me`,
  CHANGE_PASSWORD: `${API_BASE_URL}/api/users/me/password`,
  UPDATE_AVATAR: `${API_BASE_URL}/api/users/me/avatar`,
  DELETE_ACCOUNT: `${API_BASE_URL}/api/users/me`,
  
  // Freelancer
  FREELANCER_PROFILE: `${API_BASE_URL}/api/freelancer/profile`,
  FREELANCER_SERVICES: `${API_BASE_URL}/api/freelancer/services`,
  FREELANCER_PORTFOLIO: `${API_BASE_URL}/api/freelancer/portfolio`,
}

/**
 * Faz logout do usuário, limpando dados locais e redirecionando
 */
export function logout(router?: any, message?: string) {
  // Limpar dados do localStorage
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  localStorage.removeItem('userProfile')
  
  // Mostrar mensagem se fornecida
  if (message) {
    toast.warning(message)
  }
  
  // Redirecionar para login se router estiver disponível
  if (router) {
    router.push('/login')
  } else if (typeof window !== 'undefined') {
    window.location.href = '/login'
  }
}

/**
 * Verifica se o token JWT está expirado
 */
export function isTokenExpired(): boolean {
  const token = localStorage.getItem('token')
  if (!token) return true
  
  try {
    // JWT é dividido em 3 partes: header.payload.signature
    const payload = JSON.parse(atob(token.split('.')[1]))
    const exp = payload.exp * 1000 // Converter para millisegundos
    return Date.now() >= exp
  } catch (error) {
    console.error('Erro ao verificar expiração do token:', error)
    return true
  }
}

/**
 * Verifica se o token vai expirar em breve (próximos 5 minutos)
 */
export function isTokenExpiringSoon(): boolean {
  const token = localStorage.getItem('token')
  if (!token) return false
  
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    const exp = payload.exp * 1000
    const fiveMinutes = 5 * 60 * 1000
    return Date.now() >= (exp - fiveMinutes) && Date.now() < exp
  } catch (error) {
    return false
  }
}

// Helper para fazer requisições autenticadas
export async function fetchWithAuth(url: string, options: RequestInit = {}) {
  const token = localStorage.getItem('token')
  
  // Verificar se o token já expirou antes de fazer a requisição
  if (token && isTokenExpired()) {
    logout(undefined, 'Sua sessão expirou. Faça login novamente.')
    throw new Error('Token expirado')
  }
  
  const headers = {
    'Content-Type': 'application/json',
    ...(token && { Authorization: `Bearer ${token}` }),
    ...options.headers,
  }

  try {
    const response = await fetch(url, {
      ...options,
      headers,
    })

    // Verificar se o token expirou durante a requisição
    if (response.status === 401) {
      const errorData = await response.json().catch(() => ({}))
      
      // Se o backend indicou que o token expirou
      if (errorData.expired === true || errorData.message?.includes('expirado')) {
        logout(undefined, 'Sua sessão expirou. Faça login novamente.')
        throw new Error('Token expirado')
      }
    }

    return response
  } catch (error: any) {
    // Se for erro de rede ou timeout
    if (error.message === 'Token expirado') {
      throw error
    }
    throw error
  }
}

// Helper para lidar com erros da API
export function handleApiError(error: any): string {
  if (error.message === 'Token expirado') {
    return 'Sua sessão expirou. Por favor, faça login novamente.'
  }
  if (error.message) {
    return error.message
  }
  if (typeof error === 'string') {
    return error
  }
  return 'Erro desconhecido. Tente novamente.'
}

/**
 * Inicializa o verificador de expiração de token
 * Verifica a cada minuto se o token está expirado
 */
export function startTokenExpirationChecker(router: any) {
  // Verificar imediatamente
  if (isTokenExpired()) {
    logout(router, 'Sua sessão expirou. Faça login novamente.')
    return
  }

  // Verificar a cada 60 segundos
  const intervalId = setInterval(() => {
    if (isTokenExpired()) {
      clearInterval(intervalId)
      logout(router, 'Sua sessão expirou. Faça login novamente.')
    } else if (isTokenExpiringSoon()) {
      // Opcional: mostrar aviso de que o token vai expirar em breve
      toast.warning('Sua sessão irá expirar em breve. Salve seu trabalho.')
    }
  }, 60000) // 60 segundos

  // Retornar função para limpar o interval
  return () => clearInterval(intervalId)
}
