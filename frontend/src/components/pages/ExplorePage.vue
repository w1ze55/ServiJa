<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  Search,
  Grid3x3,
  List,
  SlidersHorizontal,
  Loader2,
  AlertCircle,
  RefreshCw,
  X,
  Briefcase,
  Heart,
  ChevronRight,
  DollarSign,
  Clock,
  Star,
} from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Card from '../ui/card.vue';
import Badge from '../ui/badge.vue';
import Select from '../ui/select.vue';
import SelectContent from '../ui/select-content.vue';
import SelectItem from '../ui/select-item.vue';
import SelectTrigger from '../ui/select-trigger.vue';
import SelectValue from '../ui/select-value.vue';
import Tabs from '../ui/tabs.vue';
import TabsList from '../ui/tabs-list.vue';
import TabsTrigger from '../ui/tabs-trigger.vue';
import ServiceCard from '../service/ServiceCard.vue';
import FreelancerCard from '../freelancer/FreelancerCard.vue';
import api from '@/api/config/axios';
import { toast } from '@/utils/toast';
import { useFavoritesStore } from '@/stores/favorites';
import { useUserStore } from '@/stores/userStore';
import { useCategoryCatalogStore } from '@/stores/categoryCatalogStore';
import { getServicePriceDisplay, getServicePriceSortValue, type ServicePriceRangeData } from '@/utils/servicePricing';
import { formatServiceRatingDisplay, hasServiceRating, type ServiceReviewSummary } from '@/utils/serviceReviews';
import { isOwnFreelancerProfile } from '@/utils/userRoles';

interface ServiceItem extends ServicePriceRangeData, ServiceReviewSummary {
  id: number;
  freelancerId: number;
  freelancerName: string;
  title: string;
  category: string;
  categorySlug?: string | null;
  subcategory?: string | null;
  subcategorySlug?: string | null;
  description: string;
  deliveryTime: number;
  deliveryUnit: string;
  workModel?: string | null;
  freelancerProfilePhoto?: string;
  tags: string[];
  images: string[];
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

interface FreelancerProfileData {
  id: number;
  location?: string | null;
  rating?: number | null;
  totalReviews?: number | null;
  available?: boolean | null;
}

interface Props {
  onNavigate: (page: string, params?: any) => void;
}

const props = defineProps<Props>();
const route = useRoute();
const router = useRouter();

const services = ref<ServiceItem[]>([]);
const isLoading = ref(false);
const error = ref('');
const searchQuery = ref('');
const selectedCategory = ref('all');
const selectedSubcategory = ref('all');
const sortBy = ref('recent');
const searchMode = ref<'services' | 'professionals'>('services');
const viewMode = ref<'grid' | 'list'>('grid');
const freelancerProfiles = ref<Record<number, FreelancerProfileData>>({});
const lastTrackedSearchSignature = ref<string | null>(null);

const favoritesStore = useFavoritesStore();
const userStore = useUserStore();
const categoryCatalogStore = useCategoryCatalogStore();

const categories = computed(() => [
  { value: 'all', label: 'Todas as categorias' },
  ...categoryCatalogStore.activePublicCategories.map((category) => ({
    value: category.slug,
    label: category.name,
  })),
]);
const subcategories = computed(() => {
  if (selectedCategory.value === 'all') {
    return [{ value: 'all', label: 'Todas as subcategorias' }];
  }

  const category = categoryCatalogStore.activePublicCategories.find(
    (item) => item.slug === selectedCategory.value
  );

  return [
    { value: 'all', label: 'Todas as subcategorias' },
    ...(category?.subcategories || []).map((subcategory) => ({
      value: subcategory.slug,
      label: subcategory.name,
    })),
  ];
});

const sortOptions = [
  { value: 'recent', label: 'Mais recentes' },
  { value: 'price-asc', label: 'Menor preço' },
  { value: 'price-desc', label: 'Maior preço' },
  { value: 'name-asc', label: 'Nome (A-Z)' },
];

const loadServices = async (options?: { trackSearch?: boolean }) => {
  isLoading.value = true;
  error.value = '';
  try {
    const response = await api.get('/services/public/all', {
      params: {
        categorySlug: selectedCategory.value !== 'all' ? selectedCategory.value : undefined,
        subcategorySlug: selectedSubcategory.value !== 'all' ? selectedSubcategory.value : undefined,
        q: searchQuery.value.trim() || undefined,
        trackSearch: options?.trackSearch ? true : undefined,
      },
    });
    services.value = response.data || [];

    if (!favoritesStore.initialized) {
      favoritesStore.fetchFavorites();
    }
  } catch (err: any) {
    console.error('Erro ao carregar serviços:', err);
    error.value = 'Não foi possível carregar os serviços. Tente novamente.';
    toast.error('Erro ao carregar serviços');
  } finally {
    isLoading.value = false;
  }
};

const normalizeRouteParam = (value: unknown) => (typeof value === 'string' ? value.trim() : '');
const normalizeSearchMode = (value: unknown): 'services' | 'professionals' =>
  value === 'professionals' ? 'professionals' : 'services';

const resolveCategorySlugFromSubcategory = (subcategorySlug: string) =>
  categoryCatalogStore.activePublicCategories.find((category) =>
    category.subcategories.some((subcategory) => subcategory.slug === subcategorySlug)
  )?.slug || '';

const buildSearchSignature = (query: string, categorySlug: string, subcategorySlug: string) =>
  [query.trim().toLowerCase(), categorySlug, subcategorySlug].join('|');

const hasExplicitSearchIntent = (query: string, categorySlug: string, subcategorySlug: string) =>
  Boolean(query.trim() || categorySlug || subcategorySlug);

const getNormalizedRouteFilters = () => {
  const mode = normalizeSearchMode(route.query.mode);
  const query = normalizeRouteParam(route.query.q);
  const subcategorySlug = normalizeRouteParam(route.query.subcategorySlug);
  const categorySlug = normalizeRouteParam(route.query.categorySlug)
    || (subcategorySlug ? resolveCategorySlugFromSubcategory(subcategorySlug) : '');

  return {
    mode,
    query,
    categorySlug,
    subcategorySlug,
  };
};

const syncFiltersToRoute = async () => {
  const nextQuery: Record<string, string> = {};

  if (searchMode.value !== 'services') {
    nextQuery.mode = searchMode.value;
  }
  if (searchQuery.value.trim()) {
    nextQuery.q = searchQuery.value.trim();
  }
  if (selectedCategory.value !== 'all') {
    nextQuery.categorySlug = selectedCategory.value;
  }
  if (selectedSubcategory.value !== 'all') {
    nextQuery.subcategorySlug = selectedSubcategory.value;
  }

  const currentQuery = {
    mode: normalizeSearchMode(route.query.mode),
    q: normalizeRouteParam(route.query.q),
    categorySlug: normalizeRouteParam(route.query.categorySlug),
    subcategorySlug: normalizeRouteParam(route.query.subcategorySlug),
  };

  if (
    currentQuery.mode === (nextQuery.mode || 'services')
    && currentQuery.q === (nextQuery.q || '')
    && currentQuery.categorySlug === (nextQuery.categorySlug || '')
    && currentQuery.subcategorySlug === (nextQuery.subcategorySlug || '')
  ) {
    return;
  }

  await router.replace({ path: route.path, query: nextQuery });
};

const applyRouteFilters = async () => {
  const { mode, query, categorySlug, subcategorySlug } = getNormalizedRouteFilters();

  searchMode.value = mode;
  searchQuery.value = query;
  selectedCategory.value = categorySlug || 'all';
  selectedSubcategory.value = subcategorySlug || 'all';

  const signature = buildSearchSignature(query, categorySlug, subcategorySlug);
  const shouldTrackSearch = hasExplicitSearchIntent(query, categorySlug, subcategorySlug)
    && signature !== lastTrackedSearchSignature.value;

  lastTrackedSearchSignature.value = hasExplicitSearchIntent(query, categorySlug, subcategorySlug)
    ? signature
    : null;

  await loadServices({ trackSearch: shouldTrackSearch });
};

const loadFreelancerProfiles = async (freelancerIds: number[]) => {
  const visibleFreelancerIds = freelancerIds.filter(
    (freelancerId) => !isOwnFreelancerProfile(userStore.user, freelancerId),
  );

  if (visibleFreelancerIds.length === 0) {
    freelancerProfiles.value = {};
    return;
  }

  try {
    const profiles = await Promise.all(
      visibleFreelancerIds.map(async (freelancerId) => {
        const response = await api.get(`/freelancer/${freelancerId}/profile`);
        return response.data as FreelancerProfileData;
      })
    );

    freelancerProfiles.value = profiles.reduce<Record<number, FreelancerProfileData>>((acc, profile) => {
      acc[profile.id] = profile;
      return acc;
    }, {});
  } catch (err) {
    console.error('Erro ao carregar perfis de freelancers:', err);
  }
};

const handleSearch = () => {
  clearTimeout(searchTimer);
  void syncFiltersToRoute();
};

let searchTimer: ReturnType<typeof setTimeout>;
const handleSearchInput = () => {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(() => {
    void syncFiltersToRoute();
  }, 500);
};

const handleCategoryChange = (value: string) => {
  selectedCategory.value = value;
  if (!subcategories.value.some((subcategory) => subcategory.value === selectedSubcategory.value)) {
    selectedSubcategory.value = 'all';
  }
  clearTimeout(searchTimer);
  void syncFiltersToRoute();
};

const handleSubcategoryChange = (value: string) => {
  selectedSubcategory.value = value;
  clearTimeout(searchTimer);
  void syncFiltersToRoute();
};

const filteredServices = computed(() => {
  let result = services.value.filter(
    (service) => !isOwnFreelancerProfile(userStore.user, service.freelancerId),
  );

  switch (sortBy.value) {
    case 'price-asc':
      result.sort((a, b) => getServicePriceSortValue(a) - getServicePriceSortValue(b));
      break;
    case 'price-desc':
      result.sort((a, b) => getServicePriceSortValue(b) - getServicePriceSortValue(a));
      break;
    case 'name-asc':
      result.sort((a, b) => a.title.localeCompare(b.title));
      break;
    case 'recent':
    default:
      result.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
      break;
  }

  return result;
});

const freelancerGroups = computed(() => {
  const groups: Record<number, { freelancerName: string; freelancerId: number; freelancerProfilePhoto?: string; services: ServiceItem[] }> = {};
  for (const svc of filteredServices.value) {
    if (!groups[svc.freelancerId]) {
      groups[svc.freelancerId] = {
        freelancerName: svc.freelancerName,
        freelancerId: svc.freelancerId,
        freelancerProfilePhoto: svc.freelancerProfilePhoto,
        services: [],
      };
    }
    groups[svc.freelancerId].services.push(svc);
  }
  return Object.values(groups);
});

const freelancerResults = computed(() =>
  freelancerGroups.value.map((group) => {
    const profile = freelancerProfiles.value[group.freelancerId];

    return {
      id: group.freelancerId,
      name: group.freelancerName,
      avatar: group.freelancerProfilePhoto,
      location: profile?.location || undefined,
      category: Array.from(new Set(group.services.map((s) => s.category))).join(', '),
      categories: Array.from(new Set(group.services.map((s) => s.category))),
      servicesCount: group.services.length,
      rating: profile?.rating ?? 5.0,
      reviewCount: profile?.totalReviews ?? 0,
      isAvailable: profile?.available ?? true,
    };
  })
);

const totalFreelancers = computed(() => freelancerResults.value.length);

const toggleFavoriteService = (id: number | string) => {
  favoritesStore.toggleFavoriteService(id);
};

const toggleFavoriteFreelancer = (id: number | string) => {
  favoritesStore.toggleFavoriteFreelancer(id);
};

const isFavoriteService = (id: number | string) => favoritesStore.isFavoriteService(id);
const isFavoriteFreelancer = (id: number | string) => favoritesStore.isFavoriteFreelancer(id);

const handleViewProfile = (providerId: string | number) => {
  if (isOwnFreelancerProfile(userStore.user, providerId)) {
    toast.error('Voce nao pode acessar o proprio perfil publico como cliente');
    return;
  }

  props.onNavigate('provider-profile', { providerId: Number(providerId) });
};

const handleViewService = (params: { providerId: number; serviceId: number }) => {
  if (isOwnFreelancerProfile(userStore.user, params.providerId)) {
    toast.error('Voce nao pode acessar o proprio servico publico como cliente');
    return;
  }

  props.onNavigate('service-detail', params);
};

const clearFilters = () => {
  searchQuery.value = '';
  selectedCategory.value = 'all';
  selectedSubcategory.value = 'all';
  sortBy.value = 'recent';
  clearTimeout(searchTimer);
  void syncFiltersToRoute();
};

const clearSearchQuery = () => {
  searchQuery.value = '';
  clearTimeout(searchTimer);
  void syncFiltersToRoute();
};

const clearCategoryFilter = () => {
  selectedCategory.value = 'all';
  selectedSubcategory.value = 'all';
  clearTimeout(searchTimer);
  void syncFiltersToRoute();
};

const clearSubcategoryFilter = () => {
  selectedSubcategory.value = 'all';
  clearTimeout(searchTimer);
  void syncFiltersToRoute();
};

const getInitials = (name: string) => {
  if (!name) return '';
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
};

onMounted(() => {
  void categoryCatalogStore.loadPublicCategories();
});

watch(
  searchMode,
  () => {
    void syncFiltersToRoute();
  }
);

watch(
  freelancerGroups,
  (groups) => {
    loadFreelancerProfiles(groups.map((group) => group.freelancerId));
  },
  { immediate: true }
);

watch(
  () => route.query,
  () => {
    void applyRouteFilters();
  },
  { immediate: true }
);

watch(
  () => categoryCatalogStore.activePublicCategories.length,
  (currentLength, previousLength) => {
    if (
      currentLength > 0
      && currentLength !== previousLength
      && normalizeRouteParam(route.query.subcategorySlug)
    ) {
      void applyRouteFilters();
    }
  }
);
</script>

<template>
  <div class="space-y-6 max-w-7xl mx-auto">
    <!-- Header -->
    <div class="flex flex-col md:flex-row md:items-end justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-[#263238] mb-2">Explorar</h1>
        <p class="text-[#435574]">
          Encontre profissionais qualificados com serviços cadastrados na plataforma
        </p>
      </div>
    </div>

    <!-- Mode Toggle -->
    <Tabs v-model="searchMode" class="w-full">
      <TabsList class="grid w-full grid-cols-2 max-w-[400px]">
        <TabsTrigger value="services" class="flex items-center gap-2">
          <Briefcase class="h-4 w-4" />
          Serviços
        </TabsTrigger>
        <TabsTrigger value="professionals" class="flex items-center gap-2">
          <SlidersHorizontal class="h-4 w-4" />
          Profissionais
        </TabsTrigger>
      </TabsList>
    </Tabs>

    <!-- Search & Filters Bar -->
    <Card class="p-4">
      <div class="flex flex-col md:flex-row gap-4">
        <!-- Search -->
        <div class="relative flex-1">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
          <input
            v-model="searchQuery"
            @input="handleSearchInput"
            @keydown.enter="handleSearch"
            type="text"
            placeholder="Buscar por serviço, profissional ou palavra-chave..."
            class="w-full pl-10 pr-10 py-2.5 rounded-lg border border-gray-200 focus:border-[#286AD8] focus:ring-2 focus:ring-[#286AD8]/20 outline-none transition-all text-sm"
          />
          <button
            v-if="searchQuery"
            @click="clearSearchQuery"
            class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
          >
            <X class="h-4 w-4" />
          </button>
        </div>

        <!-- Category Filter -->
        <div class="w-full md:w-56">
          <Select :model-value="selectedCategory" @update:model-value="(val) => handleCategoryChange(val as string)">
            <SelectTrigger>
              <SelectValue :label="categories.find(c => c.value === selectedCategory)?.label" placeholder="Categoria" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="cat in categories" :key="cat.value" :value="cat.value">
                {{ cat.label }}
              </SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div class="w-full md:w-56">
          <Select :model-value="selectedSubcategory" @update:model-value="(val) => handleSubcategoryChange(val as string)">
            <SelectTrigger>
              <SelectValue :label="subcategories.find(c => c.value === selectedSubcategory)?.label" placeholder="Subcategoria" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="subcategory in subcategories" :key="subcategory.value" :value="subcategory.value">
                {{ subcategory.label }}
              </SelectItem>
            </SelectContent>
          </Select>
        </div>

        <!-- Sort -->
        <div class="w-full md:w-48">
          <Select :model-value="sortBy" @update:model-value="(val) => sortBy = val as string">
            <SelectTrigger>
              <SelectValue :label="sortOptions.find(o => o.value === sortBy)?.label" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="recent">Mais recentes</SelectItem>
              <SelectItem value="price-asc">Menor preço</SelectItem>
              <SelectItem value="price-desc">Maior preço</SelectItem>
              <SelectItem value="name-asc">Nome (A-Z)</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <!-- View Mode Toggle -->
        <div class="flex gap-1">
          <Button
            :variant="viewMode === 'grid' ? 'default' : 'outline'"
            size="icon"
            @click="viewMode = 'grid'"
          >
            <Grid3x3 class="h-4 w-4" />
          </Button>
          <Button
            :variant="viewMode === 'list' ? 'default' : 'outline'"
            size="icon"
            @click="viewMode = 'list'"
          >
            <List class="h-4 w-4" />
          </Button>
        </div>
      </div>

      <!-- Active Filters -->
      <div v-if="searchQuery || selectedCategory !== 'all' || selectedSubcategory !== 'all'" class="flex items-center gap-2 mt-3 pt-3 border-t border-gray-100">
        <SlidersHorizontal class="h-4 w-4 text-[#435574]" />
        <span class="text-sm text-[#435574]">Filtros ativos:</span>
        <Badge v-if="searchQuery" variant="secondary" class="gap-1">
          "{{ searchQuery }}"
          <button @click="clearSearchQuery" class="ml-1 hover:text-red-500">
            <X class="h-3 w-3" />
          </button>
        </Badge>
        <Badge v-if="selectedCategory !== 'all'" variant="secondary" class="gap-1">
          {{ categories.find(c => c.value === selectedCategory)?.label }}
          <button @click="clearCategoryFilter" class="ml-1 hover:text-red-500">
            <X class="h-3 w-3" />
          </button>
        </Badge>
        <Badge v-if="selectedSubcategory !== 'all'" variant="secondary" class="gap-1">
          {{ subcategories.find(c => c.value === selectedSubcategory)?.label }}
          <button @click="clearSubcategoryFilter" class="ml-1 hover:text-red-500">
            <X class="h-3 w-3" />
          </button>
        </Badge>
        <Button variant="ghost" size="sm" @click="clearFilters" class="text-xs text-[#286AD8]">
          Limpar tudo
        </Button>
      </div>
    </Card>

    <!-- Results Content -->
    <div v-if="isLoading" class="flex items-center justify-center py-20">
      <div class="text-center">
        <Loader2 class="h-10 w-10 text-[#286AD8] animate-spin mx-auto mb-4" />
        <p class="text-[#435574]">Carregando resultados...</p>
      </div>
    </div>

    <div v-else-if="error" class="text-center py-16">
      <AlertCircle class="h-12 w-12 text-red-400 mx-auto mb-4" />
      <h3 class="text-lg font-semibold text-[#263238] mb-2">Ops, algo deu errado</h3>
      <p class="text-[#435574] mb-4">{{ error }}</p>
      <Button @click="loadServices">
        <RefreshCw class="h-4 w-4 mr-2" /> Tentar novamente
      </Button>
    </div>

    <template v-else-if="searchMode === 'professionals'">
      <div class="flex items-center justify-between text-sm mb-4">
        <p class="text-[#435574]">
          <span class="font-semibold text-[#263238]">{{ totalFreelancers }}</span>
          {{ totalFreelancers === 1 ? 'profissional encontrado' : 'profissionais encontrados' }}
        </p>
      </div>
      <div v-if="freelancerResults.length === 0" class="text-center py-16">
        <div class="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <Search class="h-8 w-8 text-[#435574]" />
        </div>
        <h3 class="text-[#263238] mb-2">Nenhum profissional encontrado</h3>
        <p class="text-[#435574] mb-6">Tente ajustar seus filtros ou fazer uma nova busca</p>
        <Button @click="clearFilters" variant="outline" class="text-primary border-primary">
          Limpar filtros
        </Button>
      </div>

      <div v-else-if="viewMode === 'grid'" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <FreelancerCard
          v-for="freelancer in freelancerResults"
          :key="freelancer.id"
          :freelancer="freelancer"
          :is-favorite="isFavoriteFreelancer(freelancer.id)"
          @view-profile="handleViewProfile"
          @toggle-favorite="toggleFavoriteFreelancer"
        />
      </div>

      <div v-else class="space-y-4">
        <Card
          v-for="freelancer in freelancerResults"
          :key="freelancer.id"
          class="p-5 hover:shadow-md hover:border-[#286AD8]/30 transition-all cursor-pointer group"
          @click="handleViewProfile(freelancer.id)"
        >
          <div class="flex gap-4">
            <div class="flex h-16 w-16 flex-shrink-0 items-center justify-center overflow-hidden rounded-full border border-blue-100 bg-blue-50 text-lg font-bold text-[#286AD8]">
              <img
                v-if="freelancer.avatar"
                :src="freelancer.avatar"
                :alt="freelancer.name"
                class="h-full w-full object-cover"
              />
              <template v-else>
                {{ getInitials(freelancer.name) }}
              </template>
            </div>

            <div class="min-w-0 flex-1">
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <h3 class="font-semibold text-[#263238] group-hover:text-[#286AD8] transition-colors mb-1">
                    {{ freelancer.name }}
                  </h3>
                  <p class="text-sm text-[#435574]">
                    {{ freelancer.servicesCount }} {{ freelancer.servicesCount === 1 ? 'serviço cadastrado' : 'serviços cadastrados' }}
                  </p>
                </div>

                <div class="flex items-center gap-2 flex-shrink-0">
                  <button
                    @click.stop="toggleFavoriteFreelancer(freelancer.id)"
                    :class="`p-2 rounded-full transition-all ${
                      isFavoriteFreelancer(freelancer.id)
                        ? 'text-red-500 bg-red-50'
                        : 'text-gray-400 hover:text-red-500 hover:bg-red-50'
                    }`"
                  >
                    <Heart :class="`h-4 w-4 ${isFavoriteFreelancer(freelancer.id) ? 'fill-current' : ''}`" />
                  </button>
                  <ChevronRight class="h-5 w-5 text-gray-300 group-hover:text-[#286AD8] transition-colors" />
                </div>
              </div>

              <p class="mt-3 text-sm text-[#435574] line-clamp-2">
                Atua em {{ freelancer.category }}.
              </p>

              <div class="mt-3 flex flex-wrap gap-2">
                <Badge
                  v-for="category in freelancer.categories.slice(0, 3)"
                  :key="category"
                  variant="secondary"
                  class="text-xs"
                >
                  {{ category }}
                </Badge>
                <span
                  v-if="freelancer.categories.length > 3"
                  class="self-center text-xs text-[#435574]"
                >
                  +{{ freelancer.categories.length - 3 }}
                </span>
              </div>
            </div>
          </div>
        </Card>
      </div>
    </template>

    <template v-else>
      <div class="flex items-center justify-between text-sm mb-4">
        <p class="text-[#435574]">
          <span class="font-semibold text-[#263238]">{{ filteredServices.length }}</span>
          {{ filteredServices.length === 1 ? 'serviço encontrado' : 'serviços encontrados' }}
        </p>
      </div>

      <div v-if="filteredServices.length === 0" class="text-center py-16">
        <div class="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <Search class="h-8 w-8 text-[#435574]" />
        </div>
        <h3 class="text-[#263238] mb-2">Nenhum serviço encontrado</h3>
        <p class="text-[#435574] mb-6">Tente ajustar seus filtros ou fazer uma nova busca</p>
        <Button @click="clearFilters" variant="outline" class="text-primary border-primary">
          Limpar filtros
        </Button>
      </div>

      <!-- Grid View -->
      <div v-else-if="viewMode === 'grid'" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
        <ServiceCard
          v-for="service in filteredServices"
          :key="service.id"
          :service="service"
          :is-favorite="isFavoriteService(service.id)"
          @navigate="handleViewService"
          @toggle-favorite="toggleFavoriteService"
        />
      </div>

      <!-- List View -->
      <div v-else class="space-y-4">
        <Card
          v-for="service in filteredServices"
          :key="service.id"
          class="p-5 hover:shadow-md hover:border-[#286AD8]/30 transition-all cursor-pointer group"
          @click="handleViewService({ providerId: service.freelancerId, serviceId: service.id })"
        >
          <div class="flex gap-4">
            <!-- Image -->
            <div class="hidden sm:block w-28 h-28 rounded-lg overflow-hidden bg-gradient-to-br from-[#286AD8]/10 to-[#435574]/10 flex-shrink-0">
              <img
                v-if="service.images && service.images.length > 0"
                :src="service.images[0]"
                :alt="service.title"
                class="w-full h-full object-cover"
              />
              <div v-else class="w-full h-full flex items-center justify-center">
                <Briefcase class="h-8 w-8 text-[#286AD8]/30" />
              </div>
            </div>

            <!-- Content -->
            <div class="flex-1 min-w-0">
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <h3 class="font-semibold text-[#263238] group-hover:text-[#286AD8] transition-colors mb-1">
                    {{ service.title }}
                  </h3>
                  <div class="flex items-center gap-3 mb-2">
                    <div class="flex items-center gap-1.5">
                      <div class="w-5 h-5 rounded-full bg-[#286AD8]/10 overflow-hidden flex items-center justify-center text-[8px] font-semibold text-[#286AD8]">
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
                      <span class="text-sm text-[#435574]">{{ service.freelancerName }}</span>
                    </div>
                    <Badge variant="secondary" class="text-xs">{{ service.category }}</Badge>
                  </div>
                </div>

                <div class="flex items-center gap-2 flex-shrink-0">
                  <button
                    @click.stop="toggleFavoriteService(service.id)"
                    :class="`p-2 rounded-full transition-all ${
                      isFavoriteService(service.id)
                        ? 'text-red-500 bg-red-50'
                        : 'text-gray-400 hover:text-red-500 hover:bg-red-50'
                    }`"
                  >
                    <Heart :class="`h-4 w-4 ${isFavoriteService(service.id) ? 'fill-current' : ''}`" />
                  </button>
                  <ChevronRight class="h-5 w-5 text-gray-300 group-hover:text-[#286AD8] transition-colors" />
                </div>
              </div>

              <p class="text-sm text-[#435574] mb-3 line-clamp-2">{{ service.description }}</p>

              <div class="flex items-center gap-1.5 text-xs text-[#435574] mb-3">
                <Star :class="`h-3.5 w-3.5 ${hasServiceRating(service) ? 'fill-yellow-400 text-yellow-400' : 'text-gray-300'}`" />
                <span>{{ formatServiceRatingDisplay(service) }}</span>
              </div>

              <div class="flex items-center gap-4">
                <div class="flex items-center gap-1 text-[#286AD8]">
                  <DollarSign class="h-4 w-4" />
                  <span class="font-bold text-sm">{{ getServicePriceDisplay(service) }}</span>
                </div>
                <div class="flex items-center gap-1 text-[#435574]">
                  <Clock class="h-3.5 w-3.5" />
                  <span class="text-xs">{{ service.deliveryTime }} {{ service.deliveryUnit === 'HOURS' ? 'hora(s)' : service.deliveryUnit === 'DAYS' ? 'dia(s)' : 'semana(s)' }}</span>
                </div>
              </div>
            </div>
          </div>
        </Card>
      </div>
    </template>
  </div>
</template>
