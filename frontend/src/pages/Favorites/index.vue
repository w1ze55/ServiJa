<template>
  <MainLayout
    :user="currentUser"
    :current-page="'favorites'"
    :on-navigate="handleNavigate"
    :on-logout="handleLogout"
  >
    <div class="space-y-6 max-w-7xl mx-auto">
      <!-- Header -->
      <div class="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <h1 class="text-2xl font-bold text-[#263238] mb-2">Meus Favoritos</h1>
          <p class="text-[#435574]">
            Sua lista de serviços e profissionais salvos na plataforma
          </p>
        </div>
      </div>

      <!-- Mode Toggle -->
      <Tabs v-model="activeTab" class="w-full">
        <TabsList class="grid w-full grid-cols-2 max-w-[400px]">
          <TabsTrigger value="services" class="flex items-center gap-2">
            <Briefcase class="h-4 w-4" />
            Serviços
          </TabsTrigger>
          <TabsTrigger value="professionals" class="flex items-center gap-2">
            <UserCircle class="h-4 w-4" />
            Profissionais
          </TabsTrigger>
        </TabsList>
      </Tabs>

      <!-- Results Content -->
      <div v-if="favoritesStore.loading && !favoritesStore.initialized" class="flex items-center justify-center py-20">
        <div class="text-center">
          <Loader2 class="h-10 w-10 text-[#286AD8] animate-spin mx-auto mb-4" />
          <p class="text-[#435574]">Carregando seus favoritos...</p>
        </div>
      </div>

      <div v-else-if="filteredFavorites.length === 0" class="text-center py-16 bg-white rounded-xl border border-dashed border-gray-200">
        <div class="w-16 h-16 bg-gray-50 rounded-full flex items-center justify-center mx-auto mb-4">
          <Heart class="h-8 w-8 text-gray-300" />
        </div>
        <h3 class="text-[#263238] font-semibold mb-2">Nenhum favorito encontrado</h3>
        <p class="text-[#435574] mb-6">
          Você ainda não favoritou nenhum {{ activeTab === 'professionals' ? 'profissional' : 'serviço' }}.
        </p>
        <Button @click="handleNavigate('explore')">
          Explorar Plataforma
        </Button>
      </div>

      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <template v-if="activeTab === 'professionals'">
          <FreelancerCard 
            v-for="fav in filteredFavorites" 
            :key="fav.id"
            :freelancer="{
              id: fav.freelancer!.userId,
              name: fav.freelancer!.name,
              avatar: fav.freelancer!.avatarUrl,
              category: fav.freelancer!.category,
              professionalTitle: fav.freelancer!.professionalTitle,
              location: fav.freelancer!.location,
              rating: 5.0,
              reviewCount: 0,
              isAvailable: true
            }"
            :is-favorite="true"
            @view-profile="handleViewProfile"
            @toggle-favorite="handleToggleFavoriteFreelancer"
          />
        </template>
        
        <template v-else>
          <ServiceCard
            v-for="fav in filteredFavorites"
            :key="fav.id"
            :service="mapService(fav.service!)"
            :is-favorite="true"
            @navigate="handleViewService"
            @toggle-favorite="handleToggleFavoriteService"
          />
        </template>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { 
  Heart, 
  RefreshCw, 
  Briefcase, 
  UserCircle, 
  Loader2 
} from 'lucide-vue-next';
import MainLayout from '@/components/layout/MainLayout.vue';
import Button from '@/components/ui/button.vue';
import Tabs from '@/components/ui/tabs.vue';
import TabsList from '@/components/ui/tabs-list.vue';
import TabsTrigger from '@/components/ui/tabs-trigger.vue';
import ServiceCard from '@/components/service/ServiceCard.vue';
import FreelancerCard from '@/components/freelancer/FreelancerCard.vue';
import { useFavoritesStore } from '@/stores/favorites';
import { useUserStore } from '@/stores/userStore';
import { useNavigation, getRoute } from '@/utils/navigation';

const router = useRouter();
const userStore = useUserStore();
const favoritesStore = useFavoritesStore();
const { logout } = useNavigation();

const activeTab = ref<'services' | 'professionals'>('services');

const currentUser = computed(() => {
  const user = userStore.user;
  return {
    id: user?.id?.toString() || '',
    name: user?.name || 'Usuário',
    email: user?.email || '',
    type: (user?.type === 'provider' ? 'provider' : 'contractor') as 'provider' | 'contractor',
    avatar: user?.avatar || undefined,
  };
});

onMounted(() => {
  if (!favoritesStore.initialized) {
    favoritesStore.fetchFavorites();
  }
});

const filteredFavorites = computed(() => {
  if (activeTab.value === 'professionals') {
    return favoritesStore.favorites.filter(f => f.freelancer);
  } else {
    return favoritesStore.favorites.filter(f => f.service);
  }
});

const mapService = (service: any) => {
  return {
    ...service,
    images: service.images || [],
    tags: service.tags || []
  };
};

const handleNavigate = (page: string, params?: Record<string, any>) => {
  if (page === 'provider-profile' && params?.providerId) {
    router.push({
      name: 'provider-profile',
      params: { providerId: params.providerId },
      query: params.serviceId ? { serviceId: params.serviceId } : {}
    });
    return;
  }

  const route = getRoute(page, currentUser.value.type);
  if (params) {
    router.push({ path: route, query: params });
  } else {
    router.push(route);
  }
};

const handleLogout = () => {
  logout();
};

const handleViewProfile = (providerId: string | number) => {
  handleNavigate('provider-profile', { providerId: Number(providerId) });
};

const handleViewService = (params: { providerId: number; serviceId: number }) => {
  handleNavigate('provider-profile', params);
};

const handleToggleFavoriteFreelancer = (id: string | number) => {
  favoritesStore.toggleFavoriteFreelancer(typeof id === 'string' ? parseInt(id, 10) : id);
};

const handleToggleFavoriteService = (id: number) => {
  favoritesStore.toggleFavoriteService(id);
};
</script>
