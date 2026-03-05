<script setup lang="ts">
import { Star, MapPin, Heart } from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Card from '../ui/card.vue';
import Badge from '../ui/badge.vue';

export interface Freelancer {
  id: string | number;
  name: string;
  avatar?: string;
  professionalTitle?: string;
  category: string;
  location?: string;
  city?: string;
  state?: string;
  rating: number;
  reviewCount: number;
  servicesCount?: number;
  categories?: string[];
  isAvailable: boolean;
}

interface Props {
  freelancer: Freelancer;
  isFavorite: boolean;
  onViewProfile: (id: string | number) => void;
  onToggleFavorite: (id: string | number) => void;
}

const props = defineProps<Props>();

const getInitials = (name: string) =>
  name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);

const handleCardClick = () => {
  props.onViewProfile(props.freelancer.id);
};

const handleToggleFavorite = (e: MouseEvent) => {
  e.stopPropagation();
  props.onToggleFavorite(props.freelancer.id);
};
</script>

<template>
  <Card
    class="p-6 hover:shadow-lg transition-all border border-gray-100 group cursor-pointer relative"
    @click="handleCardClick"
  >
    <!-- Favorite Toggle Overlay -->
    <button
      @click="handleToggleFavorite"
      :class="`absolute top-4 right-4 p-1.5 rounded-full transition-all z-10 ${
        isFavorite
          ? 'bg-red-50 text-red-500'
          : 'bg-gray-50 text-gray-400 hover:text-red-500 hover:bg-red-50'
      }`"
    >
      <Heart :class="`h-4 w-4 ${isFavorite ? 'fill-current' : ''}`" />
    </button>

    <div class="flex items-center gap-4 mb-4">
      <div class="w-16 h-16 rounded-full bg-blue-50 overflow-hidden flex items-center justify-center text-xl font-bold text-[#286AD8] border border-blue-100 flex-shrink-0">
        <img
          v-if="freelancer.avatar"
          :src="freelancer.avatar"
          :alt="freelancer.name"
          class="w-full h-full object-cover"
        />
        <template v-else>
          {{ getInitials(freelancer.name) }}
        </template>
      </div>
      <div class="min-w-0 pr-8">
        <h3 class="font-bold text-[#263238] group-hover:text-[#286AD8] transition-colors truncate">
          {{ freelancer.name }}
        </h3>
        <p v-if="freelancer.professionalTitle" class="text-xs text-[#286AD8] font-medium truncate mb-1">
          {{ freelancer.professionalTitle }}
        </p>
        <p v-if="freelancer.servicesCount !== undefined" class="text-xs text-[#435574]">
          {{ freelancer.servicesCount }} {{ freelancer.servicesCount === 1 ? 'serviço cadastrado' : 'serviços cadastrados' }}
        </p>
        <div v-else class="flex items-center gap-1 text-[#435574] mt-1">
          <Star class="h-3 w-3 fill-yellow-400 text-yellow-400" />
          <span class="text-xs font-semibold text-[#263238]">{{ freelancer.rating.toFixed(1) }}</span>
          <span class="text-[10px]">({{ freelancer.reviewCount }})</span>
        </div>
      </div>
    </div>

    <div class="space-y-3">
      <div class="flex items-center gap-1 text-[#435574]">
        <MapPin class="h-3 w-3" />
        <span class="text-xs truncate">
          {{ freelancer.location || `${freelancer.city || 'Local'}, ${freelancer.state || ''}` }}
        </span>
      </div>

      <p class="text-xs text-[#435574] line-clamp-1">
        Atua em: <span class="font-semibold text-[#263238]">{{ freelancer.category || (freelancer.categories?.join(', ')) }}</span>
      </p>
      
      <div class="flex flex-wrap gap-2 py-1 min-h-[28px]">
        <template v-if="freelancer.categories">
          <Badge v-for="cat in freelancer.categories.slice(0, 2)" :key="cat" variant="outline" class="text-[10px] bg-blue-50/30 text-[#286AD8] border-none">
            {{ cat }}
          </Badge>
          <span v-if="freelancer.categories.length > 2" class="text-[10px] text-gray-400 self-center">
            +{{ freelancer.categories.length - 2 }}
          </span>
        </template>
        <Badge v-else variant="outline" class="text-[10px] bg-blue-50/30 text-[#286AD8] border-none">
          {{ freelancer.category }}
        </Badge>
      </div>

      <Button class="w-full bg-white text-[#286AD8] border-[#286AD8] hover:bg-blue-50 hover:text-[#286AD8]" variant="outline">
        Ver Perfil Completo
      </Button>
    </div>
  </Card>
</template>
