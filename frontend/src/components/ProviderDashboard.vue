<script setup lang="ts">
import { ref, computed } from 'vue';
import {
  MessageSquare,
  Star,
  DollarSign,
  TrendingUp,
  Calendar,
  Clock,
  CheckCircle,
  XCircle,
  Search,
} from 'lucide-vue-next';
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
  clientName: string;
  clientAvatar: string;
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
    id: 'q1',
    providerId: '1',
    providerName: 'Carlos Silva',
    providerAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Carlos',
    clientName: 'Maria Santos',
    clientAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Maria',
    serviceTitle: 'Instalação de ventiladores de teto',
    status: 'pending',
    price: 'R$ 450,00',
    description: 'Instalação de 3 ventiladores de teto com acabamento e teste completo.',
    date: '2026-01-20',
    messages: 2,
  },
  {
    id: 'q2',
    providerId: '2',
    providerName: 'Fernanda Alves',
    providerAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Fernanda',
    clientName: 'João Silva',
    clientAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Joao',
    serviceTitle: 'Design de logo e identidade visual',
    status: 'accepted',
    price: 'R$ 2.500,00',
    description: 'Criação de logo, cartão de visitas, papelaria e manual de marca.',
    date: '2026-01-18',
    messages: 8,
  },
  {
    id: 'q3',
    providerId: '3',
    providerName: 'Roberto Pereira',
    providerAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Roberto',
    clientName: 'Ana Costa',
    clientAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Ana',
    serviceTitle: 'Reparo de vazamento',
    status: 'completed',
    price: 'R$ 280,00',
    description: 'Identificação e reparo de vazamento no banheiro.',
    date: '2026-01-15',
    messages: 3,
  },
];

const activeTab = ref('all');

const getStatusColor = (status: string) => {
  switch (status) {
    case 'pending':
      return 'bg-yellow-100 text-yellow-800';
    case 'accepted':
      return 'bg-green-100 text-green-800';
    case 'in_progress':
      return 'bg-purple-100 text-purple-800';
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
    case 'in_progress':
      return Clock;
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

const totalRevenue = computed(() => {
  return mockQuotes
    .filter((q) => q.status === 'completed')
    .reduce((acc, q) => {
      const priceValue = parseFloat(q.price.replace(/[^\d,]/g, '').replace(',', '.'));
      return acc + priceValue;
    }, 0)
    .toFixed(2);
});
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <!-- Main Content -->
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <!-- Welcome Section -->
      <div class="mb-8">
        <h1 class="text-gray-900 mb-2">Dashboard do Prestador</h1>
        <p class="text-gray-600">Gerencie suas solicitações e acompanhe seu desempenho</p>
      </div>

      <!-- Stats -->
      <div class="grid md:grid-cols-4 gap-6 mb-8">
        <Card class="p-6">
          <div class="flex items-center justify-between mb-2">
            <span class="text-gray-600">Novas solicitações</span>
            <MessageSquare class="text-yellow-500" :size="24" />
          </div>
          <p class="text-gray-900">
            {{ mockQuotes.filter((q) => q.status === 'pending').length }}
          </p>
          <p class="text-gray-600 mt-1">Aguardando resposta</p>
        </Card>

        <Card class="p-6">
          <div class="flex items-center justify-between mb-2">
            <span class="text-gray-600">Em andamento</span>
            <Calendar class="text-green-500" :size="24" />
          </div>
          <p class="text-gray-900">
            {{ mockQuotes.filter((q) => q.status === 'accepted').length }}
          </p>
          <p class="text-gray-600 mt-1">Serviços ativos</p>
        </Card>

        <Card class="p-6">
          <div class="flex items-center justify-between mb-2">
            <span class="text-gray-600">Concluídos</span>
            <Star class="text-blue-500" :size="24" />
          </div>
          <p class="text-gray-900">
            {{ mockQuotes.filter((q) => q.status === 'completed').length }}
          </p>
          <p class="text-gray-600 mt-1">Este mês</p>
        </Card>

        <Card class="p-6">
          <div class="flex items-center justify-between mb-2">
            <span class="text-gray-600">Faturamento</span>
            <DollarSign class="text-purple-500" :size="24" />
          </div>
          <p class="text-gray-900">R$ {{ totalRevenue }}</p>
          <p class="text-gray-600 mt-1">Este mês</p>
        </Card>
      </div>

      <!-- Performance Card -->
      <Card class="p-6 mb-8 bg-gradient-to-r from-green-50 to-blue-50 border-green-200">
        <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div>
            <h3 class="text-gray-900 mb-1">Seu desempenho está ótimo! 🎉</h3>
            <p class="text-gray-600">
              Você tem 98% de taxa de resposta e 4.9 estrelas de avaliação
            </p>
          </div>
          <Button @click="props.onNavigate('provider-profile', '1')">
            <TrendingUp :size="18" class="mr-2" />
            Ver Meu Perfil
          </Button>
        </div>
      </Card>

      <!-- Requests -->
      <Card class="p-6">
        <h2 class="text-gray-900 mb-6">Solicitações de Orçamento</h2>

        <Tabs :model-value="activeTab" @update:model-value="(val: string) => (activeTab = val)">
          <TabsList class="mb-6">
            <TabsTrigger value="pending">
              Novas ({{ mockQuotes.filter((q) => q.status === 'pending').length }})
            </TabsTrigger>
            <TabsTrigger value="accepted">
              Em andamento ({{ mockQuotes.filter((q) => q.status === 'accepted').length }})
            </TabsTrigger>
            <TabsTrigger value="completed">
              Concluídos ({{ mockQuotes.filter((q) => q.status === 'completed').length }})
            </TabsTrigger>
            <TabsTrigger value="all"> Todos ({{ mockQuotes.length }}) </TabsTrigger>
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
                    :src="quote.clientAvatar"
                    :alt="quote.clientName"
                    class="w-16 h-16 rounded-lg object-cover"
                  />

                  <div class="flex-1">
                    <div class="flex items-start justify-between mb-2">
                      <div>
                        <h3 class="text-gray-900 mb-1">{{ quote.serviceTitle }}</h3>
                        <p class="text-gray-600">Cliente: {{ quote.clientName }}</p>
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
                      <template v-if="quote.status !== 'pending'">
                        <span class="text-gray-900">{{ quote.price }}</span>
                        <span>•</span>
                      </template>
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
                        Responder
                      </Button>

                      <template v-if="quote.status === 'pending'">
                        <Button variant="default">Enviar Orçamento</Button>
                        <Button variant="outline">Recusar</Button>
                      </template>

                      <Button v-if="quote.status === 'accepted'" variant="default">
                        Marcar como Concluído
                      </Button>

                      <Badge v-if="quote.status === 'completed'" variant="secondary" class="px-3 py-2">
                        ⭐ Aguardando avaliação do cliente
                      </Badge>
                    </div>
                  </div>
                </div>
              </Card>

              <div v-if="filteredQuotes.length === 0" class="text-center py-12">
                <div
                  class="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4"
                >
                  <Search class="text-gray-400" :size="32" />
                </div>
                <h3 class="text-gray-900 mb-2">
                  Nenhuma solicitação
                  {{ activeTab !== 'all' ? `no status "${getStatusText(activeTab)}"` : '' }}
                </h3>
                <p class="text-gray-600">
                  {{
                    activeTab === 'pending'
                      ? 'Você receberá notificações quando novos clientes solicitarem seus serviços'
                      : 'Não há solicitações nesta categoria no momento'
                  }}
                </p>
              </div>
            </div>
          </TabsContent>
        </Tabs>
      </Card>

      <!-- Tips Card -->
      <Card class="p-6 mt-8">
        <h3 class="text-gray-900 mb-4">💡 Dicas para melhorar seu perfil</h3>
        <ul class="space-y-2 text-gray-700">
          <li class="flex items-start gap-2">
            <span class="text-blue-600">•</span>
            <span>Responda rapidamente às solicitações para manter uma boa taxa de resposta</span>
          </li>
          <li class="flex items-start gap-2">
            <span class="text-blue-600">•</span>
            <span>Mantenha seu portfólio atualizado com fotos de trabalhos recentes</span>
          </li>
          <li class="flex items-start gap-2">
            <span class="text-blue-600">•</span>
            <span>Peça aos clientes satisfeitos para deixarem avaliações</span>
          </li>
          <li class="flex items-start gap-2">
            <span class="text-blue-600">•</span>
            <span>Seja claro e detalhado nos orçamentos que você enviar</span>
          </li>
        </ul>
      </Card>
    </div>
  </div>
</template>




