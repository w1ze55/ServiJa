<script setup lang="ts">
import { ref, computed } from 'vue';
import { Star, MapPin, Heart } from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Avatar from '../ui/avatar.vue';
import AvatarImage from '../ui/avatar-image.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import Badge from '../ui/badge.vue';

export interface Freelancer {
  id: string;
  name: string;
  avatar?: string;
  city: string;
  state: string;
  rating: number;
  reviewCount: number;
  priceRange: string;
  category: string;
  isAvailable: boolean;
  isFavorite?: boolean;
}

interface Props {
  freelancer: Freelancer;
  onViewProfile: (id: string) => void;
  onToggleFavorite: (id: string) => void;
}

const props = defineProps<Props>();

const isFavorite = ref(props.freelancer.isFavorite || false);

const handleToggleFavorite = (e: MouseEvent) => {
  e.stopPropagation();
  isFavorite.value = !isFavorite.value;
  props.onToggleFavorite(props.freelancer.id);
};

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
};

const handleCardClick = () => {
  props.onViewProfile(props.freelancer.id);
};

const handleButtonClick = (e: MouseEvent) => {
  e.stopPropagation();
  props.onViewProfile(props.freelancer.id);
};
</script>

<template>
  <div
    class="bg-white rounded-lg border border-gray-200 hover:border-[#286AD8] transition-all hover:shadow-lg cursor-pointer group"
    @click="handleCardClick"
  >
    <div class="p-4">
      <!-- Header -->
      <div class="flex items-start gap-3 mb-3">
        <Avatar class="h-12 w-12 border-2 border-gray-100">
          <AvatarImage :src="freelancer.avatar" :alt="freelancer.name" />
          <AvatarFallback class="bg-[#286AD8] text-white">
            {{ getInitials(freelancer.name) }}
          </AvatarFallback>
        </Avatar>

        <div class="flex-1 min-w-0">
          <h4 class="text-[#263238] truncate group-hover:text-[#286AD8] transition-colors">
            {{ freelancer.name }}
          </h4>
          <div class="flex items-center gap-1 text-[#435574] mt-1">
            <MapPin class="h-3 w-3" />
            <span class="text-sm truncate">
              {{ freelancer.city }}, {{ freelancer.state }}
            </span>
          </div>
        </div>

        <button
          @click="handleToggleFavorite"
          :class="`p-2 rounded-full transition-all ${
            isFavorite
              ? 'text-red-500 bg-red-50'
              : 'text-gray-400 hover:text-red-500 hover:bg-red-50'
          }`"
        >
          <Heart :class="`h-4 w-4 ${isFavorite ? 'fill-current' : ''}`" />
        </button>
      </div>

      <!-- Category -->
      <Badge variant="secondary" class="mb-3 bg-gray-100 text-[#435574] hover:bg-gray-200">
        {{ freelancer.category }}
      </Badge>

      <!-- Rating -->
      <div class="flex items-center gap-2 mb-3">
        <div class="flex items-center gap-1">
          <Star class="h-4 w-4 fill-yellow-400 text-yellow-400" />
          <span class="text-[#263238]">{{ freelancer.rating.toFixed(1) }}</span>
        </div>
        <span class="text-sm text-[#435574]">({{ freelancer.reviewCount }} avaliações)</span>
      </div>

      <!-- Price Range and Availability -->
      <div class="mb-4 flex items-end justify-between gap-2">
        <div class="flex-1 min-w-0">
          <p class="text-sm text-[#435574]">Faixa de preço</p>
          <p class="text-[#286AD8]">{{ freelancer.priceRange }}</p>
        </div>
        <Badge v-if="freelancer.isAvailable" class="bg-green-100 text-green-700 hover:bg-green-100 flex-shrink-0">
          Disponível
        </Badge>
      </div>

      <!-- CTA -->
      <Button
        @click="handleButtonClick"
        class="w-full bg-[#286AD8] hover:bg-[#435574] transition-colors"
      >
        Ver perfil
      </Button>
    </div>
  </div>
</template>

