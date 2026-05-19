<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import MainLayout from '@/components/layout/MainLayout.vue';
import ChatBox from '@/components/shared/ChatBox.vue';
import type { ChatMessage } from '@/components/shared/ChatBox.vue';
import { chatService } from '@/api/services/chatService';
import type { ConversationDTO, MessageDTO } from '@/api/services/chatService';
import { useChatWebSocket } from '@/composables/useChatWebSocket';
import { uploadFiles } from '@/api/uploadService';
import { useNavigation } from '@/utils/navigation';
import { toast } from '@/utils/toast';
import { ArrowLeft } from 'lucide-vue-next';

const route = useRoute();
const router = useRouter();
const { navigate, logout } = useNavigation();

const serviceRequestId = Number(route.params.serviceRequestId);
const user = ref<any>(null);
const conversation = ref<ConversationDTO | null>(null);
const messages = ref<ChatMessage[]>([]);
const isSending = ref(false);
const loading = ref(true);
const error = ref('');

const { onMessage, connect } = useChatWebSocket();

const currentUserRole = computed<'client' | 'freelancer'>(() =>
  user.value?.type === 'provider' ? 'freelancer' : 'client'
);

const otherPartyEmail = computed(() => {
  if (!conversation.value || !user.value) return '';
  return user.value.email === conversation.value.contractorEmail
    ? conversation.value.providerEmail
    : conversation.value.contractorEmail;
});

function displayName(email: string) {
  return email.split('@')[0].replace(/[._]/g, ' ');
}

function toDisplay(msg: MessageDTO): ChatMessage {
  const isMe = msg.senderId === user.value?.email;
  return {
    id: msg.id,
    sender: msg.senderType === 'contractor' ? 'client' : 'freelancer',
    senderName: isMe ? user.value.name : displayName(msg.senderId),
    message: msg.content,
    timestamp: msg.sentAt,
    attachments: msg.attachmentUrl
      ? [{ id: msg.id, name: 'Anexo', url: msg.attachmentUrl, size: '' }]
      : undefined,
  };
}

onMounted(async () => {
  const userData = localStorage.getItem('user');
  if (!userData) { router.push('/login'); return; }
  user.value = JSON.parse(userData);

  try {
    conversation.value = await chatService.getOrCreateConversation(serviceRequestId);

    const page = await chatService.getMessages(conversation.value.id);
    messages.value = page.content.map(toDisplay);

    await chatService.markAsRead(conversation.value.id);

    onMessage.value = (msg: MessageDTO) => {
      if (messages.value.some((m) => m.id === msg.id)) return;
      messages.value.push(toDisplay(msg));
      chatService.markAsRead(conversation.value!.id).catch(() => {});
    };
    connect(conversation.value.id);
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '';
    if (msg.toLowerCase().includes('habilitado')) {
      error.value = 'O chat ainda não está disponível. Aguarde a confirmação do pagamento.';
    } else {
      error.value = 'Erro ao carregar o chat.';
    }
  } finally {
    loading.value = false;
  }
});

async function handleSend(messageText: string, attachments: File[]) {
  if (isSending.value || !conversation.value) return;
  isSending.value = true;

  try {
    if (messageText.trim()) {
      const msg = await chatService.sendMessage(conversation.value.id, messageText.trim());
      if (!messages.value.some((m) => m.id === msg.id)) {
        messages.value.push(toDisplay(msg));
      }
    }

    if (attachments.length > 0) {
      const urls = await uploadFiles(attachments);
      for (const url of urls) {
        const isImage = /\.(jpg|jpeg|png|gif|webp)$/i.test(url);
        const msg = await chatService.sendMessage(
          conversation.value.id,
          isImage ? '📷 Imagem' : '📎 Arquivo',
          isImage ? 'image' : 'file',
          url
        );
        if (!messages.value.some((m) => m.id === msg.id)) {
          messages.value.push(toDisplay(msg));
        }
      }
    }
  } catch {
    toast.error('Erro ao enviar mensagem');
  } finally {
    isSending.value = false;
  }
}

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
    <div class="max-w-3xl mx-auto">
      <!-- Header -->
      <div class="flex items-center gap-3 mb-4">
        <button
          @click="router.push('/messages')"
          class="p-2 rounded-full hover:bg-gray-100 transition-colors"
        >
          <ArrowLeft class="h-5 w-5 text-gray-600" />
        </button>
        <div>
          <h1 class="text-lg font-semibold text-gray-900">
            {{ otherPartyEmail ? displayName(otherPartyEmail) : 'Chat' }}
          </h1>
          <p class="text-xs text-gray-500">Solicitação #{{ serviceRequestId }}</p>
        </div>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="flex items-center justify-center py-24 text-gray-400">
        Carregando...
      </div>

      <!-- Error -->
      <div
        v-else-if="error"
        class="rounded-xl border border-dashed border-gray-200 bg-gray-50 p-10 text-center text-sm text-gray-500"
      >
        {{ error }}
      </div>

      <!-- Chat -->
      <div
        v-else
        class="bg-white rounded-xl border border-gray-200 shadow-sm p-4 min-h-[520px] flex flex-col"
      >
        <ChatBox
          :messages="messages"
          :current-user-role="currentUserRole"
          :is-locked="conversation?.locked ?? false"
          :is-sending="isSending"
          @send-message="handleSend"
        />
      </div>
    </div>
  </MainLayout>
</template>
