import api from '@/api/config/axios'

export interface LoginCredentials {
  email: string
  password: string
}

export interface AuthUser {
  id: string
  name: string
  email: string
  type: 'contractor' | 'provider' | 'admin'
  avatar?: string
  token?: string
}

export interface LoginResult {
  user?: AuthUser
  requires2fa?: boolean
  challengeToken?: string
}

interface RawAuthResponse {
  token?: string
  tokenType?: string
  requires2fa?: boolean
  user?: { id: number; name: string; email: string; userType: string; avatarUrl?: string }
}

function toAuthUser(data: RawAuthResponse, fallbackEmail = ''): AuthUser {
  const apiUserType = data.user?.userType
  const userType =
    apiUserType === 'ADMIN'
      ? 'admin'
      : apiUserType === 'FREELANCER'
        ? 'provider'
        : 'contractor'
  return {
    id: data.user?.id?.toString() || '',
    name: data.user?.name || '',
    email: data.user?.email || fallbackEmail,
    type: userType as AuthUser['type'],
    avatar: data.user?.avatarUrl || undefined,
    token: data.token,
  }
}

export const authService = {
  async login(credentials: LoginCredentials): Promise<LoginResult> {
    const response = await api.post<RawAuthResponse>('/auth/admin/login', credentials)
    const data = response.data

    if (data.requires2fa && data.token) {
      return { requires2fa: true, challengeToken: data.token }
    }
    return { user: toAuthUser(data, credentials.email) }
  },

  async verifyTwoFactor(challengeToken: string, code: string): Promise<AuthUser> {
    const response = await api.post<RawAuthResponse>('/auth/2fa/verify', {
      challengeToken,
      code,
    })
    return toAuthUser(response.data)
  },
}
