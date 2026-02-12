<script setup lang="ts">
import { ref, computed } from 'vue';
import { Heart, Grid3x3, List, Search } from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Badge from '../ui/badge.vue';
import Select from '../ui/select.vue';
import SelectContent from '../ui/select-content.vue';
import SelectItem from '../ui/select-item.vue';
import SelectTrigger from '../ui/select-trigger.vue';
import SelectValue from '../ui/select-value.vue';

interface Category {
  id: string;
  name: string;
  icon: string;
  description: string;
  freelancerCount: number;
  isFavorite: boolean;
}

interface Props {
  onNavigate: (page: string, params?: any) => void;
}

const props = defineProps<Props>();

const viewMode = ref<'grid' | 'list'>('grid');
const searchQuery = ref('');
const sortBy = ref('popular');
const categories = ref<Category[]>([
  {
    id: '1',
    name: 'Construção Civil',
    icon: '🏗️',
    description: 'Pedreiros, encanadores, eletricistas e mais',
    freelancerCount: 245,
    isFavorite: false,
  },
  {
    id: '2',
    name: 'Design Gráfico',
    icon: '🎨',
    description: 'Criação de logos, identidade visual e materiais gráficos',
    freelancerCount: 189,
    isFavorite: false,
  },
  {
    id: '3',
    name: 'Desenvolvimento Web',
    icon: '💻',
    description: 'Sites, aplicativos e sistemas personalizados',
    freelancerCount: 312,
    isFavorite: true,
  },
  {
    id: '4',
    name: 'Consultoria Empresarial',
    icon: '📊',
    description: 'Estratégia, finanças e gestão de negócios',
    freelancerCount: 156,
    isFavorite: false,
  },
  {
    id: '5',
    name: 'Marketing Digital',
    icon: '📱',
    description: 'Mídias sociais, SEO, anúncios e conteúdo',
    freelancerCount: 198,
    isFavorite: false,
  },
  {
    id: '6',
    name: 'Educação',
    icon: '📚',
    description: 'Aulas particulares, cursos e treinamentos',
    freelancerCount: 134,
    isFavorite: false,
  },
  {
    id: '7',
    name: 'Saúde e Bem-estar',
    icon: '⚕️',
    description: 'Personal trainer, nutrição e terapias',
    freelancerCount: 87,
    isFavorite: false,
  },
  {
    id: '8',
    name: 'Eventos',
    icon: '🎉',
    description: 'Fotografia, decoração e organização de eventos',
    freelancerCount: 76,
    isFavorite: true,
  },
  {
    id: '9',
    name: 'Beleza',
    icon: '💅',
    description: 'Cabeleireiro, manicure, maquiagem e estética',
    freelancerCount: 165,
    isFavorite: false,
  },
  {
    id: '10',
    name: 'Automóveis',
    icon: '🚗',
    description: 'Mecânica, funilaria e estética automotiva',
    freelancerCount: 98,
    isFavorite: false,
  },
  {
    id: '11',
    name: 'Limpeza',
    icon: '🧹',
    description: 'Limpeza residencial, comercial e pós-obra',
    freelancerCount: 142,
    isFavorite: false,
  },
  {
    id: '12',
    name: 'Jardinagem',
    icon: '🌿',
    description: 'Paisagismo, manutenção e cuidados com plantas',
    freelancerCount: 67,
    isFavorite: false,
  },
]);

const handleToggleFavorite = (id: string) => {
  categories.value = categories.value.map((cat) =>
    cat.id === id ? { ...cat, isFavorite: !cat.isFavorite } : cat
  );
};

const filteredCategories = computed(() =>
  categories.value.filter((cat) =>
    cat.name.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
);

const sortedCategories = computed(() => {
  const filtered = [...filteredCategories.value];
  switch (sortBy.value) {
    case 'name-asc':
      return filtered.sort((a, b) => a.name.localeCompare(b.name));
    case 'name-desc':
      return filtered.sort((a, b) => b.name.localeCompare(a.name));
    case 'freelancers-desc':
      return filtered.sort((a, b) => b.freelancerCount - a.freelancerCount);
    case 'freelancers-asc':
      return filtered.sort((a, b) => a.freelancerCount - b.freelancerCount);
    default: // popular
      return filtered.sort((a, b) => b.freelancerCount - a.freelancerCount);
  }
});
</script>

<template>
  <div class="space-y-6 max-w-7xl mx-auto">
    <!-- Header -->
    <div>
      <h1 class="text-[#263238] mb-2">Explorar Serviços</h1>
      <p class="text-[#435574]">
        Encontre profissionais especializados em diversas categorias
      </p>
    </div>

    <!-- Filters and View Controls -->
    <div class="bg-white rounded-lg border border-gray-200 p-4">
      <div class="flex flex-col md:flex-row gap-4 justify-between">
        <!-- Sort -->
        <div class="w-full md:w-64">
          <Select :model-value="sortBy" @update:model-value="(val) => sortBy = val as string">
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="popular">Mais populares</SelectItem>
              <SelectItem value="name-asc">Nome (A-Z)</SelectItem>
              <SelectItem value="name-desc">Nome (Z-A)</SelectItem>
              <SelectItem value="freelancers-desc">Mais freelancers</SelectItem>
              <SelectItem value="freelancers-asc">Menos freelancers</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <!-- View Mode -->
        <div class="flex gap-2">
          <Button
            :variant="viewMode === 'grid' ? 'default' : 'outline'"
            size="icon"
            @click="viewMode = 'grid'"
            :class="viewMode === 'grid' ? 'bg-[#286AD8]' : ''"
          >
            <Grid3x3 class="h-4 w-4" />
          </Button>
          <Button
            :variant="viewMode === 'list' ? 'default' : 'outline'"
            size="icon"
            @click="viewMode = 'list'"
            :class="viewMode === 'list' ? 'bg-[#286AD8]' : ''"
          >
            <List class="h-4 w-4" />
          </Button>
        </div>
      </div>
    </div>

    <!-- Results Count -->
    <div class="flex items-center justify-between">
      <p class="text-[#435574]">
        {{ sortedCategories.length }}
        {{ sortedCategories.length === 1 ? 'categoria' : 'categorias' }}
        <span v-if="searchQuery">encontrada(s)</span>
      </p>
      <Button
        variant="ghost"
        @click="props.onNavigate('favorites')"
        class="text-[#286AD8] hover:text-[#435574]"
      >
        <Heart class="h-4 w-4 mr-2 fill-red-500 text-red-500" />
        Ver favoritos
      </Button>
    </div>

    <!-- Categories Grid/List -->
    <div v-if="sortedCategories.length === 0" class="text-center py-12">
      <div class="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
        <Search class="h-8 w-8 text-[#435574]" />
      </div>
      <h3 class="text-[#263238] mb-2">Nenhuma categoria encontrada</h3>
      <p class="text-[#435574] mb-4">
        Tente ajustar sua busca ou explore outras opções
      </p>
      <Button @click="searchQuery = ''" variant="outline" class="border-[#286AD8] text-[#286AD8]">
        Limpar busca
      </Button>
    </div>
    <div v-else-if="viewMode === 'grid'" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div
        v-for="category in sortedCategories"
        :key="category.id"
        class="bg-white border border-gray-200 rounded-lg p-6 hover:border-[#286AD8] hover:shadow-lg transition-all group relative cursor-pointer"
        @click="props.onNavigate('search-results', { category: category.id })"
      >
        <div
          @click.stop="handleToggleFavorite(category.id)"
          :class="`absolute top-4 right-4 p-2 rounded-full transition-all cursor-pointer ${
            category.isFavorite
              ? 'text-red-500 bg-red-50'
              : 'text-gray-400 hover:text-red-500 hover:bg-red-50'
          }`"
        >
          <Heart :class="`h-4 w-4 ${category.isFavorite ? 'fill-current' : ''}`" />
        </div>

        <div class="text-5xl mb-4">{{ category.icon }}</div>
        <h3 class="text-[#263238] group-hover:text-[#286AD8] transition-colors mb-2">
          {{ category.name }}
        </h3>
        <p class="text-sm text-[#435574] mb-4">{{ category.description }}</p>
        <Badge class="bg-blue-100 text-[#286AD8] hover:bg-blue-100">
          {{ category.freelancerCount }} profissionais
        </Badge>
      </div>
    </div>
    <div v-else class="space-y-4">
      <div
        v-for="category in sortedCategories"
        :key="category.id"
        class="w-full bg-white border border-gray-200 rounded-lg p-6 hover:border-[#286AD8] hover:shadow-lg transition-all group cursor-pointer"
        @click="props.onNavigate('search-results', { category: category.id })"
      >
        <div class="flex items-start gap-4">
          <div class="text-4xl">{{ category.icon }}</div>
          <div class="flex-1 min-w-0">
            <div class="flex items-start justify-between gap-4 mb-2">
              <h3 class="text-[#263238] group-hover:text-[#286AD8] transition-colors">
                {{ category.name }}
              </h3>
              <div
                @click.stop="handleToggleFavorite(category.id)"
                :class="`p-2 rounded-full transition-all flex-shrink-0 cursor-pointer ${
                  category.isFavorite
                    ? 'text-red-500 bg-red-50'
                    : 'text-gray-400 hover:text-red-500 hover:bg-red-50'
                }`"
              >
                <Heart :class="`h-4 w-4 ${category.isFavorite ? 'fill-current' : ''}`" />
              </div>
            </div>
            <p class="text-[#435574] mb-3">{{ category.description }}</p>
            <Badge class="bg-blue-100 text-[#286AD8] hover:bg-blue-100">
              {{ category.freelancerCount }} profissionais disponíveis
            </Badge>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

