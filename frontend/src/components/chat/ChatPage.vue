<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import {
  ArrowLeft,
  Send,
  Paperclip,
  Smile,
  MoreVertical,
  Circle,
  Flag,
  X,
  Check,
  CheckCheck,
  Clock,
  Image as ImageIcon,
  FileText as FileIcon,
} from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Input from '../ui/input.vue';
import Avatar from '../ui/avatar.vue';
import AvatarImage from '../ui/avatar-image.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import DropdownMenu from '../ui/dropdown-menu.vue';
import DropdownMenuContent from '../ui/dropdown-menu-content.vue';
import DropdownMenuItem from '../ui/dropdown-menu-item.vue';
import DropdownMenuSeparator from '../ui/dropdown-menu-separator.vue';
import DropdownMenuTrigger from '../ui/dropdown-menu-trigger.vue';
import AlertDialog from '../ui/alert-dialog.vue';
import AlertDialogAction from '../ui/alert-dialog-action.vue';
import AlertDialogCancel from '../ui/alert-dialog-cancel.vue';
import AlertDialogContent from '../ui/alert-dialog-content.vue';
import AlertDialogDescription from '../ui/alert-dialog-description.vue';
import AlertDialogFooter from '../ui/alert-dialog-footer.vue';
import AlertDialogHeader from '../ui/alert-dialog-header.vue';
import AlertDialogTitle from '../ui/alert-dialog-title.vue';

interface Message {
  id: string;
  text: string;
  sender: 'me' | 'them';
  timestamp: string;
  status: 'sending' | 'sent' | 'delivered' | 'read';
  attachments?: Attachment[];
}

interface Attachment {
  id: string;
  type: 'image' | 'document';
  url: string;
  name: string;
  size: number;
}

interface Props {
  freelancerId: string;
}

interface Emits {
  (e: 'back'): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const messageText = ref('');
const messages = ref<Message[]>([
  {
    id: '1',
    text: 'Olá! Vi sua solicitação de orçamento. Posso ajudar com a instalação elétrica.',
    sender: 'them',
    timestamp: '2024-11-10T14:20:00',
    status: 'read',
  },
  {
    id: '2',
    text: 'Oi! Ótimo! Você tem disponibilidade para fazer uma vistoria?',
    sender: 'me',
    timestamp: '2024-11-10T14:25:00',
    status: 'read',
  },
  {
    id: '3',
    text: 'Sim! Posso ir amanhã pela manhã se for bom para você.',
    sender: 'them',
    timestamp: '2024-11-10T14:27:00',
    status: 'read',
  },
  {
    id: '4',
    text: 'Perfeito! Às 10h está bom?',
    sender: 'me',
    timestamp: '2024-11-10T14:30:00',
    status: 'read',
  },
  {
    id: '5',
    text: 'Combinado! Vou levar os equipamentos para fazer as medições.',
    sender: 'them',
    timestamp: '2024-11-10T14:32:00',
    status: 'read',
  },
]);
const isOnline = ref(true);
const isTyping = ref(false);
const showReportDialog = ref(false);
const showEmojiPicker = ref(false);
const messagesEndRef = ref<HTMLDivElement | null>(null);
const fileInputRef = ref<HTMLInputElement | null>(null);

// Mock freelancer data
const freelancer = {
  id: props.freelancerId,
  name: 'João Silva',
  avatar: '',
  category: 'Eletricista',
  lastSeen: '2024-11-10T14:32:00',
};

// Common emojis
const emojis = ['👍', '👏', '😊', '🙏', '👌', '✅', '❌', '⚡', '🔧', '💡'];

watch(messages, () => {
  scrollToBottom();
}, { deep: true });

onMounted(() => {
  scrollToBottom();

  // Simular digitação do freelancer
  const typingInterval = setInterval(() => {
    if (Math.random() > 0.95) {
      isTyping.value = true;
      setTimeout(() => {
        isTyping.value = false;
      }, 3000);
    }
  }, 10000);

  return () => clearInterval(typingInterval);
});

const scrollToBottom = () => {
  messagesEndRef.value?.scrollIntoView({ behavior: 'smooth' });
};

const handleSend = () => {
  if (!messageText.value.trim()) return;

  const newMessage: Message = {
    id: Date.now().toString(),
    text: messageText.value,
    sender: 'me',
    timestamp: new Date().toISOString(),
    status: 'sending',
  };

  messages.value.push(newMessage);
  messageText.value = '';

  // Simular envio
  setTimeout(() => {
    const msg = messages.value.find((m) => m.id === newMessage.id);
    if (msg) msg.status = 'sent';
  }, 500);

  setTimeout(() => {
    const msg = messages.value.find((m) => m.id === newMessage.id);
    if (msg) msg.status = 'delivered';
  }, 1000);

  setTimeout(() => {
    const msg = messages.value.find((m) => m.id === newMessage.id);
    if (msg) msg.status = 'read';
  }, 2000);
};

const handleKeyPress = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    handleSend();
  }
};

const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement;
  const files = Array.from(target.files || []);

  files.forEach((file) => {
    const attachment: Attachment = {
      id: Date.now().toString(),
      type: file.type.startsWith('image/') ? 'image' : 'document',
      url: URL.createObjectURL(file),
      name: file.name,
      size: file.size,
    };

    const newMessage: Message = {
      id: Date.now().toString(),
      text: '',
      sender: 'me',
      timestamp: new Date().toISOString(),
      status: 'sending',
      attachments: [attachment],
    };

    messages.value.push(newMessage);

    // Simular envio
    setTimeout(() => {
      const msg = messages.value.find((m) => m.id === newMessage.id);
      if (msg) msg.status = 'read';
    }, 1500);
  });

  if (fileInputRef.value) {
    fileInputRef.value.value = '';
  }
};

const insertEmoji = (emoji: string) => {
  messageText.value += emoji;
  showEmojiPicker.value = false;
};

const handleReport = () => {
  // toast.success('Denúncia enviada. Nossa equipe irá avaliar.');
  showReportDialog.value = false;
};

const formatTime = (timestamp: string) => {
  return new Date(timestamp).toLocaleTimeString('pt-BR', {
    hour: '2-digit',
    minute: '2-digit',
  });
};

const formatFileSize = (bytes: number) => {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
};

const getStatusIcon = (status: Message['status']) => {
  switch (status) {
    case 'sending':
      return Clock;
    case 'sent':
      return Check;
    case 'delivered':
      return CheckCheck;
    case 'read':
      return CheckCheck;
  }
};

const getStatusIconClass = (status: Message['status']) => {
  return status === 'read' ? 'status-read' : 'status-unread';
};

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
};
</script>

<template>
  <div class="chat-container">
    <!-- Chat Header -->
    <div class="chat-header">
      <div class="chat-header-content">
        <div class="chat-header-left">
          <Button
            variant="ghost"
            size="icon"
            @click="emit('back')"
            class="back-button"
          >
            <ArrowLeft class="icon-md" />
          </Button>

          <Avatar class="avatar">
            <AvatarImage :src="freelancer.avatar" :alt="freelancer.name" />
            <AvatarFallback class="avatar-fallback">
              {{ getInitials(freelancer.name) }}
            </AvatarFallback>
          </Avatar>

          <div class="freelancer-info">
            <p class="freelancer-name">{{ freelancer.name }}</p>
            <div class="status-info">
              <template v-if="isOnline">
                <Circle class="status-indicator online" />
                <span>Online</span>
              </template>
              <span v-else>
                Visto por último às
                {{ new Date(freelancer.lastSeen).toLocaleTimeString('pt-BR', {
                  hour: '2-digit',
                  minute: '2-digit',
                }) }}
              </span>
            </div>
          </div>
        </div>

        <DropdownMenu>
          <DropdownMenuTrigger>
            <Button variant="ghost" size="icon">
              <MoreVertical class="icon-md" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem @click="showReportDialog = true">
              <Flag class="icon-sm" />
              Denunciar conversa
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </div>

    <!-- Messages -->
    <div class="messages-container">
      <div
        v-for="message in messages"
        :key="message.id"
        :class="['message-wrapper', message.sender === 'me' ? 'message-me' : 'message-them']"
      >
        <div
          :class="['message-bubble', message.sender === 'me' ? 'message-bubble-me' : 'message-bubble-them']"
        >
          <div v-if="message.attachments && message.attachments.length > 0" class="attachments">
            <div v-for="attachment in message.attachments" :key="attachment.id" class="attachment-item">
              <img
                v-if="attachment.type === 'image'"
                :src="attachment.url"
                :alt="attachment.name"
                class="attachment-image"
              />
              <div v-else class="attachment-document">
                <FileIcon class="icon-md" />
                <div class="attachment-document-info">
                  <p class="attachment-name">{{ attachment.name }}</p>
                  <p class="attachment-size">
                    {{ formatFileSize(attachment.size) }}
                  </p>
                </div>
              </div>
            </div>
          </div>

          <p v-if="message.text" class="message-text">{{ message.text }}</p>

          <div
            :class="['message-footer', message.sender === 'me' ? 'message-footer-me' : 'message-footer-them']"
          >
            <span>{{ formatTime(message.timestamp) }}</span>
            <component
              v-if="message.sender === 'me'"
              :is="getStatusIcon(message.status)"
              :class="['status-icon', getStatusIconClass(message.status)]"
            />
          </div>
        </div>
      </div>

      <div v-if="isTyping" class="typing-indicator">
        <div class="typing-bubble">
          <div class="typing-dots">
            <div class="typing-dot" />
            <div class="typing-dot delay-1" />
            <div class="typing-dot delay-2" />
          </div>
        </div>
      </div>

      <div ref="messagesEndRef" />
    </div>

    <!-- Emoji Picker -->
    <div v-if="showEmojiPicker" class="emoji-picker">
      <div class="emoji-picker-header">
        <p class="emoji-picker-title">Emojis frequentes</p>
        <Button
          variant="ghost"
          size="icon"
          @click="showEmojiPicker = false"
        >
          <X class="icon-sm" />
        </Button>
      </div>
      <div class="emoji-grid">
        <Button
          v-for="emoji in emojis"
          :key="emoji"
          variant="ghost"
          @click="insertEmoji(emoji)"
          class="text-2xl"
        >
          {{ emoji }}
        </Button>
      </div>
    </div>

    <!-- Input -->
    <div class="input-container">
      <div class="input-wrapper">
        <input
          ref="fileInputRef"
          type="file"
          multiple
          @change="handleFileSelect"
          class="file-input"
          accept="image/*,.pdf,.doc,.docx"
        />

        <Button
          variant="ghost"
          size="icon"
          @click="fileInputRef?.click()"
        >
          <Paperclip class="icon-md" />
        </Button>

        <Button
          variant="ghost"
          size="icon"
          @click="showEmojiPicker = !showEmojiPicker"
        >
          <Smile class="icon-md" />
        </Button>

        <Input
          placeholder="Digite sua mensagem..."
          :value="messageText"
          @input="(e: Event) => messageText = (e.target as HTMLInputElement).value"
          @keypress="handleKeyPress"
          class="message-input"
        />

        <Button
          variant="default"
          size="icon"
          @click="handleSend"
          :disabled="!messageText.trim()"
        >
          <Send class="icon-sm" />
        </Button>
      </div>
    </div>

    <!-- Report Dialog -->
    <AlertDialog :open="showReportDialog" @update:open="showReportDialog = $event">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Denunciar conversa</AlertDialogTitle>
          <AlertDialogDescription>
            Você está denunciando uma conversa com {{ freelancer.name }}.
            <br />
            <br />
            Nossa equipe irá revisar o histórico de mensagens e tomar as medidas adequadas.
            Esse conteúdo será mantido em sigilo.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel @click="showReportDialog = false">Cancelar</AlertDialogCancel>
          <AlertDialogAction @click="handleReport" variant="destructive">
            Confirmar denúncia
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>

<style scoped>
.chat-container {
  max-width: 80rem;
  margin: 0 auto;
  height: calc(100vh - 8rem);
  display: flex;
  flex-direction: column;
  background-color: white;
  border-radius: 0.5rem;
  border: 1px solid #e5e7eb;
  overflow: hidden;
}

.chat-header {
  border-bottom: 1px solid #e5e7eb;
  padding: 1rem;
  background-color: white;
}

.chat-header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chat-header-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.back-button {
  display: none;
}

@media (max-width: 768px) {
  .back-button {
    display: inline-flex;
  }
}

.avatar {
  height: 2.5rem;
  width: 2.5rem;
}

.avatar-fallback {
  background-color: #286AD8;
  color: white;
}

.freelancer-info {
  display: flex;
  flex-direction: column;
}

.freelancer-name {
  color: #263238;
  margin: 0;
}

.status-info {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.875rem;
  color: #435574;
}

.status-indicator {
  height: 0.5rem;
  width: 0.5rem;
}

.status-indicator.online {
  fill: #10b981;
  color: #10b981;
}

.icon-sm {
  height: 1rem;
  width: 1rem;
}

.icon-md {
  height: 1.25rem;
  width: 1.25rem;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  background-color: #f9fafb;
}

.message-wrapper {
  display: flex;
}

.message-me {
  justify-content: flex-end;
}

.message-them {
  justify-content: flex-start;
}

.message-bubble {
  max-width: 70%;
  border-radius: 0.5rem;
  padding: 0.75rem;
}

.message-bubble-me {
  background-color: #286AD8;
  color: white;
}

.message-bubble-them {
  background-color: white;
  color: #263238;
  border: 1px solid #e5e7eb;
}

.attachments {
  margin-bottom: 0.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.attachment-item {
  display: block;
}

.attachment-image {
  border-radius: 0.25rem;
  max-width: 100%;
  height: auto;
}

.attachment-document {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem;
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 0.25rem;
}

.attachment-document-info {
  flex: 1;
  min-width: 0;
}

.attachment-name {
  font-size: 0.875rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin: 0;
}

.attachment-size {
  font-size: 0.75rem;
  opacity: 0.7;
  margin: 0;
}

.message-text {
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
}

.message-footer {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  justify-content: flex-end;
  margin-top: 0.25rem;
  font-size: 0.75rem;
}

.message-footer-me {
  color: rgba(255, 255, 255, 0.7);
}

.message-footer-them {
  color: #435574;
}

.status-icon {
  height: 0.75rem;
  width: 0.75rem;
}

.status-read {
  color: #286AD8;
}

.status-unread {
  color: #9ca3af;
}

.typing-indicator {
  display: flex;
  justify-content: flex-start;
}

.typing-bubble {
  background-color: white;
  border: 1px solid #e5e7eb;
  border-radius: 0.5rem;
  padding: 0.75rem;
}

.typing-dots {
  display: flex;
  gap: 0.25rem;
}

.typing-dot {
  width: 0.5rem;
  height: 0.5rem;
  background-color: #435574;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out;
}

.typing-dot.delay-1 {
  animation-delay: 0.1s;
}

.typing-dot.delay-2 {
  animation-delay: 0.2s;
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.emoji-picker {
  border-top: 1px solid #e5e7eb;
  padding: 0.75rem;
  background-color: white;
}

.emoji-picker-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.emoji-picker-title {
  font-size: 0.875rem;
  color: #435574;
  margin: 0;
}

.emoji-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.input-container {
  border-top: 1px solid #e5e7eb;
  padding: 1rem;
  background-color: white;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 0.5rem;
}

.file-input {
  display: none;
}

.message-input {
  flex: 1;
}

</style>

