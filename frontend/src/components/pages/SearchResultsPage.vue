<script setup lang="ts">
import { ref, computed } from 'vue';
import { SlidersHorizontal, Search } from 'lucide-vue-next';
import Button from '../ui/button.vue';
import FreelancerCard from '../freelancer/FreelancerCard.vue';
import AdvancedFilters from '../search/AdvancedFilters.vue';

interface Freelancer {
  id: string;
  name: string;
  city: string;
  state: string;
  rating: number;
  reviewCount: number;
  priceRange: string;
  category: string;
  isAvailable: boolean;
  isFavorite: boolean;
}

interface Filters {
  location: string;
  maxDistance: number;
  minPrice: number;
  maxPrice: number;
  minRating: number;
  availability: string[];
  categories: string[];
  sortBy: string;
}

interface Props {
  searchQuery?: string;
  onNavigate: (page: string, params?: any) => void;
}

const props = defineProps<Props>();

const defaultFilters: Filters = {
  location: '',
  maxDistance: 50,
  minPrice: 0,
  maxPrice: 5000,
  minRating: 0,
  availability: [],
  categories: [],
  sortBy: 'relevance',
};

const showFilters = ref(false);
const filters = ref<Filters>(defaultFilters);
const favoriteFreelancers = ref<string[]>([]);
const currentPage = ref(1);
const resultsPerPage = 12;

// Mock data - substituir por dados reais da API
const allFreelancers: Freelancer[] = [
  {
    id: '1',
    name: 'João Silva',
    city: 'São Paulo',
    state: 'SP',
    rating: 4.9,
    reviewCount: 128,
    priceRange: 'R$ 150 - R$ 300',
    category: 'Eletricista',
    isAvailable: true,
    isFavorite: false,
  },
  {
    id: '2',
    name: 'Maria Santos',
    city: 'Rio de Janeiro',
    state: 'RJ',
    rating: 5.0,
    reviewCount: 95,
    priceRange: 'R$ 200 - R$ 400',
    category: 'Designer Gráfico',
    isAvailable: true,
    isFavorite: false,
  },
  {
    id: '3',
    name: 'Carlos Oliveira',
    city: 'Belo Horizonte',
    state: 'MG',
    rating: 4.8,
    reviewCount: 203,
    priceRange: 'R$ 180 - R$ 350',
    category: 'Encanador',
    isAvailable: false,
    isFavorite: false,
  },
  {
    id: '4',
    name: 'Ana Costa',
    city: 'Curitiba',
    state: 'PR',
    rating: 4.7,
    reviewCount: 87,
    priceRange: 'R$ 250 - R$ 500',
    category: 'Desenvolvedora Web',
    isAvailable: true,
    isFavorite: false,
  },
  {
    id: '5',
    name: 'Pedro Almeida',
    city: 'Salvador',
    state: 'BA',
    rating: 4.9,
    reviewCount: 156,
    priceRange: 'R$ 120 - R$ 250',
    category: 'Pintor',
    isAvailable: true,
    isFavorite: false,
  },
  {
    id: '6',
    name: 'Juliana Pereira',
    city: 'Brasília',
    state: 'DF',
    rating: 5.0,
    reviewCount: 74,
    priceRange: 'R$ 300 - R$ 600',
    category: 'Arquiteta',
    isAvailable: true,
    isFavorite: false,
  },
  {
    id: '7',
    name: 'Rafael Souza',
    city: 'Fortaleza',
    state: 'CE',
    rating: 4.6,
    reviewCount: 112,
    priceRange: 'R$ 100 - R$ 200',
    category: 'Marceneiro',
    isAvailable: true,
    isFavorite: false,
  },
  {
    id: '8',
    name: 'Fernanda Lima',
    city: 'Recife',
    state: 'PE',
    rating: 4.8,
    reviewCount: 98,
    priceRange: 'R$ 180 - R$ 350',
    category: 'Fotógrafa',
    isAvailable: true,
    isFavorite: false,
  },
  {
    id: '9',
    name: 'Lucas Martins',
    city: 'Porto Alegre',
    state: 'RS',
    rating: 4.9,
    reviewCount: 145,
    priceRange: 'R$ 200 - R$ 400',
    category: 'Pedreiro',
    isAvailable: false,
    isFavorite: false,
  },
  {
    id: '10',
    name: 'Camila Rocha',
    city: 'Manaus',
    state: 'AM',
    rating: 5.0,
    reviewCount: 67,
    priceRange: 'R$ 250 - R$ 500',
    category: 'Consultora de Marketing',
    isAvailable: true,
    isFavorite: false,
  },
  {
    id: '11',
    name: 'Bruno Fernandes',
    city: 'Goiânia',
    state: 'GO',
    rating: 4.7,
    reviewCount: 89,
    priceRange: 'R$ 150 - R$ 300',
    category: 'Jardineiro',
    isAvailable: true,
    isFavorite: false,
  },
  {
    id: '12',
    name: 'Patricia Gomes',
    city: 'Belém',
    state: 'PA',
    rating: 4.8,
    reviewCount: 76,
    priceRange: 'R$ 180 - R$ 350',
    category: 'Personal Trainer',
    isAvailable: true,
    isFavorite: false,
  },
];

// Aplicar filtros
const filteredFreelancers = computed(() =>
  allFreelancers.filter((freelancer) => {
    // Filtro de rating
    if (filters.value.minRating > 0 && freelancer.rating < filters.value.minRating) {
      return false;
    }

    // Filtro de disponibilidade
    if (
      filters.value.availability.includes('today') &&
      !freelancer.isAvailable
    ) {
      return false;
    }

    // Filtro de categoria
    if (
      filters.value.categories.length > 0 &&
      !filters.value.categories.includes(freelancer.category)
    ) {
      return false;
    }

    return true;
  })
);

// Ordenar resultados
const sortedFreelancers = computed(() => {
  const filtered = [...filteredFreelancers.value];
  switch (filters.value.sortBy) {
    case 'rating-desc':
      return filtered.sort((a, b) => b.rating - a.rating);
    case 'rating-asc':
      return filtered.sort((a, b) => a.rating - b.rating);
    case 'price-asc':
      return filtered.sort((a, b) => a.priceRange.localeCompare(b.priceRange));
    case 'price-desc':
      return filtered.sort((a, b) => b.priceRange.localeCompare(a.priceRange));
    default: // relevance
      return filtered.sort((a, b) => b.reviewCount - a.reviewCount);
  }
});

// Paginação
const totalPages = computed(() =>
  Math.ceil(sortedFreelancers.value.length / resultsPerPage)
);
const startIndex = computed(() => (currentPage.value - 1) * resultsPerPage);
const endIndex = computed(() => startIndex.value + resultsPerPage);
const currentFreelancers = computed(() =>
  sortedFreelancers.value.slice(startIndex.value, endIndex.value)
);

const handleToggleFavorite = (id: string) => {
  if (favoriteFreelancers.value.includes(id)) {
    favoriteFreelancers.value = favoriteFreelancers.value.filter((fid) => fid !== id);
  } else {
    favoriteFreelancers.value = [...favoriteFreelancers.value, id];
  }
};

const handleApplyFilters = (newFilters: Filters) => {
  filters.value = newFilters;
  currentPage.value = 1;
};

const activeFiltersCount = computed(() => {
  let count = 0;
  if (filters.value.location) count++;
  if (filters.value.minRating > 0) count++;
  if (filters.value.availability.length > 0) count++;
  if (filters.value.categories.length > 0) count++;
  if (filters.value.minPrice > 0 || filters.value.maxPrice < 5000) count++;
  return count;
});
</script>

<template>
  <div class="space-y-6 max-w-7xl mx-auto">
    <!-- Header -->
    <div>
      <h1 class="text-[#263238] mb-2">
        {{ searchQuery ? `Resultados para "${searchQuery}"` : 'Resultados da Busca' }}
      </h1>
      <p class="text-[#435574]">
        {{ sortedFreelancers.length }}
        {{ sortedFreelancers.length === 1 ? 'profissional encontrado' : 'profissionais encontrados' }}
      </p>
    </div>

    <!-- Filters Bar -->
    <div class="bg-white rounded-lg border border-gray-200 p-4">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <Button
            @click="showFilters = true"
            variant="outline"
            class="text-primary border-primary hover:bg-blue-50"
          >
            <SlidersHorizontal class="h-4 w-4 mr-2" />
            Filtros
            <span
              v-if="activeFiltersCount > 0"
              class="ml-2 bg-primary text-white rounded-full w-5 h-5 flex items-center justify-center text-xs"
            >
              {{ activeFiltersCount }}
            </span>
          </Button>

          <Button
            v-if="activeFiltersCount > 0"
            @click="
              filters = defaultFilters;
              currentPage = 1;
            "
            variant="ghost"
            class="text-[#435574] hover:text-[#263238]"
          >
            Limpar filtros
          </Button>
        </div>

        <div class="text-sm text-[#435574]">
          Página {{ currentPage }} de {{ totalPages }}
        </div>
      </div>
    </div>

    <!-- Results Grid -->
    <div v-if="currentFreelancers.length === 0" class="text-center py-12">
      <div class="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
        <Search class="h-8 w-8 text-[#435574]" />
      </div>
      <h3 class="text-[#263238] mb-2">Nenhum resultado encontrado</h3>
      <p class="text-[#435574] mb-4">
        Tente ajustar seus filtros ou fazer uma nova busca
      </p>
      <Button
        @click="
          filters = defaultFilters;
          currentPage = 1;
        "
        variant="outline"
        class="text-primary border-primary"
      >
        Limpar filtros
      </Button>
    </div>
    <template v-else>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <FreelancerCard
          v-for="freelancer in currentFreelancers"
          :key="freelancer.id"
          :freelancer="{
            ...freelancer,
            isFavorite: favoriteFreelancers.includes(freelancer.id),
          }"
          :on-view-profile="(id) => props.onNavigate('freelancer-profile', { id })"
          :on-toggle-favorite="handleToggleFavorite"
        />
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="flex items-center justify-center gap-2 mt-8">
        <Button
          @click="currentPage = Math.max(1, currentPage - 1)"
          :disabled="currentPage === 1"
          variant="outline"
        >
          Anterior
        </Button>

        <Button
          v-for="page in Array.from({ length: totalPages }, (_, i) => i + 1)"
          :key="page"
          @click="currentPage = page"
          :variant="currentPage === page ? 'default' : 'outline'"
        >
          {{ page }}
        </Button>

        <Button
          @click="currentPage = Math.min(totalPages, currentPage + 1)"
          :disabled="currentPage === totalPages"
          variant="outline"
        >
          Próxima
        </Button>
      </div>
    </template>

    <!-- Advanced Filters Modal -->
    <AdvancedFilters
      :is-open="showFilters"
      :on-close="() => showFilters = false"
      :on-apply-filters="handleApplyFilters"
      :current-filters="filters"
    />
  </div>
</template>

