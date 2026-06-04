<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue';
import { Send, Paperclip, X, Lock, MessageSquare } from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Textarea from '../ui/textarea.vue';
import FileAttachment from '../shared/FileAttachment.vue';
import { toast } from '@/utils/toast';

export interface ChatMessage {
  id: string;
  sender: 'client' | 'freelancer';
  senderName: string;
  message: string;
  timestamp: string;
  status?: string;
  readAt?: string | null;
  attachments?: Array<{
    id: string;
    name: string;
    url: string;
    size: string;
  }>;
}

interface Props {
  messages: ChatMessage[];
  currentUserRole: 'client' | 'freelancer';
  isLocked: boolean;
  isSending?: boolean;
  serviceStatus?:
    | 'pending'
    | 'sent'
    | 'negotiating'
    | 'accepted'
    | 'in_progress'
    | 'delivered'
    | 'rejected'
    | 'completed'
    | 'cancelled'
    | 'archived';
}

const props = defineProps<Props>();

const lockMessage = computed(() => {
  if (props.serviceStatus === 'completed') {
    return 'Serviço concluído. O chat foi encerrado.';
  } else if (props.serviceStatus === 'rejected') {
    return 'Serviço rejeitado. O chat foi encerrado.';
  } else if (props.serviceStatus === 'archived') {
    return 'Serviço arquivado. O chat foi encerrado.';
  }
  return 'O chat será liberado automaticamente após a confirmação do pagamento.';
});

const emit = defineEmits<{
  (e: 'send-message', message: string, attachments: File[]): void;
}>();

const newMessage = ref('');
const messageAttachments = ref<File[]>([]);
const messageAttachmentInput = ref<HTMLInputElement | null>(null);
const scrollContainer = ref<HTMLElement | null>(null);

async function scrollToBottom() {
  await nextTick();
  const el = scrollContainer.value;
  if (el) el.scrollTop = el.scrollHeight;
}

onMounted(scrollToBottom);
watch(() => props.messages.length, scrollToBottom);

const handleAttachmentChange = (event: Event) => {
  const target = event.target as HTMLInputElement;
  if (!target.files) return;

  const newFiles = Array.from(target.files);
  const totalSize = messageAttachments.value.reduce((acc, file) => acc + file.size, 0) +
    newFiles.reduce((acc, file) => acc + file.size, 0);

  if (totalSize > 25 * 1024 * 1024) {
    toast.error('O tamanho total dos anexos não pode exceder 25MB');
    return;
  }

  messageAttachments.value = [...messageAttachments.value, ...newFiles];
  if (messageAttachmentInput.value) {
    messageAttachmentInput.value.value = '';
  }
};

const removeAttachment = (index: number) => {
  messageAttachments.value.splice(index, 1);
};

const send = () => {
  const message = newMessage.value.trim();

  if (!message && messageAttachments.value.length === 0) {
    toast.error('Digite uma mensagem ou adicione um anexo');
    return;
  }

  emit('send-message', message, [...messageAttachments.value]);
  newMessage.value = '';
  messageAttachments.value = [];
};

const handleComposerKeydown = (event: KeyboardEvent) => {
  if (event.key !== 'Enter' || event.shiftKey || event.isComposing) {
    return;
  }

  event.preventDefault();

  if (props.isSending) {
    return;
  }

  send();
};

const formatDate = (dateString: string) => {
  const date = new Date(dateString);
  if (isNaN(date.getTime())) return dateString;
  return date.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
};
</script>

<template>
  <div class="flex h-full w-full flex-col gap-4">
    <div
      v-if="!messages || messages.length === 0"
      class="flex flex-1 items-center justify-center rounded-xl border border-dashed border-gray-200 bg-gray-50 px-6 py-8 text-center"
    >
      <div>
        <MessageSquare class="mx-auto mb-3 h-10 w-10 text-gray-300" />
        <p class="text-[#435574]">Nenhuma mensagem ainda</p>
      </div>
    </div>

    <div
      v-else
      ref="scrollContainer"
      class="flex-1 space-y-4 overflow-y-auto rounded-xl border border-gray-100 bg-slate-50/60 p-4 pr-3"
    >
      <div
        v-for="msg in messages"
        :key="msg.id"
        :class="`flex ${msg.sender === currentUserRole ? 'justify-end' : 'justify-start'}`"
      >
        <div
          :class="`max-w-[80%] rounded-lg p-3 ${
            msg.sender === currentUserRole
              ? 'bg-[#286AD8] text-white'
              : 'bg-gray-100 text-[#263238]'
          }`"
        >
          <p class="text-xs font-medium mb-1 opacity-80">{{ msg.senderName }}</p>
          <p class="text-sm whitespace-pre-wrap break-words">{{ msg.message }}</p>
          <div v-if="msg.attachments && msg.attachments.length > 0" class="mt-3 space-y-2">
            <FileAttachment
              v-for="att in msg.attachments"
              :key="att.id"
              :name="att.name"
              :url="att.url"
              :size="att.size"
              variant="chat"
            />
          </div>
          <p class="text-xs mt-1 opacity-60">{{ formatDate(msg.timestamp) }}</p>
        </div>
      </div>
    </div>

    <!-- Message Input -->
    <div v-if="!isLocked" class="space-y-3">
      <!-- Selected Files Preview -->
      <div v-if="messageAttachments.length > 0" class="flex flex-wrap gap-2">
        <div
          v-for="(file, index) in messageAttachments"
          :key="index"
          class="flex items-center gap-2 bg-blue-50 border border-blue-100 px-3 py-1.5 rounded-full text-sm text-blue-700"
        >
          <Paperclip class="h-3.5 w-3.5" />
          <span class="max-w-[150px] truncate" :title="file.name">{{ file.name }}</span>
          <button
            @click="removeAttachment(index)"
            class="hover:bg-blue-100 p-0.5 rounded-full text-blue-600 transition-colors"
            title="Remover anexo"
          >
            <X class="h-3.5 w-3.5" />
          </button>
        </div>
      </div>
      <div class="flex gap-2">
        <Textarea
          v-model="newMessage"
          placeholder="Digite sua mensagem..."
          :rows="2"
          class="flex-1 min-h-[5.5rem]"
          @keydown="handleComposerKeydown"
        />
        <div class="flex flex-col gap-2">
          <Button
            @click="send"
            :disabled="(!newMessage.trim() && messageAttachments.length === 0) || isSending"
            variant="default"
          >
            <Send class="h-4 w-4" />
          </Button>
          <input
            ref="messageAttachmentInput"
            type="file"
            multiple
            @change="handleAttachmentChange"
            class="hidden"
          />
          <Button
            @click="messageAttachmentInput?.click()"
            variant="outline"
            size="icon"
            :disabled="isSending"
          >
            <Paperclip class="h-4 w-4" />
          </Button>
        </div>
      </div>
    </div> <!-- Quando o serviço estiver concluído deve aparecer outra mensagem -->
    <div v-else class="flex items-center justify-center rounded-lg border border-dashed border-gray-200 bg-gray-50 p-4">
      <p class="text-sm text-[#435574] flex items-center gap-2 text-center">
        <Lock class="h-4 w-4 text-[#286AD8]" />
        {{ lockMessage }}
      </p>
    </div>
  </div>
</template>
