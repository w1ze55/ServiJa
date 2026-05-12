<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import {
  ArrowLeft,
  Briefcase,
  Check,
  ChevronRight,
  Filter,
  LoaderCircle,
  MapPin,
  Navigation,
  RotateCcw,
  Search,
  SlidersHorizontal,
  Star,
  X,
} from "lucide-vue-next";
import "leaflet/dist/leaflet.css";
import L from "leaflet";
import Avatar from "@/components/ui/avatar.vue";
import AvatarImage from "@/components/ui/avatar-image.vue";
import AvatarFallback from "@/components/ui/avatar-fallback.vue";
import Select from "@/components/ui/select.vue";
import SelectContent from "@/components/ui/select-content.vue";
import SelectItem from "@/components/ui/select-item.vue";
import SelectTrigger from "@/components/ui/select-trigger.vue";
import SelectValue from "@/components/ui/select-value.vue";
import locationPinSvg from "@/assets/location-pin-svgrepo-com (2).svg?raw";
import api from "@/api/config/axios";
import { toast } from "@/utils/toast";
import { useCategoryCatalogStore } from "@/stores/categoryCatalogStore";

interface Provider {
  markerId: string;
  id: string;
  name: string;
  avatar?: string;
  professionalTitle: string;
  rating: number;
  totalReviews: number;
  location: string;
  latitude: number;
  longitude: number;
  distance?: number;
  available: boolean;
  completedJobs?: number;
  category?: string;
  categoryId?: number | null;
  categorySlug?: string;
  categoryLabel?: string;
  subcategoryId?: number | null;
  subcategorySlug?: string;
  subcategoryLabel?: string;
  subcategoryIds?: number[];
  subcategoryLabels?: string[];
}

interface UserLocation {
  latitude: number;
  longitude: number;
  address: string;
  city: string;
  state: string;
}

interface MapFilters {
  radius: number;
  minRating: number;
  categoryId: number | null;
  categorySlug: string;
  subcategoryId: number | null;
  subcategorySlug: string;
  onlyAvailable: boolean;
}

const router = useRouter();
const categoryCatalogStore = useCategoryCatalogStore();

const mapContainer = ref<HTMLElement | null>(null);
const map = ref<L.Map | null>(null);
const userMarker = ref<L.Marker | null>(null);
const providerLayer = ref<L.LayerGroup | null>(null);
const currentZoom = ref(12);
const isLoading = ref(false);
const isLocating = ref(false);
const selectedProvider = ref<Provider | null>(null);
const showFilters = ref(false);
const searchQuery = ref("");
const debouncedSearchQuery = ref("");
const nearbyProviders = ref<Provider[]>([]);

const userLocation = ref<UserLocation>({
  latitude: 0,
  longitude: 0,
  address: "",
  city: "",
  state: "",
});

const createDefaultFilters = (): MapFilters => ({
  radius: 50,
  minRating: 0,
  categoryId: null,
  categorySlug: "",
  subcategoryId: null,
  subcategorySlug: "",
  onlyAvailable: false,
});

const appliedFilters = ref<MapFilters>(createDefaultFilters());
const draftFilters = ref<MapFilters>(createDefaultFilters());
let searchDebounceTimer: ReturnType<typeof setTimeout> | null = null;
const MIN_PROVIDER_PIN_ZOOM = 6;
const RADIUS_FILTER_MIN = 5;
const RADIUS_FILTER_MAX = 100;

const categories = computed(() => categoryCatalogStore.activePublicCategories);
const draftCategory = computed(
  () =>
    categories.value.find(
      (category) => category.id === draftFilters.value.categoryId,
    ) || null,
);
const draftSubcategories = computed(
  () => draftCategory.value?.subcategories || [],
);
const draftSubcategory = computed(
  () =>
    draftSubcategories.value.find(
      (subcategory) => subcategory.id === draftFilters.value.subcategoryId,
    ) || null,
);

const hasActiveFilters = computed(() => {
  const defaults = createDefaultFilters();
  return (
    appliedFilters.value.radius !== defaults.radius ||
    appliedFilters.value.minRating !== defaults.minRating ||
    !!appliedFilters.value.categoryId ||
    !!appliedFilters.value.subcategoryId ||
    appliedFilters.value.onlyAvailable
  );
});

const searchTerm = computed(() => debouncedSearchQuery.value.trim());

const displayedProviders = computed(() => {
  return nearbyProviders.value;
});

const selectedCategoryLabel = computed(() => {
  const category = categories.value.find(
    (item) => item.id === appliedFilters.value.categoryId,
  );
  return category?.name || "";
});

const selectedSubcategoryLabel = computed(() => {
  const category = categories.value.find(
    (item) => item.id === appliedFilters.value.categoryId,
  );
  return (
    category?.subcategories.find(
      (item) => item.id === appliedFilters.value.subcategoryId,
    )?.name || ""
  );
});

const radiusSliderProgress = computed(() => {
  const normalizedRadius = Math.min(
    RADIUS_FILTER_MAX,
    Math.max(RADIUS_FILTER_MIN, draftFilters.value.radius),
  );
  const progress =
    ((normalizedRadius - RADIUS_FILTER_MIN) /
      (RADIUS_FILTER_MAX - RADIUS_FILTER_MIN)) *
    100;
  return `${progress}%`;
});

const normalizeText = (value?: string | null) =>
  (value || "")
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .trim()
    .toLowerCase();

const isValidCoordinate = (latitude: unknown, longitude: unknown) => {
  const lat = Number(latitude);
  const lng = Number(longitude);
  return (
    Number.isFinite(lat) &&
    Number.isFinite(lng) &&
    lat >= -90 &&
    lat <= 90 &&
    lng >= -180 &&
    lng <= 180 &&
    !(lat === 0 && lng === 0)
  );
};

const resolveAddressCoordinates = async (address: any) => {
  if (isValidCoordinate(address?.latitude, address?.longitude)) {
    return {
      latitude: Number(address.latitude),
      longitude: Number(address.longitude),
    };
  }

  const fullAddress = `${address.street}, ${address.number}, ${address.neighborhood}, ${address.city}, ${address.state}, Brasil`;
  const geoResponse = await fetch(
    `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(fullAddress)}&limit=1`,
  );
  const geoData = await geoResponse.json();

  if (
    !geoData ||
    geoData.length === 0 ||
    !isValidCoordinate(geoData[0].lat, geoData[0].lon)
  ) {
    return null;
  }

  return {
    latitude: Number(geoData[0].lat),
    longitude: Number(geoData[0].lon),
  };
};

const escapeHtml = (value: string) =>
  value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");

const getInitials = (name: string) => {
  return name
    .split(" ")
    .filter(Boolean)
    .map((part) => part[0])
    .join("")
    .substring(0, 2)
    .toUpperCase();
};

const markerPinSvg = locationPinSvg
  .replace(/fill="[^"]*"/gi, 'fill="currentColor"')
  .replace(/#231F20/gi, "currentColor");

const stringToColor = (str: string) => {
  let hash = 0;
  for (let i = 0; i < str.length; i += 1) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash);
  }
  const h = Math.abs(hash) % 360;
  return `hsl(${h}, 45%, 42%)`;
};

const formatDistance = (distance?: number) => {
  const normalizedDistance = Number(distance || 0);
  if (normalizedDistance < 1) {
    return `${Math.round(normalizedDistance * 1000)}m`;
  }
  return `${normalizedDistance.toFixed(1)}km`;
};

const getCategoryDisplay = (provider: Provider) =>
  provider.categoryLabel ||
  provider.category ||
  provider.professionalTitle ||
  "Área não informada";

const getSubcategoryDisplay = (provider: Provider) =>
  provider.subcategoryLabel || provider.subcategoryLabels?.[0] || "";

const normalizeProvider = (
  rawProvider: any,
  index: number,
): Provider | null => {
  if (!isValidCoordinate(rawProvider?.latitude, rawProvider?.longitude)) {
    return null;
  }

  const latitude = Number(rawProvider.latitude);
  const longitude = Number(rawProvider.longitude);
  const id = String(rawProvider.id);

  return {
    markerId:
      rawProvider.markerId ||
      `${id}-${latitude.toFixed(6)}-${longitude.toFixed(6)}-${index}`,
    id,
    name: rawProvider.name || "Profissional",
    avatar: rawProvider.avatar || rawProvider.profilePhoto || "",
    professionalTitle:
      rawProvider.professionalTitle || rawProvider.profession || "Profissional",
    rating: rawProvider.rating ? Number(rawProvider.rating) : 0,
    totalReviews: rawProvider.totalReviews || rawProvider.reviewCount || 0,
    location: rawProvider.location || "",
    latitude,
    longitude,
    distance: rawProvider.distance ? Number(rawProvider.distance) : 0,
    available: rawProvider.available ?? true,
    completedJobs: rawProvider.completedJobs ?? undefined,
    category: rawProvider.category || rawProvider.categoryLabel || "",
    categoryId: rawProvider.categoryId ?? null,
    categorySlug: rawProvider.categorySlug || "",
    categoryLabel: rawProvider.categoryLabel || rawProvider.category || "",
    subcategoryId: rawProvider.subcategoryId ?? null,
    subcategorySlug: rawProvider.subcategorySlug || "",
    subcategoryLabel:
      rawProvider.subcategoryLabel || rawProvider.subcategory || "",
    subcategoryIds: Array.isArray(rawProvider.subcategoryIds)
      ? rawProvider.subcategoryIds
      : [],
    subcategoryLabels: Array.isArray(rawProvider.subcategoryLabels)
      ? rawProvider.subcategoryLabels
      : [],
  };
};

const createUserIcon = () => {
  return L.divIcon({
    className: "custom-user-marker",
    html: `
      <div class="user-marker-icon" aria-label="Sua localização">
        <div class="marker-pin-shape user-marker-pin" aria-hidden="true">
          ${markerPinSvg}
        </div>
      </div>
    `,
    iconSize: [58, 82],
    iconAnchor: [29, 82],
  });
};

const createProviderIcon = (provider: Provider) => {
  const initials = escapeHtml(getInitials(provider.name));
  const avatar = provider.avatar
    ? `<img src="${escapeHtml(provider.avatar)}" alt="${escapeHtml(provider.name)}" class="provider-marker-image" />`
    : `<span>${initials}</span>`;

  return L.divIcon({
    className: "custom-provider-marker",
    html: `
      <div class="provider-marker-icon ${provider.available ? "is-available" : "is-unavailable"}" aria-label="${escapeHtml(provider.name)}">
        <div class="marker-pin-shape provider-marker-pin" aria-hidden="true">
          ${markerPinSvg}
        </div>
        <div class="provider-marker-content">
          <div class="provider-marker-avatar">
            ${avatar}
          </div>
        </div>
      </div>
    `,
    iconSize: [58, 82],
    iconAnchor: [29, 82],
  });
};

const initMap = () => {
  if (!mapContainer.value || map.value) return;

  const mapInstance = L.map(mapContainer.value, {
    zoomControl: false,
  }).setView([-23.5505, -46.6333], 12);

  map.value = mapInstance;
  currentZoom.value = mapInstance.getZoom();

  L.control.zoom({ position: "bottomleft" }).addTo(mapInstance);
  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",).addTo(mapInstance);

  providerLayer.value = L.layerGroup().addTo(mapInstance);
  mapInstance.on("zoomend", () => {
    currentZoom.value = mapInstance.getZoom();
    updateProviderMarkers();

    if (currentZoom.value < MIN_PROVIDER_PIN_ZOOM) {
      selectedProvider.value = null;
    }
  });
};

const loadUserAddressFromProfile = async () => {
  isLocating.value = true;

  try {
    await loadLocationFromAddress();
  } finally {
    isLocating.value = false;
  }
};

const loadLocationFromAddress = async () => {
  try {
    const response = await api.get("/addresses");
    const addresses = response.data || [];
    const defaultAddress =
      addresses.find((addr: any) => addr.isDefault) || addresses[0];

    if (!defaultAddress) {
      nearbyProviders.value = [];
      clearProviderMarkers();
      toast.info(
        "Cadastre um endereço no seu perfil para ver profissionais próximos.",
      );
      return;
    }

    const coordinates = await resolveAddressCoordinates(defaultAddress);

    if (!coordinates) {
      nearbyProviders.value = [];
      clearProviderMarkers();
      toast.error(
        "Não foi possível localizar seu endereço no mapa. Verifique os dados cadastrados.",
      );
      return;
    }

    userLocation.value = {
      latitude: coordinates.latitude,
      longitude: coordinates.longitude,
      address: defaultAddress.street,
      city: defaultAddress.city,
      state: defaultAddress.state,
    };

    map.value?.setView(
      [userLocation.value.latitude, userLocation.value.longitude],
      14,
    );
    updateUserMarker();
    await loadNearbyProviders();
  } catch (error) {
    console.error("Erro ao carregar endereço:", error);
    nearbyProviders.value = [];
    clearProviderMarkers();
    toast.error("Não foi possível carregar sua localização.");
  }
};

const buildNearbyParams = () => {
  const params: Record<string, unknown> = {
    latitude: userLocation.value.latitude,
    longitude: userLocation.value.longitude,
    radius: appliedFilters.value.radius,
  };

  if (searchTerm.value) params.q = searchTerm.value;
  if (appliedFilters.value.categoryId)
    params.categoryId = appliedFilters.value.categoryId;
  if (appliedFilters.value.categorySlug)
    params.categorySlug = appliedFilters.value.categorySlug;
  if (appliedFilters.value.subcategoryId)
    params.subcategoryId = appliedFilters.value.subcategoryId;
  if (appliedFilters.value.subcategorySlug)
    params.subcategorySlug = appliedFilters.value.subcategorySlug;
  if (appliedFilters.value.minRating > 0)
    params.minRating = appliedFilters.value.minRating;
  if (appliedFilters.value.onlyAvailable) params.available = true;
  if (userLocation.value.city) params.city = userLocation.value.city;
  if (userLocation.value.state) params.state = userLocation.value.state;

  return params;
};

const loadNearbyProviders = async () => {
  if (
    !isValidCoordinate(
      userLocation.value.latitude,
      userLocation.value.longitude,
    )
  ) {
    nearbyProviders.value = [];
    clearProviderMarkers();
    return;
  }

  isLoading.value = true;

  try {
    const response = await api.get("/freelancer/nearby", {
      params: buildNearbyParams(),
    });

    nearbyProviders.value = (response.data || [])
      .map((provider: any, index: number) => normalizeProvider(provider, index))
      .filter(
        (provider: Provider | null): provider is Provider => provider !== null,
      );
  } catch (error: any) {
    console.error("Erro ao carregar profissionais:", error);
    nearbyProviders.value = [];
    clearProviderMarkers();
    if (error.response?.status !== 403) {
      toast.error("Erro ao carregar profissionais");
    }
  } finally {
    isLoading.value = false;
  }
};

const refreshMapData = async () => {
  await loadNearbyProviders();
};

const updateUserMarker = () => {
  if (
    !map.value ||
    !isValidCoordinate(
      userLocation.value.latitude,
      userLocation.value.longitude,
    )
  )
    return;

  const latLng: L.LatLngExpression = [
    userLocation.value.latitude,
    userLocation.value.longitude,
  ];

  if (userMarker.value) {
    userMarker.value.setLatLng(latLng);
    return;
  }

  userMarker.value = L.marker(latLng, { icon: createUserIcon() }).addTo(
    map.value as L.Map,
  );
  userMarker.value.bindTooltip("Sua localização", {
    direction: "top",
    offset: [0, -48],
  });
};

const clearProviderMarkers = () => {
  providerLayer.value?.clearLayers();
};

const updateProviderMarkers = () => {
  if (!map.value) return;

  if (!providerLayer.value) {
    providerLayer.value = L.layerGroup().addTo(map.value as L.Map);
  }

  providerLayer.value.clearLayers();

  if (currentZoom.value < MIN_PROVIDER_PIN_ZOOM) {
    return;
  }

  displayedProviders.value.forEach((provider) => {
    const marker = L.marker([provider.latitude, provider.longitude], {
      icon: createProviderIcon(provider),
      riseOnHover: true,
    });

    marker.on("click", () => {
      selectedProvider.value = provider;
    });

    marker.addTo(providerLayer.value as L.LayerGroup);
  });
};

const fitMapToBounds = () => {
  if (
    !map.value ||
    !isValidCoordinate(
      userLocation.value.latitude,
      userLocation.value.longitude,
    )
  )
    return;

  if (displayedProviders.value.length === 0) {
    map.value.setView(
      [userLocation.value.latitude, userLocation.value.longitude],
      14,
    );
    return;
  }

  const bounds = L.latLngBounds([
    [userLocation.value.latitude, userLocation.value.longitude],
  ]);
  displayedProviders.value.forEach((provider) => {
    bounds.extend([provider.latitude, provider.longitude]);
  });

  map.value.fitBounds(bounds, { padding: [50, 50], maxZoom: 15 });
};

const centerOnProvider = (provider: Provider) => {
  selectedProvider.value = provider;
  map.value?.setView([provider.latitude, provider.longitude], 16);
};

const goBack = () => {
  router.back();
};

const goToProviderProfile = (provider: Provider) => {
  selectedProvider.value = null;
  router.push({
    name: "provider-profile",
    params: { providerId: provider.id },
  });
};

const openFilters = () => {
  draftFilters.value = { ...appliedFilters.value };
  showFilters.value = true;
};

const closeFilters = () => {
  showFilters.value = false;
};

const applyFilters = async () => {
  appliedFilters.value = { ...draftFilters.value };
  showFilters.value = false;
  await refreshMapData();
};

const clearFilters = async () => {
  const defaults = createDefaultFilters();
  draftFilters.value = { ...defaults };
  appliedFilters.value = { ...defaults };
  showFilters.value = false;
  await refreshMapData();
};

const handleDraftCategoryChange = (value: string) => {
  const categoryId = value ? Number(value) : null;
  const category = categories.value.find((item) => item.id === categoryId);
  draftFilters.value.categoryId = category?.id || null;
  draftFilters.value.categorySlug = category?.slug || "";
  draftFilters.value.subcategoryId = null;
  draftFilters.value.subcategorySlug = "";
};

const handleDraftSubcategoryChange = (value: string) => {
  const subcategoryId = value ? Number(value) : null;
  const subcategory = draftSubcategories.value.find(
    (item) => item.id === subcategoryId,
  );
  draftFilters.value.subcategoryId = subcategory?.id || null;
  draftFilters.value.subcategorySlug = subcategory?.slug || "";
};

watch(searchQuery, (value) => {
  if (searchDebounceTimer) {
    clearTimeout(searchDebounceTimer);
  }

  searchDebounceTimer = setTimeout(() => {
    debouncedSearchQuery.value = value.trim();
  }, 350);
});

watch(debouncedSearchQuery, async () => {
  await refreshMapData();
});

watch(displayedProviders, () => {
  updateProviderMarkers();
  fitMapToBounds();

  if (
    selectedProvider.value &&
    !displayedProviders.value.some(
      (provider) => provider.markerId === selectedProvider.value?.markerId,
    )
  ) {
    selectedProvider.value = null;
  }
});

onMounted(async () => {
  initMap();
  categoryCatalogStore.loadPublicCategories().catch((error) => {
    console.error("Erro ao carregar categorias:", error);
  });
  await loadUserAddressFromProfile();
});

onUnmounted(() => {
  if (searchDebounceTimer) {
    clearTimeout(searchDebounceTimer);
  }
  clearProviderMarkers();
  userMarker.value?.remove();
  map.value?.remove();
  map.value = null;
  providerLayer.value = null;
  userMarker.value = null;
});
</script>

<template>
  <div class="map-container w-full relative overflow-hidden">
    <div ref="mapContainer" class="absolute inset-0 z-0" />

    <div class="absolute top-0 left-0 right-0 z-[700] pointer-events-none">
      <div class="flex items-start gap-3 p-3 pointer-events-auto">
        <button
          @click="goBack"
          class="h-10 w-10 rounded-xl bg-white shadow-md flex items-center justify-center text-[#263238] hover:bg-gray-50 transition flex-shrink-0 mt-0.5"
          aria-label="Voltar"
        >
          <ArrowLeft class="h-5 w-5" />
        </button>

        <div class="flex-1 flex flex-col gap-2">
          <div class="flex gap-2">
            <div class="relative flex-1">
              <Search
                class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400"
              />
              <input
                v-model="searchQuery"
                placeholder="Buscar profissional, serviço ou categoria..."
                class="w-full h-10 pl-9 pr-4 rounded-xl bg-white shadow-md text-sm text-[#263238] placeholder-gray-400 outline-none border border-transparent focus:border-[#286AD8]/40"
              />
            </div>
            <button
              @click="openFilters"
              class="h-10 px-3 rounded-xl bg-white shadow-md flex items-center gap-1.5 text-sm font-medium transition flex-shrink-0"
              :class="
                hasActiveFilters
                  ? 'text-[#286AD8] bg-blue-50 shadow-none'
                  : 'text-[#263238]'
              "
            >
              <Filter class="h-4 w-4" />
              <span class="hidden sm:inline">Filtros</span>
            </button>
          </div>

          <div v-if="hasActiveFilters" class="hidden sm:flex flex-wrap gap-2">
            <span
              v-if="appliedFilters.minRating > 0"
              class="bg-white/90 shadow-sm rounded-full px-3 py-1 text-xs font-medium text-[#435574]"
            >
              {{ appliedFilters.minRating }}+ estrelas
            </span>
            <span
              v-if="selectedCategoryLabel"
              class="bg-white/90 shadow-sm rounded-full px-3 py-1 text-xs font-medium text-[#435574]"
            >
              {{ selectedCategoryLabel }}
            </span>
            <span
              v-if="selectedSubcategoryLabel"
              class="bg-white/90 shadow-sm rounded-full px-3 py-1 text-xs font-medium text-[#435574]"
            >
              {{ selectedSubcategoryLabel }}
            </span>
            <span
              v-if="appliedFilters.onlyAvailable"
              class="bg-white/90 shadow-sm rounded-full px-3 py-1 text-xs font-medium text-[#435574]"
            >
              Disponíveis
            </span>
          </div>
        </div>
      </div>
    </div>

    <button
      @click="loadUserAddressFromProfile"
      :disabled="isLocating"
      class="absolute right-4 z-[700] h-11 w-11 rounded-xl bg-white shadow-md flex items-center justify-center text-[#263238] hover:bg-gray-50 transition disabled:opacity-60 location-button"
      title="Atualizar localização"
      aria-label="Atualizar localização"
    >
      <Navigation v-if="!isLocating" class="h-5 w-5" />
      <div
        v-else
        class="h-4 w-4 rounded-full border-2 border-[#286AD8] border-t-transparent animate-spin"
      />
    </button>

    <div
      class="absolute top-16 left-1/2 -translate-x-1/2 z-[650] hidden lg:block mt-2 pointer-events-none"
    >
      <div
        v-if="!isLoading && displayedProviders.length > 0"
        class="bg-white/90 backdrop-blur-sm shadow-md rounded-full px-4 py-1.5 text-xs font-medium text-[#435574]"
      >
        {{ displayedProviders.length }}
        {{
          displayedProviders.length === 1
            ? "profissional encontrado"
            : "profissionais encontrados"
        }}
        · raio {{ appliedFilters.radius }}km
      </div>
      <div
        v-if="isLoading"
        class="bg-white/90 backdrop-blur-sm shadow-md rounded-full px-4 py-1.5 text-xs font-medium text-[#435574] flex items-center gap-2"
      >
        <div
          class="h-3 w-3 rounded-full border-2 border-[#286AD8] border-t-transparent animate-spin"
        />
        Buscando...
      </div>
    </div>

    <aside
      class="hidden lg:flex absolute left-3 z-[650] flex-col gap-0 overflow-hidden sidebar-desktop"
    >
      <div
        class="bg-white rounded-2xl shadow-lg overflow-hidden flex flex-col h-full"
      >
        <div class="px-4 pt-4 pb-3 border-b border-gray-100">
          <p
            class="text-xs uppercase tracking-widest text-gray-400 font-medium mb-0.5"
          >
            Próximos de você
          </p>
          <div class="flex items-center justify-between">
            <h2 class="text-base font-semibold text-[#263238]">
              {{ userLocation.city || "-"
              }}<span v-if="userLocation.state"
                >, {{ userLocation.state }}</span
              >
            </h2>
            <button
              @click="refreshMapData"
              :disabled="isLoading"
              class="h-7 w-7 rounded-lg hover:bg-gray-100 flex items-center justify-center text-[#286AD8] transition disabled:opacity-40"
              aria-label="Atualizar profissionais"
            >
              <LoaderCircle
                v-if="isLoading"
                class="h-3.5 w-3.5 animate-spin"
              />
              <Navigation v-else class="h-3.5 w-3.5" />
            </button>
          </div>
        </div>

        <div
          v-if="isLoading"
          class="flex-1 flex flex-col items-center justify-center gap-3 p-8 text-center"
        >
          <div
            class="h-8 w-8 rounded-full border-2 border-[#286AD8] border-t-transparent animate-spin"
          />
          <p class="text-sm text-gray-400">Buscando profissionais...</p>
        </div>

        <div
          v-else-if="displayedProviders.length === 0"
          class="flex-1 flex flex-col items-center justify-center gap-3 p-8 text-center"
        >
          <div
            class="h-14 w-14 rounded-2xl bg-gray-100 flex items-center justify-center"
          >
            <MapPin class="h-6 w-6 text-gray-400" />
          </div>
          <div>
            <p class="text-sm font-medium text-[#263238]">
              Nenhum profissional aqui
            </p>
            <p class="text-xs text-gray-400 mt-1">
              Ajuste a busca ou aumente o raio
            </p>
          </div>
        </div>

        <div v-else class="flex-1 overflow-y-auto divide-y divide-gray-100">
          <button
            v-for="provider in displayedProviders"
            :key="provider.markerId"
            @click="centerOnProvider(provider)"
            class="w-full text-left px-4 py-3.5 hover:bg-gray-50 transition-colors flex items-center gap-3"
            :class="{
              'bg-[#286AD8]/5 border-l-2 border-l-[#286AD8]':
                selectedProvider?.markerId === provider.markerId,
            }"
          >
            <div class="relative flex-shrink-0">
              <Avatar class="h-11 w-11">
                <AvatarImage :src="provider.avatar" :alt="provider.name" />
                <AvatarFallback
                  class="text-white text-sm font-semibold"
                  :style="{ background: stringToColor(provider.name) }"
                >
                  {{ getInitials(provider.name) }}
                </AvatarFallback>
              </Avatar>
              <span
                class="absolute -bottom-0.5 -right-0.5 h-3.5 w-3.5 rounded-full border-2 border-white"
                :class="provider.available ? 'bg-emerald-500' : 'bg-gray-300'"
              />
            </div>

            <div class="flex-1 min-w-0">
              <p
                class="text-sm font-semibold text-[#263238] truncate leading-tight"
              >
                {{ provider.name }}
              </p>
              <p class="text-xs text-[#286AD8] truncate mt-0.5">
                {{ provider.professionalTitle }}
              </p>
              <div class="flex items-center gap-2.5 mt-1.5">
                <span class="flex items-center gap-0.5 text-xs text-gray-500">
                  <Star class="h-3 w-3 fill-amber-400 text-amber-400" />
                  {{ provider.rating > 0 ? provider.rating.toFixed(1) : "-" }}
                </span>
                <span class="text-gray-300">·</span>
                <span class="text-xs text-gray-400">{{
                  formatDistance(provider.distance)
                }}</span>
              </div>
            </div>

            <ChevronRight class="h-4 w-4 text-gray-300 flex-shrink-0" />
          </button>
        </div>
      </div>
    </aside>

    <div
      class="lg:hidden absolute bottom-0 left-0 right-0 z-[650] mobile-sheet rounded-t-2xl bg-white shadow-2xl overflow-hidden"
    >
      <div class="px-4 pt-3 pb-2 border-b border-gray-100">
        <div class="w-8 h-1 bg-gray-200 rounded-full mx-auto mb-3" />
        <div class="flex items-center justify-between">
          <p class="text-sm font-semibold text-[#263238]">
            {{ displayedProviders.length }}
            {{
              displayedProviders.length === 1 ? "profissional" : "profissionais"
            }}
          </p>
          <p class="text-xs text-gray-400">
            raio de {{ appliedFilters.radius }}km
          </p>
        </div>
      </div>

      <div class="mobile-list overflow-y-auto">
        <div v-if="isLoading" class="p-8 flex flex-col items-center gap-3">
          <div
            class="h-6 w-6 rounded-full border-2 border-[#286AD8] border-t-transparent animate-spin"
          />
          <p class="text-sm text-gray-400">Buscando...</p>
        </div>

        <div
          v-else-if="displayedProviders.length === 0"
          class="p-8 flex flex-col items-center gap-2 text-center"
        >
          <MapPin class="h-7 w-7 text-gray-300" />
          <p class="text-sm text-gray-400">Nenhum profissional encontrado</p>
        </div>

        <div v-else>
          <button
            v-for="provider in displayedProviders"
            :key="provider.markerId"
            @click="centerOnProvider(provider)"
            class="w-full text-left px-4 py-3 border-b border-gray-100 flex items-center gap-3 hover:bg-gray-50 transition-colors"
            :class="{
              'bg-[#286AD8]/5':
                selectedProvider?.markerId === provider.markerId,
            }"
          >
            <div class="relative flex-shrink-0">
              <Avatar class="h-10 w-10">
                <AvatarImage :src="provider.avatar" :alt="provider.name" />
                <AvatarFallback
                  class="text-white text-sm font-semibold"
                  :style="{ background: stringToColor(provider.name) }"
                >
                  {{ getInitials(provider.name) }}
                </AvatarFallback>
              </Avatar>
              <span
                class="absolute -bottom-0.5 -right-0.5 h-3 w-3 rounded-full border-2 border-white"
                :class="provider.available ? 'bg-emerald-500' : 'bg-gray-300'"
              />
            </div>

            <div class="flex-1 min-w-0">
              <p class="text-sm font-semibold text-[#263238] truncate">
                {{ provider.name }}
              </p>
              <p class="text-xs text-[#286AD8] truncate">
                {{ provider.professionalTitle }}
              </p>
              <div class="flex items-center gap-2 mt-1">
                <span class="flex items-center gap-0.5 text-xs text-gray-400">
                  <Star class="h-3 w-3 fill-amber-400 text-amber-400" />
                  {{ provider.rating > 0 ? provider.rating.toFixed(1) : "-" }}
                </span>
                <span class="text-gray-300 text-xs">·</span>
                <span class="text-xs text-gray-400">{{
                  formatDistance(provider.distance)
                }}</span>
              </div>
            </div>
          </button>
        </div>
      </div>
    </div>

    <Transition name="fade">
      <div
        v-if="showFilters"
        class="fixed inset-0 z-[1200] bg-black/40"
        @click="closeFilters"
      />
    </Transition>

    <Transition name="drawer">
      <aside
        v-if="showFilters"
        class="fixed right-0 top-0 z-[1210] flex h-full w-full max-w-md flex-col overflow-hidden bg-white shadow-2xl"
      >
        <div
          class="p-6 border-b border-gray-200 flex items-center justify-between bg-white"
        >
          <div class="flex items-center gap-2">
            <SlidersHorizontal class="h-5 w-5 text-[#286AD8]" />
            <h3 class="font-semibold text-[#263238]">Filtros</h3>
          </div>
          <button
            @click="closeFilters"
            class="h-9 w-9 rounded-lg hover:bg-gray-100 flex items-center justify-center text-[#435574]"
          >
            <X class="h-5 w-5" />
          </button>
        </div>

        <div class="flex-1 overflow-y-auto p-6 space-y-6">
          <div class="space-y-3">
            <div class="flex items-center justify-between">
              <label
                class="text-sm font-medium text-[#263238] flex items-center gap-2"
              >
                <MapPin class="h-4 w-4 text-[#286AD8]" />
                Distância
              </label>
              <span class="text-sm font-semibold text-[#286AD8]"
                >{{ draftFilters.radius }} km</span
              >
            </div>
            <input
              v-model.number="draftFilters.radius"
              type="range"
              :min="RADIUS_FILTER_MIN"
              :max="RADIUS_FILTER_MAX"
              step="5"
              class="map-radius-slider"
              :style="{ '--slider-progress': radiusSliderProgress }"
              aria-label="Distância máxima"
            />
            <div class="flex justify-between text-xs text-gray-400">
              <span>5 km</span>
              <span>100 km</span>
            </div>
          </div>

          <div class="space-y-3">
            <label
              class="text-sm font-medium text-[#263238] flex items-center gap-2"
            >
              <Star class="h-4 w-4 text-[#286AD8]" />
              Avaliação mínima
            </label>
            <div class="grid grid-cols-6 gap-2">
              <button
                v-for="rating in [0, 1, 2, 3, 4, 5]"
                :key="rating"
                @click="draftFilters.minRating = rating"
                class="h-11 rounded-lg border text-sm font-medium flex items-center justify-center gap-1 transition"
                :class="
                  draftFilters.minRating === rating
                    ? 'border-[#286AD8] bg-blue-50 text-[#286AD8]'
                    : 'border-gray-200 text-[#435574] hover:border-gray-300'
                "
              >
                <Star
                  v-if="rating > 0"
                  class="h-3.5 w-3.5 fill-amber-400 text-amber-400"
                />
                {{ rating === 0 ? "Todas" : `${rating}+` }}
              </button>
            </div>
          </div>

          <div class="space-y-3">
            <label
              class="text-sm font-medium text-[#263238] flex items-center gap-2"
            >
              <Briefcase class="h-4 w-4 text-[#286AD8]" />
              Categoria
            </label>
            <Select
              :model-value="
                draftFilters.categoryId ? String(draftFilters.categoryId) : ''
              "
              @update:model-value="handleDraftCategoryChange"
            >
              <SelectTrigger>
                <SelectValue
                  :label="draftCategory?.name || undefined"
                  placeholder="Todas as categorias"
                />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="">Todas as categorias</SelectItem>
                <SelectItem
                  v-for="category in categories"
                  :key="category.id"
                  :value="String(category.id)"
                >
                  {{ category.name }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div class="space-y-3">
            <label
              class="text-sm font-medium text-[#263238] flex items-center gap-2"
            >
              <Briefcase class="h-4 w-4 text-[#286AD8]" />
              Subcategoria
            </label>
            <Select
              :model-value="
                draftFilters.subcategoryId
                  ? String(draftFilters.subcategoryId)
                  : ''
              "
              @update:model-value="handleDraftSubcategoryChange"
            >
              <SelectTrigger
                :class="!draftCategory ? 'pointer-events-none opacity-60' : ''"
              >
                <SelectValue
                  :label="draftSubcategory?.name || undefined"
                  :placeholder="
                    draftCategory
                      ? 'Todas as subcategorias'
                      : 'Selecione uma categoria'
                  "
                />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="">Todas as subcategorias</SelectItem>
                <SelectItem
                  v-for="subcategory in draftSubcategories"
                  :key="subcategory.id"
                  :value="String(subcategory.id)"
                >
                  {{ subcategory.name }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <label
            class="flex items-center justify-between rounded-xl border border-gray-200 px-4 py-3 cursor-pointer"
          >
            <span class="text-sm font-medium text-[#263238]"
              >Somente disponiveis</span
            >
            <input
              type="checkbox"
              v-model="draftFilters.onlyAvailable"
              class="h-4 w-4 rounded accent-[#286AD8]"
            />
          </label>
        </div>

        <div class="p-6 border-t border-gray-200 flex gap-3">
          <button
            @click="clearFilters"
            class="h-11 px-4 rounded-xl border border-gray-200 text-[#263238] text-sm font-medium flex items-center justify-center gap-2 hover:bg-gray-50 transition"
          >
            <RotateCcw class="h-4 w-4" />
            Limpar
          </button>
          <button
            @click="applyFilters"
            class="flex-1 h-11 rounded-xl bg-[#263238] text-white text-sm font-medium flex items-center justify-center gap-2 hover:bg-[#263238]/90 transition"
          >
            <Check class="h-4 w-4" />
            Aplicar filtros
          </button>
        </div>
      </aside>
    </Transition>

    <Transition name="modal">
      <div
        v-if="selectedProvider"
        class="fixed inset-0 z-[1300] flex items-center justify-center p-4"
      >
        <div
          class="absolute inset-0 bg-black/45"
          @click="selectedProvider = null"
        />
        <div
          class="relative w-full max-w-lg max-h-[90vh] overflow-hidden rounded-2xl bg-white shadow-2xl"
        >
          <button
            @click="selectedProvider = null"
            class="absolute right-4 top-4 z-10 h-9 w-9 rounded-full bg-white/90 text-[#435574] shadow-sm flex items-center justify-center hover:bg-white"
            aria-label="Fechar"
          >
            <X class="h-4 w-4" />
          </button>

          <div
            class="relative h-24 border-b border-gray-100 bg-[#f8fafc]"
          >
            <div class="absolute -bottom-9 left-6">
              <div class="relative">
                <Avatar class="h-20 w-20 ring-4 ring-white shadow-md">
                  <AvatarImage
                    :src="selectedProvider.avatar"
                    :alt="selectedProvider.name"
                  />
                  <AvatarFallback
                    class="text-white text-xl font-bold"
                    :style="{
                      background: stringToColor(selectedProvider.name),
                    }"
                  >
                    {{ getInitials(selectedProvider.name) }}
                  </AvatarFallback>
                </Avatar>
                <span
                  class="absolute bottom-1 right-1 h-4 w-4 rounded-full border-2 border-white"
                  :class="
                    selectedProvider.available
                      ? 'bg-emerald-500'
                      : 'bg-gray-300'
                  "
                />
              </div>
            </div>
          </div>

          <div class="max-h-[calc(90vh-6rem)] overflow-y-auto px-6 pb-6 pt-12">
            <div
              class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between"
            >
              <div class="min-w-0">
                <div class="flex flex-wrap items-center gap-2">
                  <h3 class="text-xl font-bold text-[#263238] leading-tight">
                    {{ selectedProvider.name }}
                  </h3>
                </div>
                <p class="mt-1 text-sm font-medium text-[#286AD8]">
                  {{ selectedProvider.professionalTitle }}
                </p>
              </div>
              <span
                class="w-fit rounded-full px-3 py-1 text-xs font-semibold"
                :class="
                  selectedProvider.available
                    ? 'bg-emerald-50 text-emerald-700'
                    : 'bg-gray-100 text-gray-500'
                "
              >
                {{ selectedProvider.available ? "Disponível" : "Indisponível" }}
              </span>
            </div>

            <div class="mt-5 grid grid-cols-3 gap-2">
              <div class="rounded-xl bg-gray-50 px-2 py-3 text-center">
                <div class="flex items-center justify-center gap-1">
                  <Star class="h-4 w-4 fill-amber-400 text-amber-400" />
                  <span class="text-sm font-bold text-[#263238]">{{
                    selectedProvider.rating > 0
                      ? selectedProvider.rating.toFixed(1)
                      : "-"
                  }}</span>
                </div>
                <p class="mt-1 text-[11px] leading-tight text-gray-400">
                  {{ selectedProvider.totalReviews }} avaliações
                </p>
              </div>
              <div class="rounded-xl bg-gray-50 px-2 py-3 text-center">
                <div class="flex items-center justify-center gap-1">
                  <Navigation class="h-4 w-4 text-[#286AD8]" />
                  <span class="text-sm font-bold text-[#263238]">{{
                    formatDistance(selectedProvider.distance)
                  }}</span>
                </div>
                <p class="mt-1 text-[11px] leading-tight text-gray-400">
                  distancia
                </p>
              </div>
              <div class="rounded-xl bg-gray-50 px-2 py-3 text-center">
                <div class="flex items-center justify-center gap-1">
                  <Briefcase class="h-4 w-4 text-[#286AD8]" />
                  <span class="text-sm font-bold text-[#263238]">{{
                    selectedProvider.completedJobs ?? "-"
                  }}</span>
                </div>
                <p class="mt-1 text-[11px] leading-tight text-gray-400">
                  trabalhos
                </p>
              </div>
            </div>

            <div class="mt-5 space-y-3">
              <div
                class="flex items-center gap-3 rounded-xl border border-gray-100 p-3"
              >
                <Briefcase class="mt-0.5 h-4 w-4 text-[#286AD8]" />
                <div class="min-w-0">
                  <p class="text-xs font-medium uppercase text-gray-400">
                    Área de atuação
                  </p>
                  <p class="mt-0.5 text-sm font-semibold text-[#263238]">
                    {{ getCategoryDisplay(selectedProvider) }}
                  </p>
                  <p
                    v-if="getSubcategoryDisplay(selectedProvider)"
                    class="mt-0.5 text-xs text-[#435574]"
                  >
                    {{ getSubcategoryDisplay(selectedProvider) }}
                  </p>
                </div>
              </div>

              <div
                v-if="selectedProvider.location"
                class="flex items-center gap-3 rounded-xl border border-gray-100 p-3"
              >
                <MapPin class="h-4 w-4 text-[#286AD8]" />
                <div class="min-w-0">
                  <p class="text-xs font-medium uppercase text-gray-400">
                    Localização
                  </p>
                  <p class="mt-0.5 text-sm font-semibold text-[#263238]">
                    {{ selectedProvider.location }}
                  </p>
                </div>
              </div>
            </div>

            <button
              @click="goToProviderProfile(selectedProvider)"
              class="mt-6 h-11 w-full rounded-xl bg-[#286AD8] text-white text-sm font-medium flex items-center justify-center hover:bg-[#286AD8]/90 transition"
            >
              Ver perfil
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style>
.map-container {
  height: 100dvh;
}

.sidebar-desktop {
  top: 72px;
  width: 300px;
  max-height: calc(100dvh - 96px);
}

.mobile-sheet {
  max-height: 48dvh;
}

.mobile-list {
  max-height: calc(48dvh - 68px);
}

.location-button {
  bottom: calc(48dvh + 16px);
}

.custom-provider-marker,
.custom-user-marker {
  background: transparent !important;
  border: none !important;
}

.provider-marker-icon,
.user-marker-icon {
  position: relative;
  width: 58px;
  height: 82px;
}

.marker-pin-shape {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: stretch;
  justify-content: center;
  color: inherit;
  filter: drop-shadow(0 12px 22px rgba(15, 23, 42, 0.24));
}

.marker-pin-shape svg {
  width: 100%;
  height: 100%;
  display: block;
}

.provider-marker-content {
  position: absolute;
  top: 15px;
  left: 50%;
  transform: translateX(-50%);
  width: 31px;
  height: 31px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.provider-marker-avatar {
  width: 31px;
  height: 31px;
  border-radius: 9999px;
  border: 2px solid #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  color: #fff;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.18);
  font-size: 10px;
  font-weight: 800;
  line-height: 1;
}

.provider-marker-icon.is-available {
  color: #22c55e;
}

.provider-marker-icon.is-available .provider-marker-avatar {
  background: #166534;
}

.provider-marker-icon.is-unavailable {
  color: #94a3b8;
}

.provider-marker-icon.is-unavailable .provider-marker-avatar {
  background: #475569;
}

.provider-marker-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-marker-icon {
  color: #ef4444;
}

.leaflet-bottom.leaflet-right {
  bottom: calc(48dvh + 76px);
  right: 12px;
}

.leaflet-control-zoom {
  border: none !important;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.16) !important;
}

.leaflet-control-zoom a {
  color: #263238 !important;
  border-bottom-color: #e5e7eb !important;
}

.map-radius-slider {
  --slider-progress: 0%;
  width: 100%;
  height: 24px;
  appearance: none;
  background: transparent;
  cursor: pointer;
}

.map-radius-slider::-webkit-slider-runnable-track {
  height: 8px;
  border-radius: 9999px;
  background: linear-gradient(
    to right,
    #286ad8 0%,
    #286ad8 var(--slider-progress),
    #e5e7eb var(--slider-progress),
    #e5e7eb 100%
  );
}

.map-radius-slider::-webkit-slider-thumb {
  appearance: none;
  width: 20px;
  height: 20px;
  margin-top: -6px;
  border-radius: 9999px;
  border: 3px solid #286ad8;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.22);
}

.map-radius-slider::-moz-range-track {
  height: 8px;
  border-radius: 9999px;
  background: #e5e7eb;
}

.map-radius-slider::-moz-range-progress {
  height: 8px;
  border-radius: 9999px;
  background: #286ad8;
}

.map-radius-slider::-moz-range-thumb {
  width: 20px;
  height: 20px;
  border-radius: 9999px;
  border: 3px solid #286ad8;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.22);
}

.map-radius-slider:focus-visible {
  outline: 2px solid rgba(40, 106, 216, 0.35);
  outline-offset: 4px;
  border-radius: 9999px;
}

@media (min-width: 1024px) {
  .location-button {
    bottom: 24px;
  }

  .leaflet-bottom.leaflet-right {
    bottom: 24px;
    right: 76px;
  }
}

@supports not (height: 100dvh) {
  .map-container {
    height: 100vh;
  }
  .sidebar-desktop {
    max-height: calc(100vh - 96px);
  }
  .mobile-sheet {
    max-height: 48vh;
  }
  .mobile-list {
    max-height: calc(48vh - 68px);
  }
  .location-button {
    bottom: calc(48vh + 16px);
  }
  .leaflet-bottom.leaflet-right {
    bottom: calc(48vh + 76px);
  }

  @media (min-width: 1024px) {
    .location-button {
      bottom: 24px;
    }
    .leaflet-bottom.leaflet-right {
      bottom: 24px;
    }
  }
}

.fade-enter-active,
.fade-leave-active,
.drawer-enter-active,
.drawer-leave-active,
.modal-enter-active,
.modal-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.fade-enter-from,
.fade-leave-to,
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from > .relative,
.modal-leave-to > .relative {
  transform: translateY(10px) scale(0.98);
}

.drawer-enter-from,
.drawer-leave-to {
  opacity: 0;
  transform: translateX(24px);
}
</style>
