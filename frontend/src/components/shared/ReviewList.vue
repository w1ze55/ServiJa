<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { Star, MessageCircle, Award, Loader2, User, ChevronLeft, ChevronRight } from 'lucide-vue-next';
import Card from '@/components/ui/card.vue';
import Badge from '@/components/ui/badge.vue';
import Avatar from '@/components/ui/avatar.vue';
import AvatarImage from '@/components/ui/avatar-image.vue';
import AvatarFallback from '@/components/ui/avatar-fallback.vue';
import Button from '@/components/ui/button.vue';

export interface ReviewItem {
  id: number;
  rating: number;
  comment: string;
  clientName: string;
  clientPhoto?: string;
  createdAt: string;
}

interface Props {
  reviews: ReviewItem[];
  isLoading?: boolean;
  pageSize?: number;
}

const props = withDefaults(defineProps<Props>(), {
  isLoading: false,
  pageSize: 5,
});

const currentPage = ref(1);

watch(() => props.reviews, () => {
  currentPage.value = 1;
});

const totalPages = computed(() => Math.ceil(props.reviews.length / props.pageSize));

const paginatedReviews = computed(() => {
  const start = (currentPage.value - 1) * props.pageSize;
  const end = start + props.pageSize;
  return props.reviews.slice(start, end);
});

const goToPage = (page: number) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page;
  }
};

const visiblePages = computed(() => {
  const pages: number[] = [];
  const total = totalPages.value;
  const current = currentPage.value;
  
  if (total <= 5) {
    for (let i = 1; i <= total; i++) pages.push(i);
  } else {
    if (current <= 3) {
      pages.push(1, 2, 3, 4, 5);
    } else if (current >= total - 2) {
      pages.push(total - 4, total - 3, total - 2, total - 1, total);
    } else {
      pages.push(current - 2, current - 1, current, current + 1, current + 2);
    }
  }
  
  return pages;
});

const averageRating = computed(() => {
  if (!props.reviews.length) return null;
  return props.reviews.reduce((sum, r) => sum + r.rating, 0) / props.reviews.length;
});

const ratingCounts = computed(() => {
  const counts: Record<number, number> = { 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 };
  props.reviews.forEach((r) => {
    counts[r.rating] = (counts[r.rating] || 0) + 1;
  });
  return counts;
});

const formatDate = (dateStr: string) =>
  new Date(dateStr).toLocaleDateString('pt-BR', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
</script>

<template>
  <div>
    <div class="flex items-center gap-3 mb-5">
      <MessageCircle class="h-6 w-6 text-[#286AD8]" />
      <h2 class="text-lg font-bold text-[#263238]">Avaliações</h2>
      <Badge v-if="reviews.length > 0" class="bg-blue-50 text-[#286AD8] border-none">
        {{ reviews.length }}
      </Badge>
    </div>

    <!-- Loading reviews -->
    <div v-if="isLoading" class="flex items-center gap-3 text-[#435574] py-6 justify-center">
      <Loader2 class="h-5 w-5 animate-spin text-[#286AD8]" />
      <span>Carregando avaliações...</span>
    </div>

    <!-- Rating summary bar -->
    <div v-else-if="reviews.length > 0">
      <Card class="p-6 mb-6">
        <div class="flex flex-col sm:flex-row items-center gap-8">
          <!-- Big average -->
          <div class="text-center shrink-0">
            <p class="text-6xl font-black text-[#263238]">{{ averageRating?.toFixed(1) }}</p>
            <div class="flex items-center justify-center gap-1 my-2">
              <Star
                v-for="s in 5"
                :key="s"
                class="h-5 w-5"
                :class="s <= Math.round(averageRating || 0) ? 'fill-yellow-400 text-yellow-400' : 'text-gray-200'"
              />
            </div>
            <p class="text-sm text-[#435574]">{{ reviews.length }} avaliações</p>
          </div>

          <!-- Rating bars -->
          <div class="flex-1 w-full space-y-2">
            <div v-for="s in [5, 4, 3, 2, 1]" :key="s" class="flex items-center gap-3">
              <span class="text-xs text-[#435574] w-3 shrink-0">{{ s }}</span>
              <Star class="h-3 w-3 fill-yellow-400 text-yellow-400 shrink-0" />
              <div class="flex-1 h-2 bg-gray-100 rounded-full overflow-hidden">
                <div
                  class="h-full bg-yellow-400 rounded-full transition-all duration-700"
                  :style="{ width: reviews.length ? `${(ratingCounts[s] / reviews.length) * 100}%` : '0%' }"
                />
              </div>
              <span class="text-xs text-gray-400 w-4 shrink-0">{{ ratingCounts[s] }}</span>
            </div>
          </div>
        </div>
      </Card>

      <!-- Individual reviews -->
      <div class="space-y-4">
        <Card v-for="review in paginatedReviews" :key="review.id" class="p-5">
          <div class="flex items-start gap-4">
            <Avatar class="h-10 w-10 shrink-0">
              <AvatarImage :src="review.clientPhoto" />
              <AvatarFallback class="bg-[#286AD8]/10 text-[#286AD8] text-sm">
                <User class="h-5 w-5" />
              </AvatarFallback>
            </Avatar>
            <div class="flex-1 min-w-0">
              <div class="flex items-center justify-between gap-4 flex-wrap">
                <p class="font-semibold text-[#263238]">{{ review.clientName }}</p>
                <p class="text-xs text-gray-400">{{ formatDate(review.createdAt) }}</p>
              </div>
              <div class="flex items-center gap-1 mt-1 mb-3">
                <Star
                  v-for="s in 5"
                  :key="s"
                  class="h-3.5 w-3.5"
                  :class="s <= review.rating ? 'fill-yellow-400 text-yellow-400' : 'text-gray-200'"
                />
              </div>
              <p class="text-[#435574] text-sm leading-relaxed">{{ review.comment }}</p>
            </div>
          </div>
        </Card>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="flex items-center justify-center gap-2 mt-6">
        <Button
          variant="outline"
          size="icon"
          :disabled="currentPage === 1"
          @click="goToPage(currentPage - 1)"
          class="h-9 w-9"
        >
          <ChevronLeft class="h-4 w-4" />
        </Button>

        <template v-for="page in visiblePages" :key="page">
          <Button
            :variant="currentPage === page ? 'default' : 'outline'"
            size="sm"
            @click="goToPage(page)"
            :class="[
              'h-9 w-9 p-0',
              currentPage === page ? 'bg-[#286AD8] hover:bg-[#286AD8]/90' : 'hover:bg-gray-100'
            ]"
          >
            {{ page }}
          </Button>
        </template>

        <Button
          variant="outline"
          size="icon"
          :disabled="currentPage === totalPages"
          @click="goToPage(currentPage + 1)"
          class="h-9 w-9"
        >
          <ChevronRight class="h-4 w-4" />
        </Button>
      </div>

      <!-- Page info -->
      <p v-if="totalPages > 1" class="text-center text-xs text-gray-400 mt-3">
        Mostrando {{ (currentPage - 1) * pageSize + 1 }}-{{ Math.min(currentPage * pageSize, reviews.length) }} de {{ reviews.length }} avaliações
      </p>
    </div>

    <!-- Empty reviews -->
    <Card v-else class="p-10 text-center">
      <Award class="h-10 w-10 text-gray-200 mx-auto mb-3" />
      <p class="font-medium text-[#263238]">Ainda sem avaliações</p>
      <p class="text-sm text-[#435574] mt-1">Este serviço ainda não foi avaliado. Seja o primeiro!</p>
    </Card>
  </div>
</template>
