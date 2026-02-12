<script setup lang="ts">
import { ref } from 'vue';
import {
  ArrowLeft,
  Share2,
  Heart,
  MessageCircle,
  FileText,
  MapPin,
  Star,
  CheckCircle,
  Award,
  Briefcase,
  Image as ImageIcon,
  MessageSquare,
} from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Avatar from '../ui/avatar.vue';
import AvatarImage from '../ui/avatar-image.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import Badge from '../ui/badge.vue';
import Tabs from '../ui/tabs.vue';
import TabsContent from '../ui/tabs-content.vue';
import TabsList from '../ui/tabs-list.vue';
import TabsTrigger from '../ui/tabs-trigger.vue';
import { toast } from '../../utils/toast';
import FreelancerServices from './FreelancerServices.vue';
import FreelancerPortfolio from './FreelancerPortfolio.vue';
import FreelancerReviews from './FreelancerReviews.vue';

interface Props {
  freelancerId: string;
  onNavigate: (page: string, params?: any) => void;
  onBack: () => void;
}

const props = defineProps<Props>();

const isFavorite = ref(false);
const activeTab = ref('overview');

// Mock data - substituir por dados reais da API
const freelancer = {
  id: props.freelancerId,
  name: 'João Silva',
  avatar: '',
  category: 'Eletricista',
  bio: 'Eletricista profissional com mais de 10 anos de experiência. Especializado em instalações residenciais, comerciais e industriais. Certificado pelo SENAI e habilitado pelo CREA.',
  rating: 4.9,
  reviewCount: 128,
  projectsCompleted: 245,
  responseTime: '2 horas',
  cities: ['São Paulo', 'Guarulhos', 'São Caetano do Sul', 'Santo André'],
  state: 'SP',
  isVerified: true,
  isTopRated: true,
  memberSince: '2021',
  badges: [
    { id: '1', name: 'Verificado', icon: CheckCircle, color: 'text-blue-500' },
    { id: '2', name: 'Mais bem avaliado', icon: Award, color: 'text-yellow-500' },
  ],
};

const handleShare = async () => {
  const url = `${window.location.origin}/freelancer/${props.freelancerId}`;

  if (navigator.share) {
    try {
      await navigator.share({
        title: `Perfil de ${freelancer.name}`,
        text: `Confira o perfil de ${freelancer.name} no Servi Já`,
        url: url,
      });
    } catch (err) {
      // User cancelled
    }
  } else {
    // Fallback: copiar para clipboard
    navigator.clipboard.writeText(url);
    toast.success('Link copiado para a área de transferência!');
  }
};

const handleToggleFavorite = () => {
  isFavorite.value = !isFavorite.value;
  toast.success(isFavorite.value ? 'Adicionado aos favoritos' : 'Removido dos favoritos');
};

const handleRequestQuote = () => {
  props.onNavigate('request-quote', { freelancerId: props.freelancerId });
};

const handleSendMessage = () => {
  props.onNavigate('chat', { freelancerId: props.freelancerId });
};

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
};
</script>

<template>
  <div class="max-w-6xl mx-auto space-y-6">
    <!-- Back Button -->
    <Button
      variant="ghost"
      @click="props.onBack"
      class="text-[#435574] hover:text-[#263238]"
    >
      <ArrowLeft class="h-4 w-4 mr-2" />
      Voltar
    </Button>

    <!-- Profile Header -->
    <div class="bg-white rounded-lg border border-gray-200 overflow-hidden">
      <div class="p-6 md:p-8">
        <div class="flex flex-col md:flex-row gap-6">
          <!-- Avatar and Basic Info -->
          <div class="flex flex-col items-center md:items-start">
            <Avatar class="h-32 w-32 border-4 border-gray-100 mb-4">
              <AvatarImage :src="freelancer.avatar" :alt="freelancer.name" />
              <AvatarFallback class="bg-[#286AD8] text-white text-2xl">
                {{ getInitials(freelancer.name) }}
              </AvatarFallback>
            </Avatar>

            <!-- Quick Stats -->
            <div class="flex gap-4 text-center md:text-left">
              <div>
                <p class="text-[#263238]">{{ freelancer.projectsCompleted }}</p>
                <p class="text-sm text-[#435574]">Projetos</p>
              </div>
              <div>
                <p class="text-[#263238]">{{ freelancer.reviewCount }}</p>
                <p class="text-sm text-[#435574]">Avaliações</p>
              </div>
            </div>
          </div>

          <!-- Main Info -->
          <div class="flex-1">
            <div class="flex flex-col md:flex-row md:items-start md:justify-between gap-4 mb-4">
              <div>
                <div class="flex items-center gap-2 mb-2">
                  <h1 class="text-[#263238]">{{ freelancer.name }}</h1>
                  <component
                    v-for="badge in freelancer.badges"
                    :key="badge.id"
                    :is="badge.icon"
                    :class="`h-5 w-5 ${badge.color}`"
                  />
                </div>
                <p class="text-[#286AD8] mb-2">{{ freelancer.category }}</p>

                <!-- Rating -->
                <div class="flex items-center gap-2 mb-3">
                  <div class="flex items-center gap-1">
                    <Star class="h-5 w-5 fill-yellow-400 text-yellow-400" />
                    <span class="text-[#263238]">{{ freelancer.rating }}</span>
                  </div>
                  <span class="text-[#435574]">({{ freelancer.reviewCount }} avaliações)</span>
                </div>

                <!-- Badges -->
                <div class="flex flex-wrap gap-2 mb-4">
                  <Badge v-if="freelancer.isVerified" class="bg-blue-100 text-blue-700 hover:bg-blue-100">
                    <CheckCircle class="h-3 w-3 mr-1" />
                    Verificado
                  </Badge>
                  <Badge v-if="freelancer.isTopRated" class="bg-yellow-100 text-yellow-700 hover:bg-yellow-100">
                    <Award class="h-3 w-3 mr-1" />
                    Mais bem avaliado
                  </Badge>
                  <Badge variant="secondary" class="bg-gray-100 text-[#435574]">
                    Membro desde {{ freelancer.memberSince }}
                  </Badge>
                </div>

                <!-- Location -->
                <div class="flex items-start gap-2 text-[#435574]">
                  <MapPin class="h-4 w-4 mt-1 flex-shrink-0" />
                  <div>
                    <p class="text-sm">Atende em:</p>
                    <p class="text-sm">
                      {{ freelancer.cities.slice(0, 3).join(', ') }}
                      <span v-if="freelancer.cities.length > 3">
                        e mais {{ freelancer.cities.length - 3 }}
                      </span>
                    </p>
                  </div>
                </div>
              </div>

              <!-- Action Buttons - Desktop -->
              <div class="hidden md:flex flex-col gap-2 min-w-[200px]">
                <Button
                  @click="handleRequestQuote"
                  class="w-full bg-[#286AD8] hover:bg-[#435574]"
                >
                  <FileText class="h-4 w-4 mr-2" />
                  Solicitar orçamento
                </Button>
                <Button
                  @click="handleSendMessage"
                  variant="outline"
                  class="w-full border-[#286AD8] text-[#286AD8] hover:bg-blue-50"
                >
                  <MessageCircle class="h-4 w-4 mr-2" />
                  Enviar mensagem
                </Button>
                <div class="flex gap-2">
                  <Button
                    @click="handleToggleFavorite"
                    variant="outline"
                    size="icon"
                    :class="isFavorite ? 'border-red-500 text-red-500' : ''"
                  >
                    <Heart :class="`h-4 w-4 ${isFavorite ? 'fill-current' : ''}`" />
                  </Button>
                  <Button @click="handleShare" variant="outline" size="icon">
                    <Share2 class="h-4 w-4" />
                  </Button>
                </div>
              </div>
            </div>

            <!-- Bio -->
            <div>
              <h3 class="text-[#263238] mb-2">Sobre</h3>
              <p class="text-[#435574]">{{ freelancer.bio }}</p>
            </div>

            <!-- Response Time -->
            <div class="mt-4 p-4 bg-blue-50 rounded-lg border border-blue-200">
              <p class="text-sm text-[#435574]">
                <strong class="text-[#263238]">Tempo de resposta:</strong>
                {{ freelancer.responseTime }} em média
              </p>
            </div>
          </div>
        </div>

        <!-- Action Buttons - Mobile -->
        <div class="md:hidden flex flex-col gap-2 mt-6">
          <Button
            @click="handleRequestQuote"
            class="w-full bg-[#286AD8] hover:bg-[#435574]"
          >
            <FileText class="h-4 w-4 mr-2" />
            Solicitar orçamento
          </Button>
          <Button
            @click="handleSendMessage"
            variant="outline"
            class="w-full border-[#286AD8] text-[#286AD8] hover:bg-blue-50"
          >
            <MessageCircle class="h-4 w-4 mr-2" />
            Enviar mensagem
          </Button>
          <div class="flex gap-2">
            <Button
              @click="handleToggleFavorite"
              variant="outline"
              :class="`flex-1 ${isFavorite ? 'border-red-500 text-red-500' : ''}`"
            >
              <Heart :class="`h-4 w-4 mr-2 ${isFavorite ? 'fill-current' : ''}`" />
              {{ isFavorite ? 'Favoritado' : 'Favoritar' }}
            </Button>
            <Button @click="handleShare" variant="outline" class="flex-1">
              <Share2 class="h-4 w-4 mr-2" />
              Compartilhar
            </Button>
          </div>
        </div>
      </div>

      <!-- Tabs -->
      <Tabs :model-value="activeTab" @update:model-value="(val: string) => activeTab = val" class="w-full">
        <div class="border-t border-gray-200">
          <TabsList class="w-full justify-start bg-transparent h-auto p-0 rounded-none">
            <TabsTrigger
              value="overview"
              class="data-[state=active]:bg-transparent data-[state=active]:border-b-2 data-[state=active]:border-[#286AD8] rounded-none px-6 py-4"
            >
              <Briefcase class="h-4 w-4 mr-2" />
              Serviços
            </TabsTrigger>
            <TabsTrigger
              value="portfolio"
              class="data-[state=active]:bg-transparent data-[state=active]:border-b-2 data-[state=active]:border-[#286AD8] rounded-none px-6 py-4"
            >
              <ImageIcon class="h-4 w-4 mr-2" />
              Portfólio
            </TabsTrigger>
            <TabsTrigger
              value="reviews"
              class="data-[state=active]:bg-transparent data-[state=active]:border-b-2 data-[state=active]:border-[#286AD8] rounded-none px-6 py-4"
            >
              <MessageSquare class="h-4 w-4 mr-2" />
              Avaliações ({{ freelancer.reviewCount }})
            </TabsTrigger>
          </TabsList>
        </div>

        <div class="p-6 md:p-8">
          <TabsContent value="overview" class="mt-0">
            <FreelancerServices
              :freelancer-id="freelancerId"
              :on-request-quote="handleRequestQuote"
            />
          </TabsContent>

          <TabsContent value="portfolio" class="mt-0">
            <FreelancerPortfolio :freelancer-id="freelancerId" />
          </TabsContent>

          <TabsContent value="reviews" class="mt-0">
            <FreelancerReviews
              :freelancer-id="freelancerId"
              :average-rating="freelancer.rating"
              :total-reviews="freelancer.reviewCount"
            />
          </TabsContent>
        </div>
      </Tabs>
    </div>
  </div>
</template>

