import { defineStore } from 'pinia';
import { favoriteService } from '../api';
import type { Favorite } from '../api/services/favoriteService';
import { toast } from '../utils/toast';

export const useFavoritesStore = defineStore('favorites', {
    state: () => ({
        favorites: [] as Favorite[],
        loading: false,
        initialized: false
    }),

    getters: {
        isFavoriteService: (state) => (serviceId: number | string) =>
            state.favorites.some(f => Number(f.service?.id) === Number(serviceId)),

        isFavoriteFreelancer: (state) => (freelancerId: number | string) =>
            state.favorites.some(f => Number(f.freelancer?.userId) === Number(freelancerId)),

        getFavoriteIdByService: (state) => (serviceId: number | string) =>
            state.favorites.find(f => Number(f.service?.id) === Number(serviceId))?.id,

        getFavoriteIdByFreelancer: (state) => (freelancerId: number | string) =>
            state.favorites.find(f => Number(f.freelancer?.userId) === Number(freelancerId))?.id
    },

    actions: {
        async fetchFavorites() {
            this.loading = true;
            try {
                this.favorites = await favoriteService.getUserFavorites();
                this.initialized = true;
            } catch (error) {
                console.error('Error fetching favorites:', error);
            } finally {
                this.loading = false;
            }
        },

        async toggleFavoriteFreelancer(freelancerId: number | string) {
            const favoriteId = this.getFavoriteIdByFreelancer(freelancerId);

            if (favoriteId) {
                try {
                    await favoriteService.removeFavorite(favoriteId);
                    this.favorites = this.favorites.filter(f => f.id !== favoriteId);
                    toast.success('Removido dos favoritos');
                } catch (error) {
                    toast.error('Erro ao remover dos favoritos');
                }
            } else {
                try {
                    const newFavorite = await favoriteService.addFavorite({ freelancerId: Number(freelancerId) });
                    this.favorites.push(newFavorite);
                    toast.success('Adicionado aos favoritos');
                } catch (error) {
                    toast.error('Erro ao adicionar aos favoritos');
                }
            }
        },

        async toggleFavoriteService(serviceId: number | string) {
            const favoriteId = this.getFavoriteIdByService(serviceId);

            if (favoriteId) {
                try {
                    await favoriteService.removeFavorite(favoriteId);
                    this.favorites = this.favorites.filter(f => f.id !== favoriteId);
                    toast.success('Removido dos favoritos');
                } catch (error) {
                    toast.error('Erro ao remover dos favoritos');
                }
            } else {
                try {
                    const newFavorite = await favoriteService.addFavorite({ serviceId: Number(serviceId) });
                    this.favorites.push(newFavorite);
                    toast.success('Adicionado aos favoritos');
                } catch (error) {
                    toast.error('Erro ao adicionar aos favoritos');
                }
            }
        }
    }
});
