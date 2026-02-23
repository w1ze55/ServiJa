<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import {
  X,
  Clock,
  DollarSign,
  Star,
  MapPin,
  CheckCircle,
  MessageCircle,
  Share2,
  Heart,
  Image as ImageIcon,
} from 'lucide-vue-next';
import Dialog from '../ui/dialog.vue';
import DialogContent from '../ui/dialog-content.vue';
import Button from '../ui/button.vue';
import Badge from '../ui/badge.vue';
import Avatar from '../ui/avatar.vue';
import api from '@/api/config/axios';

interface Service {
  id: string;
  title: string;
  category: string;
  description: string;
  price: number;
  deliveryTime: number;
  deliveryUnit: string;
  isActive: boolean;
  createdAt: string;
  coverImage?: string;
  images?: string[];
  tags?: string[];
  features?: string[];
}

interface FreelancerProfile {
  name: string;
  profilePicture?: string;
  rating?: number;
  reviewsCount?: number;
  city?: string;
  state?: string;
  completedJobs?: number;
  responseTime?: string;
}

interface Props {
  open: boolean;
  service: Service | null;
  onClose: () => void;
}

const props = defineProps<Props>();

// Dados do freelancer
const freelancerProfile = ref<FreelancerProfile | null>(null);
const isLoadingProfile = ref(false);

// Carregar perfil do freelancer
const loadFreelancerProfile = async () => {
  isLoadingProfile.value = true;
  try {
    const response = await api.get('/freelancer/profile');
    const data = response.data;
    
    console.log('Dados do perfil recebidos:', data);
    
    freelancerProfile.value = {
      name: data.name || 'Freelancer',
      profilePicture: data.profilePhoto,
      rating: data.rating ? Number(data.rating) : 0,
      reviewsCount: data.totalReviews || 0,
      city: data.location, // Já vem formatado como "Cidade, UF" do backend
      state: undefined,
      completedJobs: data.completedJobs || 0,
      responseTime: 'Menos de 24h', // Valor padrão, pode ser calculado depois
    };
  } catch (error) {
    console.error('Erro ao carregar perfil:', error);
    // Fallback com dados do localStorage
    const userString = localStorage.getItem('user');
    const user = userString ? JSON.parse(userString) : null;
    freelancerProfile.value = {
      name: user?.name || 'Freelancer',
      profilePicture: undefined,
      rating: 0,
      reviewsCount: 0,
      city: undefined,
      state: undefined,
      completedJobs: 0,
      responseTime: 'Não informado',
    };
  } finally {
    isLoadingProfile.value = false;
  }
};

// Dados computados do freelancer
const freelancer = computed(() => {
  if (!freelancerProfile.value) return null;
  
  return {
    name: freelancerProfile.value.name,
    avatar: freelancerProfile.value.profilePicture || `https://api.dicebear.com/7.x/avataaars/svg?seed=${freelancerProfile.value.name}`,
    rating: freelancerProfile.value.rating || 0,
    reviewsCount: freelancerProfile.value.reviewsCount || 0,
    location: freelancerProfile.value.city || 'Localização não informada',
    completedJobs: freelancerProfile.value.completedJobs || 0,
    responseTime: freelancerProfile.value.responseTime || 'Não informado',
  };
});

// Carregar perfil quando o modal abrir
watch(() => props.open, (newValue) => {
  if (newValue && !freelancerProfile.value) {
    loadFreelancerProfile();
  }
});

onMounted(() => {
  if (props.open) {
    loadFreelancerProfile();
  }
});

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value);
};

const formatDeliveryTime = (time: number, unit: string) => {
  if (!time) return 'Não informado';
  const unitLabel = unit === 'semanas' ? (time === 1 ? 'semana' : 'semanas') : (time === 1 ? 'dia' : 'dias');
  return `${time} ${unitLabel}`;
};

// Mock das características do serviço
const getServiceFeatures = () => {
  return [
    'Atendimento profissional',
    'Materiais inclusos',
    'Garantia de 90 dias',
    'Horário flexível',
  ];
};

// Mock de imagens
const getServiceImages = () => {
  if (props.service?.images && props.service.images.length > 0) {
    return props.service.images;
  }
  return [
    'https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=800&q=80',
    'https://images.unsplash.com/photo-1581092160562-40aa08e78837?w=800&q=80',
    'https://images.unsplash.com/photo-1621905252507-b35492cc74b4?w=800&q=80',
  ];
};

const selectedImage = ref(0);
</script>

<template>
  <Dialog :open="open" @update:open="(val) => !val && onClose()">
    <DialogContent class="max-w-4xl max-h-[90vh] overflow-y-auto p-0">
      <div v-if="service">
        <!-- Header com imagem de capa -->
        <div class="relative">
          <!-- Galeria de Imagens -->
          <div class="relative w-full h-80 bg-gray-200">
            <img
              :src="getServiceImages()[selectedImage]"
              :alt="service.title"
              class="w-full h-full object-cover"
            />

            <!-- Status Badge -->
            <div class="absolute top-4 left-4">
              <Badge
                :class="
                  service.isActive
                    ? 'bg-green-500 text-white hover:bg-green-500'
                    : 'bg-gray-500 text-white hover:bg-gray-500'
                "
              >
                {{ service.isActive ? 'Ativo' : 'Inativo' }}
              </Badge>
            </div>

            <!-- Thumbnails -->
            <div class="absolute bottom-4 left-1/2 -translate-x-1/2 flex gap-2">
              <button
                v-for="(img, index) in getServiceImages()"
                :key="index"
                @click="selectedImage = index"
                :class="[
                  'w-16 h-16 rounded-lg overflow-hidden border-2 transition-all',
                  selectedImage === index
                    ? 'border-white scale-110'
                    : 'border-transparent opacity-70 hover:opacity-100'
                ]"
              >
                <img :src="img" :alt="`Imagem ${index + 1}`" class="w-full h-full object-cover" />
              </button>
            </div>
          </div>
        </div>

        <div class="p-6">
          <!-- Info do Freelancer -->
          <div class="flex items-center gap-3 mb-6 pb-6 border-b">
            <Avatar class="h-12 w-12">
              <img v-if="freelancer" :src="freelancer.avatar" :alt="freelancer.name" />
              <div v-else class="w-full h-full bg-gray-200 animate-pulse"></div>
            </Avatar>
            <div class="flex-1">
              <h3 class="text-[#263238] font-medium">
                {{ freelancer?.name || 'Carregando...' }}
              </h3>
              <div v-if="freelancer" class="flex items-center gap-2 text-sm text-[#435574]">
                <div class="flex items-center gap-1">
                  <Star class="h-4 w-4 fill-yellow-400 text-yellow-400" />
                  <span>{{ freelancer.rating.toFixed(1) }}</span>
                  <span class="text-gray-400">({{ freelancer.reviewsCount }})</span>
                </div>
                <span class="text-gray-400">•</span>
                <div class="flex items-center gap-1">
                  <MapPin class="h-4 w-4" />
                  {{ freelancer.location }}
                </div>
              </div>
              <div v-else class="h-5 w-48 bg-gray-200 animate-pulse rounded"></div>
            </div>
          </div>

          <!-- Título e Categoria -->
          <div class="mb-6">
            <div class="flex items-start justify-between mb-2">
              <h2 class="text-2xl text-[#263238] flex-1 pr-4">
                {{ service.title }}
              </h2>
              <div class="flex gap-2">
                <Button variant="ghost" size="icon" class="text-gray-600 hover:text-red-500">
                  <Heart class="h-5 w-5" />
                </Button>
                <Button variant="ghost" size="icon" class="text-gray-600 hover:text-[#286AD8]">
                  <Share2 class="h-5 w-5" />
                </Button>
              </div>
            </div>
            <Badge variant="secondary" class="text-sm">
              {{ service.category }}
            </Badge>
          </div>

          <!-- Preço e Prazo -->
          <div class="grid grid-cols-2 gap-4 mb-6">
            <div class="p-4 bg-blue-50 rounded-lg">
              <div class="flex items-center gap-2 text-[#286AD8] mb-1">
                <DollarSign class="h-5 w-5" />
                <span class="text-sm font-medium">Preço</span>
              </div>
              <p class="text-2xl text-[#263238] font-bold">
                {{ formatCurrency(service.price) }}
              </p>
            </div>

            <div class="p-4 bg-green-50 rounded-lg">
              <div class="flex items-center gap-2 text-green-600 mb-1">
                <Clock class="h-5 w-5" />
                <span class="text-sm font-medium">Prazo de Entrega</span>
              </div>
              <p class="text-2xl text-[#263238] font-bold">
                {{ formatDeliveryTime(service.deliveryTime, service.deliveryUnit) }}
              </p>
            </div>
          </div>

          <!-- Descrição -->
          <div class="mb-6">
            <h3 class="text-lg text-[#263238] font-semibold mb-3">Sobre o Serviço</h3>
            <p class="text-[#435574] leading-relaxed whitespace-pre-line">
              {{ service.description }}
            </p>
          </div>

          <!-- Características -->
          <div class="mb-6">
            <h3 class="text-lg text-[#263238] font-semibold mb-3">O que está incluído</h3>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div
                v-for="(feature, index) in getServiceFeatures()"
                :key="index"
                class="flex items-center gap-2 text-[#435574]"
              >
                <CheckCircle class="h-5 w-5 text-green-500 flex-shrink-0" />
                <span>{{ feature }}</span>
              </div>
            </div>
          </div>

          <!-- Info Adicional do Freelancer -->
          <div class="bg-gray-50 rounded-lg p-4 mb-6">
            <div v-if="freelancer" class="grid grid-cols-3 gap-4 text-center">
              <div>
                <p class="text-2xl text-[#263238] font-bold">{{ freelancer.completedJobs }}</p>
                <p class="text-sm text-[#435574]">Trabalhos Concluídos</p>
              </div>
              <div>
                <p class="text-2xl text-[#263238] font-bold">{{ freelancer.rating.toFixed(1) }}</p>
                <p class="text-sm text-[#435574]">Avaliação Média</p>
              </div>
              <div>
                <p class="text-2xl text-[#263238] font-bold">{{ freelancer.responseTime }}</p>
                <p class="text-sm text-[#435574]">Tempo de Resposta</p>
              </div>
            </div>
            <div v-else class="grid grid-cols-3 gap-4 text-center">
              <div v-for="i in 3" :key="i">
                <div class="h-8 bg-gray-200 animate-pulse rounded mb-2"></div>
                <div class="h-4 bg-gray-200 animate-pulse rounded"></div>
              </div>
            </div>
          </div>

          <!-- Ações -->
          <div class="flex gap-3">
            <Button 
              class="flex-1 bg-[#286AD8] hover:bg-[#435574] text-white"
              size="lg"
            >
              <MessageCircle class="h-5 w-5 mr-2" />
              Contratar Serviço
            </Button>
            <Button 
              variant="outline"
              class="border-[#286AD8] text-[#286AD8] hover:bg-blue-50"
              size="lg"
            >
              Enviar Mensagem
            </Button>
          </div>

          <!-- Nota informativa -->
          <div class="mt-4 p-3 bg-blue-50 rounded-lg">
            <p class="text-sm text-[#435574] text-center">
              💡 <strong>Modo de Visualização:</strong> Este é o preview de como os clientes verão seu serviço
            </p>
          </div>
        </div>
      </div>
    </DialogContent>
  </Dialog>
</template>
