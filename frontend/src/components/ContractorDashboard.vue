<script setup lang="ts">
import { ref, computed } from 'vue';
import { Search, MessageSquare, Star, Clock, CheckCircle, XCircle, TrendingUp } from 'lucide-vue-next';
import Button from './ui/button.vue';
import Card from './ui/card.vue';
import Badge from './ui/badge.vue';
import Tabs from './ui/tabs.vue';
import TabsContent from './ui/tabs-content.vue';
import TabsList from './ui/tabs-list.vue';
import TabsTrigger from './ui/tabs-trigger.vue';

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
  | 'freelancer-invoices'
  | 'freelancer-portfolio'
  | 'freelancer-schedule'
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

interface Quote {
  id: string;
  providerId: string;
  providerName: string;
  providerAvatar: string;
  serviceTitle: string;
  status: 'pending' | 'accepted' | 'rejected' | 'completed';
  price: string;
  description: string;
  date: string;
  messages: number;
}

interface Props {
  onNavigate: (page: Page, providerId?: string, chatId?: string) => void;
  user: User;
  quotes?: Quote[];
}

const props = defineProps<Props>();

// Mock data
const mockQuotes: Quote[] = props.quotes || [
  {
    id: '1',
    providerId: '1',
    providerName: 'Carlos Silva',
    providerAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Carlos',
    serviceTitle: 'Instalação elétrica',
    status: 'pending',
    price: 'R$ 500',
    description: 'Preciso de instalação elétrica completa',
    date: '2026-01-20',
    messages: 2,
  },
  {
    id: '2',
    providerId: '2',
    providerName: 'Maria Santos',
    providerAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Maria',
    serviceTitle: 'Reparo hidráulico',
    status: 'accepted',
    price: 'R$ 300',
    description: 'Vazamento no banheiro',
    date: '2026-01-19',
    messages: 5,
  },
];

const activeTab = ref('all');

const getStatusColor = (status: string) => {
  switch (status) {
    case 'pending':
      return 'bg-yellow-100 text-yellow-800';
    case 'accepted':
      return 'bg-green-100 text-green-800';
    case 'rejected':
      return 'bg-red-100 text-red-800';
    case 'completed':
      return 'bg-blue-100 text-blue-800';
    default:
      return 'bg-gray-100 text-gray-800';
  }
};

const getStatusIcon = (status: string) => {
  switch (status) {
    case 'pending':
      return Clock;
    case 'accepted':
      return CheckCircle;
    case 'rejected':
      return XCircle;
    case 'completed':
      return Star;
    default:
      return null;
  }
};

const getStatusText = (status: string) => {
  switch (status) {
    case 'pending':
      return 'Aguardando';
    case 'accepted':
      return 'Aceito';
    case 'rejected':
      return 'Recusado';
    case 'completed':
      return 'Concluído';
    default:
      return status;
  }
};

const filteredQuotes = computed(() =>
  activeTab.value === 'all'
    ? mockQuotes
    : mockQuotes.filter((q) => q.status === activeTab.value)
);
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <!-- Main Content -->
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <!-- Welcome Section -->
      <div class="mb-8">
        <h1 class="text-gray-900 mb-2">Bem-vindo, {{ user.name }}!</h1>
        <p class="text-gray-600">Gerencie suas solicitações e serviços contratados</p>
      </div>

      <!-- Stats -->
      <div class="grid md:grid-cols-4 gap-6 mb-8">
        <Card class="p-6">
          <div class="flex items-center justify-between mb-2">
            <span class="text-gray-600">Aguardando</span>
            <Clock class="text-yellow-500" :size="24" />
          </div>
          <p class="text-gray-900">
            {{ mockQuotes.filter((q) => q.status === 'pending').length }}
          </p>
        </Card>

        <Card class="p-6">
          <div class="flex items-center justify-between mb-2">
            <span class="text-gray-600">Em andamento</span>
            <CheckCircle class="text-green-500" :size="24" />
          </div>
          <p class="text-gray-900">
            {{ mockQuotes.filter((q) => q.status === 'accepted').length }}
          </p>
        </Card>

        <Card class="p-6">
          <div class="flex items-center justify-between mb-2">
            <span class="text-gray-600">Concluídos</span>
            <Star class="text-blue-500" :size="24" />
          </div>
          <p class="text-gray-900">
            {{ mockQuotes.filter((q) => q.status === 'completed').length }}
          </p>
        </Card>

        <Card class="p-6">
          <div class="flex items-center justify-between mb-2">
            <span class="text-gray-600">Mensagens</span>
            <MessageSquare class="text-purple-500" :size="24" />
          </div>
          <p class="text-gray-900">
            {{ mockQuotes.reduce((acc, q) => acc + q.messages, 0) }}
          </p>
        </Card>
      </div>

      <!-- Quick Actions -->
      <Card class="p-6 mb-8 bg-gradient-to-r from-blue-50 to-purple-50 border-blue-200">
        <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div>
            <h3 class="text-gray-900 mb-1">Precisa de um novo serviço?</h3>
            <p class="text-gray-600">Busque entre centenas de profissionais qualificados</p>
          </div>
          <Button @click="props.onNavigate('search')">
            <Search :size="18" class="mr-2" />
            Buscar Profissionais
          </Button>
        </div>
      </Card>

      <!-- Quotes -->
      <Card class="p-6">
        <h2 class="text-gray-900 mb-6">Minhas Solicitações</h2>

        <Tabs :model-value="activeTab" @update:model-value="(val: string) => (activeTab = val)">
          <TabsList class="mb-6">
            <TabsTrigger value="all"> Todos ({{ mockQuotes.length }}) </TabsTrigger>
            <TabsTrigger value="pending">
              Aguardando ({{ mockQuotes.filter((q) => q.status === 'pending').length }})
            </TabsTrigger>
            <TabsTrigger value="accepted">
              Em andamento ({{ mockQuotes.filter((q) => q.status === 'accepted').length }})
            </TabsTrigger>
            <TabsTrigger value="completed">
              Concluídos ({{ mockQuotes.filter((q) => q.status === 'completed').length }})
            </TabsTrigger>
          </TabsList>

          <TabsContent :value="activeTab">
            <div class="space-y-4">
              <Card
                v-for="quote in filteredQuotes"
                :key="quote.id"
                class="p-6 hover:shadow-md transition-shadow"
              >
                <div class="flex flex-col md:flex-row gap-6">
                  <img
                    :src="quote.providerAvatar"
                    :alt="quote.providerName"
                    class="w-16 h-16 rounded-lg object-cover"
                  />

                  <div class="flex-1">
                    <div class="flex items-start justify-between mb-2">
                      <div>
                        <h3 class="text-gray-900 mb-1">{{ quote.serviceTitle }}</h3>
                        <p class="text-gray-600">Prestador: {{ quote.providerName }}</p>
                      </div>
                      <Badge :class="getStatusColor(quote.status)">
                        <span class="flex items-center gap-1">
                          <component :is="getStatusIcon(quote.status)" :size="16" />
                          {{ getStatusText(quote.status) }}
                        </span>
                      </Badge>
                    </div>

                    <p class="text-gray-700 mb-4">{{ quote.description }}</p>

                    <div class="flex flex-wrap items-center gap-4 mb-4 text-gray-600">
                      <span class="text-gray-900">{{ quote.price }}</span>
                      <span>•</span>
                      <span>{{ new Date(quote.date).toLocaleDateString('pt-BR') }}</span>
                      <span>•</span>
                      <span>{{ quote.messages }} mensagens</span>
                    </div>

                    <div class="flex gap-2">
                      <Button
                        variant="outline"
                        @click="props.onNavigate('chat', undefined, quote.id)"
                      >
                        <MessageSquare :size="18" class="mr-2" />
                        Chat
                      </Button>

                      <Button v-if="quote.status === 'pending'" variant="default">
                        Aceitar
                      </Button>
                      <Button v-if="quote.status === 'pending'" variant="outline">
                        Recusar
                      </Button>

                      <Button v-if="quote.status === 'completed'" variant="default">
                        <Star :size="18" class="mr-2" />
                        Avaliar
                      </Button>

                      <Button
                        variant="ghost"
                        @click="props.onNavigate('provider-profile', quote.providerId)"
                      >
                        Ver Perfil
                      </Button>
                    </div>
                  </div>
                </div>
              </Card>

              <div v-if="filteredQuotes.length === 0" class="text-center py-12">
                <div class="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
                  <Search class="text-gray-400" :size="32" />
                </div>
                <h3 class="text-gray-900 mb-2">
                  Nenhuma solicitação
                  {{ activeTab !== 'all' ? `no status "${getStatusText(activeTab)}"` : '' }}
                </h3>
                <p class="text-gray-600 mb-4">Comece buscando profissionais para seus projetos</p>
                <Button @click="props.onNavigate('search')">Buscar Profissionais</Button>
              </div>
            </div>
          </TabsContent>
        </Tabs>
      </Card>
    </div>
  </div>
</template>




