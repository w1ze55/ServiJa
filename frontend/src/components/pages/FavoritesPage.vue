<script setup lang="ts">
import { ref, computed } from 'vue';
import { Heart, Search, Star, MapPin, Briefcase, Trash2 } from 'lucide-vue-next';
import Input from '../ui/input.vue';
import Button from '../ui/button.vue';
import Avatar from '../ui/avatar.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import AvatarImage from '../ui/avatar-image.vue';
import Badge from '../ui/badge.vue';
import Card from '../ui/card.vue';
import AlertDialog from '../ui/alert-dialog.vue';
import AlertDialogAction from '../ui/alert-dialog-action.vue';
import AlertDialogCancel from '../ui/alert-dialog-cancel.vue';
import AlertDialogContent from '../ui/alert-dialog-content.vue';
import AlertDialogDescription from '../ui/alert-dialog-description.vue';
import AlertDialogFooter from '../ui/alert-dialog-footer.vue';
import AlertDialogHeader from '../ui/alert-dialog-header.vue';
import AlertDialogTitle from '../ui/alert-dialog-title.vue';

interface Freelancer {
  id: string;
  name: string;
  avatar?: string;
  profession: string;
  category: string;
  location: string;
  rating: number;
  reviewCount: number;
  skills: string[];
  isAvailable: boolean;
}

interface Props {
  onNavigate: (page: string, params?: any) => void;
}

const props = defineProps<Props>();

const searchQuery = ref('');
const freelancerToRemove = ref<Freelancer | null>(null);
const currentPage = ref(1);
const itemsPerPage = 6;

// Mock data - substituir por dados reais da API
const favorites = ref<Freelancer[]>([
  {
    id: '1',
    name: 'João Silva',
    avatar: '',
    profession: 'Eletricista Profissional',
    category: 'Eletricista',
    location: 'São Paulo, SP',
    rating: 4.9,
    reviewCount: 127,
    skills: ['Instalação', 'Manutenção', 'Automação'],
    isAvailable: true,
  },
  {
    id: '2',
    name: 'Maria Santos',
    avatar: '',
    profession: 'Designer Gráfica',
    category: 'Design',
    location: 'Rio de Janeiro, RJ',
    rating: 5.0,
    reviewCount: 89,
    skills: ['Branding', 'UI/UX', 'Ilustração'],
    isAvailable: true,
  },
  {
    id: '3',
    name: 'Pedro Oliveira',
    avatar: '',
    profession: 'Encanador Especializado',
    category: 'Encanamento',
    location: 'São Paulo, SP',
    rating: 4.8,
    reviewCount: 156,
    skills: ['Hidráulica', 'Desentupimento', 'Instalação'],
    isAvailable: false,
  },
  {
    id: '4',
    name: 'Ana Costa',
    avatar: '',
    profession: 'Desenvolvedora Web',
    category: 'Tecnologia',
    location: 'Belo Horizonte, MG',
    rating: 4.9,
    reviewCount: 203,
    skills: ['React', 'Node.js', 'TypeScript'],
    isAvailable: true,
  },
  {
    id: '5',
    name: 'Carlos Mendes',
    avatar: '',
    profession: 'Pintor Residencial',
    category: 'Pintura',
    location: 'Curitiba, PR',
    rating: 4.7,
    reviewCount: 98,
    skills: ['Pintura', 'Textura', 'Acabamento'],
    isAvailable: true,
  },
  {
    id: '6',
    name: 'Juliana Alves',
    avatar: '',
    profession: 'Fotógrafa Profissional',
    category: 'Fotografia',
    location: 'São Paulo, SP',
    rating: 5.0,
    reviewCount: 145,
    skills: ['Eventos', 'Retratos', 'Edição'],
    isAvailable: true,
  },
]);

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
};

// Filtrar favoritos por busca
const filteredFavorites = computed(() =>
  favorites.value.filter(
    (freelancer) =>
      freelancer.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      freelancer.profession.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      freelancer.category.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
);

// Paginação
const totalPages = computed(() => Math.ceil(filteredFavorites.value.length / itemsPerPage));
const startIndex = computed(() => (currentPage.value - 1) * itemsPerPage);
const endIndex = computed(() => startIndex.value + itemsPerPage);
const currentFavorites = computed(() =>
  filteredFavorites.value.slice(startIndex.value, endIndex.value)
);

const handleRemoveFavorite = () => {
  if (freelancerToRemove.value) {
    favorites.value = favorites.value.filter((f) => f.id !== freelancerToRemove.value!.id);
    freelancerToRemove.value = null;
  }
};

const handleViewProfile = (freelancerId: string) => {
  props.onNavigate('freelancer-profile', { id: freelancerId });
};
</script>

<template>
  <div class="max-w-7xl mx-auto space-y-6">
    <!-- Header -->
    <div>
      <h1 class="text-[#263238] mb-2">Meus Favoritos</h1>
      <p class="text-[#435574]">
        Freelancers que você marcou como favoritos ({{ favorites.length }})
      </p>
    </div>

    <!-- Search -->
    <div class="relative max-w-md">
      <Search class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-[#435574]" />
      <Input
        placeholder="Buscar nos favoritos..."
        :model-value="searchQuery"
        @update:model-value="(val) => {
          searchQuery = val as string;
          currentPage = 1;
        }"
        class="pl-10"
      />
    </div>

    <!-- Empty State -->
    <Card v-if="favorites.length === 0" class="p-12 text-center">
      <div class="flex flex-col items-center gap-4">
        <div class="w-16 h-16 rounded-full bg-gray-100 flex items-center justify-center">
          <Heart class="h-8 w-8 text-gray-400" />
        </div>
        <div>
          <h3 class="text-[#263238] mb-2">Nenhum favorito ainda</h3>
          <p class="text-[#435574] max-w-md mx-auto mb-6">
            Quando você marcar freelancers como favoritos, eles aparecerão aqui
            para fácil acesso.
          </p>
          <Button
            @click="props.onNavigate('explore')"
          >
            Explorar Serviços
          </Button>
        </div>
      </div>
    </Card>
    <Card v-else-if="currentFavorites.length === 0" class="p-12 text-center">
      <div class="flex flex-col items-center gap-4">
        <Search class="h-12 w-12 text-gray-400" />
        <div>
          <h3 class="text-[#263238] mb-2">Nenhum resultado encontrado</h3>
          <p class="text-[#435574]">Tente buscar com outros termos</p>
        </div>
      </div>
    </Card>
    <template v-else>
      <!-- Favorites Grid -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <Card
          v-for="freelancer in currentFavorites"
          :key="freelancer.id"
          class="p-6 hover:shadow-lg transition-shadow"
        >
          <!-- Header -->
          <div class="flex items-start justify-between mb-4">
            <div
              @click="handleViewProfile(freelancer.id)"
              class="flex items-center gap-3 cursor-pointer flex-1"
            >
              <Avatar class="h-14 w-14">
                <AvatarImage :src="freelancer.avatar" :alt="freelancer.name" />
                <AvatarFallback class="bg-[#286AD8] text-white">
                  {{ getInitials(freelancer.name) }}
                </AvatarFallback>
              </Avatar>
              <div class="flex-1 min-w-0">
                <h3 class="text-[#263238] truncate">{{ freelancer.name }}</h3>
                <p class="text-sm text-[#435574] truncate">
                  {{ freelancer.profession }}
                </p>
              </div>
            </div>
            <Button
              @click="freelancerToRemove = freelancer"
              variant="ghost"
              size="icon"
              class="text-red-500 hover:text-red-700 hover:bg-red-50 flex-shrink-0"
            >
              <Heart class="h-5 w-5 fill-red-500" />
            </Button>
          </div>

          <!-- Rating -->
          <div class="flex items-center gap-2 mb-3">
            <div class="flex items-center gap-1">
              <Star class="h-4 w-4 fill-yellow-400 text-yellow-400" />
              <span class="text-sm text-[#263238]">
                {{ freelancer.rating.toFixed(1) }}
              </span>
            </div>
            <span class="text-sm text-[#435574]">
              ({{ freelancer.reviewCount }} avaliações)
            </span>
          </div>

          <!-- Location -->
          <div class="flex items-center gap-2 text-sm text-[#435574] mb-3">
            <MapPin class="h-4 w-4" />
            <span>{{ freelancer.location }}</span>
          </div>

          <!-- Skills -->
          <div class="flex flex-wrap gap-2 mb-4">
            <Badge
              v-for="(skill, index) in freelancer.skills.slice(0, 3)"
              :key="index"
              variant="secondary"
              class="text-xs"
            >
              {{ skill }}
            </Badge>
          </div>

          <!-- Availability -->
          <div class="flex items-center justify-between mb-4 pb-4 border-b">
            <Badge
              :class="
                freelancer.isAvailable
                  ? 'bg-green-100 text-green-700 hover:bg-green-100'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-100'
              "
            >
              {{ freelancer.isAvailable ? 'Disponível' : 'Ocupado' }}
            </Badge>
          </div>

          <!-- Actions -->
          <div class="flex gap-2">
            <Button
              @click="handleViewProfile(freelancer.id)"
              class="flex-1"
              size="sm"
            >
              Ver Perfil
            </Button>
            <Button
              @click="
                props.onNavigate('request-quote', {
                  freelancerId: freelancer.id,
                  freelancerName: freelancer.name,
                })
              "
              variant="outline"
              class="flex-1 text-primary border-primary hover:bg-blue-50"
              size="sm"
            >
              Solicitar Orçamento
            </Button>
          </div>
        </Card>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="flex items-center justify-center gap-2">
        <Button
          @click="currentPage = Math.max(1, currentPage - 1)"
          :disabled="currentPage === 1"
          variant="outline"
          size="sm"
        >
          Anterior
        </Button>
        <div class="flex items-center gap-1">
          <Button
            v-for="page in Array.from({ length: totalPages }, (_, i) => i + 1)"
            :key="page"
            @click="currentPage = page"
            :variant="currentPage === page ? 'default' : 'outline'"
            size="sm"
          >
            {{ page }}
          </Button>
        </div>
        <Button
          @click="currentPage = Math.min(totalPages, currentPage + 1)"
          :disabled="currentPage === totalPages"
          variant="outline"
          size="sm"
        >
          Próxima
        </Button>
      </div>
    </template>

    <!-- Remove Confirmation Dialog -->
    <AlertDialog :open="!!freelancerToRemove" @update:open="(val) => freelancerToRemove = val ? freelancerToRemove : null">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Remover dos Favoritos</AlertDialogTitle>
          <AlertDialogDescription>
            Tem certeza que deseja remover <strong>{{ freelancerToRemove?.name }}</strong> da
            sua lista de favoritos? Você poderá adicioná-lo novamente a qualquer momento.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Cancelar</AlertDialogCancel>
          <AlertDialogAction
            @click="handleRemoveFavorite"
            class="bg-destructive hover:bg-destructive/90"
          >
            Sim, Remover
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>

