<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import {
  Home,
  Compass,
  FileText,
  Heart,
  Settings,
  Menu,
  X,
  MapPin,
  User,
  LogOut,
  MessageSquare,
  Bell,
  Clock,
  Briefcase,
  Calendar,
  Images,
  DollarSign,
  Wallet,
  Receipt,
  Package,
  Map,
  FolderKanban,
} from 'lucide-vue-next';
import Avatar from '../ui/avatar.vue';
import AvatarImage from '../ui/avatar-image.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import DropdownMenu from '../ui/dropdown-menu.vue';
import DropdownMenuContent from '../ui/dropdown-menu-content.vue';
import DropdownMenuItem from '../ui/dropdown-menu-item.vue';
import DropdownMenuLabel from '../ui/dropdown-menu-label.vue';
import DropdownMenuSeparator from '../ui/dropdown-menu-separator.vue';
import DropdownMenuTrigger from '../ui/dropdown-menu-trigger.vue';
import logo from '@/assets/favicon-32x32.png';
import api from '@/api/config/axios';
import { useUserStore } from '@/stores/userStore';

const router = useRouter();
const userStore = useUserStore();

interface User {
  id: string;
  name: string;
  email: string;
  type: 'contractor' | 'provider';
  avatar?: string;
}

interface UserAddress {
  id: number;
  label: string;
  city: string;
  state: string;
  isDefault: boolean;
}

interface Props {
  user: User;
  currentPage: string;
  onNavigate: (page: string) => void;
  onLogout: () => void;
  onSearch?: (query: string) => void;
}

const props = defineProps<Props>();

// Usar o usuário da store se disponível, caso contrário usar das props
const currentUser = computed(() => userStore.user || props.user);

const sidebarOpen = ref(false);
const sidebarExpanded = ref(false);

// Localização do usuário
const userLocation = ref('Carregando...');
const userAddresses = ref<UserAddress[]>([]);
const defaultAddress = ref<UserAddress | null>(null);

// Carregar endereços do usuário
const loadUserAddresses = async () => {
  try {
    const response = await api.get('/addresses');
    userAddresses.value = response.data || [];
    defaultAddress.value = userAddresses.value.find(addr => addr.isDefault) || userAddresses.value[0] || null;
    if (defaultAddress.value) {
      userLocation.value = `${defaultAddress.value.city}, ${defaultAddress.value.state}`;
    } else {
      getUserLocationFromGPS();
    }
  } catch (error) {
    console.error('Erro ao carregar endereços:', error);
    getUserLocationFromGPS();
  }
};

const getUserLocationFromGPS = () => {
  if ('geolocation' in navigator) {
    navigator.geolocation.getCurrentPosition(
      async (position) => {
        try {
          // Reverse geocoding usando Nominatim
          const response = await fetch(
            `https://nominatim.openstreetmap.org/reverse?lat=${position.coords.latitude}&lon=${position.coords.longitude}&format=json`
          );
          const data = await response.json();

          const city = data.address?.city || data.address?.town || data.address?.village || '';
          const state = data.address?.state ? getStateAbbreviation(data.address.state) : '';

          if (city && state) {
            userLocation.value = `${city}, ${state}`;
          } else {
            userLocation.value = 'Localização não definida';
          }
        } catch (e) {
          userLocation.value = 'Localização não definida';
        }
      },
      () => {
        userLocation.value = 'Localização não definida';
      },
      { timeout: 5000 }
    );
  } else {
    userLocation.value = 'Localização não definida';
  }
};

// Converter nome do estado para sigla
const getStateAbbreviation = (stateName: string): string => {
  const states: Record<string, string> = {
    'Acre': 'AC', 'Alagoas': 'AL', 'Amapá': 'AP', 'Amazonas': 'AM',
    'Bahia': 'BA', 'Ceará': 'CE', 'Distrito Federal': 'DF', 'Espírito Santo': 'ES',
    'Goiás': 'GO', 'Maranhão': 'MA', 'Mato Grosso': 'MT', 'Mato Grosso do Sul': 'MS',
    'Minas Gerais': 'MG', 'Pará': 'PA', 'Paraíba': 'PB', 'Paraná': 'PR',
    'Pernambuco': 'PE', 'Piauí': 'PI', 'Rio de Janeiro': 'RJ', 'Rio Grande do Norte': 'RN',
    'Rio Grande do Sul': 'RS', 'Rondônia': 'RO', 'Roraima': 'RR', 'Santa Catarina': 'SC',
    'São Paulo': 'SP', 'Sergipe': 'SE', 'Tocantins': 'TO'
  };
  return states[stateName] || stateName.substring(0, 2).toUpperCase();
};

// Abrir mapa (navegar para a página do mapa)
const openMap = () => {
  router.push('/map');
};

// Lifecycle
onMounted(() => {
  loadUserAddresses();
});

// Menu items based on user type
const menuItems = computed(() =>
  currentUser.value.type === 'contractor'
    ? [
        // Menu para Clientes
        { id: 'home', label: 'Início', icon: Home },
        { id: 'explore', label: 'Explorar', icon: Compass },
        { id: 'requests', label: 'Minhas Solicitações', icon: FileText },
        { id: 'messages', label: 'Mensagens', icon: MessageSquare },
        { id: 'favorites', label: 'Favoritos', icon: Heart },
        { id: 'settings', label: 'Configurações', icon: Settings },
      ]
    : [
        // Menu para Freelancers
        { id: 'freelancer-dashboard', label: 'Dashboard', icon: Home },
        { id: 'freelancer-professional-profile', label: 'Portfólio Profissional', icon: FolderKanban },
        { id: 'freelancer-services', label: 'Meus Serviços', icon: Briefcase },
        { id: 'freelancer-proposals', label: 'Propostas', icon: FileText },
        { id: 'freelancer-deliveries', label: 'Entregas', icon: Package },
        { id: 'freelancer-wallet', label: 'Carteira', icon: Wallet },
        { id: 'freelancer-invoices', label: 'Faturas', icon: Receipt },
        { id: 'messages', label: 'Mensagens', icon: MessageSquare },
        { id: 'freelancer-portfolio', label: 'Galeria de Trabalhos', icon: Images },
        { id: 'freelancer-schedule', label: 'Agenda', icon: Calendar },
        { id: 'settings', label: 'Configurações', icon: Settings },
      ]
);

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
};
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <!-- Mobile Menu Overlay -->
    <div
      v-if="sidebarOpen"
      class="fixed inset-0 bg-black/50 z-[55] lg:hidden"
      @click="sidebarOpen = false"
    />

    <!-- Sidebar - Desktop with Hover -->
    <aside
      @mouseenter="sidebarExpanded = true"
      @mouseleave="sidebarExpanded = false"
      :class="`
        fixed top-0 left-0 h-full bg-[#263238] text-white z-[60]
        transition-all duration-300 ease-in-out
        hidden lg:block
        ${sidebarExpanded ? 'w-64' : 'w-20'}
      `"
    >
      <div class="flex flex-col h-full">
        <!-- Logo -->
        <div
          :class="`p-6 border-b border-white/10 transition-all duration-300 ${
            sidebarExpanded ? 'px-6' : 'px-4'
          }`"
        >
          <div class="flex items-center justify-center">
              <h1
                :class="`text-white transition-all duration-300 flex items-center gap-2 ${
                  sidebarExpanded ? 'opacity-100' : 'opacity-0 w-0'
                }`"
              >
                <img :src="`${logo}`" alt="Servi Já" /> <span class="text-white text-xl font-bold">Servi Já</span>
              </h1>
            <span v-if="!sidebarExpanded" class="text-white font-bold"><img :src="`${logo}`" alt="Servi Já" /></span>
          </div>
        </div>

        <!-- Location -->
        <div
          :class="`p-4 border-b border-white/10 transition-all duration-300 ${
            sidebarExpanded ? '' : 'px-2'
          }`"
        >
          <button
            @click="openMap"
            :class="`w-full flex items-center gap-2 text-white/70 hover:text-white transition-colors ${
              sidebarExpanded ? 'text-left' : 'justify-center'
            }`"
            title="Ver mapa com profissionais próximos"
          >
            <MapPin class="h-4 w-4 flex-shrink-0" />
            <div
              :class="`flex-1 min-w-0 overflow-hidden transition-all duration-300 ${
                sidebarExpanded ? 'opacity-100' : 'opacity-0 w-0'
              }`"
            >
              <p class="text-xs text-white/50">Localização</p>
              <p class="text-sm truncate">{{ userLocation }}</p>
            </div>
          </button>
        </div>

        <!-- Navigation -->
        <nav
          :class="`flex-1 p-4 space-y-1 overflow-y-auto ${sidebarExpanded ? '' : 'px-2'}`"
        >
          <button
            v-for="item in menuItems"
            :key="item.id"
            @click="props.onNavigate(item.id)"
            :class="`
              w-full flex items-center gap-3 rounded-lg transition-all duration-200
              ${sidebarExpanded ? 'px-4 py-3' : 'px-3 py-3 justify-center'}
              ${
                currentPage === item.id
                  ? 'bg-[#286AD8] text-white'
                  : 'text-white/70 hover:bg-white/10 hover:text-white'
              }
            `"
            :title="!sidebarExpanded ? item.label : ''"
          >
            <component :is="item.icon" class="h-5 w-5 flex-shrink-0" />
            <span
              :class="`transition-all duration-300 whitespace-nowrap ${
                sidebarExpanded ? 'opacity-100' : 'opacity-0 w-0 overflow-hidden'
              }`"
            >
              {{ item.label }}
            </span>
          </button>
        </nav>

        <!-- User Profile -->
        <div
          :class="`p-4 border-t border-white/10 transition-all duration-300 ${
            sidebarExpanded ? '' : 'px-2'
          }`"
        >
          <div :class="`flex items-center gap-3 ${sidebarExpanded ? '' : 'justify-center'}`">
            <Avatar class="h-10 w-10 flex-shrink-0">
              <AvatarImage :src="currentUser.avatar" :alt="currentUser.name" />
              <AvatarFallback class="bg-[#286AD8] text-white">
                {{ getInitials(currentUser.name) }}
              </AvatarFallback>
            </Avatar>
            <div
              :class="`flex-1 min-w-0 overflow-hidden transition-all duration-300 ${
                sidebarExpanded ? 'opacity-100' : 'opacity-0 w-0'
              }`"
            >
              <p class="text-sm truncate text-white">{{ currentUser.name }}</p>
              <p class="text-xs text-white/50">
                {{ currentUser.type === 'contractor' ? 'Contratante' : 'Prestador' }}
              </p>
            </div>
          </div>
        </div>
      </div>
    </aside>

    <!-- Sidebar - Mobile -->
    <aside
      :class="`
        fixed top-0 left-0 h-full w-64 bg-[#263238] text-white z-[60]
        transform transition-transform duration-300 ease-in-out
        ${sidebarOpen ? 'translate-x-0' : '-translate-x-full'}
        lg:hidden
      `"
    >
      <div class="flex flex-col h-full">
        <!-- Logo -->
        <div class="p-6 border-b border-white/10">
          <div class="flex items-center justify-between">
            <h1 class="text-white">Servi Já</h1>
            <button
              @click="sidebarOpen = false"
              class="text-white hover:text-gray-300"
            >
              <X class="h-6 w-6" />
            </button>
          </div>
        </div>

        <!-- Location -->
        <div class="p-4 border-b border-white/10">
          <button
            @click="openMap(); sidebarOpen = false;"
            class="w-full flex items-center gap-2 text-white/70 hover:text-white transition-colors text-left"
          >
            <MapPin class="h-4 w-4 flex-shrink-0" />
            <div class="flex-1 min-w-0">
              <p class="text-xs text-white/50">Localização</p>
              <p class="text-sm truncate">{{ userLocation }}</p>
            </div>
            <Map class="h-4 w-4 text-white/50" />
          </button>
        </div>

        <!-- Navigation -->
        <nav class="flex-1 p-4 space-y-1 overflow-y-auto">
          <button
            v-for="item in menuItems"
            :key="item.id"
            @click="
              props.onNavigate(item.id);
              sidebarOpen = false;
            "
            :class="`
              w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors
              ${
                currentPage === item.id
                  ? 'bg-[#286AD8] text-white'
                  : 'text-white/70 hover:bg-white/10 hover:text-white'
              }
            `"
          >
            <component :is="item.icon" class="h-5 w-5" />
            <span>{{ item.label }}</span>
          </button>
        </nav>

        <!-- User Profile -->
        <div class="p-4 border-t border-white/10">
          <div class="flex items-center gap-3">
            <Avatar class="h-10 w-10">
              <AvatarImage :src="currentUser.avatar" :alt="currentUser.name" />
              <AvatarFallback class="bg-[#286AD8] text-white">
                {{ getInitials(currentUser.name) }}
              </AvatarFallback>
            </Avatar>
            <div class="flex-1 min-w-0">
              <p class="text-sm truncate text-white">{{ currentUser.name }}</p>
              <p class="text-xs text-white/50">
                {{ currentUser.type === 'contractor' ? 'Contratante' : 'Prestador' }}
              </p>
            </div>
          </div>
        </div>
      </div>
    </aside>

    <!-- Main Content -->
    <div
      :class="`transition-all duration-300 ${sidebarExpanded ? 'lg:ml-64' : 'lg:ml-20'}`"
    >
      <!-- Top Bar -->
      <header class="bg-white border-b border-gray-200 sticky top-0 z-30">
        <div class="px-4 py-4">
          <div class="flex items-center justify-between gap-4">
            <!-- Mobile Menu Button -->
            <button
              @click="sidebarOpen = true"
              class="lg:hidden text-[#263238] hover:text-[#435574]"
            >
              <Menu class="h-6 w-6" />
            </button>

            <!-- Logo - visible on desktop only -->
            <div class="hidden lg:block">
              <h2 class="text-[#263238]">Servi Já</h2>
            </div>

            <!-- Spacer -->
            <div class="flex-1" />

            <!-- Notifications Button -->
            <button
              @click="props.onNavigate('notifications')"
              class="relative p-2 text-[#435574] hover:bg-gray-100 rounded-lg transition-colors"
            >
              <Bell class="h-6 w-6" />
              <!-- Notification Badge -->
              <span class="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full" />
            </button>

            <!-- User Menu -->
            <DropdownMenu>
              <DropdownMenuTrigger>
                <button class="flex items-center gap-2 hover:opacity-80 transition-opacity">
                  <Avatar class="h-10 w-10 border-2 border-[#286AD8]">
                    <AvatarImage :src="currentUser.avatar" :alt="currentUser.name" />
                    <AvatarFallback class="bg-[#286AD8] text-white">
                      {{ getInitials(currentUser.name) }}
                    </AvatarFallback>
                  </Avatar>
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" class="w-56">
                <DropdownMenuLabel>Minha Conta</DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem
                  @click="
                    props.onNavigate(
                      currentUser.type === 'contractor'
                        ? 'profile'
                        : 'freelancer-profile-settings'
                    )
                  "
                >
                  <User class="mr-2 h-4 w-4" />
                  <span>Perfil</span>
                </DropdownMenuItem>
                <DropdownMenuItem @click="props.onNavigate('notifications')">
                  <Bell class="mr-2 h-4 w-4" />
                  <span>Notificações</span>
                </DropdownMenuItem>
                <DropdownMenuItem
                  v-if="currentUser.type === 'contractor'"
                  @click="props.onNavigate('contracts')"
                >
                  <Clock class="mr-2 h-4 w-4" />
                  <span>Histórico de Contratações</span>
                </DropdownMenuItem>
                <DropdownMenuItem
                  v-else
                  @click="props.onNavigate('freelancer-dashboard')"
                >
                  <DollarSign class="mr-2 h-4 w-4" />
                  <span>Dashboard</span>
                </DropdownMenuItem>
                <DropdownMenuItem @click="props.onNavigate('settings')">
                  <Settings class="mr-2 h-4 w-4" />
                  <span>Configurações</span>
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem @click="props.onLogout">
                  <LogOut class="mr-2 h-4 w-4" />
                  <span>Sair</span>
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
      </header>

      <!-- Page Content -->
      <main class="p-6">
        <slot />
      </main>
    </div>
  </div>
</template>
<style scoped>
/* Estilo personalizado para a scrollbar da sidebar */
nav::-webkit-scrollbar {
  width: 5px;
}

nav::-webkit-scrollbar-track {
  background: transparent;
}

nav::-webkit-scrollbar-thumb {
  background: #435574;
  border-radius: 10px;
}

nav::-webkit-scrollbar-thumb:hover {
  background: #286AD8;
}

/* Para garantir que a scrollbar não empurre o conteúdo */
nav {
  scrollbar-width: thin;
  scrollbar-color: #435574 transparent;
}
</style>

