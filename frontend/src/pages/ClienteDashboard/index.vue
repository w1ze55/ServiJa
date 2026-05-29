<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import {
  ArrowRight,
  Briefcase,
  ChevronLeft,
  ChevronRight,
  Compass,
  FolderKanban,
  Heart,
  Loader2,
  MapPin,
  RefreshCw,
  Sparkles,
  ClipboardList,
  User,
} from 'lucide-vue-next';
import MainLayout from '@/components/layout/MainLayout.vue';
import Card from '@/components/ui/card.vue';
import Button from '@/components/ui/button.vue';
import Badge from '@/components/ui/badge.vue';
import Tabs from '@/components/ui/tabs.vue';
import TabsList from '@/components/ui/tabs-list.vue';
import TabsTrigger from '@/components/ui/tabs-trigger.vue';
import FreelancerCard from '@/components/freelancer/FreelancerCard.vue';
import ServiceCard from '@/components/service/ServiceCard.vue';
import { clientHomeService, type ClientHomeResponse, type ClientHomeFreelancerItem, type ClientHomeServiceItem } from '@/api/services/clientHomeService';
import { useNavigation } from '@/utils/navigation';
import { useFavoritesStore } from '@/stores/favorites';
import { formatApiDate, formatTimeAgoFromApiDate } from '@/utils/apiDate';
import { getStatusLabel } from '@/utils/statusTranslations';
import { toast } from '@/utils/toast';

const router = useRouter();
const { navigate, logout } = useNavigation();
const favoritesStore = useFavoritesStore();

const user = ref<any>(null);
const homeData = ref<ClientHomeResponse | null>(null);
const isLoading = ref(false);
const error = ref('');
const favoritesTab = ref<'services' | 'professionals'>('services');
const featuredProfessionalsPage = ref(0);
const featuredServicesPage = ref(0);

const CAROUSEL_ITEMS_PER_PAGE = 3;

const regionLabel = computed(() => homeData.value?.region?.displayName || 'toda a plataforma');
const isRegionalized = computed(() => Boolean(homeData.value?.region?.localized));
const interestCategories = computed(() => homeData.value?.interestCategories || []);
const recentRequests = computed(() => homeData.value?.recentRequests || []);
const activeProjects = computed(() => homeData.value?.activeProjects || []);
const favoriteServices = computed(() => homeData.value?.favorites?.services || []);
const favoriteProfessionals = computed(() => homeData.value?.favorites?.professionals || []);
const featuredServices = computed(() => homeData.value?.featuredServices || []);
const featuredProfessionals = computed(() => homeData.value?.featuredProfessionals || []);
const totalFavorites = computed(
  () => (homeData.value?.favorites?.serviceCount || 0) + (homeData.value?.favorites?.professionalCount || 0)
);

const favoritePreviewItems = computed(() =>
  favoritesTab.value === 'services' ? favoriteServices.value : favoriteProfessionals.value
);

const chunkItems = <T,>(items: T[], size: number) => {
  const pages: T[][] = [];
  for (let index = 0; index < items.length; index += size) {
    pages.push(items.slice(index, index + size));
  }
  return pages;
};

const featuredProfessionalsPages = computed(() =>
  chunkItems(featuredProfessionals.value, CAROUSEL_ITEMS_PER_PAGE)
);

const featuredServicesPages = computed(() =>
  chunkItems(featuredServices.value, CAROUSEL_ITEMS_PER_PAGE)
);

const featuredProfessionalsTotalPages = computed(() => featuredProfessionalsPages.value.length);
const featuredServicesTotalPages = computed(() => featuredServicesPages.value.length);

const clampCarouselPage = (page: number, totalPages: number) => {
  if (totalPages <= 0) {
    return 0;
  }
  return Math.min(page, totalPages - 1);
};

watch(featuredProfessionalsTotalPages, totalPages => {
  featuredProfessionalsPage.value = clampCarouselPage(featuredProfessionalsPage.value, totalPages);
});

watch(featuredServicesTotalPages, totalPages => {
  featuredServicesPage.value = clampCarouselPage(featuredServicesPage.value, totalPages);
});

const handleNavigate = (page: string, params?: Record<string, any>) => {
  navigate(page, 'contractor', params);
};

const handleLogout = () => {
  logout();
};

const handleSearch = (query: string) => {
  router.push({ path: '/search', query: { q: query } });
};

const loadHome = async (showRefreshToast = false) => {
  isLoading.value = true;
  error.value = '';

  try {
    homeData.value = await clientHomeService.getHome();
    if (showRefreshToast) {
      toast.success('Página inicial atualizada.');
    }
  } catch (err: any) {
    console.error('Erro ao carregar a home do cliente:', err);
    error.value = err.response?.data?.message || 'Não foi possível carregar sua página inicial.';
    toast.error(error.value);
  } finally {
    isLoading.value = false;
  }
};

const ensureFavoritesStore = async () => {
  if (!favoritesStore.initialized) {
    await favoritesStore.fetchFavorites();
  }
};

const handleRefresh = async () => {
  await Promise.all([loadHome(true), ensureFavoritesStore()]);
};

const openRequestDetail = (requestId: number) => {
  router.push({ path: '/cliente-request-detail', query: { requestId } });
};

const openProjectDetail = (requestId: number) => {
  router.push({ path: '/cliente-project-detail', query: { requestId } });
};

const handleViewProfile = (providerId: string | number) => {
  router.push({
    name: 'provider-profile',
    params: { providerId: Number(providerId) },
  });
};

const handleViewService = (params: { providerId: number; serviceId: number }) => {
  router.push({
    name: 'service-detail',
    params: {
      providerId: params.providerId,
      serviceId: params.serviceId,
    },
  });
};

const handleToggleFavoriteService = async (id: number) => {
  await favoritesStore.toggleFavoriteService(id);
  await loadHome();
};

const handleToggleFavoriteFreelancer = async (id: string | number) => {
  await favoritesStore.toggleFavoriteFreelancer(id);
  await loadHome();
};

const goToPreviousFeaturedProfessionalsPage = () => {
  featuredProfessionalsPage.value = Math.max(featuredProfessionalsPage.value - 1, 0);
};

const goToNextFeaturedProfessionalsPage = () => {
  featuredProfessionalsPage.value = Math.min(
    featuredProfessionalsPage.value + 1,
    Math.max(featuredProfessionalsTotalPages.value - 1, 0)
  );
};

const goToPreviousFeaturedServicesPage = () => {
  featuredServicesPage.value = Math.max(featuredServicesPage.value - 1, 0);
};

const goToNextFeaturedServicesPage = () => {
  featuredServicesPage.value = Math.min(
    featuredServicesPage.value + 1,
    Math.max(featuredServicesTotalPages.value - 1, 0)
  );
};

const mapServiceForCard = (service: ClientHomeServiceItem) => ({
  ...service,
  category: service.categoryLabel || service.category || 'Serviço',
  freelancerProfilePhoto: service.freelancerProfilePhoto || undefined,
  images: service.images || [],
  tags: service.tags || [],
});

const mapFreelancerForCard = (freelancer: ClientHomeFreelancerItem) => ({
  id: freelancer.id,
  name: freelancer.name,
  avatar: freelancer.avatar || undefined,
  professionalTitle: freelancer.professionalTitle || undefined,
  category: freelancer.category || 'Profissional',
  location: freelancer.location || undefined,
  rating: Number(freelancer.rating || 0),
  reviewCount: freelancer.totalReviews || 0,
  isAvailable: freelancer.available ?? true,
});

const getRequestTitle = (item: { serviceName: string; categoryLabel?: string | null; category?: string | null }) =>
  item.categoryLabel || item.category || 'Solicitação';

const getStatusClasses = (status: string) => {
  const normalizedStatus = status?.toUpperCase();
  if (normalizedStatus === 'PENDING' || normalizedStatus === 'SENT') {
    return 'bg-amber-100 text-amber-700';
  }
  if (normalizedStatus === 'NEGOTIATING' || normalizedStatus === 'ACCEPTED') {
    return 'bg-sky-100 text-sky-700';
  }
  if (normalizedStatus === 'IN_PROGRESS') {
    return 'bg-blue-100 text-blue-700';
  }
  if (normalizedStatus === 'DELIVERED') {
    return 'bg-violet-100 text-violet-700';
  }
  if (normalizedStatus === 'COMPLETED') {
    return 'bg-emerald-100 text-emerald-700';
  }
  if (normalizedStatus === 'REJECTED' || normalizedStatus === 'CANCELLED') {
    return 'bg-rose-100 text-rose-700';
  }
  return 'bg-gray-100 text-gray-700';
};

onMounted(async () => {
  const userData = localStorage.getItem('user');

  if (!userData) {
    router.push('/login');
    return;
  }

  user.value = JSON.parse(userData);

  if (user.value.type !== 'contractor') {
    router.push('/freelancer-dashboard');
    return;
  }

  await Promise.all([loadHome(), ensureFavoritesStore()]);
});
</script>

<template>
  <MainLayout
    v-if="user"
    :user="user"
    current-page="home"
    :on-navigate="handleNavigate"
    :on-logout="handleLogout"
    :on-search="handleSearch"
  >
    <div class="mx-auto max-w-7xl space-y-8">
      <section class="rounded-[28px] border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div class="relative flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
          <div class="max-w-3xl space-y-4">
            <div class="flex flex-wrap items-center gap-3">
              <Badge variant="outline" class="bg-[#286AD8] text-white hover:bg-[#286AD8]">
                <MapPin class="mr-1 h-3.5 w-3.5" />
                {{ isRegionalized ? regionLabel : 'Sem região definida' }}
              </Badge>
            </div>

            <div>
              <h1 class="mb-2 text-3xl font-bold text-[#263238] sm:text-4xl">
                Profissionais e serviços alinhados ao seu momento
              </h1>
              <p class="max-w-2xl text-sm leading-6 text-[#435574] sm:text-base">
                Acompanhe o que já está em andamento, retome seus favoritos e descubra novas opções
                {{ isRegionalized ? `em ${regionLabel}` : 'na plataforma' }} com base nas suas contratações recentes.
              </p>
            </div>

            <div v-if="interestCategories.length" class="flex flex-wrap gap-2">
              <Badge
                v-for="category in interestCategories"
                :key="category"
                variant="outline"
                class="border-[#286AD8]/15 bg-white/85 text-[#286AD8]"
              >
                {{ category }}
              </Badge>
            </div>
          </div>

          <div class="flex flex-col gap-3 sm:flex-row">
            <Button class="bg-[#286AD8] hover:bg-[#1f57b5]" @click="handleNavigate('explore')">
              <Compass class="mr-2 h-4 w-4" />
              Explorar serviços
            </Button>
          </div>
        </div>
      </section>

      <div v-if="isLoading && !homeData" class="rounded-3xl border bg-white p-14 text-center">
        <Loader2 class="mx-auto mb-4 h-10 w-10 animate-spin text-[#286AD8]" />
        <p class="text-[#435574]">Carregando sua página inicial...</p>
      </div>

      <div v-else-if="error && !homeData" class="rounded-3xl border border-red-200 bg-red-50 p-8 text-center">
        <h2 class="mb-2 text-xl font-semibold text-[#263238]">Não foi possível carregar a home</h2>
        <p class="mx-auto mb-6 max-w-2xl text-sm text-[#435574]">
          {{ error }}
        </p>
        <Button class="bg-[#286AD8] hover:bg-[#1f57b5]" @click="loadHome">
          Tentar novamente
        </Button>
      </div>

      <template v-else-if="homeData">
        <div class="grid gap-6 xl:grid-cols-3">
          <Card class="rounded-3xl border-0 bg-white p-6 shadow-sm">
            <div class="mb-5 flex items-start justify-between gap-4">
              <div>
                <div class="mb-2 flex items-center gap-2">
                  <div class="rounded-2xl bg-[#286AD8]/10 p-2 text-[#286AD8]">
                    <ClipboardList class="h-5 w-5" />
                  </div>
                  <p class="text-sm font-semibold uppercase tracking-[0.12em] text-[#435574]">
                    Últimas solicitações
                  </p>
                </div>
              </div>
              <Button variant="ghost" class="text-[#286AD8]" @click="handleNavigate('my-requests')">
                Ver todas
                <ArrowRight class="ml-2 h-4 w-4" />
              </Button>
            </div>

            <div v-if="recentRequests.length" class="space-y-3">
              <button
                v-for="request in recentRequests"
                :key="request.id"
                class="w-full rounded-2xl border border-slate-100 p-4 text-left transition-colors hover:border-[#286AD8]/25 hover:bg-slate-50"
                @click="openRequestDetail(request.id)"
              >
                <div class="mb-2 flex items-start justify-between gap-3">
                  <div class="min-w-0">
                    <p class="truncate font-semibold text-[#263238]">{{ request.serviceName }}</p>
                    <p class="truncate text-xs text-[#435574]">{{ getRequestTitle(request) }}</p>
                  </div>
                  <Badge :class="getStatusClasses(request.status)">
                    {{ getStatusLabel(request.status) }}
                  </Badge>
                </div>
                <div class="mt-3 flex items-center justify-between gap-3 text-xs text-[#435574]">
                  <span>{{ formatApiDate(request.createdAt, { day: '2-digit', month: '2-digit', year: 'numeric' }) }}</span>
                  <span>{{ formatTimeAgoFromApiDate(request.updatedAt || request.createdAt) }}</span>
                </div>
              </button>
            </div>

            <div v-else class="rounded-2xl border border-dashed border-slate-200 bg-slate-50/70 p-6 text-center">
              <p class="mb-2 font-medium text-[#263238]">Nenhuma solicitação recente</p>
              <p class="mb-4 text-sm text-[#435574]">
                Quando você abrir novas solicitações, elas aparecerão aqui.
              </p>
              <Button variant="outline" @click="handleNavigate('explore')">
                Buscar profissionais
              </Button>
            </div>
          </Card>

          <Card class="rounded-3xl border-0 bg-white p-6 shadow-sm">
            <div class="mb-5 flex items-start justify-between gap-4">
              <div>
                <div class="mb-2 flex items-center gap-2">
                  <div class="rounded-2xl bg-emerald-500/10 p-2 text-emerald-600">
                    <FolderKanban class="h-5 w-5" />
                  </div>
                  <p class="text-sm font-semibold uppercase tracking-[0.12em] text-[#435574]">
                    Projetos em andamento
                  </p>
                </div>
              </div>
              <Button variant="ghost" class="text-[#286AD8]" @click="handleNavigate('cliente-projects')">
                Ver todos
                <ArrowRight class="ml-2 h-4 w-4" />
              </Button>
            </div>

            <div v-if="activeProjects.length" class="space-y-3">
              <button
                v-for="project in activeProjects"
                :key="project.id"
                class="w-full rounded-2xl border border-slate-100 p-4 text-left transition-colors hover:border-[#286AD8]/25 hover:bg-slate-50"
                @click="openProjectDetail(project.id)"
              >
                <div class="mb-2 flex items-start justify-between gap-3">
                  <div class="min-w-0">
                    <p class="truncate font-semibold text-[#263238]">{{ project.serviceName }}</p>
                    <p class="truncate text-xs text-[#435574]">
                      {{ project.freelancerName || 'Profissional definido após a contratação' }}
                    </p>
                  </div>
                  <Badge :class="getStatusClasses(project.status)">
                    {{ getStatusLabel(project.status) }}
                  </Badge>
                </div>
                <div class="mt-3 flex items-center justify-between gap-3 text-xs text-[#435574]">
                  <span>{{ formatApiDate(project.createdAt, { day: '2-digit', month: '2-digit', year: 'numeric' }) }}</span>
                  <span>{{ formatTimeAgoFromApiDate(project.updatedAt || project.createdAt) }}</span>
                </div>
              </button>
            </div>

            <div v-else class="rounded-2xl border border-dashed border-slate-200 bg-slate-50/70 p-6 text-center">
              <p class="mb-2 font-medium text-[#263238]">Nenhum projeto em andamento</p>
              <p class="mb-4 text-sm text-[#435574]">
                Seus projetos ativos aparecerão aqui assim que uma proposta for aceita.
              </p>
              <Button variant="outline" @click="handleNavigate('my-requests')">
                Ver solicitações
              </Button>
            </div>
          </Card>

          <Card class="rounded-3xl border-0 bg-white p-6 shadow-sm">
            <div class="mb-5 flex items-start justify-between gap-4">
              <div>
                <div class="mb-2 flex items-center gap-2">
                  <div class="rounded-2xl bg-rose-500/10 p-2 text-rose-500">
                    <Heart class="h-5 w-5" />
                  </div>
                  <p class="text-sm font-semibold uppercase tracking-[0.12em] text-[#435574]">
                    Favoritos
                  </p>
                </div>
              </div>
              <div class="flex items-center gap-2 sm:gap-3">
                <Tabs v-model="favoritesTab" class="shrink-0">
                  <TabsList class="grid h-10 grid-cols-2 rounded-2xl bg-slate-100 p-1">
                    <TabsTrigger
                      value="services"
                      class="h-8 w-8 rounded-xl p-0 text-[#435574] data-[state=active]:text-[#286AD8]"
                      aria-label="Serviços favoritos"
                      title="Serviços"
                    >
                      <Briefcase class="h-4 w-4" />
                      <span class="sr-only">Serviços</span>
                    </TabsTrigger>
                    <TabsTrigger
                      value="professionals"
                      class="h-8 w-8 rounded-xl p-0 text-[#435574] data-[state=active]:text-[#286AD8]"
                      aria-label="Profissionais favoritos"
                      title="Profissionais"
                    >
                      <User class="h-4 w-4" />
                      <span class="sr-only">Profissionais</span>
                    </TabsTrigger>
                  </TabsList>
                </Tabs>

                <Button variant="ghost" class="text-[#286AD8]" @click="handleNavigate('favorites')">
                  Ver tudo
                  <ArrowRight class="ml-2 h-4 w-4" />
                </Button>
              </div>
            </div>

            <div v-if="favoritePreviewItems.length" class="space-y-3">
              <template v-if="favoritesTab === 'services'">
                <button
                  v-for="service in favoriteServices"
                  :key="service.id"
                  class="flex min-h-[112px] w-full flex-col justify-between rounded-2xl border border-slate-100 p-4 text-left transition-colors hover:border-[#286AD8]/25 hover:bg-slate-50"
                  @click="handleViewService({ providerId: service.freelancerId, serviceId: service.id })"
                >
                  <div class="mb-2 flex items-start justify-between gap-3">
                    <div class="min-w-0">
                      <p class="truncate font-semibold text-[#263238]">{{ service.title }}</p>
                      <p class="truncate text-xs text-[#435574]">{{ service.freelancerName }}</p>
                    </div>
                    <button
                      class="rounded-full bg-rose-50 p-2 text-rose-500 transition-colors hover:bg-rose-100"
                      @click.stop="handleToggleFavoriteService(service.id)"
                    >
                      <Heart class="h-4 w-4 fill-current" />
                    </button>
                  </div>
                  <p class="line-clamp-2 text-sm text-[#435574]">{{ service.description }}</p>
                </button>
              </template>

              <template v-else>
                <button
                  v-for="freelancer in favoriteProfessionals"
                  :key="freelancer.id"
                  class="flex min-h-[112px] w-full flex-col justify-between rounded-2xl border border-slate-100 p-4 text-left transition-colors hover:border-[#286AD8]/25 hover:bg-slate-50"
                  @click="handleViewProfile(freelancer.id)"
                >
                  <div class="mb-2 flex items-start justify-between gap-3">
                    <div class="min-w-0">
                      <p class="truncate font-semibold text-[#263238]">{{ freelancer.name }}</p>
                      <p class="truncate text-xs text-[#435574]">
                        {{ freelancer.professionalTitle || freelancer.category || 'Profissional' }}
                      </p>
                    </div>
                    <button
                      class="rounded-full bg-rose-50 p-2 text-rose-500 transition-colors hover:bg-rose-100"
                      @click.stop="handleToggleFavoriteFreelancer(freelancer.id)"
                    >
                      <Heart class="h-4 w-4 fill-current" />
                    </button>
                  </div>
                  <div class="mt-3 flex items-center justify-between gap-3 text-xs text-[#435574]">
                    <span class="truncate">{{ freelancer.location || 'Localização não informada' }}</span>
                    <span>{{ Number(freelancer.rating || 0).toFixed(1) }} ★</span>
                  </div>
                </button>
              </template>
            </div>

            <div v-else class="mt-4 rounded-2xl border border-dashed border-slate-200 bg-slate-50/70 p-6 text-center">
              <p class="mb-2 font-medium text-[#263238]">Nenhum favorito salvo</p>
              <p class="mb-4 text-sm text-[#435574]">
                Salve serviços e profissionais para acompanhar de perto depois.
              </p>
              <Button variant="outline" @click="handleNavigate('explore')">
                Explorar agora
              </Button>
            </div>
          </Card>
        </div>

        <section class="space-y-4">
          <div class="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <div class="flex items-center gap-2">
                <User class="h-5 w-5 text-[#286AD8]" />
                <h2 class="text-2xl font-bold text-[#263238]">
                  {{ isRegionalized ? `Profissionais em destaque em ${regionLabel}` : 'Profissionais em destaque na plataforma' }}
                </h2>
              </div>
              <p class="mt-1 text-sm text-[#435574]">
                Priorizados pelas categorias contratadas recentemente e pela qualidade das avaliações mais atuais.
              </p>
            </div>
            <div class="flex items-center gap-3 self-start sm:self-auto">
              <div class="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="icon"
                  class="bg-white"
                  :disabled="featuredProfessionalsPage === 0"
                  @click="goToPreviousFeaturedProfessionalsPage"
                >
                  <ChevronLeft class="h-4 w-4" />
                </Button>
                <Button
                  variant="outline"
                  size="icon"
                  class="bg-white"
                  :disabled="featuredProfessionalsPage >= featuredProfessionalsTotalPages - 1"
                  @click="goToNextFeaturedProfessionalsPage"
                >
                  <ChevronRight class="h-4 w-4" />
                </Button>
              </div>
              <Button variant="outline" class="bg-white" @click="handleNavigate('explore')">
                Ver mais opções
              </Button>
            </div>
          </div>

          <div v-if="featuredProfessionals.length" class="space-y-3">
            <div class="overflow-hidden">
              <div
                class="flex transition-transform duration-300 ease-out"
                :style="{ transform: `translateX(-${featuredProfessionalsPage * 100}%)` }"
              >
                <div
                  v-for="(professionalPage, pageIndex) in featuredProfessionalsPages"
                  :key="`featured-professionals-page-${pageIndex}`"
                  class="w-full shrink-0"
                >
                  <div class="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3">
                    <FreelancerCard
                      v-for="freelancer in professionalPage"
                      :key="freelancer.id"
                      :freelancer="mapFreelancerForCard(freelancer)"
                      :is-favorite="favoritesStore.isFavoriteFreelancer(freelancer.id)"
                      @view-profile="handleViewProfile"
                      @toggle-favorite="handleToggleFavoriteFreelancer"
                    />
                  </div>
                </div>
              </div>
            </div>
            <p v-if="featuredProfessionalsTotalPages > 1" class="text-sm text-[#435574]">
              {{ featuredProfessionalsPage + 1 }} / {{ featuredProfessionalsTotalPages }}
            </p>
          </div>

          <div v-else class="rounded-3xl border border-dashed border-slate-200 bg-white p-8 text-center">
            <p class="mb-2 text-lg font-semibold text-[#263238]">Ainda não há profissionais em destaque</p>
            <p class="mb-5 text-sm text-[#435574]">
              Assim que houver profissionais bem avaliados para o seu contexto, eles aparecerão aqui.
            </p>
            <Button variant="outline" @click="handleNavigate('explore')">
              Explorar profissionais
            </Button>
          </div>
        </section>

        <section class="space-y-4">
          <div class="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <div class="flex items-center gap-2">
                <Briefcase class="h-5 w-5 text-[#286AD8]" />
                <h2 class="text-2xl font-bold text-[#263238]">
                  {{ isRegionalized ? `Serviços em destaque em ${regionLabel}` : 'Serviços em destaque na plataforma' }}
                </h2>
              </div>
              <p class="mt-1 text-sm text-[#435574]">
                Seleção de serviços ativos com melhor desempenho recente e aderência ao seu histórico.
              </p>
            </div>
            <div class="flex items-center gap-3 self-start sm:self-auto">
              <div class="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="icon"
                  class="bg-white"
                  :disabled="featuredServicesPage === 0"
                  @click="goToPreviousFeaturedServicesPage"
                >
                  <ChevronLeft class="h-4 w-4" />
                </Button>
                <Button
                  variant="outline"
                  size="icon"
                  class="bg-white"
                  :disabled="featuredServicesPage >= featuredServicesTotalPages - 1"
                  @click="goToNextFeaturedServicesPage"
                >
                  <ChevronRight class="h-4 w-4" />
                </Button>
              </div>
              <Button variant="outline" class="bg-white" @click="handleNavigate('explore')">
                Abrir catálogo
              </Button>
            </div>
          </div>

          <div v-if="featuredServices.length" class="space-y-3">
            <div class="overflow-hidden">
              <div
                class="flex transition-transform duration-300 ease-out"
                :style="{ transform: `translateX(-${featuredServicesPage * 100}%)` }"
              >
                <div
                  v-for="(servicePage, pageIndex) in featuredServicesPages"
                  :key="`featured-services-page-${pageIndex}`"
                  class="w-full shrink-0"
                >
                  <div class="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3">
                    <ServiceCard
                      v-for="service in servicePage"
                      :key="service.id"
                      :service="mapServiceForCard(service)"
                      :is-favorite="favoritesStore.isFavoriteService(service.id)"
                      @navigate="handleViewService"
                      @toggle-favorite="handleToggleFavoriteService"
                    />
                  </div>
                </div>
              </div>
            </div>
            <p v-if="featuredServicesTotalPages > 1" class="text-sm text-[#435574]">
              {{ featuredServicesPage + 1 }} / {{ featuredServicesTotalPages }}
            </p>
          </div>

          <div v-else class="rounded-3xl border border-dashed border-slate-200 bg-white p-8 text-center">
            <p class="mb-2 text-lg font-semibold text-[#263238]">Nenhum serviço em destaque por enquanto</p>
            <p class="mb-5 text-sm text-[#435574]">
              Continue explorando a plataforma para abrir mais contexto de recomendação.
            </p>
            <Button variant="outline" @click="handleNavigate('explore')">
              Explorar serviços
            </Button>
          </div>
        </section>
      </template>
    </div>
  </MainLayout>
</template>
