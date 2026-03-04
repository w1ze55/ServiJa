import api from '../config/axios';

export interface Client {
  id: string;
  name: string;
  email: string;
  avatar?: string;
  company?: string;
}

export interface Quote {
  id: string;
  serviceTitle: string;
  description: string;
  budget?: number;
  status: 'pending' | 'accepted' | 'in_progress' | 'rejected' | 'completed';
  freelancerId: string;
  freelancerName: string;
  createdAt: string;
}

export interface Contract {
  id: string;
  serviceTitle: string;
  freelancerName: string;
  status: 'active' | 'completed' | 'cancelled';
  startDate: string;
  endDate?: string;
  price: number;
}

export const clientService = {
  /**
   * Obtém perfil do cliente
   */
  async getProfile(clientId: string): Promise<Client> {
    const response = await api.get(`/clients/${clientId}`);
    return response.data;
  },

  /**
   * Atualiza perfil do cliente
   */
  async updateProfile(data: Partial<Client>): Promise<Client> {
    const response = await api.put('/clients/profile', data);
    return response.data;
  },

  /**
   * Lista solicitações de orçamento do cliente
   */
  async getQuotes(): Promise<Quote[]> {
    const response = await api.get('/clients/quotes');
    return response.data;
  },

  /**
   * Cria nova solicitação de orçamento
   */
  async createQuote(quote: Omit<Quote, 'id' | 'status' | 'createdAt'>): Promise<Quote> {
    const response = await api.post('/clients/quotes', quote);
    return response.data;
  },

  /**
   * Lista contratos do cliente
   */
  async getContracts(): Promise<Contract[]> {
    const response = await api.get('/clients/contracts');
    return response.data;
  },

  /**
   * Obtém detalhes de um contrato
   */
  async getContract(contractId: string): Promise<Contract> {
    const response = await api.get(`/clients/contracts/${contractId}`);
    return response.data;
  },

  /**
   * Lista favoritos do cliente
   */
  async getFavorites(): Promise<string[]> {
    const response = await api.get('/clients/favorites');
    return response.data;
  },

  /**
   * Adiciona freelancer aos favoritos
   */
  async addFavorite(freelancerId: string): Promise<void> {
    await api.post('/clients/favorites', { freelancerId });
  },

  /**
   * Remove freelancer dos favoritos
   */
  async removeFavorite(freelancerId: string): Promise<void> {
    await api.delete(`/clients/favorites/${freelancerId}`);
  },
};

