import api from '../config/axios';

export interface Freelancer {
  id: string;
  name: string;
  email: string;
  avatar?: string;
  bio?: string;
  skills?: string[];
  rating?: number;
  reviewsCount?: number;
}

export interface FreelancerProfile extends Freelancer {
  portfolioItems?: PortfolioItem[];
  services?: Service[];
  reviews?: Review[];
}

export interface PortfolioItem {
  id: string;
  title: string;
  description: string;
  imageUrl?: string;
  projectUrl?: string;
}

export interface Service {
  id: string;
  title: string;
  description: string;
  price: number;
  deliveryTime: string;
  category?: string;
  workModel?: string | null;
}

export interface Review {
  id: string;
  rating: number;
  comment: string;
  clientName: string;
  clientAvatar?: string;
  date: string;
}

export const freelancerService = {
  /**
   * Obtém perfil completo do freelancer
   */
  async getProfile(freelancerId: string): Promise<FreelancerProfile> {
    const response = await api.get(`/freelancers/${freelancerId}`);
    return response.data;
  },

  /**
   * Atualiza perfil do freelancer
   */
  async updateProfile(data: Partial<FreelancerProfile>): Promise<FreelancerProfile> {
    const response = await api.put('/freelancers/profile', data);
    return response.data;
  },

  /**
   * Lista freelancers com filtros
   */
  async searchFreelancers(params: {
    query?: string;
    category?: string;
    minRating?: number;
    maxPrice?: number;
  }): Promise<Freelancer[]> {
    const response = await api.get('/freelancers/search', { params });
    return response.data;
  },

  /**
   * Adiciona item ao portfólio
   */
  async addPortfolioItem(item: Omit<PortfolioItem, 'id'>): Promise<PortfolioItem> {
    const response = await api.post('/freelancers/portfolio', item);
    return response.data;
  },

  /**
   * Remove item do portfólio
   */
  async removePortfolioItem(itemId: string): Promise<void> {
    await api.delete(`/freelancers/portfolio/${itemId}`);
  },

  /**
   * Lista serviços do freelancer
   */
  async getServices(freelancerId: string): Promise<Service[]> {
    const response = await api.get(`/freelancers/${freelancerId}/services`);
    return response.data;
  },

  /**
   * Adiciona novo serviço
   */
  async addService(service: Omit<Service, 'id'>): Promise<Service> {
    const response = await api.post('/freelancers/services', service);
    return response.data;
  },

  /**
   * Atualiza serviço
   */
  async updateService(serviceId: string, service: Partial<Service>): Promise<Service> {
    const response = await api.put(`/freelancers/services/${serviceId}`, service);
    return response.data;
  },

  /**
   * Remove serviço
   */
  async removeService(serviceId: string): Promise<void> {
    await api.delete(`/freelancers/services/${serviceId}`);
  },

  async refineBio(bio: string): Promise<string> {
    const response = await api.post<{ refinedBio: string }>('/freelancer/ai/refine-bio', { bio });
    return response.data.refinedBio;
  },
};
