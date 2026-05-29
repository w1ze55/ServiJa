<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import MainLayout from '@/components/layout/MainLayout.vue';
import { chatService } from '@/api/services/chatService';
import type { ConversationDTO } from '@/api/services/chatService';
import { useNavigation } from '@/utils/navigation';
import { MessageSquare, Search } from 'lucide-vue-next';

const router = useRouter();
const { navigate, logout } = useNavigation();

const user = ref<any>(null);
const conversations = ref<ConversationDTO[]>([]);
const loading = ref(true);
const searchQuery = ref('');

onMounted(async () => {
  const userData = localStorage.getItem('user');
  if (!userData) { router.push('/login'); return; }
  user.value = JSON.parse(userData);

  try {
    conversations.value = await chatService.listConversations();
  } finally {
    loading.value = false;
  }
});

function displayName(email: string) {
  if (!email) return '';
  return email.split('@')[0].replace(/[._]/g, ' ');
}

function otherParty(conv: ConversationDTO) {
  return user.value?.email === conv.contractorEmail ? conv.providerEmail : conv.contractorEmail;
}

function unreadForMe(conv: ConversationDTO): number {
  const role = user.value?.type === 'provider' ? 'provider' : 'contractor';
  return conv.unreadCount?.[role] ?? 0;
}

function formatTime(iso: string | null | undefined) {
  if (!iso) return '';
  const d = new Date(iso);
  const now = new Date();
  const sameDay =
    d.getDate() === now.getDate() &&
    d.getMonth() === now.getMonth() &&
    d.getFullYear() === now.getFullYear();
  return sameDay
    ? d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
    : d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' });
}

const filtered = computed(() => {
  const q = searchQuery.value.toLowerCase();
  if (!q) return conversations.value;
  return conversations.value.filter((c) =>
    otherParty(c).toLowerCase().includes(q)
  );
});

const handleNavigate = (page: string, params?: any) => {
  const userType = user.value?.type === 'provider' ? 'provider' : 'contractor';
  navigate(page, userType, params);
};
const handleLogout = () => logout();
const handleSearch = (query: string) => router.push({ path: '/search', query: { q: query } });
</script>

<template>
  <MainLayout
    v-if="user"
    :user="user"
    current-page="messages"
    :on-navigate="handleNavigate"
    :on-logout="handleLogout"
    :on-search="handleSearch"
  >
    <div class="max-w-2xl mx-auto">
      <h1 class="text-2xl font-bold text-gray-900 mb-6">Mensagens</h1>

      <!-- Busca -->
      <div class="relative mb-4">
        <Search class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
        <input
          v-model="searchQuery"
          placeholder="Buscar conversa..."
          class="w-full pl-9 pr-4 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      <!-- Loading -->
      <div v-if="loading" class="space-y-3">
        <div
          v-for="i in 4"
          :key="i"
          class="h-20 bg-gray-100 rounded-xl animate-pulse"
        />
      </div>

      <!-- Vazio -->
      <div
        v-else-if="filtered.length === 0"
        class="text-center py-16 text-gray-400"
      >
        <MessageSquare class="h-12 w-12 mx-auto mb-3 text-gray-200" />
        <p class="font-medium">Nenhuma conversa ainda</p>
        <p class="text-sm mt-1">
          As conversas aparecem automaticamente após a confirmação do pagamento.
        </p>
      </div>

      <!-- Lista -->
      <div v-else class="space-y-2">
        <button
          v-for="conv in filtered"
          :key="conv.id"
          @click="router.push(`/chat/${conv.serviceRequestId}`)"
          class="w-full flex items-center gap-4 p-4 bg-white rounded-xl border border-gray-200 hover:border-blue-300 hover:shadow-sm transition-all text-left"
        >
          <!-- Avatar inicial -->
          <div class="flex-shrink-0 h-11 w-11 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 font-semibold text-sm uppercase">
            {{ displayName(otherParty(conv)).charAt(0) || '?' }}
          </div>

          <!-- Info -->
          <div class="flex-1 min-w-0">
            <div class="flex items-center justify-between mb-0.5">
              <span class="font-medium text-gray-900 text-sm capitalize truncate">
                {{ displayName(otherParty(conv)) || otherParty(conv) }}
              </span>
              <span class="text-xs text-gray-400 ml-2 flex-shrink-0">
                {{ formatTime(conv.lastMessage?.sentAt) }}
              </span>
            </div>
            <div class="flex items-center justify-between">
              <p class="text-xs text-gray-500 truncate max-w-[260px]">
                {{ conv.lastMessage?.content || 'Conversa iniciada' }}
              </p>
              <span
                v-if="unreadForMe(conv) > 0"
                class="ml-2 flex-shrink-0 h-5 w-5 rounded-full bg-blue-600 text-white text-xs flex items-center justify-center font-semibold"
              >
                {{ unreadForMe(conv) > 9 ? '9+' : unreadForMe(conv) }}
              </span>
            </div>
          </div>
        </button>
      </div>
    </div>
  </MainLayout>
</template>
