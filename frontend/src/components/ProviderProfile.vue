<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import {
  ArrowLeft,
  Star,
  MapPin,
  Clock,
  MessageSquare,
  Briefcase,
  Award,
  Sparkles,
  Send,
  Heart,
} from 'lucide-vue-next';
import Button from './ui/button.vue';
import Card from './ui/card.vue';
import Badge from './ui/badge.vue';
import Tabs from './ui/tabs.vue';
import TabsContent from './ui/tabs-content.vue';
import TabsList from './ui/tabs-list.vue';
import TabsTrigger from './ui/tabs-trigger.vue';
import Textarea from './ui/textarea.vue';
import Dialog from './ui/dialog.vue';
import DialogContent from './ui/dialog-content.vue';
import DialogHeader from './ui/dialog-header.vue';
import DialogTitle from './ui/dialog-title.vue';
import DialogTrigger from './ui/dialog-trigger.vue';
import { toast } from '../utils/toast';
import { useFavoritesStore } from '../stores/favorites';

export interface User {
  id: string;
  name: string;
  email: string;
  type: 'contractor' | 'provider';
  avatar?: string;
}

export type Page =
  | 'home'
  | 'explore'
  | 'search-results'
  | 'requests'
  | 'messages'
  | 'favorites'
  | 'contracts'
  | 'notifications'
  | 'settings'
  | 'profile'
  | 'freelancer-profile'
  | 'freelancer-dashboard'
  | 'freelancer-onboarding'
  | 'freelancer-profile-settings'
  | 'freelancer-services'
  | 'freelancer-service-edit'
  | 'freelancer-proposals'
  | 'freelancer-proposal-detail'
  | 'freelancer-deliveries'
  | 'freelancer-wallet'

  | 'freelancer-portfolio'

  | 'freelancer-kyc'
  | 'freelancer-account-settings'
  | 'add-address'
  | 'request-quote'
  | 'quote-detail'
  | 'chat'
  | 'checkout'
  | 'payment-confirmation'
  | 'service-timeline'
  | 'delivery-approval'
  | 'rate-freelancer'
  | 'contractor-dashboard'
  | 'provider-dashboard'
  | 'provider-profile'
  | 'search'
  | 'landing';

interface Provider {
  id: string;
  name: string;
  avatar: string;
  category: string;
  location: string;
  rating: number;
  reviewCount: number;
  completedJobs: number;
  price: string;
  description: string;
  skills: string[];
  availability: string;
  responseTime: string;
  portfolio: {
    id: string;
    image: string;
    title: string;
    description: string;
  }[];
  reviews: {
    id: string;
    author: string;
    avatar: string;
    rating: number;
    date: string;
    comment: string;
  }[];
  aiSummary: string;
}

interface Props {
  providerId: string;
  onNavigate: (page: Page) => void;
  user: User;
  provider?: Provider;
}

const props = defineProps<Props>();

// Mock data
const providers: Provider[] = props.provider
  ? [props.provider]
  : [
      {
        id: '1',
        name: 'Carlos Silva',
        avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Carlos',
        category: 'Eletricista',
        location: 'São Paulo, SP',
        rating: 4.9,
        reviewCount: 127,
        completedJobs: 234,
        price: 'R$ 80-150/hora',
        description:
          'Eletricista profissional com mais de 10 anos de experiência em instalações residenciais e comerciais. Especializado em sistemas elétricos, automação e energia solar.',
        skills: [
          'Instalação elétrica',
          'Manutenção preventiva',
          'Automação residencial',
          'Energia solar',
          'Quadros elétricos',
        ],
        availability: 'Seg-Sex: 8h-18h',
        responseTime: '~2 horas',
        portfolio: [],
        reviews: [],
        aiSummary:
          '✨ Reconhecido pela rapidez no atendimento e eficiência na resolução de problemas.',
      },
    ];

const provider = providers.find((p) => p.id === props.providerId);

const quoteMessage = ref('');
const showQuoteDialog = ref(false);

const handleSendQuote = () => {
  if (!quoteMessage.value.trim()) {
    toast.error('Por favor, descreva o serviço que você precisa');
    return;
  }

  toast.success('Solicitação de orçamento enviada com sucesso!');
  showQuoteDialog.value = false;
  quoteMessage.value = '';

  setTimeout(() => {
    props.onNavigate('contractor-dashboard');
  }, 1500);
};

const favoritesStore = useFavoritesStore();

onMounted(() => {
  if (!favoritesStore.initialized) {
    favoritesStore.fetchFavorites();
  }
});

const isFavorite = computed(() => {
  const numericId = parseInt(props.providerId);
  return favoritesStore.isFavoriteFreelancer(numericId);
});

const handleToggleFavorite = () => {
  const numericId = parseInt(props.providerId);
  favoritesStore.toggleFavoriteFreelancer(numericId);
};
</script>

<template>
  <div v-if="!provider" class="min-h-screen bg-gray-50 flex items-center justify-center">
    <p>Prestador não encontrado</p>
  </div>

  <div v-else class="min-h-screen bg-gray-50">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <!-- Profile Header -->
      <Card class="p-6 mb-6">
        <div class="flex flex-col md:flex-row gap-6">
          <img
            :src="provider.avatar"
            :alt="provider.name"
            class="w-32 h-32 rounded-xl object-cover"
          />

          <div class="flex-1">
            <div class="flex items-start justify-between gap-4 mb-3">
              <div>
                <h1 class="text-gray-900 mb-2">{{ provider.name }}</h1>
                <p class="text-gray-600 mb-2">{{ provider.category }}</p>
                <div class="flex items-center gap-2">
                  <MapPin :size="16" class="text-gray-400" />
                  <span class="text-gray-600">{{ provider.location }}</span>
                </div>
              </div>

              <div class="text-right">
                <div class="flex items-center gap-2 mb-2">
                  <Star class="text-yellow-500 fill-yellow-500" :size="24" />
                  <span class="text-gray-900">{{ provider.rating.toFixed(1) }}</span>
                </div>
                <p class="text-gray-600">{{ provider.reviewCount }} avaliações</p>
              </div>
            </div>

            <div class="flex flex-wrap gap-4 mb-4 text-gray-600">
              <div class="flex items-center gap-2">
                <Briefcase :size="18" />
                <span>{{ provider.completedJobs }} trabalhos concluídos</span>
              </div>
              <div class="flex items-center gap-2">
                <Clock :size="18" />
                <span>Responde em {{ provider.responseTime }}</span>
              </div>
              <div class="flex items-center gap-2">
                <Award :size="18" />
                <span>{{ provider.availability }}</span>
              </div>
            </div>

            <div class="flex gap-3">
              <Dialog v-model:open="showQuoteDialog">
                <DialogTrigger as-child>
                  <Button size="lg" class="flex-1 md:flex-initial">Solicitar Orçamento</Button>
                </DialogTrigger>
                <DialogContent>
                  <DialogHeader>
                    <DialogTitle>Solicitar Orçamento</DialogTitle>
                  </DialogHeader>
                  <div class="space-y-4">
                    <div>
                      <label class="text-gray-700 mb-2 block">
                        Descreva o serviço que você precisa
                      </label>
                      <Textarea
                        placeholder="Ex: Preciso instalar 3 ventiladores de teto na sala e quartos..."
                        :rows="6"
                        v-model="quoteMessage"
                      />
                    </div>
                    <Button @click="handleSendQuote" class="w-full">
                      <Send :size="18" class="mr-2" />
                      Enviar Solicitação
                    </Button>
                  </div>
                </DialogContent>
              </Dialog>

              <Button 
                variant="outline" 
                size="lg"
                @click="handleToggleFavorite"
                :class="isFavorite ? 'text-red-500 border-red-200 bg-red-50' : ''"
              >
                <Heart :size="20" :class="`mr-2 ${isFavorite ? 'fill-current' : ''}`" />
                {{ isFavorite ? 'Favorito' : 'Favoritar' }}
              </Button>

              <Button variant="outline" size="lg">
                <MessageSquare :size="20" class="mr-2" />
                Mensagem
              </Button>
            </div>
          </div>
        </div>
      </Card>

      <div class="grid lg:grid-cols-3 gap-6">
        <!-- Main Content -->
        <div class="lg:col-span-2">
          <Tabs default-value="about" class="space-y-6">
            <TabsList class="grid w-full grid-cols-3">
              <TabsTrigger value="about">Sobre</TabsTrigger>
              <TabsTrigger value="portfolio">
                Portfólio ({{ provider.portfolio.length }})
              </TabsTrigger>
              <TabsTrigger value="reviews">Avaliações ({{ provider.reviewCount }})</TabsTrigger>
            </TabsList>

            <TabsContent value="about">
              <Card class="p-6">
                <h3 class="text-gray-900 mb-4">Sobre o profissional</h3>
                <p class="text-gray-700 mb-6">{{ provider.description }}</p>

                <h3 class="text-gray-900 mb-3">Habilidades</h3>
                <div class="flex flex-wrap gap-2">
                  <Badge v-for="(skill, idx) in provider.skills" :key="idx" variant="secondary">
                    {{ skill }}
                  </Badge>
                </div>
              </Card>
            </TabsContent>

            <TabsContent value="portfolio">
              <div v-if="provider.portfolio.length > 0" class="grid md:grid-cols-2 gap-4">
                <Card
                  v-for="item in provider.portfolio"
                  :key="item.id"
                  class="overflow-hidden hover:shadow-lg transition-shadow"
                >
                  <img :src="item.image" :alt="item.title" class="w-full h-48 object-cover" />
                  <div class="p-4">
                    <h4 class="text-gray-900 mb-1">{{ item.title }}</h4>
                    <p class="text-gray-600">{{ item.description }}</p>
                  </div>
                </Card>
              </div>
              <Card v-else class="p-6 text-center">
                <p class="text-gray-600">Nenhum item no portfólio ainda</p>
              </Card>
            </TabsContent>

            <TabsContent value="reviews">
              <!-- AI Summary -->
              <Card class="p-4 mb-6 bg-gradient-to-r from-purple-50 to-blue-50 border-purple-200">
                <div class="flex items-start gap-3">
                  <div
                    class="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center flex-shrink-0"
                  >
                    <Sparkles class="text-purple-600" :size="20" />
                  </div>
                  <div>
                    <h3 class="text-gray-900 mb-2">Resumo Inteligente das Avaliações</h3>
                    <p class="text-gray-700">{{ provider.aiSummary }}</p>
                  </div>
                </div>
              </Card>

              <!-- Reviews List -->
              <div class="space-y-4">
                <Card v-for="review in provider.reviews" :key="review.id" class="p-6">
                  <div class="flex items-start gap-4">
                    <img :src="review.avatar" :alt="review.author" class="w-12 h-12 rounded-full" />
                    <div class="flex-1">
                      <div class="flex items-center justify-between mb-2">
                        <div>
                          <p class="text-gray-900">{{ review.author }}</p>
                          <p class="text-gray-600">
                            {{ new Date(review.date).toLocaleDateString('pt-BR') }}
                          </p>
                        </div>
                        <div class="flex items-center gap-1">
                          <Star
                            v-for="i in 5"
                            :key="i"
                            :size="16"
                            :class="
                              i <= review.rating
                                ? 'text-yellow-500 fill-yellow-500'
                                : 'text-gray-300'
                            "
                          />
                        </div>
                      </div>
                      <p class="text-gray-700">{{ review.comment }}</p>
                    </div>
                  </div>
                </Card>
              </div>
            </TabsContent>
          </Tabs>
        </div>

        <!-- Sidebar -->
        <div class="space-y-6">
          <!-- Pricing -->
          <Card class="p-6">
            <h3 class="text-gray-900 mb-3">Faixa de preço</h3>
            <p class="text-gray-700 mb-4">{{ provider.price }}</p>
            <p class="text-gray-600">*Valores podem variar de acordo com a complexidade do serviço</p>
          </Card>

          <!-- Stats -->
          <Card class="p-6">
            <h3 class="text-gray-900 mb-4">Estatísticas</h3>
            <div class="space-y-3">
              <div class="flex items-center justify-between">
                <span class="text-gray-600">Taxa de resposta</span>
                <span class="text-gray-900">98%</span>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-gray-600">Trabalhos concluídos</span>
                <span class="text-gray-900">{{ provider.completedJobs }}</span>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-gray-600">Taxa de satisfação</span>
                <span class="text-gray-900">97%</span>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-gray-600">Membro desde</span>
                <span class="text-gray-900">2023</span>
              </div>
            </div>
          </Card>

          <!-- Verificações -->
          <Card class="p-6">
            <h3 class="text-gray-900 mb-4">Verificações</h3>
            <div class="space-y-2">
              <div class="flex items-center gap-2 text-green-600">
                <div class="w-5 h-5 bg-green-100 rounded-full flex items-center justify-center">
                  ✓
                </div>
                <span>E-mail verificado</span>
              </div>
              <div class="flex items-center gap-2 text-green-600">
                <div class="w-5 h-5 bg-green-100 rounded-full flex items-center justify-center">
                  ✓
                </div>
                <span>Telefone verificado</span>
              </div>
              <div class="flex items-center gap-2 text-green-600">
                <div class="w-5 h-5 bg-green-100 rounded-full flex items-center justify-center">
                  ✓
                </div>
                <span>Identidade verificada</span>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </div>
  </div>
</template>




