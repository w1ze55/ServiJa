<script setup lang="ts">
import { ref } from 'vue';
import { ChevronRight, TrendingUp, Star } from 'lucide-vue-next';
import Button from '../ui/button.vue';
import FreelancerCard from '../freelancer/FreelancerCard.vue';
import SearchWithAutocomplete from '../search/SearchWithAutocomplete.vue';

interface Category {
  id: string;
  name: string;
  icon: string;
  count: number;
}

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

interface Props {
  onNavigate: (page: string, params?: any) => void;
  onSearch: (query: string) => void;
}

const props = defineProps<Props>();

const favoriteFreelancers = ref<string[]>([]);

// Mock data - substituir por dados reais da API
const categories: Category[] = [
  { id: '1', name: 'Construção', icon: '🏗️', count: 245 },
  { id: '2', name: 'Design', icon: '🎨', count: 189 },
  { id: '3', name: 'Tecnologia', icon: '💻', count: 312 },
  { id: '4', name: 'Consultoria', icon: '📊', count: 156 },
  { id: '5', name: 'Marketing', icon: '📱', count: 198 },
  { id: '6', name: 'Educação', icon: '📚', count: 134 },
  { id: '7', name: 'Saúde', icon: '⚕️', count: 87 },
  { id: '8', name: 'Eventos', icon: '🎉', count: 76 },
];

const featuredFreelancers: Freelancer[] = [
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
];

const handleToggleFavorite = (id: string) => {
  if (favoriteFreelancers.value.includes(id)) {
    favoriteFreelancers.value = favoriteFreelancers.value.filter((fid) => fid !== id);
  } else {
    favoriteFreelancers.value = [...favoriteFreelancers.value, id];
  }
};
</script>

<template>
  <div class="space-y-8 max-w-7xl mx-auto">
    <!-- Hero Section -->
    <div class="bg-gradient-to-r from-[#286AD8] via-[#435574] to-[#263238] rounded-2xl p-8 md:p-12 text-white">
      <div class="max-w-3xl">
        <h1 class="text-white mb-4">
          Encontre o profissional perfeito para o seu projeto
        </h1>
        <p class="text-white/90 mb-6">
          Conecte-se com freelancers qualificados na sua região. Rápido, fácil e confiável.
        </p>

        <!-- Search Bar -->
        <div class="bg-white rounded-lg p-2">
          <SearchWithAutocomplete
            :on-search="props.onSearch"
            placeholder="O que você está procurando?"
          />
        </div>

        <!-- Quick Stats -->
        <div class="grid grid-cols-3 gap-4 mt-8">
          <div class="text-center">
            <p class="text-white/90 mb-1">Freelancers</p>
            <p class="text-white">12.500+</p>
          </div>
          <div class="text-center">
            <p class="text-white/90 mb-1">Projetos</p>
            <p class="text-white">45.200+</p>
          </div>
          <div class="text-center">
            <p class="text-white/90 mb-1">Avaliações</p>
            <p class="text-white">
              98%
              <Star class="inline h-4 w-4 fill-yellow-400 text-yellow-400" />
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- Categories Section -->
    <section>
      <div class="flex items-center justify-between mb-6">
        <div>
          <h2 class="text-[#263238] mb-2">Explore por Categoria</h2>
          <p class="text-[#435574]">Descubra profissionais especializados</p>
        </div>
        <Button
          variant="ghost"
          @click="props.onNavigate('explore')"
          class="text-[#286AD8] hover:text-[#435574]"
        >
          Ver todas
          <ChevronRight class="ml-1 h-4 w-4" />
        </Button>
      </div>

      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div
          v-for="category in categories.slice(0, 8)"
          :key="category.id"
          @click="props.onNavigate('search-results', { category: category.name })"
          class="bg-white border border-gray-200 rounded-lg p-6 hover:border-[#286AD8] hover:shadow-lg transition-all text-center group cursor-pointer"
        >
          <div class="text-4xl mb-3">{{ category.icon }}</div>
          <h4 class="text-[#263238] group-hover:text-[#286AD8] transition-colors mb-1">
            {{ category.name }}
          </h4>
          <p class="text-sm text-[#435574]">{{ category.count }} profissionais</p>
        </div>
      </div>
    </section>

    <!-- Featured Freelancers -->
    <section>
      <div class="flex items-center justify-between mb-6">
        <div>
          <div class="flex items-center gap-2 mb-2">
            <TrendingUp class="h-5 w-5 text-[#286AD8]" />
            <h2 class="text-[#263238]">Freelancers em Destaque</h2>
          </div>
          <p class="text-[#435574]">Profissionais mais bem avaliados na sua região</p>
        </div>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <FreelancerCard
          v-for="freelancer in featuredFreelancers"
          :key="freelancer.id"
          :freelancer="{
            ...freelancer,
            isFavorite: favoriteFreelancers.includes(freelancer.id),
          }"
          :on-view-profile="(id) => props.onNavigate('freelancer-profile', { id })"
          :on-toggle-favorite="handleToggleFavorite"
        />
      </div>

      <div class="text-center mt-8">
        <Button
          @click="props.onNavigate('search-results')"
          variant="outline"
          size="lg"
          class="border-[#286AD8] text-[#286AD8] hover:bg-blue-50"
        >
          Ver todos os profissionais
          <ChevronRight class="ml-1 h-4 w-4" />
        </Button>
      </div>
    </section>

    <!-- How It Works -->
    <section class="bg-white rounded-2xl p-8 border border-gray-200">
      <h2 class="text-[#263238] text-center mb-8">Como Funciona</h2>
      <div class="grid md:grid-cols-3 gap-8">
        <div class="text-center">
          <div class="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <span class="text-[#286AD8] text-2xl">🔍</span>
          </div>
          <h4 class="text-[#263238] mb-2">1. Busque</h4>
          <p class="text-[#435574]">
            Encontre o profissional ideal usando filtros de localização, preço e avaliação
          </p>
        </div>
        <div class="text-center">
          <div class="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <span class="text-[#286AD8] text-2xl">💬</span>
          </div>
          <h4 class="text-[#263238] mb-2">2. Conecte</h4>
          <p class="text-[#435574]">
            Converse diretamente com o freelancer e solicite orçamentos personalizados
          </p>
        </div>
        <div class="text-center">
          <div class="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <span class="text-[#286AD8] text-2xl">✅</span>
          </div>
          <h4 class="text-[#263238] mb-2">3. Contrate</h4>
          <p class="text-[#435574]">
            Finalize o serviço e avalie sua experiência para ajudar outros usuários
          </p>
        </div>
      </div>
    </section>
  </div>
</template>

