import api from '../config/axios';

export interface ClientHomeRequestItem {
  id: number;
  freelancerId?: number | null;
  freelancerName?: string | null;
  freelancerAvatar?: string | null;
  serviceName: string;
  categoryLabel?: string | null;
  category?: string | null;
  subcategoryLabel?: string | null;
  subcategory?: string | null;
  description: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface ClientHomeFreelancerItem {
  id: number;
  name: string;
  avatar?: string | null;
  professionalTitle?: string | null;
  rating?: number | null;
  totalReviews?: number | null;
  location?: string | null;
  available?: boolean | null;
  category?: string | null;
}

export interface ClientHomeServiceItem {
  id: number;
  freelancerId: number;
  freelancerName: string;
  title: string;
  category?: string | null;
  categoryLabel?: string | null;
  subcategory?: string | null;
  subcategoryLabel?: string | null;
  description: string;
  serviceRating?: number | null;
  serviceReviewCount?: number | null;
  price: number;
  priceType: string;
  deliveryTime: number;
  deliveryUnit: string;
  workModel?: string | null;
  tags: string[];
  images: string[];
  isActive: boolean;
  freelancerProfilePhoto?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ClientHomeResponse {
  region: {
    city?: string | null;
    state?: string | null;
    displayName: string;
    localized: boolean;
  };
  interestCategories: string[];
  recentRequests: ClientHomeRequestItem[];
  activeProjects: ClientHomeRequestItem[];
  favorites: {
    serviceCount: number;
    professionalCount: number;
    services: ClientHomeServiceItem[];
    professionals: ClientHomeFreelancerItem[];
  };
  featuredProfessionals: ClientHomeFreelancerItem[];
  featuredServices: ClientHomeServiceItem[];
}

export const clientHomeService = {
  async getHome(): Promise<ClientHomeResponse> {
    const response = await api.get('/client-home');
    return response.data;
  },
};
