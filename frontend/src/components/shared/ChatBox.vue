<script setup lang="ts">
import { ref } from 'vue';
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
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: 'send-message', message: string, attachments: File[]): void;
}>();

const newMessage = ref('');
const messageAttachments = ref<File[]>([]);
const messageAttachmentInput = ref<HTMLInputElement | null>(null);

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
  if (!newMessage.value.trim() && messageAttachments.value.length === 0) {
    toast.error('Digite uma mensagem ou adicione um anexo');
    return;
  }
  emit('send-message', newMessage.value, messageAttachments.value);
  newMessage.value = '';
  messageAttachments.value = [];
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
  <div class="flex flex-col h-full w-full">
    <div v-if="!messages || messages.length === 0" class="text-center py-8">
      <MessageSquare class="h-10 w-10 text-gray-300 mx-auto mb-3" />
      <p class="text-[#435574]">Nenhuma mensagem ainda</p>
    </div>

    <div v-else class="space-y-4 max-h-96 overflow-y-auto mb-4">
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
          <p class="text-sm">{{ msg.message }}</p>
          <div v-if="msg.attachments && msg.attachments.length > 0" class="mt-3 space-y-2">
            <FileAttachment
              v-for="att in msg.attachments"
              :key="att.id"
              :name="att.name"
              :url="att.url"
              :size="att.size"
              variant="compact"
              :class="msg.sender === currentUserRole ? 'bg-white/10 border-white/20 text-white' : ''"
            />
          </div>
          <p class="text-xs mt-1 opacity-60">{{ formatDate(msg.timestamp) }}</p>
        </div>
      </div>
    </div>

    <!-- Message Input -->
    <div v-if="!isLocked" class="space-y-3 mt-auto">
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
          class="flex-1"
        />
        <div class="flex flex-col gap-2">
          <Button
            @click="send"
            :disabled="(!newMessage.trim() && messageAttachments.length === 0) || isSending"
            class="bg-[#286AD8] hover:bg-[#435574]"
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
    </div>
    <div v-else class="flex items-center justify-center p-4 bg-gray-50 rounded-lg border border-dashed border-gray-200 mt-auto">
      <p class="text-sm text-[#435574] flex items-center gap-2 text-center">
        <Lock class="h-4 w-4 text-[#286AD8]" />
        O chat será liberado automaticamente após a confirmação do pagamento.
      </p>
    </div>
  </div>
</template>
