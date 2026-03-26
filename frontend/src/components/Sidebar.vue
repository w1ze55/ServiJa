<script setup lang="ts">
import { computed } from 'vue';
import {
  Home,
  Search,
  MessageSquare,
  User,
  FileText,
  LogOut,
  LayoutDashboard,
  Star,
  Heart,
} from 'lucide-vue-next';
import Button from './ui/button.vue';
import Separator from './ui/separator.vue';
import logo from '@/assets/favicon-32x32.png';

export interface UserType {
  id: string;
  name: string;
  email: string;
  type: 'contractor' | 'provider';
  avatar?: string;
}

export type Page =
  | 'home'
  | 'explore'
  | 'search-results'
  | 'requests'
  | 'messages'
  | 'favorites'
  | 'contracts'
  | 'notifications'
  | 'settings'
  | 'profile'
  | 'freelancer-profile'
  | 'freelancer-dashboard'
  | 'freelancer-onboarding'
  | 'freelancer-profile-settings'
  | 'freelancer-services'
  | 'freelancer-service-edit'
  | 'freelancer-proposals'
  | 'freelancer-proposal-detail'
  | 'freelancer-deliveries'
  | 'freelancer-wallet'

  | 'freelancer-portfolio'

  | 'freelancer-kyc'
  | 'freelancer-account-settings'
  | 'add-address'
  | 'request-quote'
  | 'quote-detail'
  | 'chat'
  | 'checkout'
  | 'payment-confirmation'
  | 'service-timeline'
  | 'delivery-approval'
  | 'rate-freelancer'
  | 'contractor-dashboard'
  | 'provider-dashboard'
  | 'provider-profile'
  | 'search'
  | 'landing';

interface Props {
  user: UserType;
  currentPage: Page;
  onNavigate: (page: Page) => void;
  onLogout: () => void;
}

const props = defineProps<Props>();

interface MenuItem {
  icon: any;
  label: string;
  page: Page;
  id: string;
}

const contractorMenuItems: MenuItem[] = [
  { icon: LayoutDashboard, label: 'Dashboard', page: 'contractor-dashboard', id: 'dashboard' },
  { icon: Search, label: 'Buscar Serviços', page: 'search', id: 'search' },
  { icon: MessageSquare, label: 'Mensagens', page: 'chat', id: 'messages' },
  { icon: Heart, label: 'Meus Favoritos', page: 'favorites', id: 'favorites' },
  { icon: Star, label: 'Avaliações', page: 'contractor-dashboard', id: 'reviews' },
];

const providerMenuItems: MenuItem[] = [
  { icon: LayoutDashboard, label: 'Dashboard', page: 'provider-dashboard', id: 'dashboard' },
  { icon: FileText, label: 'Solicitações', page: 'provider-dashboard', id: 'requests' },
  { icon: MessageSquare, label: 'Mensagens', page: 'chat', id: 'messages' },
  { icon: User, label: 'Meu Perfil', page: 'provider-profile', id: 'profile' },
];

const menuItems = computed(() =>
  props.user.type === 'contractor' ? contractorMenuItems : providerMenuItems
);
</script>

<template>
  <div class="w-64 bg-white border-r border-gray-200 h-screen flex flex-col fixed left-0 top-0">
    <!-- Logo -->
    <div class="p-6 border-b border-gray-200">
      <div class="flex items-center gap-2 mb-2">
        <div class="w-10 h-10 bg-gradient-to-br from-blue-600 to-purple-600 rounded-lg flex items-center justify-center">
          <img :src="`${logo}`" alt="Servi Já" />
        </div>
        <img :src="`${logo}`" alt="Servi Já" />
        <span class="text-gray-900">Servi Já</span>
      </div>
      <p class="text-gray-600 mt-2">
        {{ user.type === 'contractor' ? 'Contratante' : 'Prestador' }}
      </p>
    </div>

    <!-- Navigation -->
    <nav class="flex-1 p-4">
      <ul class="space-y-2">
        <li v-for="item in menuItems" :key="item.id">
          <button
            @click="props.onNavigate(item.page)"
            :class="`w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${
              currentPage === item.page
                ? 'bg-blue-50 text-blue-600'
                : 'text-gray-700 hover:bg-gray-50'
            }`"
          >
            <component :is="item.icon" :size="20" />
            <span>{{ item.label }}</span>
          </button>
        </li>
      </ul>
    </nav>

    <!-- Bottom Actions -->
    <div class="p-4 border-t border-gray-200">
      <Button variant="ghost" class="w-full justify-start" @click="props.onNavigate('landing')">
        <Home :size="20" class="mr-3" />
        Página Inicial
      </Button>
      <Separator class="my-2" />
      <Button
        variant="ghost"
        class="w-full justify-start text-red-600 hover:text-red-700 hover:bg-red-50"
        @click="props.onLogout"
      >
        <LogOut :size="20" class="mr-3" />
        Sair
      </Button>
    </div>
  </div>
</template>




