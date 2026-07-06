<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue';
import { Send, Paperclip, X, Lock, MessageSquare, Mic, Trash2, Pause, Play } from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Textarea from '../ui/textarea.vue';
import FileAttachment from '../shared/FileAttachment.vue';
import ChatAudioPlayer from '../shared/ChatAudioPlayer.vue';
import { toast } from '@/utils/toast';
import {
  CHAT_ATTACHMENT_ACCEPT,
  isAudioFile,
  shouldHideAttachmentPlaceholder,
  type ChatMessageType,
} from '@/utils/chatAttachments';
import { useAudioRecorder } from '@/composables/useAudioRecorder';

export interface ChatMessage {
  id: string;
  sender: 'client' | 'freelancer';
  isOwn: boolean;
  senderName: string;
  type: ChatMessageType;
  message: string;
  timestamp: string;
  status?: string;
  readAt?: string | null;
  attachments?: Array<{
    id: string;
    name: string;
    url: string;
    size: string;
    type?: string;
  }>;
}

interface Props {
  messages: ChatMessage[];
  currentUserRole: 'client' | 'freelancer' | 'contractor' | 'provider';
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

const MAX_TOTAL_ATTACHMENT_SIZE = 25 * 1024 * 1024;
const MAX_FILE_SIZE = 10 * 1024 * 1024;

const newMessage = ref('');
const messageAttachments = ref<File[]>([]);
const messageAttachmentInput = ref<HTMLInputElement | null>(null);
const scrollContainer = ref<HTMLElement | null>(null);
const {
  recorderState,
  recordingError,
  recordingElapsed,
  isSupported: audioRecordingSupported,
  start: startAudioRecording,
  stop: stopAudioRecording,
  pause: pauseAudioRecording,
  resume: resumeAudioRecording,
  cancel: cancelAudioRecording,
} = useAudioRecorder();

const isRecording = computed(
  () =>
    recorderState.value === 'recording' ||
    recorderState.value === 'paused' ||
    recorderState.value === 'stopping'
);

const hasContent = computed(
  () => newMessage.value.trim() !== '' || messageAttachments.value.length > 0
);

async function scrollToBottom() {
  await nextTick();
  const el = scrollContainer.value;
  if (el) el.scrollTop = el.scrollHeight;
}

onMounted(scrollToBottom);
watch(() => props.messages.length, scrollToBottom);

function appendAttachments(files: File[]) {
  // Valida o tamanho por arquivo (limite real do backend: 10MB).
  const validFiles: File[] = [];
  for (const file of files) {
    if (file.size > MAX_FILE_SIZE) {
      toast.error(`O arquivo "${file.name}" excede o tamanho máximo de 10MB.`);
      continue;
    }
    validFiles.push(file);
  }

  if (validFiles.length === 0) {
    return false;
  }

  const totalSize =
    messageAttachments.value.reduce((acc, file) => acc + file.size, 0) +
    validFiles.reduce((acc, file) => acc + file.size, 0);

  if (totalSize > MAX_TOTAL_ATTACHMENT_SIZE) {
    toast.error('O tamanho total dos anexos não pode exceder 25MB');
    return false;
  }

  messageAttachments.value = [...messageAttachments.value, ...validFiles];
  return true;
}

const handleAttachmentChange = (event: Event) => {
  const target = event.target as HTMLInputElement;
  if (!target.files) return;

  appendAttachments(Array.from(target.files));
  if (messageAttachmentInput.value) {
    messageAttachmentInput.value.value = '';
  }
};

const removeAttachment = (index: number) => {
  messageAttachments.value.splice(index, 1);
};

const send = async () => {
  // Se estiver gravando/pausado, finaliza a gravação e anexa o áudio antes de enviar.
  if (recorderState.value === 'recording' || recorderState.value === 'paused') {
    const audioFile = await stopAudioRecording();
    if (audioFile) {
      appendAttachments([audioFile]);
    }
  }

  if (recorderState.value === 'stopping') {
    return;
  }

  const message = newMessage.value.trim();

  if (!message && messageAttachments.value.length === 0) {
    toast.error('Digite uma mensagem ou adicione um anexo');
    return;
  }

  emit('send-message', message, [...messageAttachments.value]);
  newMessage.value = '';
  messageAttachments.value = [];
};

const handleStartRecording = async () => {
  const started = await startAudioRecording();
  if (!started && recordingError.value) {
    toast.error(recordingError.value);
  }
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
        <MessageSquare class="mx-auto mb-3 h-9 w-9 text-gray-300" />
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
        :class="`flex ${msg.isOwn ? 'justify-end' : 'justify-start'}`"
      >
        <div
          :class="`max-w-[80%] rounded-lg p-3 ${
            msg.isOwn
              ? 'bg-[#286AD8] text-white'
              : 'bg-gray-100 text-[#263238]'
          }`"
        >
          <p class="mb-1 text-xs font-medium opacity-80">{{ msg.senderName }}</p>
          <p
            v-if="!shouldHideAttachmentPlaceholder(msg.message, msg.type, !!msg.attachments?.length)"
            class="text-sm whitespace-pre-wrap break-words"
          >
            {{ msg.message }}
          </p>
          <div v-if="msg.attachments && msg.attachments.length > 0" class="mt-3 space-y-2">
            <template v-for="att in msg.attachments" :key="att.id">
              <ChatAudioPlayer
                v-if="msg.type === 'audio'"
                :src="att.url"
                :is-own="msg.isOwn"
              />
              <FileAttachment
                v-else
                :name="att.name"
                :url="att.url"
                :size="att.size"
                :type="att.type"
                variant="chat"
              />
            </template>
          </div>
          <p class="mt-1 text-xs opacity-60">{{ formatDate(msg.timestamp) }}</p>
        </div>
      </div>
    </div>

    <div v-if="!isLocked" class="space-y-3">
      <div
        v-if="recorderState === 'error' && recordingError"
        class="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700"
      >
        {{ recordingError }}
      </div>

      <div v-if="messageAttachments.length > 0" class="flex flex-wrap gap-2">
        <div
          v-for="(file, index) in messageAttachments"
          :key="index"
          class="flex items-center gap-2 rounded-full border border-blue-100 bg-blue-50 px-3 py-1.5 text-sm text-blue-700"
        >
          <Mic v-if="isAudioFile(file)" class="h-3.5 w-3.5" />
          <Paperclip v-else class="h-3.5 w-3.5" />
          <span class="max-w-[150px] truncate" :title="file.name">{{ file.name }}</span>
          <button
            @click="removeAttachment(index)"
            class="rounded-full p-0.5 text-blue-600 transition-colors hover:bg-blue-100"
            title="Remover anexo"
          >
            <X class="h-3.5 w-3.5" />
          </button>
        </div>
      </div>

      <input
        ref="messageAttachmentInput"
        type="file"
        multiple
        :accept="CHAT_ATTACHMENT_ACCEPT"
        @change="handleAttachmentChange"
        class="hidden"
      />

      <div
        class="flex min-h-12 w-full items-center gap-1 rounded-3xl border bg-white px-1.5 py-1 transition-colors"
        :class="
          isRecording
            ? 'border-blue-200 text-blue-700'
            : 'border-gray-200 focus-within:border-blue-500 focus-within:ring-2 focus-within:ring-blue-500/30'
        "
      >
        <!-- Modo gravação: controles ocupam o lugar do textarea -->
        <template v-if="isRecording">
          <Button
            type="button"
            variant="ghost"
            size="icon"
            class="h-10 w-10 flex-shrink-0 rounded-full text-red-600 hover:bg-red-100 hover:text-red-700 [&_svg]:!size-5"
            :disabled="recorderState === 'stopping'"
            title="Cancelar gravação"
            @click="cancelAudioRecording"
          >
            <Trash2 />
          </Button>
          <div class="flex flex-1 items-center gap-2 px-2 text-sm">
            <span v-if="recorderState !== 'paused'" class="recording-dot" />
            <span class="font-medium tabular-nums">
              {{ recorderState === 'stopping' ? 'Finalizando áudio...' : recordingElapsed }}
            </span>
          </div>
          <Button
            v-if="recorderState !== 'stopping'"
            type="button"
            variant="ghost"
            size="icon"
            class="h-10 w-10 flex-shrink-0 rounded-full text-blue-700 hover:bg-blue-100 [&_svg]:!size-5"
            :title="recorderState === 'paused' ? 'Retomar' : 'Pausar'"
            @click="recorderState === 'paused' ? resumeAudioRecording() : pauseAudioRecording()"
          >
            <Play v-if="recorderState === 'paused'" />
            <Pause v-else />
          </Button>
          <Button
            :disabled="recorderState === 'stopping' || isSending"
            variant="ghost"
            size="icon"
            class="h-10 w-10 flex-shrink-0 rounded-full text-blue-600 hover:bg-blue-100 hover:text-blue-700 [&_svg]:!size-5"
            title="Enviar áudio"
            @click="send"
          >
            <Send />
          </Button>
        </template>

        <!-- Modo normal: anexar + textarea + (enviar | microfone) -->
        <template v-else>
          <Button
            @click="messageAttachmentInput?.click()"
            variant="ghost"
            size="icon"
            class="h-10 w-10 flex-shrink-0 rounded-full text-gray-500 hover:text-gray-700 [&_svg]:!size-5"
            :disabled="isSending"
            title="Anexar arquivo"
          >
            <Paperclip />
          </Button>
          <Textarea
            v-model="newMessage"
            autosize
            rows="1"
            placeholder="Digite sua mensagem..."
            class="flex-1 min-h-10 max-h-32 resize-none rounded-none border-0 bg-transparent px-2 py-1.5 leading-6 shadow-none focus-visible:ring-0"
            @keydown="handleComposerKeydown"
          />
          <Button
            v-if="hasContent"
            :disabled="isSending"
            variant="ghost"
            size="icon"
            class="h-10 w-10 flex-shrink-0 rounded-full text-blue-600 hover:bg-blue-50 hover:text-blue-700 [&_svg]:!size-5"
            title="Enviar mensagem"
            @click="send"
          >
            <Send />
          </Button>
          <Button
            v-else
            type="button"
            :disabled="isSending || !audioRecordingSupported"
            variant="ghost"
            size="icon"
            class="h-10 w-10 flex-shrink-0 rounded-full text-gray-500 hover:text-gray-700 [&_svg]:!size-5"
            :title="
              audioRecordingSupported
                ? 'Gravar áudio'
                : 'Gravação de áudio indisponível neste navegador'
            "
            @click="handleStartRecording"
          >
            <Mic />
          </Button>
        </template>
      </div>
    </div>
    <div
      v-else
      class="flex items-center justify-center rounded-lg border border-dashed border-gray-200 bg-gray-50 p-4"
    >
      <p class="flex items-center gap-2 text-center text-sm text-[#435574]">
        <Lock class="h-4 w-4 text-[#286AD8]" />
        {{ lockMessage }}
      </p>
    </div>
  </div>
</template>

<style scoped>
.recording-dot {
  display: inline-block;
  width: 0.5rem;
  height: 0.5rem;
  border-radius: 9999px;
  background-color: #dc2626;
  animation: recording-pulse 1.2s ease-in-out infinite;
}

@keyframes recording-pulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.35;
    transform: scale(0.7);
  }
}
</style>
