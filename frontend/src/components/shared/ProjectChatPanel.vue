<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import ChatBox from './ChatBox.vue';
import type { ChatMessage } from './ChatBox.vue';
import { chatService } from '@/api/services/chatService';
import type { ConversationDTO, MessageDTO } from '@/api/services/chatService';
import { useChatWebSocket } from '@/composables/useChatWebSocket';
import { uploadFiles, getUploadErrorMessage } from '@/api/uploadService';
import { toast } from '@/utils/toast';
import {
  getAttachmentPlaceholder,
  resolveAttachmentKindFromFile,
  resolveChatMessageType,
} from '@/utils/chatAttachments';

interface Props {
  serviceRequestId: number;
  currentUserRole: 'client' | 'freelancer' | 'contractor' | 'provider';
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
  try {
    const rawUser = localStorage.getItem('user');
    if (rawUser) {
      const parsedUser = JSON.parse(rawUser);
      if (parsedUser?.email) {
        return String(parsedUser.email);
      }
    }
  } catch {
    // Fallback para o token abaixo.
  }

  const token = localStorage.getItem('token');
  if (!token) return '';
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.sub || payload.email || '';
  } catch {
    return '';
  }
}

function sameEmail(a?: string | null, b?: string | null) {
  return !!a && !!b && a.trim().toLowerCase() === b.trim().toLowerCase();
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
  const isMe = sameEmail(msg.senderId, userEmail.value);
  const messageType = resolveChatMessageType(msg.type, msg.attachmentUrl);

  return {
    id: msg.id,
    sender: msg.senderType === 'contractor' ? 'client' : 'freelancer',
    isOwn: isMe,
    senderName: isMe ? 'Você' : displayName(msg.senderId),
    type: messageType,
    message: msg.content,
    timestamp: msg.sentAt,
    attachments: msg.attachmentUrl
      ? [
          {
            id: msg.id,
            name: resolveAttachmentName(msg),
            url: msg.attachmentUrl,
            size: '',
            type: messageType,
          },
        ]
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
      if (messages.value.some((message) => message.id === msg.id)) return;
      messages.value.push(toDisplay(msg));
      chatService.markAsRead(conversation.value!.id).catch(() => {});
    };
    connect(conversation.value.id);
  } catch (e: any) {
    const message = e?.response?.data?.message || e?.message || '';
    if (message.toLowerCase().includes('habilitado')) {
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
      if (!messages.value.some((message) => message.id === msg.id)) {
        messages.value.push(toDisplay(msg));
      }
    }
    if (attachments.length > 0) {
      const urls = await uploadFiles(attachments);
      for (const [index, url] of urls.entries()) {
        const file = attachments[index];
        const fileName = file?.name || 'Anexo';
        const attachmentType = file ? resolveAttachmentKindFromFile(file) : 'file';
        const msg = await chatService.sendMessage(
          conversation.value.id,
          getAttachmentPlaceholder(attachmentType),
          attachmentType,
          url,
          fileName
        );
        if (!messages.value.some((message) => message.id === msg.id)) {
          messages.value.push(toDisplay(msg));
        }
      }
    }
  } catch (err) {
    toast.error(getUploadErrorMessage(err));
  } finally {
    isSending.value = false;
  }
}
</script>

<template>
  <div class="flex h-[32rem] min-h-[32rem] flex-col">
    <div v-if="loading" class="flex flex-1 items-center justify-center py-16 text-gray-400">
      Carregando chat...
    </div>
    <div
      v-else-if="error"
      class="flex flex-1 items-center justify-center rounded-xl border border-dashed border-gray-200 bg-gray-50 p-10 text-center text-sm text-gray-500"
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
