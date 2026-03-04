<script setup lang="ts">
import { ref, computed } from 'vue';
import { Send, Paperclip, ArrowLeft, MoreVertical } from 'lucide-vue-next';
import Button from './ui/button.vue';
import Card from './ui/card.vue';
import Textarea from './ui/textarea.vue';

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

interface Message {
  id: string;
  senderId: string;
  senderName: string;
  senderAvatar: string;
  content: string;
  timestamp: string;
  isOwn: boolean;
}

interface Quote {
  id: string;
  providerId: string;
  providerName: string;
  providerAvatar: string;
  serviceTitle: string;
  status: 'pending' | 'accepted' | 'in_progress' | 'rejected' | 'completed';
  price?: string;
  description: string;
  date: string;
  messages: number;
}

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
  portfolio: any[];
  reviews: any[];
  aiSummary: string;
}

interface Props {
  chatId: string;
  onNavigate: (page: Page, providerId?: string, chatId?: string) => void;
  user: User;
  quote?: Quote;
  provider?: Provider;
}

const props = defineProps<Props>();

// Mock data - em produção viria de props ou store
const mockQuotes: Quote[] = props.quote
  ? [props.quote]
  : [
      {
        id: props.chatId,
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
    ];

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
        description: 'Eletricista profissional',
        skills: [],
        availability: 'Seg-Sex: 8h-18h',
        responseTime: '~2 horas',
        portfolio: [],
        reviews: [],
        aiSummary: '',
      },
    ];

const quote = computed(() => mockQuotes.find((q) => q.id === props.chatId));
const provider = computed(() =>
  providers.find((p) => p.id === quote.value?.providerId)
);

const message = ref('');
const messages = ref<Message[]>([
  {
    id: 'm1',
    senderId: props.user.type === 'contractor' ? 'contractor' : 'provider',
    senderName: props.user.name,
    senderAvatar: props.user.avatar || '',
    content: 'Olá! Gostaria de solicitar um orçamento para o serviço.',
    timestamp: '2026-01-20T10:00:00',
    isOwn: true,
  },
  {
    id: 'm2',
    senderId: props.user.type === 'contractor' ? 'provider' : 'contractor',
    senderName:
      props.user.type === 'contractor'
        ? provider.value?.name || 'Prestador'
        : 'Cliente',
    senderAvatar:
      props.user.type === 'contractor'
        ? provider.value?.avatar || ''
        : 'https://api.dicebear.com/7.x/avataaars/svg?seed=client',
    content: 'Olá! Claro, vou precisar de algumas informações. Pode me descrever melhor o trabalho?',
    timestamp: '2026-01-20T10:15:00',
    isOwn: false,
  },
]);

const handleSendMessage = () => {
  if (!message.value.trim()) return;

  const newMessage: Message = {
    id: `m${messages.value.length + 1}`,
    senderId: props.user.id,
    senderName: props.user.name,
    senderAvatar: props.user.avatar || '',
    content: message.value,
    timestamp: new Date().toISOString(),
    isOwn: true,
  };

  messages.value = [...messages.value, newMessage];
  message.value = '';
};

const handleKeyPress = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    handleSendMessage();
  }
};

const otherParty = computed(() =>
  props.user.type === 'contractor'
    ? { name: provider.value?.name || 'Prestador', avatar: provider.value?.avatar || '' }
    : {
        name: 'Cliente ' + props.chatId,
        avatar: `https://api.dicebear.com/7.x/avataaars/svg?seed=client${props.chatId}`,
      }
);
</script>

<template>
  <div v-if="!quote || !provider" class="min-h-screen bg-gray-50 flex items-center justify-center">
    <p>Chat não encontrado</p>
  </div>

  <div v-else class="min-h-screen bg-gray-50 flex flex-col">
    <!-- Header -->
    <header class="bg-white shadow-sm">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-4">
            <Button
              variant="ghost"
              size="sm"
              @click="
                props.onNavigate(
                  props.user.type === 'provider' ? 'provider-dashboard' : 'contractor-dashboard'
                )
              "
            >
              <ArrowLeft :size="20" />
            </Button>

            <img :src="otherParty.avatar" :alt="otherParty.name" class="w-10 h-10 rounded-full" />

            <div>
              <h2 class="text-gray-900">{{ otherParty.name }}</h2>
              <p class="text-gray-600">{{ quote?.serviceTitle }}</p>
            </div>
          </div>

          <div class="flex items-center gap-3">
            <Button variant="ghost" size="sm">
              <MoreVertical :size="20" />
            </Button>
          </div>
        </div>
      </div>
    </header>

    <!-- Chat Content -->
    <div class="flex-1 max-w-5xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6 flex flex-col">
      <!-- Service Info Card -->
      <Card class="p-4 mb-4 bg-blue-50 border-blue-200">
        <div class="flex items-start justify-between gap-4">
          <div>
            <h3 class="text-gray-900 mb-1">{{ quote?.serviceTitle }}</h3>
            <p class="text-gray-600">{{ quote?.description }}</p>
            <p v-if="quote?.price" class="text-gray-900 mt-2">Orçamento: {{ quote.price }}</p>
          </div>
          <Button
            size="sm"
            @click="
              props.onNavigate(
                props.user.type === 'contractor' ? 'provider-profile' : 'contractor-dashboard',
                provider?.id
              )
            "
          >
            Ver Detalhes
          </Button>
        </div>
      </Card>

      <!-- Messages -->
      <div class="flex-1 space-y-4 mb-4 overflow-y-auto">
        <div
          v-for="msg in messages"
          :key="msg.id"
          :class="`flex gap-3 ${msg.isOwn ? 'flex-row-reverse' : 'flex-row'}`"
        >
          <img
            :src="msg.senderAvatar"
            :alt="msg.senderName"
            class="w-8 h-8 rounded-full flex-shrink-0"
          />

          <div :class="`flex flex-col ${msg.isOwn ? 'items-end' : 'items-start'} max-w-xl`">
            <div class="flex items-center gap-2 mb-1">
              <span class="text-gray-600">{{ msg.senderName }}</span>
              <span class="text-gray-400">
                {{
                  new Date(msg.timestamp).toLocaleTimeString('pt-BR', {
                    hour: '2-digit',
                    minute: '2-digit',
                  })
                }}
              </span>
            </div>

            <div
              :class="`p-3 rounded-lg ${
                msg.isOwn ? 'bg-blue-600 text-white' : 'bg-white text-gray-900 border'
              }`"
            >
              <p class="whitespace-pre-wrap">{{ msg.content }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Message Input -->
      <Card class="p-4">
        <div class="flex items-end gap-2">
          <Button variant="ghost" size="sm">
            <Paperclip :size="20" />
          </Button>

          <Textarea
            placeholder="Digite sua mensagem..."
            v-model="message"
            @keydown="handleKeyPress"
            :rows="2"
            class="flex-1 resize-none"
          />

          <Button @click="handleSendMessage" :disabled="!message.trim()">
            <Send :size="20" />
          </Button>
        </div>

        <p class="text-gray-500 mt-2">Pressione Enter para enviar, Shift+Enter para nova linha</p>
      </Card>
    </div>
  </div>
</template>

