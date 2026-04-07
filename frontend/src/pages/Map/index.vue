<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { MapPin, Navigation, ArrowLeft, Star, MessageSquare, Phone, Filter, Search } from 'lucide-vue-next'
import 'leaflet/dist/leaflet.css'
import L from 'leaflet'
import Button from '@/components/ui/button.vue'
import Avatar from '@/components/ui/avatar.vue'
import AvatarImage from '@/components/ui/avatar-image.vue'
import AvatarFallback from '@/components/ui/avatar-fallback.vue'
import Input from '@/components/ui/input.vue'
import api from '@/api/config/axios'
import { toast } from '@/utils/toast'

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

// Buscar endereço do perfil
const loadUserAddressFromProfile = async () => {
  isLocating.value = true
  
  try {
    const response = await api.get('/addresses')
    const addresses = response.data || []
    const defaultAddress = addresses.find((addr: any) => addr.isDefault) || addresses[0]
    
    if (!defaultAddress) {
      toast.info('Cadastre um endereço no seu perfil para ver profissionais próximos.')
      return
    }

    const fullAddress = `${defaultAddress.street}, ${defaultAddress.number}, ${defaultAddress.neighborhood}, ${defaultAddress.city}, ${defaultAddress.state}, Brasil`
    
    try {
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
    } catch (geoError) {
      console.error('Erro no geocoding:', geoError)
      toast.error('Erro ao converter endereço em coordenadas.')
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
    
    if (response.data && response.data.length > 0) {
      nearbyProviders.value = response.data.map((p: any) => ({
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
    }
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
  if (!map.value || filteredProviders.value.length === 0) return
  
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
  <div class="map-container w-full flex flex-col bg-gray-100">
    <!-- Header -->
    <header class="bg-white shadow-sm z-10">
      <div class="flex items-center justify-between px-4 py-3">
        <div class="flex items-center gap-3">
          <Button variant="ghost" size="icon" @click="goBack">
            <ArrowLeft class="h-5 w-5" />
          </Button>
          <div>
            <h1 class="text-lg font-semibold text-[#263238]">Profissionais Próximos</h1>
            <p class="text-sm text-[#435574]">{{ userLocation.city }}, {{ userLocation.state }}</p>
          </div>
        </div>
        
        <div class="flex items-center gap-2">
          <Button 
            variant="outline" 
            size="sm"
            @click="showFilters = !showFilters"
            :class="{ 'bg-blue-50 border-blue-300': showFilters }"
          >
            <Filter class="h-4 w-4 mr-1" />
            Filtros
          </Button>
        </div>
      </div>
      
      <!-- Barra de busca e filtros -->
      <div class="px-4 pb-3 space-y-3">
        <div class="relative">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
          <Input 
            v-model="searchQuery"
            placeholder="Buscar por nome ou profissão..."
            class="pl-10"
          />
        </div>
        
        <!-- Filtros expandidos -->
        <div v-if="showFilters" class="flex flex-wrap gap-2 pt-2 border-t">
          <select 
            v-model="selectedCategory"
            class="px-3 py-1.5 text-sm border rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">Todas as categorias</option>
            <option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</option>
          </select>
          
          <select 
            v-model="searchRadius"
            @change="loadNearbyProviders"
            class="px-3 py-1.5 text-sm border rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option :value="10">10 km</option>
            <option :value="25">25 km</option>
            <option :value="50">50 km</option>
            <option :value="100">100 km</option>
          </select>
          
          <label class="flex items-center gap-2 px-3 py-1.5 text-sm border rounded-lg bg-white cursor-pointer">
            <input type="checkbox" v-model="onlyAvailable" class="rounded" />
            Apenas disponíveis
          </label>
        </div>
      </div>
    </header>

    <!-- Conteúdo principal -->
    <div class="flex-1 flex">
      <!-- Lista de profissionais (sidebar) -->
      <aside class="w-96 bg-white shadow-lg overflow-hidden flex flex-col hidden lg:flex">
        <div class="p-4 border-b bg-gray-50">
          <div class="flex items-center justify-between">
            <div>
              <h2 class="font-semibold text-[#263238]">
                {{ filteredProviders.length }} {{ filteredProviders.length === 1 ? 'profissional' : 'profissionais' }}
              </h2>
              <p class="text-sm text-[#435574]">Raio de {{ searchRadius }}km</p>
            </div>
            <Button 
              variant="ghost" 
              size="sm"
              @click="loadNearbyProviders"
              :disabled="isLoading"
            >
              <Navigation class="h-4 w-4" :class="{ 'animate-spin': isLoading }" />
            </Button>
          </div>
        </div>
        
        <!-- Loading -->
        <div v-if="isLoading" class="flex-1 flex items-center justify-center">
          <div class="text-center">
            <div class="animate-spin h-8 w-8 border-3 border-blue-500 border-t-transparent rounded-full mx-auto mb-3" />
            <p class="text-[#435574]">Buscando profissionais...</p>
          </div>
        </div>
        
        <!-- Lista vazia -->
        <div v-else-if="filteredProviders.length === 0" class="flex-1 flex items-center justify-center p-6">
          <div class="text-center">
            <div class="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <MapPin class="h-8 w-8 text-gray-400" />
            </div>
            <p class="font-medium text-[#263238]">Nenhum profissional encontrado</p>
            <p class="text-sm text-[#435574] mt-1">Tente aumentar o raio de busca</p>
          </div>
        </div>
        
        <!-- Lista de profissionais -->
        <div v-else class="flex-1 overflow-y-auto">
          <div
            v-for="provider in filteredProviders"
            :key="provider.id"
            @click="centerOnProvider(provider)"
            class="p-4 border-b cursor-pointer transition-all hover:bg-gray-50"
            :class="{ 'bg-blue-50 border-l-4 border-l-blue-500': selectedProvider?.id === provider.id }"
          >
            <div class="flex items-start gap-3">
              <div class="relative flex-shrink-0">
                <Avatar class="h-12 w-12">
                  <AvatarImage :src="provider.avatar" :alt="provider.name" />
                  <AvatarFallback 
                    :class="provider.available ? 'bg-green-500' : 'bg-gray-400'" 
                    class="text-white font-semibold"
                  >
                    {{ getInitials(provider.name) }}
                  </AvatarFallback>
                </Avatar>
                <div 
                  :class="provider.available ? 'bg-green-500' : 'bg-gray-400'"
                  class="absolute -bottom-0.5 -right-0.5 w-4 h-4 rounded-full border-2 border-white"
                />
              </div>
              
              <div class="flex-1 min-w-0">
                <div class="flex items-start justify-between gap-2">
                  <div>
                    <h3 class="font-semibold text-[#263238] truncate">{{ provider.name }}</h3>
                    <p class="text-sm text-[#435574]">{{ provider.professionalTitle }}</p>
                  </div>
                  <span 
                    :class="provider.available ? 'text-green-600 bg-green-50' : 'text-gray-500 bg-gray-100'"
                    class="text-xs px-2 py-0.5 rounded-full whitespace-nowrap"
                  >
                    {{ provider.available ? 'Disponível' : 'Ocupado' }}
                  </span>
                </div>
                
                <div class="flex items-center flex-wrap gap-x-3 gap-y-1 mt-2 text-sm">
                  <div class="flex items-center gap-1">
                    <Star class="h-3.5 w-3.5 text-yellow-500 fill-yellow-500" />
                    <span class="font-medium">{{ provider.rating.toFixed(1) }}</span>
                    <span class="text-gray-400">({{ provider.totalReviews }})</span>
                  </div>
                  <div class="flex items-center gap-1 text-[#435574]">
                    <Navigation class="h-3.5 w-3.5" />
                    <p>Aproximadamente</p>
                    {{ formatDistance(provider.distance || 0) }}
                  </div>
                </div>
              </div>
            </div>
            
            <div v-if="selectedProvider?.id === provider.id" class="mt-3 flex gap-2">
              <Button 
                v-if="provider.available"
                @click.stop="contactProvider(provider)"
                size="sm"
                class="flex-1"
              >
                <MessageSquare class="h-4 w-4 mr-1" />
                Contatar
              </Button>
              <Button variant="outline" size="sm" class="flex-1">
                Ver Perfil
              </Button>
            </div>
          </div>
        </div>
      </aside>

      <!-- Mapa -->
      <div class="flex-1 relative">
        <div ref="mapContainer" class="h-full w-full" />
        
        <!-- Botão de atualizar localização -->
        <Button
          @click="loadUserAddressFromProfile"
          :disabled="isLocating"
          class="absolute bottom-24 right-4 z-[1000] bg-white text-[#263238] hover:bg-gray-100 shadow-lg"
          size="icon"
          title="Atualizar localização"
        >
          <Navigation v-if="!isLocating" class="h-5 w-5" />
          <div v-else class="animate-spin h-5 w-5 border-2 border-blue-500 border-t-transparent rounded-full" />
        </Button>
        
        <!-- Lista mobile (bottom sheet) -->
        <div class="lg:hidden absolute bottom-0 left-0 right-0 bg-white rounded-t-2xl shadow-lg mobile-list-container overflow-hidden">
          <div class="p-4 border-b">
            <div class="w-10 h-1 bg-gray-300 rounded-full mx-auto mb-3" />
            <div class="flex items-center justify-between">
              <div>
                <h2 class="font-semibold text-[#263238]">
                  {{ filteredProviders.length }} {{ filteredProviders.length === 1 ? 'profissional' : 'profissionais' }}
                </h2>
                <p class="text-sm text-[#435574]">Raio de {{ searchRadius }}km</p>
              </div>
            </div>
          </div>
          
          <div class="mobile-list-scroll overflow-y-auto">
            <div v-if="isLoading" class="p-8 text-center">
              <div class="animate-spin h-6 w-6 border-2 border-blue-500 border-t-transparent rounded-full mx-auto mb-2" />
              <p class="text-[#435574] text-sm">Buscando...</p>
            </div>
            
            <div v-else-if="filteredProviders.length === 0" class="p-8 text-center">
              <MapPin class="h-8 w-8 text-gray-400 mx-auto mb-2" />
              <p class="text-[#435574] text-sm">Nenhum profissional encontrado</p>
            </div>
            
            <div v-else>
              <div
                v-for="provider in filteredProviders"
                :key="provider.id"
                @click="centerOnProvider(provider)"
                class="p-4 border-b flex items-center gap-3"
                :class="{ 'bg-blue-50': selectedProvider?.id === provider.id }"
              >
                <Avatar class="h-10 w-10 flex-shrink-0">
                  <AvatarImage :src="provider.avatar" :alt="provider.name" />
                  <AvatarFallback 
                    :class="provider.available ? 'bg-green-500' : 'bg-gray-400'" 
                    class="text-white text-sm"
                  >
                    {{ getInitials(provider.name) }}
                  </AvatarFallback>
                </Avatar>
                
                <div class="flex-1 min-w-0">
                  <p class="font-medium text-[#263238] truncate">{{ provider.name }}</p>
                  <p class="text-sm text-[#435574]">{{ provider.professionalTitle }}</p>
                  <div class="flex items-center gap-2 text-xs mt-0.5">
                    <span class="flex items-center gap-0.5">
                      <Star class="h-3 w-3 text-yellow-500 fill-yellow-500" />
                      {{ provider.rating.toFixed(1) }}
                    </span>
                    <span class="text-[#435574]">{{ formatDistance(provider.distance || 0) }}</span>
                  </div>
                </div>
                
                <Button 
                  v-if="provider.available"
                  @click.stop="contactProvider(provider)"
                  size="sm"
                >
                  <MessageSquare class="h-4 w-4" />
                </Button>
              </div>
            </div>
          </div>
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
  border-radius: 12px;
}

.leaflet-popup-content {
  margin: 8px 12px;
}

/* Usar dvh para altura dinâmica do viewport */
.map-container {
  height: 100dvh;
}

/* Lista mobile com altura baseada em dvh */
.mobile-list-container {
  max-height: 50dvh;
}

.mobile-list-scroll {
  max-height: calc(50dvh - 80px);
}

/* Fallback para navegadores que não suportam dvh */
@supports not (height: 100dvh) {
  .map-container {
    height: 100vh;
  }
  
  .mobile-list-container {
    max-height: 50vh;
  }
  
  .mobile-list-scroll {
    max-height: calc(50vh - 80px);
  }
}
</style>
