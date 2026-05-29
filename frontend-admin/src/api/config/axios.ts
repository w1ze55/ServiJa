import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_URL
const cleanBaseURL = (API_BASE_URL ?? '').replace(/\/api\/?$/, '')
const resolvedBaseURL = cleanBaseURL ? `${cleanBaseURL}/api` : '/api'

if (!API_BASE_URL) {
  console.warn(
    'VITE_API_URL is not defined for frontend-admin. Falling back to "/api".'
  )
}

const api = axios.create({
  baseURL: resolvedBaseURL,
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    let token = localStorage.getItem('token')

    if (!token) {
      const user = localStorage.getItem('user')
      if (user) {
        try {
          const userData = JSON.parse(user) as { token?: string }
          token = userData.token
        } catch {
          // ignore
        }
      }
    }

    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error: AxiosError) => Promise.reject(error)
)

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')

      if (window.location.pathname !== '/login') {
        window.location.href = '/login?expired=true'
      }
    }
    return Promise.reject(error)
  }
)

export default api
