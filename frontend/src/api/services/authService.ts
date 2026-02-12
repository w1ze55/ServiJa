import api from '../config/axios';

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface RegisterData {
  name: string;
  email: string;
  password: string;
  type: 'contractor' | 'provider';
}

export interface User {
  id: string;
  name: string;
  email: string;
  type: 'contractor' | 'provider';
  avatar?: string;
  token?: string;
}

export const authService = {
  /**
   * Realiza login do usuário
   */
  async login(credentials: LoginCredentials): Promise<User> {
    const response = await api.post('/auth/login', credentials);
    return response.data;
  },

  /**
   * Registra novo usuário
   */
  async register(data: RegisterData): Promise<User> {
    const response = await api.post('/auth/register', data);
    return response.data;
  },

  /**
   * Obtém dados do usuário autenticado
   */
  async getCurrentUser(): Promise<User> {
    const response = await api.get('/auth/me');
    return response.data;
  },

  /**
   * Realiza logout do usuário
   */
  async logout(): Promise<void> {
    await api.post('/auth/logout');
    localStorage.removeItem('user');
  },

  /**
   * Solicita recuperação de senha
   */
  async forgotPassword(email: string): Promise<void> {
    await api.post('/auth/forgot-password', { email });
  },

  /**
   * Redefine a senha do usuário
   */
  async resetPassword(token: string, newPassword: string): Promise<void> {
    await api.post('/auth/reset-password', { token, newPassword });
  },

  /**
   * Verifica email do usuário
   */
  async verifyEmail(token: string): Promise<void> {
    await api.post('/auth/verify-email', { token });
  },
};

