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

export const authService = {
  async login(credentials: LoginCredentials): Promise<AuthUser> {
    const response = await api.post<{
      token?: string
      user?: { id: number; name: string; email: string; userType: string; avatarUrl?: string }
    }>('/auth/admin/login', credentials)

    const userData = response.data
    const apiUserType = userData.user?.userType
    const userType =
      apiUserType === 'ADMIN'
        ? 'admin'
        : apiUserType === 'FREELANCER'
          ? 'provider'
          : 'contractor'

    return {
      id: userData.user?.id?.toString() || '',
      name: userData.user?.name || '',
      email: userData.user?.email || credentials.email,
      type: userType as AuthUser['type'],
      avatar: userData.user?.avatarUrl || undefined,
      token: userData.token,
    }
  },
}
