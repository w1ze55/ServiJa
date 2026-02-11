import api from '../config/axios';

export interface DashboardStats {
  totalProjects: number;
  activeProjects: number;
  completedProjects: number;
  totalEarnings?: number;
  totalSpent?: number;
  pendingQuotes: number;
}

export interface RecentActivity {
  id: string;
  type: 'quote' | 'contract' | 'payment' | 'message' | 'review';
  title: string;
  description: string;
  date: string;
  status?: string;
}

export const dashboardService = {
  /**
   * Obtém estatísticas do dashboard do freelancer
   */
  async getFreelancerStats(): Promise<DashboardStats> {
    const response = await api.get('/dashboard/freelancer/stats');
    return response.data;
  },

  /**
   * Obtém atividades recentes do freelancer
   */
  async getFreelancerActivities(): Promise<RecentActivity[]> {
    const response = await api.get('/dashboard/freelancer/activities');
    return response.data;
  },

  /**
   * Obtém estatísticas do dashboard do cliente
   */
  async getClientStats(): Promise<DashboardStats> {
    const response = await api.get('/dashboard/client/stats');
    return response.data;
  },

  /**
   * Obtém atividades recentes do cliente
   */
  async getClientActivities(): Promise<RecentActivity[]> {
    const response = await api.get('/dashboard/client/activities');
    return response.data;
  },
};

