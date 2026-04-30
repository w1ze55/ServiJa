<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { MapPin, Navigation, ArrowLeft, Star, MessageSquare, Filter, Search, X, ChevronRight, Briefcase, Clock } from 'lucide-vue-next'
import 'leaflet/dist/leaflet.css'
import L from 'leaflet'
import Button from '@/components/ui/button.vue'
import Avatar from '@/components/ui/avatar.vue'
import AvatarImage from '@/components/ui/avatar-image.vue'
import AvatarFallback from '@/components/ui/avatar-fallback.vue'
import Input from '@/components/ui/input.vue'
import api from '@/api/config/axios'
import { toast } from '@/utils/toast'
import { useUserStore } from '@/stores/userStore'

interface Provider {
  id: string
  name: string
  avatar?: string
  professionalTitle: string
  rating: number
  totalReviews: number
  location: string
  latitude: number
  longitude: number
  distance?: number
  available: boolean
  category?: string
}

interface UserLocation {
  latitude: number
  longitude: number
  address: string
  city: string
  state: string
}

const router = useRouter()
const userStore = useUserStore()

// Estado
const mapContainer = ref<HTMLElement | null>(null)
const map = ref<L.Map | null>(null)
const userMarker = ref<L.Marker | null>(null)
const providerMarkers = ref<L.Marker[]>([])
const isLoading = ref(false)
const isLocating = ref(false)
const selectedProvider = ref<Provider | null>(null)
const searchRadius = ref(50)
const searchQuery = ref('')

const userLocation = ref<UserLocation>({
  latitude: 0,
  longitude: 0,
  address: '',
  city: '',
  state: ''
})

const nearbyProviders = ref<Provider[]>([])

// Filtros
const showFilters = ref(false)
const selectedCategory = ref('')
const onlyAvailable = ref(false)

// Categorias disponíveis
const categories = [
  'Eletricista',
  'Encanador',
  'Pintor',
  'Diarista',
  'Marceneiro',
  'Desenvolvedor',
  'Designer',
  'Pedreiro',
  'Jardineiro'
]

// Profissionais filtrados
const filteredProviders = computed(() => {
  let result = nearbyProviders.value

  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(p => 
      p.name.toLowerCase().includes(query) ||
      p.professionalTitle.toLowerCase().includes(query)
    )
  }

  if (selectedCategory.value && selectedCategory.value !== 'Todos') {
    result = result.filter(p => 
      p.professionalTitle.toLowerCase().includes(selectedCategory.value.toLowerCase()) ||
      p.category?.toLowerCase().includes(selectedCategory.value.toLowerCase())
    )
  }

  if (onlyAvailable.value) {
    result = result.filter(p => p.available)
  }

  return result
})

// Ícones customizados
const createUserIcon = () => {
  return L.divIcon({
    className: 'custom-marker',
    html: `
      <div class="relative">
        <div class="w-10 h-10 bg-blue-500 rounded-full border-4 border-white shadow-lg flex items-center justify-center">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <circle cx="12" cy="12" r="3"/>
          </svg>
        </div>
        <div class="absolute -bottom-1 left-1/2 transform -translate-x-1/2 w-0 h-0 border-l-4 border-r-4 border-t-8 border-l-transparent border-r-transparent border-t-blue-500"></div>
      </div>
    `,
    iconSize: [40, 48],
    iconAnchor: [20, 48]
  })
}

const createProviderIcon = (available: boolean, rating: number) => {
  const bgColor = available ? 'bg-green-500' : 'bg-gray-400'
  const borderColor = available ? 'border-green-600' : 'border-gray-500'
  
  return L.divIcon({
    className: 'custom-marker',
    html: `
      <div class="relative group cursor-pointer">
        <div class="w-10 h-10 ${bgColor} rounded-full border-2 ${borderColor} shadow-lg flex items-center justify-center text-white font-bold text-sm transition-transform hover:scale-110">
          ${rating > 0 ? rating.toFixed(1) : '…'}
        </div>
        <div class="absolute -bottom-1 left-1/2 transform -translate-x-1/2 w-0 h-0 border-l-4 border-r-4 border-t-8 border-l-transparent border-r-transparent ${available ? 'border-t-green-500' : 'border-t-gray-400'}"></div>
      </div>
    `,
    iconSize: [40, 48],
    iconAnchor: [20, 48]
  })
}

// Inicializar mapa
const initMap = () => {
  if (!mapContainer.value || map.value) return

  map.value = L.map(mapContainer.value, {
    zoomControl: false
  })

  // Adicionar controle de zoom no canto direito
  L.control.zoom({ position: 'topright' }).addTo(map.value as L.Map)

  // Adicionar tiles do OpenStreetMap
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
  }).addTo(map.value as L.Map)
}

// Buscar localização a partir do perfil cadastrado no banco
const loadUserAddressFromProfile = async () => {
  isLocating.value = true

  try {
    if (userStore.user?.type === 'provider') {
      await loadLocationFromServiceCities()
    } else {
      await loadLocationFromAddress()
    }
  } finally {
    isLocating.value = false
  }
}

// Freelancer: posiciona o mapa na própria cidade de atendimento e exibe apenas a si mesmo
const loadLocationFromServiceCities = async () => {
  try {
    const response = await api.get('/freelancer/profile')
    const profile = response.data
    const serviceCities: any[] = profile?.serviceCities || []

    const citiesWithCoords = serviceCities.filter(
      (sc: any) => sc.latitude != null && sc.longitude != null
    )

    if (citiesWithCoords.length === 0) {
      toast.info('Cadastre uma cidade de atendimento no seu perfil para visualizá-la no mapa.')
      return
    }

    // Centraliza no mapa pela primeira cidade cadastrada
    const firstCity = citiesWithCoords[0]
    userLocation.value = {
      latitude: parseFloat(firstCity.latitude),
      longitude: parseFloat(firstCity.longitude),
      address: '',
      city: firstCity.city,
      state: firstCity.state
    }

    if (map.value) {
      map.value.setView([userLocation.value.latitude, userLocation.value.longitude], 10)
    }

    updateUserMarker()

    // Freelancer só vê a si mesmo: monta os marcadores com suas próprias cidades de atendimento
    nearbyProviders.value = citiesWithCoords.map((sc: any) => ({
      id: String(profile.id),
      name: profile.name,
      avatar: profile.profilePhoto || '',
      professionalTitle: profile.profession || 'Profissional',
      rating: profile.rating ? Number(profile.rating) : 0,
      totalReviews: profile.totalReviews || 0,
      location: `${sc.city}, ${sc.state}`,
      latitude: parseFloat(sc.latitude),
      longitude: parseFloat(sc.longitude),
      distance: 0,
      available: profile.available ?? true,
      category: profile.category || ''
    }))

    updateProviderMarkers()
    fitMapToBounds()
  } catch (error) {
    console.error('Erro ao carregar cidades de atendimento:', error)
    toast.error('Não foi possível carregar suas cidades de atendimento.')
  }
}

// Contratante: geocodifica o endereço padrão cadastrado
const loadLocationFromAddress = async () => {
  try {
    const response = await api.get('/addresses')
    const addresses = response.data || []
    const defaultAddress = addresses.find((addr: any) => addr.isDefault) || addresses[0]

    if (!defaultAddress) {
      toast.info('Cadastre um endereço no seu perfil para ver profissionais próximos.')
      return
    }

    const fullAddress = `${defaultAddress.street}, ${defaultAddress.number}, ${defaultAddress.neighborhood}, ${defaultAddress.city}, ${defaultAddress.state}, Brasil`

    const geoResponse = await fetch(
      `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(fullAddress)}&limit=1`
    )
    const geoData = await geoResponse.json()

    if (geoData && geoData.length > 0) {
      userLocation.value = {
        latitude: parseFloat(geoData[0].lat),
        longitude: parseFloat(geoData[0].lon),
        address: defaultAddress.street,
        city: defaultAddress.city,
        state: defaultAddress.state
      }

      if (map.value) {
        map.value.setView([userLocation.value.latitude, userLocation.value.longitude], 14)
      }

      updateUserMarker()
      await loadNearbyProviders()
    } else {
      toast.error('Não foi possível localizar seu endereço no mapa. Verifique os dados cadastrados.')
    }
  } catch (error) {
    console.error('Erro ao carregar endereço:', error)
    updateUserMarker()
    await loadNearbyProviders()
  } finally {
    isLocating.value = false
  }
}

const loadNearbyProviders = async () => {
  isLoading.value = true
  nearbyProviders.value = []
  
  try {
    const response = await api.get('/freelancer/nearby', {
      params: {
        latitude: userLocation.value.latitude,
        longitude: userLocation.value.longitude,
        radius: searchRadius.value
      }
    })
    
    nearbyProviders.value = (response.data || []).map((p: any) => ({
      id: String(p.id),
      name: p.name,
      avatar: p.avatar || '',
      professionalTitle: p.professionalTitle || 'Profissional',
      rating: p.rating ? Number(p.rating) : 0,
      totalReviews: p.totalReviews || 0,
      location: p.location || '',
      latitude: Number(p.latitude),
      longitude: Number(p.longitude),
      distance: p.distance || 0,
      available: p.available ?? true,
      category: p.category || ''
    }))

    updateProviderMarkers()
    fitMapToBounds()
  } catch (error: any) {
    console.error('Erro ao carregar profissionais:', error)
    if (error.response?.status !== 403) {
      toast.error('Erro ao carregar profissionais')
    }
  } finally {
    isLoading.value = false
  }
}

const fitMapToBounds = () => {
  if (!map.value || userLocation.value.latitude === 0) return

  if (filteredProviders.value.length === 0) {
    map.value.setView([userLocation.value.latitude, userLocation.value.longitude], 14)
    return
  }

  const bounds = L.latLngBounds([
    [userLocation.value.latitude, userLocation.value.longitude]
  ])

  filteredProviders.value.forEach(p => {
    bounds.extend([p.latitude, p.longitude])
  })

  map.value.fitBounds(bounds, { padding: [50, 50], maxZoom: 15 })
}

// Atualizar marcador do usuário
const updateUserMarker = () => {
  if (!map.value) return
  
  if (userMarker.value) {
    userMarker.value.setLatLng([userLocation.value.latitude, userLocation.value.longitude])
  } else {
    userMarker.value = L.marker(
      [userLocation.value.latitude, userLocation.value.longitude],
      { icon: createUserIcon() }
    ).addTo(map.value as L.Map)
    
    userMarker.value.bindPopup('<strong>Sua localização</strong>')
  }
}

// Atualizar marcadores dos profissionais
const updateProviderMarkers = () => {
  if (!map.value) return
  
  providerMarkers.value.forEach(marker => marker.remove())
  providerMarkers.value = []
  
  filteredProviders.value.forEach(provider => {
    const marker = L.marker(
      [provider.latitude, provider.longitude],
      { icon: createProviderIcon(provider.available, provider.rating) }
    ).addTo(map.value as L.Map)
    
    marker.bindPopup(`
      <div class="p-2 min-w-[200px]">
        <strong class="text-lg">${provider.name}</strong>
        <p class="text-gray-600">${provider.professionalTitle}</p>
        <div class="flex items-center gap-1 mt-1">
          <span class="text-yellow-500">…</span>
          <span>${provider.rating.toFixed(1)}</span>
          <span class="text-gray-400">(${provider.totalReviews})</span>
        </div>
      </div>
    `)
    
    marker.on('click', () => {
      selectedProvider.value = provider
    })
    
    providerMarkers.value.push(marker)
  })
}

// Centralizar no profissional
const centerOnProvider = (provider: Provider) => {
  selectedProvider.value = provider
  if (map.value) {
    map.value.setView([provider.latitude, provider.longitude], 16)
  }
}

// Contatar profissional
const contactProvider = (provider: Provider) => {
  // TODO: Implementar navegação para chat ou perfil
  toast.info(`Contatando ${provider.name}...`)
}

const goBack = () => {
  router.back()
}

const getInitials = (name: string) => {
  return name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase()
}

const formatDistance = (distance: number) => {
  if (distance < 1) {
    return `${Math.round(distance * 1000)}m`
  }
  return `${distance.toFixed(1)}km`
}

const stringToColor = (str: string) => {
  let hash = 0
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash)
  }
  const h = Math.abs(hash) % 360
  return `hsl(${h}, 45%, 42%)`
}

// Watch para atualizar marcadores quando filtros mudam
watch([searchQuery, selectedCategory, onlyAvailable], () => {
  updateProviderMarkers()
})

// Lifecycle
onMounted(async () => {
  initMap()
  await loadUserAddressFromProfile()
})
</script>

<template>
  <div class="map-container w-full relative overflow-hidden">

    <!-- Mapa (base) -->
    <div ref="mapContainer" class="absolute inset-0 z-0" />

    <!-- Barra superior flutuante -->
    <div class="absolute top-0 left-0 right-0 z-[1000] pointer-events-none">
      <div class="flex items-start gap-3 p-3 pointer-events-auto">

        <!-- Botão voltar -->
        <button
          @click="goBack"
          class="h-10 w-10 rounded-xl bg-white shadow-md flex items-center justify-center text-[#263238] hover:bg-gray-50 transition flex-shrink-0 mt-0.5"
        >
          <ArrowLeft class="h-5 w-5" />
        </button>

        <!-- Busca + filtros -->
        <div class="flex-1 flex flex-col gap-2">
          <div class="flex gap-2">
            <div class="relative flex-1">
              <Search class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
              <input
                v-model="searchQuery"
                placeholder="Buscar profissional ou serviço..."
                class="w-full h-10 pl-9 pr-4 rounded-xl bg-white shadow-md text-sm text-[#263238] placeholder-gray-400 outline-none border border-transparent focus:border-[#435574]/30"
              />
            </div>
            <button
              @click="showFilters = !showFilters"
              class="h-10 px-3 rounded-xl bg-white shadow-md flex items-center gap-1.5 text-sm font-medium transition flex-shrink-0"
              :class="showFilters ? 'text-[#435574] bg-[#435574]/10 shadow-none' : 'text-[#263238]'"
            >
              <Filter class="h-4 w-4" />
              <span class="hidden sm:inline">Filtros</span>
            </button>
          </div>

          <!-- Filtros expandidos -->
          <div v-if="showFilters" class="flex flex-wrap gap-2">
            <select
              v-model="selectedCategory"
              class="h-9 px-3 rounded-xl bg-white shadow-md text-sm text-[#263238] outline-none border border-transparent focus:border-[#435574]/30"
            >
              <option value="">Todas as categorias</option>
              <option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</option>
            </select>

            <select
              v-model="searchRadius"
              @change="loadNearbyProviders"
              class="h-9 px-3 rounded-xl bg-white shadow-md text-sm text-[#263238] outline-none border border-transparent focus:border-[#435574]/30"
            >
              <option :value="10">10 km</option>
              <option :value="25">25 km</option>
              <option :value="50">50 km</option>
              <option :value="100">100 km</option>
            </select>

            <label class="h-9 px-3 rounded-xl bg-white shadow-md flex items-center gap-2 text-sm text-[#263238] cursor-pointer">
              <input type="checkbox" v-model="onlyAvailable" class="rounded accent-[#435574]" />
              Só disponíveis
            </label>
          </div>
        </div>
      </div>
    </div>

    <!-- Botão atualizar localização -->
    <button
      @click="loadUserAddressFromProfile"
      :disabled="isLocating"
      class="absolute bottom-6 right-4 z-[1000] h-11 w-11 rounded-xl bg-white shadow-md flex items-center justify-center text-[#263238] hover:bg-gray-50 transition disabled:opacity-60"
      title="Atualizar localização"
    >
      <Navigation v-if="!isLocating" class="h-5 w-5" />
      <div v-else class="h-4 w-4 rounded-full border-2 border-[#435574] border-t-transparent animate-spin" />
    </button>

    <!-- Pill de contagem (desktop) -->
    <div class="absolute top-16 left-1/2 -translate-x-1/2 z-[1000] hidden lg:block mt-2 pointer-events-none">
      <div v-if="!isLoading && filteredProviders.length > 0" class="bg-white/90 backdrop-blur-sm shadow-md rounded-full px-4 py-1.5 text-xs font-medium text-[#435574]">
        {{ filteredProviders.length }} {{ filteredProviders.length === 1 ? 'profissional encontrado' : 'profissionais encontrados' }} · raio {{ searchRadius }}km
      </div>
      <div v-if="isLoading" class="bg-white/90 backdrop-blur-sm shadow-md rounded-full px-4 py-1.5 text-xs font-medium text-[#435574] flex items-center gap-2">
        <div class="h-3 w-3 rounded-full border-2 border-[#435574] border-t-transparent animate-spin" />
        Buscando...
      </div>
    </div>

    <!-- Sidebar desktop -->
    <aside class="hidden lg:flex absolute left-3 z-[1000] flex-col gap-0 overflow-hidden sidebar-desktop">
      <div class="bg-white rounded-2xl shadow-lg overflow-hidden flex flex-col h-full">

        <div class="px-4 pt-4 pb-3 border-b border-gray-100">
          <p class="text-xs uppercase tracking-widest text-gray-400 font-medium mb-0.5">Próximos de você</p>
          <div class="flex items-center justify-between">
            <h2 class="text-base font-semibold text-[#263238]">
              {{ userLocation.city || '—' }}<span v-if="userLocation.state">, {{ userLocation.state }}</span>
            </h2>
            <button
              @click="loadNearbyProviders"
              :disabled="isLoading"
              class="h-7 w-7 rounded-lg hover:bg-gray-100 flex items-center justify-center text-[#435574] transition disabled:opacity-40"
            >
              <Navigation class="h-3.5 w-3.5" :class="{ 'animate-spin': isLoading }" />
            </button>
          </div>
        </div>

        <!-- Loading -->
        <div v-if="isLoading" class="flex-1 flex flex-col items-center justify-center gap-3 p-8 text-center">
          <div class="h-8 w-8 rounded-full border-2 border-[#435574] border-t-transparent animate-spin" />
          <p class="text-sm text-gray-400">Buscando profissionais...</p>
        </div>

        <!-- Vazio -->
        <div v-else-if="filteredProviders.length === 0" class="flex-1 flex flex-col items-center justify-center gap-3 p-8 text-center">
          <div class="h-14 w-14 rounded-2xl bg-gray-100 flex items-center justify-center">
            <MapPin class="h-6 w-6 text-gray-400" />
          </div>
          <div>
            <p class="text-sm font-medium text-[#263238]">Nenhum profissional aqui</p>
            <p class="text-xs text-gray-400 mt-1">Aumente o raio de busca</p>
          </div>
        </div>

        <!-- Lista -->
        <div v-else class="flex-1 overflow-y-auto divide-y divide-gray-100">
          <button
            v-for="provider in filteredProviders"
            :key="provider.id"
            @click="centerOnProvider(provider)"
            class="w-full text-left px-4 py-3.5 hover:bg-gray-50 transition-colors flex items-center gap-3"
            :class="{ 'bg-[#435574]/5 border-l-2 border-l-[#435574]': selectedProvider?.id === provider.id }"
          >
            <div class="relative flex-shrink-0">
              <Avatar class="h-11 w-11">
                <AvatarImage :src="provider.avatar" :alt="provider.name" />
                <AvatarFallback class="text-white text-sm font-semibold" :style="{ background: stringToColor(provider.name) }">
                  {{ getInitials(provider.name) }}
                </AvatarFallback>
              </Avatar>
              <span
                class="absolute -bottom-0.5 -right-0.5 h-3.5 w-3.5 rounded-full border-2 border-white"
                :class="provider.available ? 'bg-emerald-500' : 'bg-gray-300'"
              />
            </div>

            <div class="flex-1 min-w-0">
              <p class="text-sm font-semibold text-[#263238] truncate leading-tight">{{ provider.name }}</p>
              <p class="text-xs text-[#435574] truncate mt-0.5">{{ provider.professionalTitle }}</p>
              <div class="flex items-center gap-2.5 mt-1.5">
                <span class="flex items-center gap-0.5 text-xs text-gray-500">
                  <Star class="h-3 w-3 fill-amber-400 text-amber-400" />
                  {{ provider.rating > 0 ? provider.rating.toFixed(1) : '–' }}
                </span>
                <span class="text-gray-300">·</span>
                <span class="text-xs text-gray-400">{{ formatDistance(provider.distance || 0) }}</span>
              </div>
            </div>

            <ChevronRight class="h-4 w-4 text-gray-300 flex-shrink-0" />
          </button>
        </div>
      </div>
    </aside>

    <!-- Card detalhe do profissional selecionado (flutuante, desktop) -->
    <Transition name="slide-up">
      <div
        v-if="selectedProvider"
        class="hidden lg:block absolute bottom-6 left-[calc(320px+24px)] z-[1001] w-80"
      >
        <div class="bg-white rounded-2xl shadow-xl overflow-hidden">
          <!-- Cabeçalho colorido -->
          <div class="relative h-16 flex-shrink-0" :style="{ background: `linear-gradient(135deg, ${stringToColor(selectedProvider.name)}22, ${stringToColor(selectedProvider.name)}44)` }">
            <button
              @click="selectedProvider = null"
              class="absolute top-3 right-3 h-6 w-6 rounded-full bg-white/80 flex items-center justify-center text-gray-500 hover:bg-white transition"
            >
              <X class="h-3.5 w-3.5" />
            </button>
            <div class="absolute -bottom-6 left-4">
              <div class="relative">
                <Avatar class="h-14 w-14 ring-2 ring-white shadow-md">
                  <AvatarImage :src="selectedProvider.avatar" :alt="selectedProvider.name" />
                  <AvatarFallback class="text-white text-lg font-bold" :style="{ background: stringToColor(selectedProvider.name) }">
                    {{ getInitials(selectedProvider.name) }}
                  </AvatarFallback>
                </Avatar>
                <span
                  class="absolute bottom-0.5 right-0.5 h-4 w-4 rounded-full border-2 border-white"
                  :class="selectedProvider.available ? 'bg-emerald-500' : 'bg-gray-300'"
                />
              </div>
            </div>
          </div>

          <div class="pt-9 px-4 pb-4">
            <div class="flex items-start justify-between gap-2 mb-3">
              <div>
                <h3 class="font-bold text-[#263238] leading-tight">{{ selectedProvider.name }}</h3>
                <p class="text-sm text-[#435574] mt-0.5">{{ selectedProvider.professionalTitle }}</p>
              </div>
              <span
                class="text-xs px-2.5 py-1 rounded-full font-medium flex-shrink-0 mt-0.5"
                :class="selectedProvider.available
                  ? 'bg-emerald-50 text-emerald-700'
                  : 'bg-gray-100 text-gray-500'"
              >
                {{ selectedProvider.available ? 'Disponível' : 'Ocupado' }}
              </span>
            </div>

            <!-- Métricas -->
            <div class="grid grid-cols-3 gap-2 mb-4">
              <div class="bg-gray-50 rounded-xl px-2 py-2.5 text-center">
                <div class="flex items-center justify-center gap-1 mb-0.5">
                  <Star class="h-3.5 w-3.5 fill-amber-400 text-amber-400" />
                  <span class="text-sm font-bold text-[#263238]">{{ selectedProvider.rating > 0 ? selectedProvider.rating.toFixed(1) : '–' }}</span>
                </div>
                <p class="text-[10px] text-gray-400 leading-tight">{{ selectedProvider.totalReviews }} avaliações</p>
              </div>
              <div class="bg-gray-50 rounded-xl px-2 py-2.5 text-center">
                <div class="flex items-center justify-center gap-1 mb-0.5">
                  <Navigation class="h-3.5 w-3.5 text-[#435574]" />
                  <span class="text-sm font-bold text-[#263238]">{{ formatDistance(selectedProvider.distance || 0) }}</span>
                </div>
                <p class="text-[10px] text-gray-400 leading-tight">distância</p>
              </div>
              <div class="bg-gray-50 rounded-xl px-2 py-2.5 text-center">
                <div class="flex items-center justify-center gap-1 mb-0.5">
                  <Briefcase class="h-3.5 w-3.5 text-[#435574]" />
                  <span class="text-sm font-bold text-[#263238]">–</span>
                </div>
                <p class="text-[10px] text-gray-400 leading-tight">trabalhos</p>
              </div>
            </div>

            <div v-if="selectedProvider.category" class="mb-4">
              <span class="inline-flex items-center gap-1 text-xs bg-[#435574]/10 text-[#435574] px-2.5 py-1 rounded-full font-medium">
                <Briefcase class="h-3 w-3" />
                {{ selectedProvider.category }}
              </span>
            </div>

            <!-- Ações -->
            <div class="flex gap-2">
              <button
                v-if="selectedProvider.available"
                @click="contactProvider(selectedProvider)"
                class="flex-1 h-10 rounded-xl bg-[#263238] text-white text-sm font-medium flex items-center justify-center gap-2 hover:bg-[#263238]/90 transition"
              >
                <MessageSquare class="h-4 w-4" />
                Contatar
              </button>
              <button
                class="flex-1 h-10 rounded-xl border border-gray-200 text-[#263238] text-sm font-medium flex items-center justify-center gap-2 hover:bg-gray-50 transition"
              >
                Ver perfil
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Bottom sheet mobile -->
    <div class="lg:hidden absolute bottom-0 left-0 right-0 z-[1000] mobile-sheet rounded-t-2xl bg-white shadow-2xl overflow-hidden">
      <div class="px-4 pt-3 pb-2 border-b border-gray-100">
        <div class="w-8 h-1 bg-gray-200 rounded-full mx-auto mb-3" />
        <div class="flex items-center justify-between">
          <p class="text-sm font-semibold text-[#263238]">
            {{ filteredProviders.length }} {{ filteredProviders.length === 1 ? 'profissional' : 'profissionais' }}
          </p>
          <p class="text-xs text-gray-400">raio de {{ searchRadius }}km</p>
        </div>
      </div>

      <div class="mobile-list overflow-y-auto">
        <div v-if="isLoading" class="p-8 flex flex-col items-center gap-3">
          <div class="h-6 w-6 rounded-full border-2 border-[#435574] border-t-transparent animate-spin" />
          <p class="text-sm text-gray-400">Buscando...</p>
        </div>

        <div v-else-if="filteredProviders.length === 0" class="p-8 flex flex-col items-center gap-2 text-center">
          <MapPin class="h-7 w-7 text-gray-300" />
          <p class="text-sm text-gray-400">Nenhum profissional encontrado</p>
        </div>

        <div v-else>
          <button
            v-for="provider in filteredProviders"
            :key="provider.id"
            @click="centerOnProvider(provider)"
            class="w-full text-left px-4 py-3 border-b border-gray-100 flex items-center gap-3 hover:bg-gray-50 transition-colors"
            :class="{ 'bg-[#435574]/5': selectedProvider?.id === provider.id }"
          >
            <div class="relative flex-shrink-0">
              <Avatar class="h-10 w-10">
                <AvatarImage :src="provider.avatar" :alt="provider.name" />
                <AvatarFallback class="text-white text-sm font-semibold" :style="{ background: stringToColor(provider.name) }">
                  {{ getInitials(provider.name) }}
                </AvatarFallback>
              </Avatar>
              <span
                class="absolute -bottom-0.5 -right-0.5 h-3 w-3 rounded-full border-2 border-white"
                :class="provider.available ? 'bg-emerald-500' : 'bg-gray-300'"
              />
            </div>

            <div class="flex-1 min-w-0">
              <p class="text-sm font-semibold text-[#263238] truncate">{{ provider.name }}</p>
              <p class="text-xs text-[#435574] truncate">{{ provider.professionalTitle }}</p>
              <div class="flex items-center gap-2 mt-1">
                <span class="flex items-center gap-0.5 text-xs text-gray-400">
                  <Star class="h-3 w-3 fill-amber-400 text-amber-400" />
                  {{ provider.rating > 0 ? provider.rating.toFixed(1) : '–' }}
                </span>
                <span class="text-gray-300 text-xs">·</span>
                <span class="text-xs text-gray-400">{{ formatDistance(provider.distance || 0) }}</span>
              </div>
            </div>

            <button
              v-if="provider.available"
              @click.stop="contactProvider(provider)"
              class="h-9 w-9 rounded-xl bg-[#263238] flex items-center justify-center text-white flex-shrink-0"
            >
              <MessageSquare class="h-4 w-4" />
            </button>
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<style>
.custom-marker {
  background: transparent !important;
  border: none !important;
}

.leaflet-popup-content-wrapper {
  border-radius: 14px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.12);
  border: none;
}

.leaflet-popup-content {
  margin: 10px 14px;
}

.leaflet-popup-tip-container {
  display: none;
}

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

@supports not (height: 100dvh) {
  .map-container { height: 100vh; }
  .sidebar-desktop { max-height: calc(100vh - 96px); }
  .mobile-sheet { max-height: 48vh; }
  .mobile-list { max-height: calc(48vh - 68px); }
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.slide-up-enter-from,
.slide-up-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
