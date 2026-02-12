<script setup lang="ts">
import { ref } from 'vue';
import { MessageSquare, Search, Clock } from 'lucide-vue-next';
import Input from '../ui/input.vue';
import Avatar from '../ui/avatar.vue';
import AvatarImage from '../ui/avatar-image.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import Badge from '../ui/badge.vue';
import Card from '../ui/card.vue';

interface Conversation {
  id: string;
  freelancerId: string;
  freelancerName: string;
  freelancerAvatar?: string;
  lastMessage: string;
  lastMessageTime: string;
  unreadCount: number;
  isOnline: boolean;
  serviceContext?: string;
}

interface Props {
  onNavigate: (page: string, params?: any) => void;
}

const props = defineProps<Props>();

// Mock data - substituir por dados reais da API
const conversations = ref<Conversation[]>([
  {
    id: '1',
    freelancerId: '1',
    freelancerName: 'João Silva',
    freelancerAvatar: '',
    lastMessage: 'Olá! Posso começar o serviço amanhã pela manhã.',
    lastMessageTime: 'há 5 min',
    unreadCount: 2,
    isOnline: true,
    serviceContext: 'Instalação Elétrica',
  },
  {
    id: '2',
    freelancerId: '2',
    freelancerName: 'Maria Santos',
    freelancerAvatar: '',
    lastMessage: 'Obrigada pela contratação! Vou enviar o cronograma.',
    lastMessageTime: 'há 1 hora',
    unreadCount: 0,
    isOnline: true,
    serviceContext: 'Design Gráfico',
  },
  {
    id: '3',
    freelancerId: '3',
    freelancerName: 'Pedro Oliveira',
    freelancerAvatar: '',
    lastMessage: 'Você: Perfeito, obrigado!',
    lastMessageTime: 'há 2 horas',
    unreadCount: 0,
    isOnline: false,
    serviceContext: 'Encanamento',
  },
  {
    id: '4',
    freelancerId: '4',
    freelancerName: 'Ana Costa',
    freelancerAvatar: '',
    lastMessage: 'Concluí a primeira etapa do projeto. Por favor, confira.',
    lastMessageTime: 'Ontem',
    unreadCount: 1,
    isOnline: false,
    serviceContext: 'Desenvolvimento Web',
  },
  {
    id: '5',
    freelancerId: '5',
    freelancerName: 'Carlos Mendes',
    freelancerAvatar: '',
    lastMessage: 'Você: Quando você pode começar?',
    lastMessageTime: '2 dias atrás',
    unreadCount: 0,
    isOnline: false,
    serviceContext: 'Pintura Residencial',
  },
]);

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
};

const handleConversationClick = (conversation: Conversation) => {
  props.onNavigate('chat', {
    freelancerId: conversation.freelancerId,
    returnPage: 'messages',
  });
};
</script>

<template>
  <div class="max-w-5xl mx-auto space-y-6">
    <!-- Header -->
    <div>
      <h1 class="text-[#263238] mb-2">Mensagens</h1>
      <p class="text-[#435574]">
        Converse com seus freelancers e acompanhe seus projetos
      </p>
    </div>

    <!-- Search -->
    <div class="relative">
      <Search class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-[#435574]" />
      <Input placeholder="Buscar conversas..." class="pl-10" />
    </div>

    <!-- Conversations List -->
    <div class="space-y-3">
      <Card v-if="conversations.length === 0" class="p-12 text-center">
        <div class="flex flex-col items-center gap-4">
          <div class="w-16 h-16 rounded-full bg-gray-100 flex items-center justify-center">
            <MessageSquare class="h-8 w-8 text-gray-400" />
          </div>
          <div>
            <h3 class="text-[#263238] mb-2">Nenhuma mensagem ainda</h3>
            <p class="text-[#435574] max-w-md mx-auto">
              Quando você conversar com freelancers ou iniciar projetos,
              suas conversas aparecerão aqui.
            </p>
          </div>
        </div>
      </Card>
      <Card
        v-for="conversation in conversations"
        :key="conversation.id"
        @click="handleConversationClick(conversation)"
        class="p-4 cursor-pointer hover:shadow-md transition-shadow"
      >
        <div class="flex items-center gap-4">
          <!-- Avatar -->
          <div class="relative">
            <Avatar class="h-14 w-14">
              <AvatarImage
                :src="conversation.freelancerAvatar"
                :alt="conversation.freelancerName"
              />
              <AvatarFallback class="bg-[#286AD8] text-white">
                {{ getInitials(conversation.freelancerName) }}
              </AvatarFallback>
            </Avatar>
            <div
              v-if="conversation.isOnline"
              class="absolute bottom-0 right-0 w-4 h-4 bg-green-500 border-2 border-white rounded-full"
            />
          </div>

          <!-- Content -->
          <div class="flex-1 min-w-0">
            <div class="flex items-start justify-between mb-1">
              <div class="flex-1 min-w-0">
                <h3 class="text-[#263238] truncate">
                  {{ conversation.freelancerName }}
                </h3>
                <p v-if="conversation.serviceContext" class="text-xs text-[#435574] truncate">
                  {{ conversation.serviceContext }}
                </p>
              </div>
              <div class="flex items-center gap-2 ml-2">
                <span class="text-xs text-[#435574] whitespace-nowrap">
                  {{ conversation.lastMessageTime }}
                </span>
                <Badge
                  v-if="conversation.unreadCount > 0"
                  class="bg-[#286AD8] hover:bg-[#286AD8] text-white rounded-full h-5 min-w-[20px] px-1.5"
                >
                  {{ conversation.unreadCount }}
                </Badge>
              </div>
            </div>
            <p
              :class="`text-sm truncate ${
                conversation.unreadCount > 0 ? 'text-[#263238]' : 'text-[#435574]'
              }`"
            >
              {{ conversation.lastMessage }}
            </p>
          </div>
        </div>
      </Card>
    </div>

    <!-- Empty State Alternative -->
    <div v-if="conversations.length === 0" class="text-center py-8">
      <p class="text-sm text-[#435574]">
        Comece explorando serviços e contratando freelancers
      </p>
    </div>
  </div>
</template>

