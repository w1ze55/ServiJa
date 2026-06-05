<script setup lang="ts">
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
  type Component,
} from "vue";
import { useRouter } from "vue-router";
import {
  Calculator,
  Camera,
  CarFront,
  Church,
  ClipboardList,
  Code2,
  DraftingCompass,
  Factory,
  Gift,
  GraduationCap,
  HandHeart,
  HeartPulse,
  Languages,
  Laptop,
  Lightbulb,
  Megaphone,
  Palette,
  PartyPopper,
  PawPrint,
  Plane,
  Scale,
  Scissors,
  Shield,
  Shirt,
  Trees,
  Truck,
  UtensilsCrossed,
  ArrowRight,
  Briefcase,
  ChevronLeft,
  ChevronRight,
  Compass,
  Hammer,
  Loader2,
  MapPin,
  Package,
  Search,
  ShieldCheck,
  Snowflake,
  Sparkles,
  User,
  Wrench,
  Zap,
} from "lucide-vue-next";
import MainLayout from "@/components/layout/MainLayout.vue";
import Button from "@/components/ui/button.vue";
import Badge from "@/components/ui/badge.vue";
import FreelancerCard from "@/components/freelancer/FreelancerCard.vue";
import ServiceCard from "@/components/service/ServiceCard.vue";
import {
  clientHomeService,
  type ClientHomeResponse,
  type ClientHomeFreelancerItem,
  type ClientHomeServiceItem,
} from "@/api/services/clientHomeService";
import { useNavigation } from "@/utils/navigation";
import { useFavoritesStore } from "@/stores/favorites";
import { useCategoryCatalogStore } from "@/stores/categoryCatalogStore";
import { toast } from "@/utils/toast";

const router = useRouter();
const { navigate, logout } = useNavigation();
const favoritesStore = useFavoritesStore();
const categoryCatalogStore = useCategoryCatalogStore();

const user = ref<any>(null);
const homeData = ref<ClientHomeResponse | null>(null);
const isLoading = ref(false);
const error = ref("");
const categoryRailViewportRef = ref<HTMLElement | null>(null);
const categoryRailTrackRef = ref<HTMLElement | null>(null);
const categoryRailOffset = ref(0);
const categoryRailContentWidth = ref(0);
const categoryRailViewportWidth = ref(0);
const isCategoryRailLoopActive = ref(false);
const isCategoryRailDragging = ref(false);
const weeklyServicesPage = ref(0);
const featuredProfessionalsPage = ref(0);
const featuredServicesPage = ref(0);

const CATEGORY_RAIL_DRAG_THRESHOLD = 6;
const CATEGORY_RAIL_LOOP_COPIES = 3;
const SERVICE_ITEMS_PER_PAGE = 3;
let categoryRailDragStartX = 0;
let categoryRailDragStartOffset = 0;
let activeCategoryRailPointerId: number | null = null;
let suppressCategoryRailClick = false;

const regionLabel = computed(
  () => homeData.value?.region?.displayName || "toda a plataforma",
);
const isRegionalized = computed(() =>
  Boolean(homeData.value?.region?.localized),
);
const interestCategories = computed(
  () => homeData.value?.interestCategories || [],
);
const weeklyMostSearchedServices = computed(
  () => homeData.value?.weeklyMostSearchedServices || [],
);
const featuredServices = computed(() => homeData.value?.featuredServices || []);
const featuredProfessionals = computed(
  () => homeData.value?.featuredProfessionals || [],
);

const categoryIconBySlug: Record<string, Component> = {
  "reformas-e-construcao": Hammer,
  "instalacao-e-manutencao-residencial": Wrench,
  "limpeza-e-servicos-domesticos": Sparkles,
  "mudancas-e-fretes": Truck,
  "jardinagem-e-areas-externas": Trees,
  "seguranca-residencial-e-empresarial": Shield,
  automotivo: CarFront,
  "tecnologia-e-informatica": Laptop,
  "desenvolvimento-e-ti": Code2,
  "design-e-criativo": Palette,
  "marketing-e-vendas": Megaphone,
  "audiovisual-e-conteudo": Camera,
  "escrita-traducao-e-idiomas": Languages,
  "administrativo-e-suporte-profissional": ClipboardList,
  "juridico-e-documentacao": Scale,
  "financas-e-contabilidade": Calculator,
  "arquitetura-engenharia-e-interiores": DraftingCompass,
  "aulas-e-educacao": GraduationCap,
  "consultoria-e-mentoria": Lightbulb,
  "saude-e-bem-estar": HeartPulse,
  "beleza-e-estetica": Scissors,
  "cuidados-pessoais-e-assistencia": HandHeart,
  "eventos-e-festas": PartyPopper,
  "gastronomia-e-alimentacao": UtensilsCrossed,
  "moda-e-costura": Shirt,
  pets: PawPrint,
  "servicos-empresariais": Briefcase,
  "servicos-industriais-e-tecnicos": Factory,
  "turismo-e-viagens": Plane,
  "servicos-religiosos-e-cerimoniais": Church,
  "personalizados-e-sob-demanda": Gift,
  outros: Package,
};

const categoryIconByKeyword: Array<{ keywords: string[]; icon: Component }> = [
  { keywords: ["eletric"], icon: Zap },
  { keywords: ["encan", "hidraul"], icon: Wrench },
  { keywords: ["limpeza", "domest"], icon: Sparkles },
  { keywords: ["foto", "video", "audiovisual", "conteudo"], icon: Camera },
  { keywords: ["design", "criativo", "ilustr", "logo"], icon: Palette },
  { keywords: ["beleza", "estetica", "maqui", "cabelo"], icon: Scissors },
  { keywords: ["constr", "pedreiro", "obra"], icon: Hammer },
  { keywords: ["jardin", "paisag"], icon: Trees },
  { keywords: ["mudanc", "frete", "carreto", "transporte"], icon: Truck },
  { keywords: ["marcen", "moveis"], icon: Package },
  {
    keywords: ["ar-condicionado", "ar condicionado", "refriger"],
    icon: Snowflake,
  },
  { keywords: ["advoc", "jurid", "contrato"], icon: Scale },
  { keywords: ["contab", "finance", "tribut"], icon: Calculator },
  { keywords: ["educ", "aula", "curso", "idioma"], icon: GraduationCap },
  { keywords: ["saude", "bem-estar", "psicolo", "nutri"], icon: HeartPulse },
  { keywords: ["pet", "animais"], icon: PawPrint },
  { keywords: ["dev", "software", "web", "mobile", "api"], icon: Code2 },
  {
    keywords: ["tecnologia", "informatica", "computador", "wi-fi"],
    icon: Laptop,
  },
  { keywords: ["marketing", "vendas", "branding", "trafego"], icon: Megaphone },
  { keywords: ["consult", "mentoria"], icon: Lightbulb },
  { keywords: ["arquitet", "engenhar", "interiores"], icon: DraftingCompass },
  { keywords: ["event", "festa", "cerimonial"], icon: PartyPopper },
  { keywords: ["gastron", "aliment", "chef", "buffet"], icon: UtensilsCrossed },
];

const chunkItems = <T,>(items: T[], size: number) => {
  const pages: T[][] = [];

  for (let index = 0; index < items.length; index += size) {
    pages.push(items.slice(index, index + size));
  }

  return pages;
};

const clampCarouselPage = (page: number, totalPages: number) => {
  if (totalPages <= 0) {
    return 0;
  }

  return Math.min(page, totalPages - 1);
};

const getCategoryIcon = (slug?: string, name?: string) => {
  if (slug && categoryIconBySlug[slug]) {
    return categoryIconBySlug[slug];
  }

  const searchableValue = `${slug || ""} ${name || ""}`.toLowerCase();
  const mappedEntry = categoryIconByKeyword.find((entry) =>
    entry.keywords.some((keyword) => searchableValue.includes(keyword)),
  );

  return mappedEntry?.icon || Briefcase;
};

const commercialCategories = computed(() =>
  categoryCatalogStore.activePublicCategories.map((category) => ({
    ...category,
    icon: getCategoryIcon(category.slug, category.name),
  })),
);

const renderedCommercialCategories = computed(() => {
  if (!commercialCategories.value.length) {
    return [];
  }

  if (!isCategoryRailLoopActive.value) {
    return commercialCategories.value.map((category) => ({
      ...category,
      loopKey: `${category.id}-0`,
    }));
  }

  return Array.from({ length: CATEGORY_RAIL_LOOP_COPIES }, (_, copyIndex) =>
    commercialCategories.value.map((category) => ({
      ...category,
      loopKey: `${category.id}-${copyIndex}`,
    })),
  ).flat();
});

const categoryRailMaxOffset = computed(() =>
  Math.max(categoryRailContentWidth.value - categoryRailViewportWidth.value, 0),
);
const categoryRailShouldShowControls = computed(
  () =>
    commercialCategories.value.length > 1 &&
    (isCategoryRailLoopActive.value || categoryRailMaxOffset.value > 0),
);
const categoryRailTrackStyle = computed(() => {
  if (!categoryRailContentWidth.value) {
    return {
      transform: "translate3d(0px, 0px, 0px)",
    };
  }

  if (!isCategoryRailLoopActive.value) {
    const clampedOffset = Math.min(
      Math.max(categoryRailOffset.value, 0),
      categoryRailMaxOffset.value,
    );

    return {
      transform: `translate3d(-${clampedOffset}px, 0px, 0px)`,
    };
  }

  const normalizedOffset =
    ((categoryRailOffset.value % categoryRailContentWidth.value) +
      categoryRailContentWidth.value) %
    categoryRailContentWidth.value;

  return {
    transform: `translate3d(-${categoryRailContentWidth.value + normalizedOffset}px, 0px, 0px)`,
  };
});

const weeklyServicesPages = computed(() =>
  chunkItems(weeklyMostSearchedServices.value, SERVICE_ITEMS_PER_PAGE),
);
const featuredProfessionalsPages = computed(() =>
  chunkItems(featuredProfessionals.value, SERVICE_ITEMS_PER_PAGE),
);
const featuredServicesPages = computed(() =>
  chunkItems(featuredServices.value, SERVICE_ITEMS_PER_PAGE),
);

const weeklyServicesTotalPages = computed(
  () => weeklyServicesPages.value.length,
);
const featuredProfessionalsTotalPages = computed(
  () => featuredProfessionalsPages.value.length,
);
const featuredServicesTotalPages = computed(
  () => featuredServicesPages.value.length,
);

const heroHighlights = computed(() => [
  {
    label: "Categorias ativas",
    value: String(commercialCategories.value.length || 0),
    icon: Compass,
  },
  {
    label: "Serviços em alta",
    value: String(weeklyMostSearchedServices.value.length || 0),
    icon: Search,
  },
  {
    label: "Curadoria confiável",
    value: "Semanal",
    icon: ShieldCheck,
  },
]);

watch(weeklyServicesTotalPages, (totalPages) => {
  weeklyServicesPage.value = clampCarouselPage(
    weeklyServicesPage.value,
    totalPages,
  );
});

watch(featuredProfessionalsTotalPages, (totalPages) => {
  featuredProfessionalsPage.value = clampCarouselPage(
    featuredProfessionalsPage.value,
    totalPages,
  );
});

watch(featuredServicesTotalPages, (totalPages) => {
  featuredServicesPage.value = clampCarouselPage(
    featuredServicesPage.value,
    totalPages,
  );
});

const handleNavigate = (page: string, params?: Record<string, any>) => {
  navigate(page, "contractor", params);
};

const handleLogout = () => {
  logout();
};

const handleSearch = (query: string) => {
  router.push({ path: "/search", query: { q: query } });
};

const loadHome = async () => {
  isLoading.value = true;
  error.value = "";

  try {
    homeData.value = await clientHomeService.getHome();
  } catch (err: any) {
    console.error("Erro ao carregar a home do cliente:", err);
    error.value =
      err.response?.data?.message ||
      "Não foi possível carregar sua página inicial.";
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

const handleViewProfile = (providerId: string | number) => {
  router.push({
    name: "provider-profile",
    params: { providerId: Number(providerId) },
  });
};

const handleViewService = (params: {
  providerId: number;
  serviceId: number;
}) => {
  router.push({
    name: "service-detail",
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

const setCategoryRailOffset = (nextOffset: number) => {
  if (isCategoryRailLoopActive.value) {
    categoryRailOffset.value = nextOffset;
    return;
  }

  categoryRailOffset.value = Math.min(
    Math.max(nextOffset, 0),
    categoryRailMaxOffset.value,
  );
};

const measureCategoryRail = async () => {
  await nextTick();

  const viewport = categoryRailViewportRef.value;
  const track = categoryRailTrackRef.value;

  if (!viewport || !track) {
    return;
  }

  const measuredSingleCopyWidth =
    track.scrollWidth /
    (isCategoryRailLoopActive.value ? CATEGORY_RAIL_LOOP_COPIES : 1);
  const shouldLoop =
    commercialCategories.value.length > 1 &&
    measuredSingleCopyWidth > viewport.clientWidth + 24;

  if (shouldLoop !== isCategoryRailLoopActive.value) {
    isCategoryRailLoopActive.value = shouldLoop;
    await nextTick();
  }

  const nextViewport = categoryRailViewportRef.value;
  const nextTrack = categoryRailTrackRef.value;

  if (!nextViewport || !nextTrack) {
    return;
  }

  categoryRailViewportWidth.value = nextViewport.clientWidth;
  categoryRailContentWidth.value =
    nextTrack.scrollWidth /
    (isCategoryRailLoopActive.value ? CATEGORY_RAIL_LOOP_COPIES : 1);

  if (!isCategoryRailLoopActive.value) {
    categoryRailOffset.value = Math.min(
      Math.max(categoryRailOffset.value, 0),
      Math.max(
        categoryRailContentWidth.value - categoryRailViewportWidth.value,
        0,
      ),
    );
  }
};

const scrollCategoryRail = (direction: "previous" | "next") => {
  if (!categoryRailViewportRef.value) {
    return;
  }

  const scrollAmount = Math.max(
    categoryRailViewportRef.value.clientWidth * 0.72,
    240,
  );
  setCategoryRailOffset(
    categoryRailOffset.value +
      (direction === "next" ? scrollAmount : -scrollAmount),
  );
};

const handleCategoryRailWheel = (event: WheelEvent) => {
  const horizontalDelta =
    Math.abs(event.deltaX) > Math.abs(event.deltaY)
      ? event.deltaX
      : event.shiftKey
        ? event.deltaY
        : 0;

  if (!horizontalDelta || !categoryRailShouldShowControls.value) {
    return;
  }

  event.preventDefault();
  setCategoryRailOffset(categoryRailOffset.value + horizontalDelta);
};

const handleCategoryRailPointerDown = (event: PointerEvent) => {
  if (
    !categoryRailShouldShowControls.value ||
    (event.pointerType === "mouse" && event.button !== 0) ||
    !categoryRailViewportRef.value
  ) {
    return;
  }

  activeCategoryRailPointerId = event.pointerId;
  isCategoryRailDragging.value = false;
  categoryRailDragStartX = event.clientX;
  categoryRailDragStartOffset = categoryRailOffset.value;
  suppressCategoryRailClick = false;
};

const handleCategoryRailPointerMove = (event: PointerEvent) => {
  if (activeCategoryRailPointerId !== event.pointerId) {
    return;
  }

  const deltaX = event.clientX - categoryRailDragStartX;

  if (!isCategoryRailDragging.value) {
    if (Math.abs(deltaX) <= CATEGORY_RAIL_DRAG_THRESHOLD) {
      return;
    }

    isCategoryRailDragging.value = true;
    suppressCategoryRailClick = true;
    categoryRailViewportRef.value?.setPointerCapture(event.pointerId);
  }

  event.preventDefault();
  setCategoryRailOffset(categoryRailDragStartOffset - deltaX);
};

const endCategoryRailPointerDrag = (event?: PointerEvent) => {
  if (
    activeCategoryRailPointerId === null ||
    (event && activeCategoryRailPointerId !== event.pointerId)
  ) {
    return;
  }

  activeCategoryRailPointerId = null;
  isCategoryRailDragging.value = false;

  if (
    event &&
    categoryRailViewportRef.value &&
    categoryRailViewportRef.value.hasPointerCapture(event.pointerId)
  ) {
    categoryRailViewportRef.value.releasePointerCapture(event.pointerId);
  }

  if (suppressCategoryRailClick) {
    window.setTimeout(() => {
      suppressCategoryRailClick = false;
    }, 0);
  }
};

const handleCategoryClick = (event: MouseEvent, categorySlug: string) => {
  if (suppressCategoryRailClick) {
    event.preventDefault();
    event.stopPropagation();
    return;
  }

  handleOpenCategory(categorySlug);
};

const resetCategoryRail = async () => {
  await measureCategoryRail();
};

const handleCategoryRailResize = () => {
  window.requestAnimationFrame(() => {
    resetCategoryRail();
  });
};

const goToWeeklyServicesPage = (direction: "previous" | "next") => {
  weeklyServicesPage.value =
    direction === "previous"
      ? Math.max(weeklyServicesPage.value - 1, 0)
      : Math.min(
          weeklyServicesPage.value + 1,
          Math.max(weeklyServicesTotalPages.value - 1, 0),
        );
};

const goToFeaturedProfessionalsPage = (direction: "previous" | "next") => {
  featuredProfessionalsPage.value =
    direction === "previous"
      ? Math.max(featuredProfessionalsPage.value - 1, 0)
      : Math.min(
          featuredProfessionalsPage.value + 1,
          Math.max(featuredProfessionalsTotalPages.value - 1, 0),
        );
};

const goToFeaturedServicesPage = (direction: "previous" | "next") => {
  featuredServicesPage.value =
    direction === "previous"
      ? Math.max(featuredServicesPage.value - 1, 0)
      : Math.min(
          featuredServicesPage.value + 1,
          Math.max(featuredServicesTotalPages.value - 1, 0),
        );
};

const handleOpenCategory = (categorySlug: string) => {
  router.push({
    name: "explore",
    query: { categorySlug },
  });
};

const mapServiceForCard = (service: ClientHomeServiceItem) => ({
  ...service,
  category: service.categoryLabel || service.category || "Serviço",
  freelancerProfilePhoto: service.freelancerProfilePhoto || undefined,
  images: service.images || [],
  tags: service.tags || [],
});

const mapFreelancerForCard = (freelancer: ClientHomeFreelancerItem) => ({
  id: freelancer.id,
  name: freelancer.name,
  avatar: freelancer.avatar || undefined,
  professionalTitle: freelancer.professionalTitle || undefined,
  category: freelancer.category || "Profissional",
  location: freelancer.location || undefined,
  rating: Number(freelancer.rating || 0),
  reviewCount: freelancer.totalReviews || 0,
  isAvailable: freelancer.available ?? true,
});

watch(
  () => commercialCategories.value.length,
  () => {
    resetCategoryRail();
  },
  { immediate: true },
);

onMounted(async () => {
  window.addEventListener("resize", handleCategoryRailResize);

  const userData = localStorage.getItem("user");

  if (!userData) {
    router.push("/login");
    return;
  }

  user.value = JSON.parse(userData);

  if (user.value.type !== "contractor") {
    router.push("/freelancer-dashboard");
    return;
  }

  await Promise.all([
    loadHome(),
    ensureFavoritesStore(),
    categoryCatalogStore.loadPublicCategories(),
  ]);

  await resetCategoryRail();
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleCategoryRailResize);
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
      <section
        class="overflow-hidden rounded-[32px] border border-slate-200 bg-[radial-gradient(circle_at_top_left,_rgba(40,106,216,0.16),_transparent_42%),linear-gradient(135deg,_#ffffff_0%,_#f8fbff_58%,_#eef5ff_100%)] p-6 shadow-sm sm:p-8"
      >
        <div class="relative space-y-8">
          <div
            class="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between"
          >
            <div class="max-w-3xl space-y-5">
              <Badge
                variant="outline"
                class="w-fit bg-[#286AD8] text-white hover:bg-[#286AD8]"
              >
                <MapPin class="mr-1 h-3.5 w-3.5" />
                {{ isRegionalized ? regionLabel : "Sem região definida" }}
              </Badge>

              <div>
                <h1
                  class="mb-3 text-3xl font-bold tracking-tight text-[#263238] sm:text-4xl"
                >
                  Descubra serviços prontos para a sua próxima contratação
                </h1>
                <p
                  class="max-w-2xl text-sm leading-6 text-[#435574] sm:text-base"
                >
                  Navegue por categorias, acompanhe o que está em alta na semana
                  e encontre profissionais bem posicionados
                  {{
                    isRegionalized
                      ? `em ${regionLabel}`
                      : "na plataforma inteira"
                  }}.
                </p>
              </div>

              <div
                v-if="interestCategories.length"
                class="flex flex-wrap gap-2"
              >
                <Badge
                  v-for="category in interestCategories"
                  :key="category"
                  variant="outline"
                  class="border-[#286AD8]/15 bg-white/90 text-[#286AD8]"
                >
                  {{ category }}
                </Badge>
              </div>
            </div>

            <div class="flex flex-col gap-3 sm:flex-row">
              <Button
                class="bg-[#286AD8] hover:bg-[#1f57b5]"
                @click="handleNavigate('explore')"
              >
                <Compass class="mr-2 h-4 w-4" />
                Explorar catálogo
              </Button>
            </div>
          </div>
        </div>
      </section>

      <div
        v-if="isLoading && !homeData"
        class="rounded-3xl border bg-white p-14 text-center"
      >
        <Loader2 class="mx-auto mb-4 h-10 w-10 animate-spin text-[#286AD8]" />
        <p class="text-[#435574]">Carregando sua página inicial...</p>
      </div>

      <div
        v-else-if="error && !homeData"
        class="rounded-3xl border border-red-200 bg-red-50 p-8 text-center"
      >
        <h2 class="mb-2 text-xl font-semibold text-[#263238]">
          Não foi possível carregar a home
        </h2>
        <p class="mx-auto mb-6 max-w-2xl text-sm text-[#435574]">
          {{ error }}
        </p>
        <Button class="bg-[#286AD8] hover:bg-[#1f57b5]" @click="loadHome">
          Tentar novamente
        </Button>
      </div>

      <template v-else-if="homeData">
        <section
          v-if="commercialCategories.length"
          class="rounded-[28px] border border-slate-200 bg-white/95 px-3 py-3 shadow-sm backdrop-blur sm:px-4"
        >
          <div class="flex items-center gap-2 sm:gap-3">
            <Button
              v-if="categoryRailShouldShowControls"
              variant="outline"
              size="icon"
              class="hidden h-10 w-10 shrink-0 rounded-full border-slate-200 bg-white text-[#435574] shadow-none hover:text-[#286AD8] sm:inline-flex"
              @click="scrollCategoryRail('previous')"
            >
              <ChevronLeft class="h-3.5 w-3.5" />
            </Button>

            <div
              ref="categoryRailViewportRef"
              :class="[
                'min-w-0 flex-1 overflow-hidden touch-pan-y select-none',
                categoryRailShouldShowControls
                  ? 'cursor-grab active:cursor-grabbing'
                  : '',
              ]"
              @wheel="handleCategoryRailWheel"
              @pointerdown="handleCategoryRailPointerDown"
              @pointermove="handleCategoryRailPointerMove"
              @pointerup="endCategoryRailPointerDrag"
              @pointercancel="endCategoryRailPointerDrag"
              @dragstart.prevent
            >
              <div
                ref="categoryRailTrackRef"
                class="flex w-max items-center gap-2 pr-3 transition-transform duration-150 ease-out will-change-transform sm:gap-3"
                :style="categoryRailTrackStyle"
              >
                <button
                  v-for="category in renderedCommercialCategories"
                  :key="category.loopKey"
                  type="button"
                  class="group inline-flex shrink-0 items-center gap-2 rounded-full px-2 py-2 text-left text-[#435574] transition-colors hover:bg-[#f6f9ff] hover:text-[#286AD8]"
                  @click="handleCategoryClick($event, category.slug)"
                >
                  <span
                    class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[#286AD8]/8 text-[#286AD8] transition-colors group-hover:bg-[#286AD8]/14 sm:h-10 sm:w-10"
                  >
                    <component
                      :is="category.icon"
                      class="h-[18px] w-[18px] sm:h-5 sm:w-5"
                    />
                  </span>
                  <span class="whitespace-nowrap text-sm font-medium">{{
                    category.name
                  }}</span>
                </button>
              </div>
            </div>

            <Button
              v-if="categoryRailShouldShowControls"
              variant="outline"
              size="icon"
              class="hidden h-10 w-10 shrink-0 rounded-full border-slate-200 bg-white text-[#435574] shadow-none hover:text-[#286AD8] sm:inline-flex"
              @click="scrollCategoryRail('next')"
            >
              <ChevronRight class="h-3.5 w-3.5" />
            </Button>
          </div>
        </section>

        <section class="space-y-4">
          <div
            class="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between"
          >
            <div>
              <div class="flex items-center gap-2">
                <Sparkles class="h-5 w-5 text-[#286AD8]" />
                <h2 class="text-2xl font-bold text-[#263238]">
                  Serviços mais buscados da semana
                </h2>
              </div>
              <p class="mt-1 text-sm text-[#435574]">
                Tendências reais de navegação no marketplace para você entrar no
                fluxo do que mais chama atenção agora.
              </p>
            </div>
            <div class="flex items-center gap-3 self-start sm:self-auto">
              <div class="flex items-center gap-2">
                <p
                  v-if="weeklyServicesTotalPages > 1"
                  class="text-sm text-[#435574]"
                >
                  {{ weeklyServicesPage + 1 }} / {{ weeklyServicesTotalPages }}
                </p>
                <Button
                  variant="outline"
                  size="icon"
                  class="bg-white"
                  :disabled="weeklyServicesPage === 0"
                  @click="goToWeeklyServicesPage('previous')"
                >
                  <ChevronLeft class="h-4 w-4" />
                </Button>
                <Button
                  variant="outline"
                  size="icon"
                  class="bg-white"
                  :disabled="weeklyServicesPage >= weeklyServicesTotalPages - 1"
                  @click="goToWeeklyServicesPage('next')"
                >
                  <ChevronRight class="h-4 w-4" />
                </Button>
              </div>
              <Button
                variant="outline"
                class="bg-white"
                @click="handleNavigate('explore')"
              >
                Abrir catálogo
              </Button>
            </div>
          </div>

          <div v-if="weeklyMostSearchedServices.length" class="space-y-3">
            <div class="overflow-hidden">
              <div
                class="flex transition-transform duration-300 ease-out"
                :style="{
                  transform: `translateX(-${weeklyServicesPage * 100}%)`,
                }"
              >
                <div
                  v-for="(servicePage, pageIndex) in weeklyServicesPages"
                  :key="`weekly-most-searched-page-${pageIndex}`"
                  class="w-full shrink-0"
                >
                  <div
                    class="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3"
                  >
                    <ServiceCard
                      v-for="service in servicePage"
                      :key="service.id"
                      :service="mapServiceForCard(service)"
                      :is-favorite="
                        favoritesStore.isFavoriteService(service.id)
                      "
                      @navigate="handleViewService"
                      @toggle-favorite="handleToggleFavoriteService"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div
            v-else
            class="rounded-3xl border border-dashed border-slate-200 bg-white p-8 text-center"
          >
            <p class="mb-2 text-lg font-semibold text-[#263238]">
              Ainda não há volume suficiente nesta semana
            </p>
            <p class="mb-5 text-sm text-[#435574]">
              Assim que as buscas públicas consolidarem tendência no período
              atual, os serviços em alta aparecerão aqui.
            </p>
            <Button variant="outline" @click="handleNavigate('explore')">
              Explorar serviços
            </Button>
          </div>
        </section>

        <section class="space-y-4">
          <div
            class="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between"
          >
            <div>
              <div class="flex items-center gap-2">
                <User class="h-5 w-5 text-[#286AD8]" />
                <h2 class="text-2xl font-bold text-[#263238]">
                  {{
                    isRegionalized
                      ? `Profissionais em destaque em ${regionLabel}`
                      : "Profissionais em destaque na plataforma"
                  }}
                </h2>
              </div>
              <p class="mt-1 text-sm text-[#435574]">
                Priorizados pelas categorias contratadas recentemente e pela
                qualidade das avaliações mais atuais.
              </p>
            </div>
            <div class="flex items-center gap-3 self-start sm:self-auto">
              <p
                v-if="featuredProfessionalsTotalPages > 1"
                class="text-sm text-[#435574]"
              >
                {{ featuredProfessionalsPage + 1 }} /
                {{ featuredProfessionalsTotalPages }}
              </p>
              <div class="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="icon"
                  class="bg-white"
                  :disabled="featuredProfessionalsPage === 0"
                  @click="goToFeaturedProfessionalsPage('previous')"
                >
                  <ChevronLeft class="h-4 w-4" />
                </Button>
                <Button
                  variant="outline"
                  size="icon"
                  class="bg-white"
                  :disabled="
                    featuredProfessionalsPage >=
                    featuredProfessionalsTotalPages - 1
                  "
                  @click="goToFeaturedProfessionalsPage('next')"
                >
                  <ChevronRight class="h-4 w-4" />
                </Button>
              </div>

              <Button
                variant="outline"
                class="bg-white"
                @click="handleNavigate('explore', { mode: 'professionals' })"
              >
                Ver mais opções
              </Button>
            </div>
          </div>

          <div v-if="featuredProfessionals.length" class="space-y-3">
            <div class="overflow-hidden">
              <div
                class="flex transition-transform duration-300 ease-out"
                :style="{
                  transform: `translateX(-${featuredProfessionalsPage * 100}%)`,
                }"
              >
                <div
                  v-for="(
                    professionalPage, pageIndex
                  ) in featuredProfessionalsPages"
                  :key="`featured-professionals-page-${pageIndex}`"
                  class="w-full shrink-0"
                >
                  <div
                    class="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3"
                  >
                    <FreelancerCard
                      v-for="freelancer in professionalPage"
                      :key="freelancer.id"
                      :freelancer="mapFreelancerForCard(freelancer)"
                      :is-favorite="
                        favoritesStore.isFavoriteFreelancer(freelancer.id)
                      "
                      @view-profile="handleViewProfile"
                      @toggle-favorite="handleToggleFavoriteFreelancer"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div
            v-else
            class="rounded-3xl border border-dashed border-slate-200 bg-white p-8 text-center"
          >
            <p class="mb-2 text-lg font-semibold text-[#263238]">
              Ainda não há profissionais em destaque
            </p>
            <p class="mb-5 text-sm text-[#435574]">
              Assim que houver profissionais bem avaliados para o seu contexto,
              eles aparecerão aqui.
            </p>
            <Button variant="outline" @click="handleNavigate('explore')">
              Explorar profissionais
            </Button>
          </div>
        </section>

        <section class="space-y-4">
          <div
            class="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between"
          >
            <div>
              <div class="flex items-center gap-2">
                <Briefcase class="h-5 w-5 text-[#286AD8]" />
                <h2 class="text-2xl font-bold text-[#263238]">
                  {{
                    isRegionalized
                      ? `Serviços em destaque em ${regionLabel}`
                      : "Serviços em destaque na plataforma"
                  }}
                </h2>
              </div>
              <p class="mt-1 text-sm text-[#435574]">
                Seleção de serviços ativos com melhor desempenho recente e
                aderência ao seu histórico.
              </p>
            </div>
            <div class="flex items-center gap-3 self-start sm:self-auto">
              <div class="flex items-center gap-2">
                <p
                  v-if="featuredServicesTotalPages > 1"
                  class="text-sm text-[#435574]"
                >
                  {{ featuredServicesPage + 1 }} /
                  {{ featuredServicesTotalPages }}
                </p>
                <Button
                  variant="outline"
                  size="icon"
                  class="bg-white"
                  :disabled="featuredServicesPage === 0"
                  @click="goToFeaturedServicesPage('previous')"
                >
                  <ChevronLeft class="h-4 w-4" />
                </Button>
                <Button
                  variant="outline"
                  size="icon"
                  class="bg-white"
                  :disabled="
                    featuredServicesPage >= featuredServicesTotalPages - 1
                  "
                  @click="goToFeaturedServicesPage('next')"
                >
                  <ChevronRight class="h-4 w-4" />
                </Button>
              </div>
              <Button
                variant="outline"
                class="bg-white"
                @click="handleNavigate('explore')"
              >
                Abrir catálogo
              </Button>
            </div>
          </div>

          <div v-if="featuredServices.length" class="space-y-3">
            <div class="overflow-hidden">
              <div
                class="flex transition-transform duration-300 ease-out"
                :style="{
                  transform: `translateX(-${featuredServicesPage * 100}%)`,
                }"
              >
                <div
                  v-for="(servicePage, pageIndex) in featuredServicesPages"
                  :key="`featured-services-page-${pageIndex}`"
                  class="w-full shrink-0"
                >
                  <div
                    class="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3"
                  >
                    <ServiceCard
                      v-for="service in servicePage"
                      :key="service.id"
                      :service="mapServiceForCard(service)"
                      :is-favorite="
                        favoritesStore.isFavoriteService(service.id)
                      "
                      @navigate="handleViewService"
                      @toggle-favorite="handleToggleFavoriteService"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div
            v-else
            class="rounded-3xl border border-dashed border-slate-200 bg-white p-8 text-center"
          >
            <p class="mb-2 text-lg font-semibold text-[#263238]">
              Nenhum serviço em destaque por enquanto
            </p>
            <p class="mb-5 text-sm text-[#435574]">
              Continue explorando a plataforma para abrir mais contexto de
              recomendação.
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
