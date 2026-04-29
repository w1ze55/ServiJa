<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { Star, Search } from 'lucide-vue-next';
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
import reviewService, { type ReviewResponse } from '@/api/services/reviewService';
import AiReviewSummary from './AiReviewSummary.vue';

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
const isLoading = ref(false);

const reviews = ref<Review[]>([]);

const loadReviews = async () => {
  if (!props.freelancerId) return;
  isLoading.value = true;
  try {
    const data = await reviewService.getReviewsForUser(props.freelancerId);
    reviews.value = data.map((r: ReviewResponse) => ({
      id: String(r.id),
      author: r.reviewerName,
      avatar: r.reviewerAvatar,
      rating: r.rating,
      date: r.createdAt,
      comment: r.comment || '',
      service: r.serviceName,
    }));
  } catch (err) {
    console.error('Erro ao carregar avaliações:', err);
    reviews.value = [];
  } finally {
    isLoading.value = false;
  }
};

onMounted(() => {
  loadReviews();
});

watch(() => props.freelancerId, () => {
  loadReviews();
});

// Calcular distribuição de ratings
const ratingDistribution = computed(() => {
  const distribution = { 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 };
  reviews.value.forEach((review) => {
    distribution[review.rating as keyof typeof distribution]++;
  });
  return distribution;
});

// Filtrar reviews
const filteredReviews = computed(() => {
  let filtered = reviews.value;

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
    <!-- Loading -->
    <div v-if="isLoading" class="flex justify-center py-12">
      <div class="animate-spin h-8 w-8 border-4 border-[#286AD8] border-t-transparent rounded-full"></div>
    </div>

    <template v-else>
    <!-- Empty State -->
    <div v-if="reviews.length === 0" class="text-center py-12 bg-gray-50 rounded-lg border border-gray-200">
      <Star class="h-12 w-12 text-gray-300 mx-auto mb-4" />
      <p class="text-[#435574]">Nenhuma avaliação recebida ainda</p>
    </div>

    <template v-else>
    <!-- AI Summary -->
    <AiReviewSummary :freelancer-id="props.freelancerId" />

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
    </template>
  </div>
</template>

