<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue';
import { Search, X, TrendingUp, Clock, Tag } from 'lucide-vue-next';
import Input from '../ui/input.vue';

interface SearchSuggestion {
  id: string;
  text: string;
  type: 'recent' | 'trending' | 'category' | 'freelancer';
}

interface Props {
  onSearch: (query: string) => void;
  placeholder?: string;
}

const props = defineProps<Props>();

const query = ref('');
const showSuggestions = ref(false);
const suggestions = ref<SearchSuggestion[]>([]);
const wrapperRef = ref<HTMLDivElement | null>(null);

// Mock data - substituir por dados reais da API
const mockSuggestions: SearchSuggestion[] = [
  { id: '1', text: 'Eletricista', type: 'recent' },
  { id: '2', text: 'Encanador', type: 'recent' },
  { id: '3', text: 'Pintor', type: 'trending' },
  { id: '4', text: 'Pedreiro', type: 'trending' },
  { id: '5', text: 'Marceneiro', type: 'category' },
  { id: '6', text: 'Jardineiro', type: 'category' },
  { id: '7', text: 'Design Gráfico', type: 'trending' },
  { id: '8', text: 'Desenvolvedor Web', type: 'trending' },
];

watch(query, () => {
  // Filtrar sugestões baseadas na query
  if (query.value.trim()) {
    const filtered = mockSuggestions.filter((s) =>
      s.text.toLowerCase().includes(query.value.toLowerCase())
    );
    suggestions.value = filtered.slice(0, 6);
  } else {
    // Mostrar sugestões recentes e em alta
    const recent = mockSuggestions.filter((s) => s.type === 'recent').slice(0, 2);
    const trending = mockSuggestions.filter((s) => s.type === 'trending').slice(0, 4);
    suggestions.value = [...recent, ...trending];
  }
});

const handleClickOutside = (event: MouseEvent) => {
  if (wrapperRef.value && !wrapperRef.value.contains(event.target as Node)) {
    showSuggestions.value = false;
  }
};

onMounted(() => {
  document.addEventListener('mousedown', handleClickOutside);
});

onUnmounted(() => {
  document.removeEventListener('mousedown', handleClickOutside);
});

const handleSearch = (searchQuery: string) => {
  if (searchQuery.trim()) {
    props.onSearch(searchQuery.trim());
    showSuggestions.value = false;
  }
};

const handleSubmit = (e: Event) => {
  e.preventDefault();
  handleSearch(query.value);
};

const handleClear = () => {
  query.value = '';
  suggestions.value = [];
};

const getIcon = (type: string) => {
  switch (type) {
    case 'recent':
      return Clock;
    case 'trending':
      return TrendingUp;
    case 'category':
      return Tag;
    default:
      return Search;
  }
};

const getLabel = (type: string) => {
  switch (type) {
    case 'recent':
      return 'Recente';
    case 'trending':
      return 'Em alta';
    case 'category':
      return 'Categoria';
    default:
      return '';
  }
};
</script>

<template>
  <div ref="wrapperRef" class="relative w-full">
    <form @submit="handleSubmit">
      <div class="relative">
        <Search class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-[#435574]" />
        <Input
          type="text"
          :placeholder="placeholder || 'Buscar por categorias, freelancers ou serviços...'"
          :model-value="query"
          @update:model-value="(val) => query = val as string"
          @focus="showSuggestions = true"
          class="pl-10 pr-10 bg-white border-gray-200 focus:border-[#286AD8]"
        />
        <button
          v-if="query"
          type="button"
          @click="handleClear"
          class="absolute right-3 top-1/2 -translate-y-1/2 text-[#435574] hover:text-[#263238]"
        >
          <X class="h-5 w-5" />
        </button>
      </div>
    </form>

    <!-- Suggestions Dropdown -->
    <div
      v-if="showSuggestions && suggestions.length > 0"
      class="absolute top-full left-0 right-0 mt-2 bg-white border border-gray-200 rounded-lg shadow-lg z-50 max-h-96 overflow-y-auto"
    >
      <div class="p-2">
        <div v-if="!query && suggestions.filter((s) => s.type === 'recent').length > 0" class="mb-2">
          <p class="px-3 py-2 text-xs text-[#435574]">Buscas recentes</p>
        </div>

        <button
          v-for="suggestion in suggestions"
          :key="suggestion.id"
          @click="
            query = suggestion.text;
            handleSearch(suggestion.text);
          "
          class="w-full flex items-center gap-3 px-3 py-2 hover:bg-gray-50 rounded-lg transition-colors text-left group"
        >
          <div class="text-[#435574] group-hover:text-[#286AD8]">
            <component :is="getIcon(suggestion.type)" class="h-4 w-4" />
          </div>
          <div class="flex-1">
            <p class="text-[#263238] group-hover:text-[#286AD8]">{{ suggestion.text }}</p>
          </div>
          <span v-if="suggestion.type === 'trending'" class="text-xs text-[#435574] bg-gray-100 px-2 py-1 rounded">
            {{ getLabel(suggestion.type) }}
          </span>
        </button>

        <div v-if="query && suggestions.length === 0" class="px-3 py-6 text-center text-[#435574]">
          <p>Nenhuma sugestão encontrada</p>
          <p class="text-sm mt-1">Pressione Enter para buscar</p>
        </div>
      </div>

      <div v-if="!query" class="border-t border-gray-200 p-3">
        <p class="text-xs text-[#435574] text-center">Digite para ver mais sugestões</p>
      </div>
    </div>
  </div>
</template>

