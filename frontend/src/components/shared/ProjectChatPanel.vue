<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import ChatBox from './ChatBox.vue';
import type { ChatMessage } from './ChatBox.vue';
import { chatService } from '@/api/services/chatService';
import type { ConversationDTO, MessageDTO } from '@/api/services/chatService';
import { useChatWebSocket } from '@/composables/useChatWebSocket';
import { uploadFiles } from '@/api/uploadService';
import { toast } from '@/utils/toast';

interface Props {
  serviceRequestId: number;
  currentUserRole: 'client' | 'freelancer';
  serviceStatus?: string;
}

const props = defineProps<Props>();

const conversation = ref<ConversationDTO | null>(null);
const messages = ref<ChatMessage[]>([]);
const isSending = ref(false);
const loading = ref(true);
const error = ref('');
const userEmail = ref('');

const isLocked = computed(
  () => (conversation.value?.locked ?? false) || props.serviceStatus === 'completed'
);

const { onMessage, connect, disconnect } = useChatWebSocket();

function getUserEmail(): string {
  const token = localStorage.getItem('token');
  if (!token) return '';
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.sub || payload.email || '';
  } catch {
    return '';
  }
}

function displayName(email: string) {
  return email.split('@')[0].replace(/[._]/g, ' ');
}

function resolveAttachmentName(msg: MessageDTO) {
  if (msg.attachmentName?.trim()) {
    return msg.attachmentName.trim();
  }

  if (!msg.attachmentUrl) {
    return 'Anexo';
  }

  try {
    const lastSegment = new URL(msg.attachmentUrl).pathname.split('/').pop();
    return lastSegment ? decodeURIComponent(lastSegment) : 'Anexo';
  } catch {
    const lastSegment = msg.attachmentUrl.split('/').pop();
    return lastSegment ? decodeURIComponent(lastSegment) : 'Anexo';
  }
}

function toDisplay(msg: MessageDTO): ChatMessage {
  const isMe = msg.senderId === userEmail.value;
  return {
    id: msg.id,
    sender: msg.senderType === 'contractor' ? 'client' : 'freelancer',
    senderName: isMe ? 'Você' : displayName(msg.senderId),
    message: msg.content,
    timestamp: msg.sentAt,
    attachments: msg.attachmentUrl
      ? [{ id: msg.id, name: resolveAttachmentName(msg), url: msg.attachmentUrl, size: '' }]
      : undefined,
  };
}

onMounted(async () => {
  userEmail.value = getUserEmail();
  try {
    conversation.value = await chatService.getOrCreateConversation(props.serviceRequestId);
    const page = await chatService.getMessages(conversation.value.id);
    messages.value = page.content.map(toDisplay);
    chatService.markAsRead(conversation.value.id).catch(() => {});

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

onUnmounted(disconnect);

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
      for (const [index, url] of urls.entries()) {
        const fileName = attachments[index]?.name || 'Anexo';
        const isImage = /\.(jpg|jpeg|png|gif|webp)$/i.test(url);
        const msg = await chatService.sendMessage(
          conversation.value.id,
          isImage ? 'Imagem' : 'Arquivo',
          isImage ? 'image' : 'file',
          url,
          fileName
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
</script>

<template>
  <div class="flex h-[32rem] min-h-[32rem] flex-col">
    <div v-if="loading" class="flex-1 flex items-center justify-center text-gray-400 py-16">
      Carregando chat...
    </div>
    <div
      v-else-if="error"
      class="flex-1 flex items-center justify-center rounded-xl border border-dashed border-gray-200 bg-gray-50 p-10 text-center text-sm text-gray-500"
    >
      {{ error }}
    </div>
    <ChatBox
      v-else
      :messages="messages"
      :current-user-role="currentUserRole"
      :is-locked="isLocked"
      :is-sending="isSending"
      :service-status="(serviceStatus as any)"
      @send-message="handleSend"
    />
  </div>
</template>
