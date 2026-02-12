<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import { X, SlidersHorizontal, MapPin, DollarSign, Star, Calendar, Briefcase } from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Label from '../ui/label.vue';
import Slider from '../ui/slider.vue';
import Checkbox from '../ui/checkbox.vue';
import Select from '../ui/select.vue';
import SelectContent from '../ui/select-content.vue';
import SelectItem from '../ui/select-item.vue';
import SelectTrigger from '../ui/select-trigger.vue';
import SelectValue from '../ui/select-value.vue';

export interface Filters {
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
  isOpen: boolean;
  onClose: () => void;
  onApplyFilters: (filters: Filters) => void;
  currentFilters: Filters;
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

const filters = ref<Filters>({ ...props.currentFilters });
const hasChanges = ref(false);

watch(
  () => props.currentFilters,
  (newFilters) => {
    filters.value = { ...newFilters };
  },
  { immediate: true }
);

watch(
  filters,
  () => {
    const changed = JSON.stringify(filters.value) !== JSON.stringify(props.currentFilters);
    hasChanges.value = changed;
  },
  { deep: true }
);

const handleApply = () => {
  props.onApplyFilters(filters.value);
  props.onClose();
};

const handleClear = () => {
  filters.value = { ...defaultFilters };
  hasChanges.value = true;
};

const handleAvailabilityChange = (value: string, checked: boolean) => {
  if (checked) {
    filters.value.availability = [...filters.value.availability, value];
  } else {
    filters.value.availability = filters.value.availability.filter((v) => v !== value);
  }
};

const handleCategoryChange = (value: string, checked: boolean) => {
  if (checked) {
    filters.value.categories = [...filters.value.categories, value];
  } else {
    filters.value.categories = filters.value.categories.filter((v) => v !== value);
  }
};

const priceRange = computed({
  get: () => [filters.value.minPrice, filters.value.maxPrice],
  set: (val) => {
    const values = Array.isArray(val) ? val : [val];
    filters.value.minPrice = values[0];
    filters.value.maxPrice = values[1] ?? values[0];
  },
});

const distanceValue = computed({
  get: () => [filters.value.maxDistance],
  set: (val) => {
    const values = Array.isArray(val) ? val : [val];
    filters.value.maxDistance = values[0];
  },
});
</script>

<template>
  <template v-if="isOpen">
    <!-- Overlay -->
    <div class="fixed inset-0 bg-black/50 z-40" @click="props.onClose" />

    <!-- Filters Panel -->
    <div
      class="fixed top-0 right-0 h-full w-full max-w-md bg-white z-50 shadow-2xl overflow-hidden flex flex-col animate-in slide-in-from-right duration-300"
    >
      <!-- Header -->
      <div class="p-6 border-b border-gray-200 flex items-center justify-between bg-white">
        <div class="flex items-center gap-2">
          <SlidersHorizontal class="h-5 w-5 text-[#286AD8]" />
          <h3 class="text-[#263238]">Filtros Avançados</h3>
        </div>
        <button @click="props.onClose" class="text-[#435574] hover:text-[#263238] transition-colors">
          <X class="h-5 w-5" />
        </button>
      </div>

      <!-- Filters Content -->
      <div class="flex-1 overflow-y-auto p-6 space-y-6">
        <!-- Location Filter -->
        <div class="space-y-3">
          <Label class="flex items-center gap-2 text-[#263238]">
            <MapPin class="h-4 w-4 text-[#286AD8]" />
            Localização
          </Label>
          <Select
            :model-value="filters.location"
            @update:model-value="(val: string) => (filters.location = val)"
          >
            <SelectTrigger>
              <SelectValue placeholder="Selecione uma cidade" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="sao-paulo">São Paulo, SP</SelectItem>
              <SelectItem value="rio-janeiro">Rio de Janeiro, RJ</SelectItem>
              <SelectItem value="belo-horizonte">Belo Horizonte, MG</SelectItem>
              <SelectItem value="brasilia">Brasília, DF</SelectItem>
              <SelectItem value="salvador">Salvador, BA</SelectItem>
              <SelectItem value="curitiba">Curitiba, PR</SelectItem>
            </SelectContent>
          </Select>

          <div class="space-y-2">
            <Label class="text-sm text-[#435574]">
              Distância máxima: {{ filters.maxDistance }} km
            </Label>
            <Slider
              :model-value="distanceValue"
              @update:model-value="(val: number[]) => (distanceValue = val)"
              :min="1"
              :max="100"
              :step="1"
              class="w-full"
            />
          </div>
        </div>

        <!-- Price Range Filter -->
        <div class="space-y-3">
          <Label class="flex items-center gap-2 text-[#263238]">
            <DollarSign class="h-4 w-4 text-[#286AD8]" />
            Faixa de Preço
          </Label>
          <div class="space-y-2">
            <div class="flex items-center justify-between text-sm text-[#435574]">
              <span>R$ {{ filters.minPrice }}</span>
              <span>R$ {{ filters.maxPrice }}</span>
            </div>
            <Slider
              :model-value="priceRange"
              @update:model-value="(val: number[]) => (priceRange = val)"
              :min="0"
              :max="10000"
              :step="50"
              class="w-full"
            />
          </div>
        </div>

        <!-- Rating Filter -->
        <div class="space-y-3">
          <Label class="flex items-center gap-2 text-[#263238]">
            <Star class="h-4 w-4 text-[#286AD8]" />
            Avaliação Mínima
          </Label>
          <div class="grid grid-cols-5 gap-2">
            <button
              v-for="rating in [1, 2, 3, 4, 5]"
              :key="rating"
              @click="filters.minRating = rating"
              :class="`
                    p-2 rounded-lg border-2 transition-all flex flex-col items-center gap-1
                    ${
                      filters.minRating === rating
                        ? 'border-[#286AD8] bg-blue-50'
                        : 'border-gray-200 hover:border-gray-300'
                    }
                  `"
            >
              <Star
                :class="`h-5 w-5 ${
                  filters.minRating === rating
                    ? 'fill-yellow-400 text-yellow-400'
                    : 'text-gray-400'
                }`"
              />
              <span class="text-xs text-[#435574]">{{ rating }}+</span>
            </button>
          </div>
        </div>

        <!-- Availability Filter -->
        <div class="space-y-3">
          <Label class="flex items-center gap-2 text-[#263238]">
            <Calendar class="h-4 w-4 text-[#286AD8]" />
            Disponibilidade
          </Label>
          <div class="space-y-2">
            <div
              v-for="option in [
                { value: 'today', label: 'Disponível hoje' },
                { value: 'week', label: 'Disponível esta semana' },
                { value: 'month', label: 'Disponível este mês' },
                { value: 'immediate', label: 'Atendimento imediato' },
              ]"
              :key="option.value"
              class="flex items-center space-x-2"
            >
              <Checkbox
                :id="option.value"
                :checked="filters.availability.includes(option.value)"
                @update:checked="(checked) => handleAvailabilityChange(option.value, checked as boolean)"
              />
              <label :for="option.value" class="text-sm text-[#435574] cursor-pointer">
                {{ option.label }}
              </label>
            </div>
          </div>
        </div>

        <!-- Category Filter -->
        <div class="space-y-3">
          <Label class="flex items-center gap-2 text-[#263238]">
            <Briefcase class="h-4 w-4 text-[#286AD8]" />
            Categorias
          </Label>
          <div class="space-y-2 max-h-64 overflow-y-auto">
            <div
              v-for="option in [
                { value: 'Eletricista', label: 'Eletricista' },
                { value: 'Encanador', label: 'Encanador' },
                { value: 'Pintor', label: 'Pintor' },
                { value: 'Pedreiro', label: 'Pedreiro' },
                { value: 'Marceneiro', label: 'Marceneiro' },
                { value: 'Jardineiro', label: 'Jardineiro' },
                { value: 'Arquiteta', label: 'Arquiteto(a)' },
                { value: 'Designer Gráfico', label: 'Designer Gráfico' },
                { value: 'Desenvolvedora Web', label: 'Desenvolvedor(a) Web' },
                { value: 'Fotógrafa', label: 'Fotógrafo(a)' },
                { value: 'Consultora de Marketing', label: 'Consultor(a) de Marketing' },
                { value: 'Personal Trainer', label: 'Personal Trainer' },
              ]"
              :key="option.value"
              class="flex items-center space-x-2"
            >
              <Checkbox
                :id="`category-${option.value}`"
                :checked="filters.categories.includes(option.value)"
                @update:checked="(checked) => handleCategoryChange(option.value, checked as boolean)"
              />
              <label :for="`category-${option.value}`" class="text-sm text-[#435574] cursor-pointer">
                {{ option.label }}
              </label>
            </div>
          </div>
        </div>

        <!-- Sort By -->
        <div class="space-y-3">
          <Label class="text-[#263238]">Ordenar por</Label>
          <Select
            :model-value="filters.sortBy"
            @update:model-value="(val: string) => (filters.sortBy = val)"
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="relevance">Relevância</SelectItem>
              <SelectItem value="rating-desc">Maior avaliação</SelectItem>
              <SelectItem value="rating-asc">Menor avaliação</SelectItem>
              <SelectItem value="price-asc">Menor preço</SelectItem>
              <SelectItem value="price-desc">Maior preço</SelectItem>
              <SelectItem value="recent">Mais recentes</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      <!-- Footer Actions -->
      <div class="p-6 border-t border-gray-200 bg-white space-y-3">
        <Button
          @click="handleApply"
          :disabled="!hasChanges"
          class="w-full bg-[#286AD8] hover:bg-[#435574] transition-colors"
          size="lg"
        >
          Aplicar Filtros
        </Button>
        <Button
          @click="handleClear"
          variant="outline"
          class="w-full border-[#286AD8] text-[#286AD8] hover:bg-blue-50"
          size="lg"
        >
          Limpar Filtros
        </Button>
      </div>
    </div>
  </template>
</template>

