<script setup lang="ts">
import { ref, computed } from 'vue';
import { Search, MapPin, Star, Filter, Sparkles } from 'lucide-vue-next';
import Button from './ui/button.vue';
import Input from './ui/input.vue';
import Card from './ui/card.vue';
import Badge from './ui/badge.vue';
import Select from './ui/select.vue';
import SelectContent from './ui/select-content.vue';
import SelectItem from './ui/select-item.vue';
import SelectTrigger from './ui/select-trigger.vue';
import SelectValue from './ui/select-value.vue';

interface Provider {
  id: string;
  name: string;
  avatar: string;
  category: string;
  location: string;
  rating: number;
  reviewCount: number;
  completedJobs: number;
  price: string;
  description: string;
  skills: string[];
  availability: string;
  responseTime: string;
}

interface Category {
  id: string;
  name: string;
  icon: string;
  count: number;
}

interface Props {
  onNavigate: (page: string, providerId?: string) => void;
  user?: any;
}

const props = defineProps<Props>();

const searchQuery = ref('');
const selectedCategory = ref<string>('all');
const sortBy = ref('rating');
const location = ref('');

// Mock data - substituir por dados reais da API
const categories: Category[] = [
  { id: '1', name: 'Eletricista', icon: '⚡', count: 156 },
  { id: '2', name: 'Encanador', icon: '🔧', count: 132 },
  { id: '3', name: 'Pintor', icon: '🎨', count: 98 },
  { id: '4', name: 'Pedreiro', icon: '🧱', count: 87 },
  { id: '5', name: 'Marceneiro', icon: '🪚', count: 65 },
  { id: '6', name: 'Jardineiro', icon: '🌱', count: 54 },
  { id: '7', name: 'Limpeza', icon: '🧹', count: 143 },
  { id: '8', name: 'Designer', icon: '✏️', count: 76 },
];

const providers: Provider[] = [
  {
    id: '1',
    name: 'Carlos Silva',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Carlos',
    category: 'Eletricista',
    location: 'São Paulo, SP',
    rating: 4.9,
    reviewCount: 127,
    completedJobs: 234,
    price: 'R$ 80-150/hora',
    description:
      'Eletricista profissional com mais de 10 anos de experiência em instalações residenciais e comerciais. Especializado em sistemas elétricos, automação e energia solar.',
    skills: ['Instalação elétrica', 'Manutenção preventiva', 'Automação residencial', 'Energia solar', 'Quadros elétricos'],
    availability: 'Seg-Sex: 8h-18h',
    responseTime: '~2 horas',
  },
  {
    id: '2',
    name: 'Fernanda Alves',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Fernanda',
    category: 'Designer',
    location: 'Rio de Janeiro, RJ',
    rating: 5.0,
    reviewCount: 89,
    completedJobs: 156,
    price: 'R$ 120-200/hora',
    description:
      'Designer gráfica especializada em identidade visual, branding e design digital. Trabalho com marcas de todos os portes, do conceito à execução.',
    skills: ['Identidade Visual', 'Branding', 'UI/UX Design', 'Design Digital', 'Ilustração'],
    availability: 'Seg-Sex: 9h-19h',
    responseTime: '~1 hora',
  },
  {
    id: '3',
    name: 'Roberto Pereira',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Roberto',
    category: 'Encanador',
    location: 'São Paulo, SP',
    rating: 4.8,
    reviewCount: 164,
    completedJobs: 312,
    price: 'R$ 70-120/hora',
    description:
      'Encanador experiente com especialização em hidráulica residencial e comercial. Atendo emergências e faço serviços preventivos.',
    skills: ['Instalação hidráulica', 'Desentupimento', 'Detecção de vazamentos', 'Aquecedores', "Caixas d'água"],
    availability: '24h (emergências)',
    responseTime: '~30 min',
  },
  {
    id: '4',
    name: 'Juliana Souza',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Juliana',
    category: 'Pintor',
    location: 'Belo Horizonte, MG',
    rating: 4.9,
    reviewCount: 98,
    completedJobs: 187,
    price: 'R$ 60-100/hora',
    description:
      'Pintora profissional especializada em pintura residencial, comercial e decorativa. Trabalho com acabamento premium.',
    skills: ['Pintura residencial', 'Textura', 'Grafiato', 'Pintura decorativa', 'Verniz'],
    availability: 'Seg-Sab: 8h-17h',
    responseTime: '~3 horas',
  },
  {
    id: '5',
    name: 'André Costa',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Andre',
    category: 'Marceneiro',
    location: 'Curitiba, PR',
    rating: 4.7,
    reviewCount: 76,
    completedJobs: 143,
    price: 'R$ 90-180/hora',
    description:
      'Marceneiro especializado em móveis planejados e marcenaria sob medida. Projetos personalizados do desenho à instalação.',
    skills: ['Móveis planejados', 'Marcenaria sob medida', 'Restauração', 'Design de móveis', 'Instalação'],
    availability: 'Seg-Sex: 8h-18h',
    responseTime: '~4 horas',
  },
  {
    id: '6',
    name: 'Patricia Lima',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Patricia',
    category: 'Limpeza',
    location: 'São Paulo, SP',
    rating: 5.0,
    reviewCount: 203,
    completedJobs: 456,
    price: 'R$ 45-80/hora',
    description:
      'Serviços de limpeza residencial e comercial. Equipe treinada e produtos próprios. Limpeza regular ou pós-obra.',
    skills: ['Limpeza residencial', 'Limpeza comercial', 'Limpeza pós-obra', 'Limpeza profunda', 'Organização'],
    availability: 'Seg-Sab: 7h-19h',
    responseTime: '~1 hora',
  },
];

const filteredProviders = computed(() => {
  const filtered = providers.filter((provider) => {
    const matchesSearch =
      searchQuery.value === '' ||
      provider.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      provider.category.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      provider.skills.some((skill) => skill.toLowerCase().includes(searchQuery.value.toLowerCase()));

    const matchesCategory = selectedCategory.value === 'all' || provider.category === selectedCategory.value;

    return matchesSearch && matchesCategory;
  });

  return filtered.sort((a, b) => {
    if (sortBy.value === 'rating') return b.rating - a.rating;
    if (sortBy.value === 'reviews') return b.reviewCount - a.reviewCount;
    if (sortBy.value === 'jobs') return b.completedJobs - a.completedJobs;
    return 0;
  });
});
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <!-- Search Section -->
    <section class="bg-gradient-to-br from-blue-600 to-purple-600 text-white py-12">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <h1 class="text-white mb-6">Encontre o profissional ideal</h1>

        <div class="bg-white rounded-xl p-2 shadow-2xl flex flex-col sm:flex-row items-center gap-2">
          <div class="flex items-center gap-2 flex-1 w-full">
            <Search class="text-gray-400 ml-2" />
            <Input
              type="text"
              placeholder="Buscar por serviço, profissional ou habilidade..."
              class="border-0 focus-visible:ring-0 flex-1"
              :model-value="searchQuery"
              @update:model-value="(val) => searchQuery = val as string"
            />
          </div>
          <div class="flex items-center gap-2 w-full sm:w-auto">
            <MapPin class="text-gray-400" :size="20" />
            <Input
              type="text"
              placeholder="Localização"
              class="border-0 focus-visible:ring-0 w-full sm:w-48"
              :model-value="location"
              @update:model-value="(val) => location = val as string"
            />
          </div>
          <Button class="bg-blue-600 hover:bg-blue-700 w-full sm:w-auto">Buscar</Button>
        </div>
      </div>
    </section>

    <!-- Categories -->
    <section class="bg-white border-b">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <div class="flex items-center gap-2 mb-4">
          <Filter :size="20" class="text-gray-600" />
          <span class="text-gray-900">Categorias populares</span>
        </div>
        <div class="flex gap-2 overflow-x-auto pb-2">
          <Badge
            :variant="selectedCategory === 'all' ? 'default' : 'outline'"
            class="cursor-pointer whitespace-nowrap"
            @click="selectedCategory = 'all'"
          >
            Todos
          </Badge>
          <Badge
            v-for="cat in categories"
            :key="cat.id"
            :variant="selectedCategory === cat.name ? 'default' : 'outline'"
            class="cursor-pointer whitespace-nowrap"
            @click="selectedCategory = cat.name"
          >
            {{ cat.icon }} {{ cat.name }} ({{ cat.count }})
          </Badge>
        </div>
      </div>
    </section>

    <!-- Results -->
    <section class="py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between mb-6">
          <div>
            <h2 class="text-gray-900">{{ filteredProviders.length }} profissionais encontrados</h2>
            <p class="text-gray-600">
              {{ selectedCategory !== 'all' ? `Categoria: ${selectedCategory}` : 'Todas as categorias' }}
            </p>
          </div>

          <Select :model-value="sortBy" @update:model-value="(val: string) => sortBy = val">
            <SelectTrigger class="w-48">
              <SelectValue placeholder="Ordenar por" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="rating">Melhor avaliação</SelectItem>
              <SelectItem value="reviews">Mais avaliações</SelectItem>
              <SelectItem value="jobs">Mais trabalhos</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <!-- AI Suggestion Banner -->
        <Card class="p-4 mb-6 bg-gradient-to-r from-purple-50 to-blue-50 border-purple-200">
          <div class="flex items-start gap-3">
            <div class="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center flex-shrink-0">
              <Sparkles class="text-purple-600" :size="20" />
            </div>
            <div>
              <h3 class="text-gray-900 mb-1">Sugestão Inteligente</h3>
              <p class="text-gray-600">
                Com base na sua busca, recomendamos profissionais com alta taxa de satisfação
                e experiência comprovada na categoria selecionada.
              </p>
            </div>
          </div>
        </Card>

        <!-- Provider Cards -->
        <div class="grid gap-6">
          <Card
            v-for="provider in filteredProviders"
            :key="provider.id"
            class="p-6 hover:shadow-lg transition-shadow cursor-pointer"
            @click="props.onNavigate('provider-profile', provider.id)"
          >
            <div class="flex flex-col md:flex-row gap-6">
              <!-- Avatar -->
              <div class="flex-shrink-0">
                <img :src="provider.avatar" :alt="provider.name" class="w-24 h-24 rounded-lg object-cover" />
              </div>

              <!-- Info -->
              <div class="flex-1 min-w-0">
                <div class="flex items-start justify-between gap-4 mb-2">
                  <div>
                    <h3 class="text-gray-900 mb-1">{{ provider.name }}</h3>
                    <p class="text-gray-600">{{ provider.category }}</p>
                  </div>
                  <div class="text-right">
                    <div class="flex items-center gap-1 mb-1">
                      <Star class="text-yellow-500 fill-yellow-500" :size="20" />
                      <span class="text-gray-900">{{ provider.rating.toFixed(1) }}</span>
                    </div>
                    <p class="text-gray-600">({{ provider.reviewCount }} avaliações)</p>
                  </div>
                </div>

                <div class="flex items-center gap-4 text-gray-600 mb-3">
                  <span class="flex items-center gap-1">
                    <MapPin :size="16" />
                    {{ provider.location }}
                  </span>
                  <span>•</span>
                  <span>{{ provider.completedJobs }} trabalhos</span>
                  <span>•</span>
                  <span>{{ provider.price }}</span>
                </div>

                <p class="text-gray-700 mb-4 line-clamp-2">{{ provider.description }}</p>

                <div class="flex flex-wrap gap-2 mb-4">
                  <Badge v-for="(skill, idx) in provider.skills.slice(0, 5)" :key="idx" variant="secondary">
                    {{ skill }}
                  </Badge>
                </div>

                <div class="flex items-center justify-between">
                  <div class="flex items-center gap-4 text-gray-600">
                    <span>⚡ Responde em {{ provider.responseTime }}</span>
                    <span>📅 {{ provider.availability }}</span>
                  </div>
                  <Button>Ver perfil</Button>
                </div>
              </div>
            </div>
          </Card>
        </div>

        <div v-if="filteredProviders.length === 0" class="text-center py-12">
          <div class="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <Search class="text-gray-400" :size="32" />
          </div>
          <h3 class="text-gray-900 mb-2">Nenhum profissional encontrado</h3>
          <p class="text-gray-600">Tente ajustar os filtros ou buscar por outro termo</p>
        </div>
      </div>
    </section>
  </div>
</template>




