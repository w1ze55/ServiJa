import { toast } from './toast'

const rawAPIBaseURL = import.meta.env.VITE_API_URL
export const API_BASE_URL = (rawAPIBaseURL ?? 'http://localhost:8080').replace(/\/api\/?$/, '')

if (!rawAPIBaseURL) {
  console.warn(
    'VITE_API_URL is not defined for frontend utilities. Falling back to "http://localhost:8080".'
  )
}

export const API_ENDPOINTS = {
  LOGIN: `${API_BASE_URL}/api/auth/login`,
  REGISTER: `${API_BASE_URL}/api/auth/register`,
  VERIFY_EMAIL: `${API_BASE_URL}/api/auth/verify-email`,
  RESEND_VERIFICATION: `${API_BASE_URL}/api/auth/resend-verification`,
  FORGOT_PASSWORD: `${API_BASE_URL}/api/auth/forgot-password`,
  RESET_PASSWORD: `${API_BASE_URL}/api/auth/reset-password`,
  TEST_EMAIL: `${API_BASE_URL}/api/auth/test-email`,

  GET_CURRENT_USER: `${API_BASE_URL}/api/users/me`,
  GET_CURRENT_USER_PROFILE: `${API_BASE_URL}/api/users/me/profile`,
  UPDATE_USER: `${API_BASE_URL}/api/users/me`,
  CHANGE_PASSWORD: `${API_BASE_URL}/api/users/me/password`,
  UPDATE_AVATAR: `${API_BASE_URL}/api/users/me/avatar`,
  DELETE_ACCOUNT: `${API_BASE_URL}/api/users/me`,

  FREELANCER_PROFILE: `${API_BASE_URL}/api/freelancer/profile`,
  FREELANCER_SERVICES: `${API_BASE_URL}/api/freelancer/services`,
  FREELANCER_PORTFOLIO: `${API_BASE_URL}/api/freelancer/portfolio`,
}

export function logout(router?: any, message?: string) {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  localStorage.removeItem('userProfile')

  if (message) {
    toast.warning(message)
  }

  if (router) {
    router.push('/login')
  } else if (typeof window !== 'undefined') {
    window.location.href = '/login'
  }
}

export function isTokenExpired(): boolean {
  const token = localStorage.getItem('token')
  if (!token) return true

  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    const exp = payload.exp * 1000
    return Date.now() >= exp
  } catch (error) {
    console.error('Erro ao verificar expiracao do token:', error)
    return true
  }
}

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

export async function fetchWithAuth(url: string, options: RequestInit = {}) {
  const token = localStorage.getItem('token')

  if (token && isTokenExpired()) {
    logout(undefined, 'Sua sessao expirou. Faca login novamente.')
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

    if (response.status === 401) {
      const errorData = await response.json().catch(() => ({}))

      if (errorData.expired === true || errorData.message?.includes('expirado')) {
        logout(undefined, 'Sua sessao expirou. Faca login novamente.')
        throw new Error('Token expirado')
      }
    }

    return response
  } catch (error: any) {
    if (error.message === 'Token expirado') {
      throw error
    }
    throw error
  }
}

export function handleApiError(error: any): string {
  if (error.message === 'Token expirado') {
    return 'Sua sessao expirou. Por favor, faca login novamente.'
  }
  if (error.message) {
    return error.message
  }
  if (typeof error === 'string') {
    return error
  }
  return 'Erro desconhecido. Tente novamente.'
}

export function startTokenExpirationChecker(router: any) {
  if (isTokenExpired()) {
    logout(router, 'Sua sessao expirou. Faca login novamente.')
    return
  }

  const intervalId = setInterval(() => {
    if (isTokenExpired()) {
      clearInterval(intervalId)
      logout(router, 'Sua sessao expirou. Faca login novamente.')
    } else if (isTokenExpiringSoon()) {
      toast.warning('Sua sessao ira expirar em breve. Salve seu trabalho.')
    }
  }, 60000)

  return () => clearInterval(intervalId)
}
