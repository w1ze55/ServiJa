import api from '../config/axios';
import type { Service } from './freelancerService';

export interface Favorite {
    id: number;
    userId: number;
    service?: Partial<Service>;
    freelancer?: {
        userId: number;
        name: string;
        professionalTitle: string;
        category: string;
        avatarUrl?: string;
    };
    createdAt: string;
}

export interface FavoriteRequest {
    serviceId?: number;
    freelancerId?: number;
}

export const favoriteService = {
    /**
     * Adiciona um serviço ou freelancer aos favoritos
     */
    async addFavorite(request: FavoriteRequest): Promise<Favorite> {
        const response = await api.post('/favorites', request);
        return response.data;
    },

    /**
     * Remove um favorito pelo ID
     */
    async removeFavorite(id: number): Promise<void> {
        await api.delete(`/favorites/${id}`);
    },

    /**
     * Lista todos os favoritos do usuário autenticado
     */
    async getUserFavorites(): Promise<Favorite[]> {
        const response = await api.get('/favorites');
        return response.data;
    },

    /**
     * Verifica se um serviço ou freelancer está favoritado
     */
    async checkFavorite(params: { serviceId?: number; freelancerId?: number }): Promise<boolean> {
        const response = await api.get('/favorites/check', { params });
        return response.data;
    }
};
