<script setup lang="ts">
import { ref, computed } from 'vue';
import { Star, Search, Sparkles, Calendar } from 'lucide-vue-next';
import Input from '../ui/input.vue';
import Button from '../ui/button.vue';
import Avatar from '../ui/avatar.vue';
import AvatarImage from '../ui/avatar-image.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import Progress from '../ui/progress.vue';
import Select from '../ui/select.vue';
import SelectContent from '../ui/select-content.vue';
import SelectItem from '../ui/select-item.vue';
import SelectTrigger from '../ui/select-trigger.vue';
import SelectValue from '../ui/select-value.vue';

interface Review {
  id: string;
  author: string;
  avatar?: string;
  rating: number;
  date: string;
  comment: string;
  service: string;
}

interface Props {
  freelancerId: string;
  averageRating: number;
  totalReviews: number;
}

const props = defineProps<Props>();

const filterRating = ref<string>('all');
const searchQuery = ref('');
const currentPage = ref(1);
const reviewsPerPage = 5;

// Mock data - substituir por dados reais da API
const aiSummary = {
  generated: '2024-11-11T14:30:00',
  strengths: [
    'Pontualidade exemplar em 95% dos atendimentos',
    'Excelente comunicação e clareza nas explicações técnicas',
    'Trabalho limpo e organizado, sempre deixa o ambiente arrumado',
    'Preços justos e honestos, sem cobranças surpresa',
    'Profundo conhecimento técnico e resolve problemas complexos',
  ],
  summary: 'João Silva é altamente recomendado pelos clientes, destacando-se pela pontualidade, profissionalismo e qualidade técnica. Os clientes frequentemente elogiam sua capacidade de explicar problemas de forma clara e resolver questões complexas com eficiência. A organização e limpeza durante o trabalho também são pontos constantemente mencionados.',
};

const reviews: Review[] = [
  {
    id: '1',
    author: 'Maria Santos',
    rating: 5,
    date: '2024-11-05',
    comment: 'Excelente profissional! Muito pontual, educado e fez um trabalho impecável. A instalação elétrica ficou perfeita e ele explicou tudo com muita paciência. Super recomendo!',
    service: 'Instalação Elétrica Residencial',
  },
  {
    id: '2',
    author: 'Carlos Oliveira',
    rating: 5,
    date: '2024-10-28',
    comment: 'Contratei para instalar ar condicionado e fiquei muito satisfeito. Chegou no horário, trabalhou com muita organização e limpeza. O preço foi justo e o resultado ficou ótimo.',
    service: 'Instalação de Ar Condicionado',
  },
  {
    id: '3',
    author: 'Ana Paula Costa',
    rating: 4,
    date: '2024-10-20',
    comment: 'Muito bom profissional, fez a manutenção preventiva e encontrou alguns problemas que poderiam ter causado curto circuito. Único ponto é que demorou um pouco mais que o esperado.',
    service: 'Manutenção Preventiva',
  },
  {
    id: '4',
    author: 'Roberto Silva',
    rating: 5,
    date: '2024-10-15',
    comment: 'João é excepcional! Resolveu um problema elétrico complexo que outros profissionais não conseguiram identificar. Muito competente e honesto.',
    service: 'Instalação Elétrica Residencial',
  },
  {
    id: '5',
    author: 'Juliana Ferreira',
    rating: 5,
    date: '2024-10-08',
    comment: 'Ótimo atendimento! Instalou ventiladores de teto e ficou perfeito. Muito cuidadoso com os detalhes e deixou tudo limpo após o serviço.',
    service: 'Instalação de Ventiladores de Teto',
  },
  {
    id: '6',
    author: 'Pedro Henrique',
    rating: 5,
    date: '2024-09-30',
    comment: 'Profissional top! Fez a instalação de painéis solares com muito profissionalismo. Explicou todo o funcionamento do sistema e tirou todas as minhas dúvidas.',
    service: 'Instalação Elétrica Residencial',
  },
  {
    id: '7',
    author: 'Fernanda Alves',
    rating: 4,
    date: '2024-09-22',
    comment: 'Serviço bem feito, profissional competente. Trocou os disjuntores e instalou DR. Recomendo!',
    service: 'Troca de Disjuntores',
  },
  {
    id: '8',
    author: 'Marcos Paulo',
    rating: 5,
    date: '2024-09-15',
    comment: 'Excelente! Pontual, educado e muito técnico. Resolveu um problema que tinha há meses. Voltarei a contratar com certeza.',
    service: 'Manutenção Preventiva',
  },
];

// Calcular distribuição de ratings
const ratingDistribution = computed(() => {
  const distribution = { 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 };
  reviews.forEach((review) => {
    distribution[review.rating as keyof typeof distribution]++;
  });
  return distribution;
});

// Filtrar reviews
const filteredReviews = computed(() => {
  let filtered = reviews;

  // Filtro por rating
  if (filterRating.value !== 'all') {
    filtered = filtered.filter((r) => r.rating === parseInt(filterRating.value));
  }

  // Filtro por palavra-chave
  if (searchQuery.value.trim()) {
    filtered = filtered.filter(
      (r) =>
        r.comment.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
        r.service.toLowerCase().includes(searchQuery.value.toLowerCase())
    );
  }

  return filtered;
});

// Paginação
const totalPages = computed(() => Math.ceil(filteredReviews.value.length / reviewsPerPage));
const startIndex = computed(() => (currentPage.value - 1) * reviewsPerPage);
const currentReviews = computed(() =>
  filteredReviews.value.slice(startIndex.value, startIndex.value + reviewsPerPage)
);

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
};

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('pt-BR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
};

const handleSearchChange = (value: string) => {
  searchQuery.value = value;
  currentPage.value = 1;
};

const handleFilterChange = (value: string) => {
  filterRating.value = value;
  currentPage.value = 1;
};

const clearFilters = () => {
  filterRating.value = 'all';
  searchQuery.value = '';
  currentPage.value = 1;
};
</script>

<template>
  <div class="space-y-6">
    <!-- AI Summary -->
    <div class="bg-gradient-to-r from-purple-50 to-blue-50 border border-purple-200 rounded-lg p-6">
      <div class="flex items-start gap-3 mb-4">
        <div class="w-10 h-10 bg-gradient-to-br from-purple-500 to-blue-500 rounded-full flex items-center justify-center flex-shrink-0">
          <Sparkles class="h-5 w-5 text-white" />
        </div>
        <div class="flex-1">
          <h3 class="text-[#263238] mb-1">Resumo Gerado por IA</h3>
          <p class="text-xs text-[#435574] flex items-center gap-1">
            <Calendar class="h-3 w-3" />
            Última atualização:
            {{ new Date(aiSummary.generated).toLocaleDateString('pt-BR', {
              day: '2-digit',
              month: '2-digit',
              year: 'numeric',
              hour: '2-digit',
              minute: '2-digit',
            }) }}
          </p>
        </div>
      </div>

      <p class="text-[#435574] mb-4">{{ aiSummary.summary }}</p>

      <div>
        <h4 class="text-[#263238] mb-2">Principais Pontos Fortes:</h4>
        <ul class="space-y-2">
          <li
            v-for="(strength, index) in aiSummary.strengths"
            :key="index"
            class="flex items-start gap-2 text-[#435574]"
          >
            <Star class="h-4 w-4 fill-yellow-400 text-yellow-400 mt-0.5 flex-shrink-0" />
            <span>{{ strength }}</span>
          </li>
        </ul>
      </div>
    </div>

    <!-- Rating Overview -->
    <div class="bg-white border border-gray-200 rounded-lg p-6">
      <div class="grid md:grid-cols-2 gap-6">
        <!-- Average Rating -->
        <div class="text-center md:text-left">
          <div class="flex items-center justify-center md:justify-start gap-3 mb-2">
            <span class="text-5xl text-[#263238]">{{ props.averageRating.toFixed(1) }}</span>
            <div>
              <div class="flex gap-1 mb-1">
                <Star
                  v-for="i in 5"
                  :key="i"
                  :class="`h-4 w-4 ${
                    i <= Math.round(props.averageRating)
                      ? 'fill-yellow-400 text-yellow-400'
                      : 'text-gray-300'
                  }`"
                />
              </div>
              <p class="text-sm text-[#435574]">{{ props.totalReviews }} avaliações</p>
            </div>
          </div>
        </div>

        <!-- Rating Distribution -->
        <div class="space-y-2">
          <div
            v-for="rating in [5, 4, 3, 2, 1]"
            :key="rating"
            class="flex items-center gap-3"
          >
            <div class="flex items-center gap-1 w-12">
              <span class="text-sm text-[#435574]">{{ rating }}</span>
              <Star class="h-3 w-3 fill-yellow-400 text-yellow-400" />
            </div>
            <Progress
              :value="props.totalReviews > 0 ? (ratingDistribution[rating as keyof typeof ratingDistribution] / props.totalReviews) * 100 : 0"
              class="flex-1 h-2"
            />
            <span class="text-sm text-[#435574] w-8 text-right">
              {{ ratingDistribution[rating as keyof typeof ratingDistribution] }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Filters -->
    <div class="flex flex-col md:flex-row gap-4">
      <div class="flex-1">
        <div class="relative">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-[#435574]" />
          <Input
            type="text"
            placeholder="Buscar por palavra-chave..."
            :model-value="searchQuery"
            @update:model-value="handleSearchChange"
            class="pl-10"
          />
        </div>
      </div>
      <Select :model-value="filterRating" @update:model-value="handleFilterChange">
        <SelectTrigger class="w-full md:w-48">
          <SelectValue placeholder="Filtrar por nota" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="all">Todas as notas</SelectItem>
          <SelectItem value="5">5 estrelas</SelectItem>
          <SelectItem value="4">4 estrelas</SelectItem>
          <SelectItem value="3">3 estrelas</SelectItem>
          <SelectItem value="2">2 estrelas</SelectItem>
          <SelectItem value="1">1 estrela</SelectItem>
        </SelectContent>
      </Select>
    </div>

    <!-- Reviews List -->
    <div class="space-y-4">
      <div
        v-if="currentReviews.length === 0"
        class="text-center py-12 bg-gray-50 rounded-lg border border-gray-200"
      >
        <p class="text-[#435574]">Nenhuma avaliação encontrada com os filtros aplicados</p>
        <Button @click="clearFilters" variant="outline" class="mt-4">
          Limpar filtros
        </Button>
      </div>
      <div
        v-for="review in currentReviews"
        v-else
        :key="review.id"
        class="bg-white border border-gray-200 rounded-lg p-6"
      >
        <div class="flex items-start gap-4">
          <Avatar class="h-12 w-12">
            <AvatarImage :src="review.avatar" :alt="review.author" />
            <AvatarFallback class="bg-[#286AD8] text-white">
              {{ getInitials(review.author) }}
            </AvatarFallback>
          </Avatar>

          <div class="flex-1">
            <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-2 mb-2">
              <div>
                <h4 class="text-[#263238]">{{ review.author }}</h4>
                <p class="text-sm text-[#435574]">{{ formatDate(review.date) }}</p>
              </div>
              <div class="flex gap-1">
                <Star
                  v-for="i in 5"
                  :key="i"
                  :class="`h-4 w-4 ${
                    i <= review.rating ? 'fill-yellow-400 text-yellow-400' : 'text-gray-300'
                  }`"
                />
              </div>
            </div>

            <p class="text-[#435574] mb-3">{{ review.comment }}</p>

            <p class="text-sm text-[#435574]">
              <strong class="text-[#263238]">Serviço:</strong> {{ review.service }}
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="flex items-center justify-center gap-2">
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
        :class="currentPage === page ? 'bg-[#286AD8]' : ''"
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
  </div>
</template>

