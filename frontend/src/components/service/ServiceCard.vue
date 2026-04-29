<script setup lang="ts">
import { Briefcase, Heart, DollarSign, Clock, Star } from 'lucide-vue-next';
import Card from '../ui/card.vue';
import Badge from '../ui/badge.vue';
import { getServicePriceDisplay, type ServicePriceRangeData } from '@/utils/servicePricing';
import { formatServiceRatingDisplay, hasServiceRating, type ServiceReviewSummary } from '@/utils/serviceReviews';

interface ServiceItem extends ServicePriceRangeData, ServiceReviewSummary {
  id: number;
  freelancerId: number;
  freelancerName: string;
  title: string;
  category: string;
  description: string;
  deliveryTime: number;
  deliveryUnit: string;
  workModel?: string | null;
  freelancerProfilePhoto?: string;
  images: string[];
}

interface Props {
  service: ServiceItem;
  isFavorite: boolean;
  onNavigate: (params: { providerId: number; serviceId: number }) => void;
  onToggleFavorite: (id: number) => void;
}

const props = defineProps<Props>();

const formatDelivery = (time: number, unit: string) => {
  const unitLabel = unit === 'HOURS' ? 'hora(s)' : unit === 'DAYS' ? 'dia(s)' : 'semana(s)';
  return `${time} ${unitLabel}`;
};

const formatWorkModel = (value?: string | null) => {
  const normalized = value?.toUpperCase();
  if (normalized === 'PRESENCIAL') return 'Presencial';
  if (normalized === 'HIBRIDO') return 'Híbrido';
  if (normalized === 'REMOTO') return 'Remoto';
  return null;
};

const getInitials = (name: string) =>
  name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);

const handleCardClick = () => {
  props.onNavigate({ providerId: props.service.freelancerId, serviceId: props.service.id });
};

const handleFavoriteClick = (e: MouseEvent) => {
  e.stopPropagation();
  props.onToggleFavorite(props.service.id);
};
</script>

<template>
  <Card
    class="group hover:shadow-lg hover:border-[#286AD8]/30 transition-all duration-200 overflow-hidden cursor-pointer"
    @click="handleCardClick"
  >
    <!-- Service Image -->
    <div class="relative h-48 bg-gradient-to-br from-[#286AD8]/10 to-[#435574]/10 overflow-hidden">
      <img
        v-if="service.images && service.images.length > 0"
        :src="service.images[0]"
        :alt="service.title"
        class="w-full h-full object-cover object-center group-hover:scale-105 transition-transform duration-300"
      />
      <div v-else class="w-full h-full flex items-center justify-center">
        <Briefcase class="h-12 w-12 text-[#286AD8]/30" />
      </div>

      <!-- Category Badge -->
      <Badge class="absolute top-3 left-3 bg-white/90 text-[#263238] backdrop-blur-sm text-xs">
        {{ service.category }}
      </Badge>

      <!-- Favorite -->
      <button
        @click="handleFavoriteClick"
        :class="`absolute top-3 right-3 p-1.5 rounded-full transition-all ${
          isFavorite
            ? 'bg-red-50 text-red-500'
            : 'bg-white/80 text-gray-400 hover:text-red-500 hover:bg-red-50'
        }`"
      >
        <Heart :class="`h-4 w-4 ${isFavorite ? 'fill-current' : ''}`" />
      </button>
    </div>

    <!-- Content -->
    <div class="p-4 flex min-h-[208px] flex-col">
      <div class="mb-2 flex items-start justify-between gap-2">
        <h3 class="min-w-0 flex-1 font-semibold text-[#263238] line-clamp-1 group-hover:text-[#286AD8] transition-colors">
          {{ service.title }}
        </h3>
        <Badge
          v-if="formatWorkModel(service.workModel)"
          variant="outline"
          class="shrink-0 bg-white text-[#435574]"
        >
          {{ formatWorkModel(service.workModel) }}
        </Badge>
      </div>

      <!-- Freelancer -->
      <div class="flex items-center justify-between gap-3 mb-3">
        <div class="flex items-center gap-2 min-w-0">
          <div class="w-6 h-6 rounded-full bg-[#286AD8]/10 overflow-hidden flex items-center justify-center text-[10px] font-semibold text-[#286AD8]">
            <img
              v-if="service.freelancerProfilePhoto"
              :src="service.freelancerProfilePhoto"
              :alt="service.freelancerName"
              class="w-full h-full object-cover"
            />
            <template v-else>
              {{ getInitials(service.freelancerName) }}
            </template>
          </div>
          <span class="text-sm text-[#435574] truncate">{{ service.freelancerName }}</span>
        </div>
        <div class="flex items-center justify-end gap-1.5 text-xs text-[#435574] flex-shrink-0">
          <Star :class="`h-3.5 w-3.5 ${hasServiceRating(service) ? 'fill-yellow-400 text-yellow-400' : 'text-gray-300'}`" />
          <span>{{ formatServiceRatingDisplay(service) }}</span>
        </div>
      </div>

      <p class="text-sm text-[#435574] mb-3 line-clamp-2">{{ service.description }}</p>

      <!-- Footer -->
      <div class="mt-auto flex items-center justify-between pt-3 border-t border-gray-100">
        <div class="flex items-center gap-1 text-[#286AD8]">
          <DollarSign class="h-4 w-4" />
          <span class="font-bold text-sm">{{ getServicePriceDisplay(service) }}</span>
        </div>
        <div class="flex items-center gap-1 text-[#435574]">
          <Clock class="h-3.5 w-3.5" />
          <span class="text-xs">{{ formatDelivery(service.deliveryTime, service.deliveryUnit) }}</span>
        </div>
      </div>
    </div>
  </Card>
</template>
