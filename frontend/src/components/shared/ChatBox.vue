<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue';
import { Send, Paperclip, X, Lock, MessageSquare, Mic, Square } from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Textarea from '../ui/textarea.vue';
import FileAttachment from '../shared/FileAttachment.vue';
import ChatAudioPlayer from '../shared/ChatAudioPlayer.vue';
import { toast } from '@/utils/toast';
import {
  CHAT_ATTACHMENT_ACCEPT,
  buildRecordedAudioFile,
  getAudioRecordingUnavailableReason,
  getMicrophoneAccessErrorMessage,
  isAudioFile,
  isAudioRecordingSupported,
  logAudioRecordingError,
  pickSupportedAudioMimeType,
  shouldHideAttachmentPlaceholder,
  type ChatMessageType,
} from '@/utils/chatAttachments';

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

const newMessage = ref('');
const messageAttachments = ref<File[]>([]);
const messageAttachmentInput = ref<HTMLInputElement | null>(null);
const scrollContainer = ref<HTMLElement | null>(null);
const recorderState = ref<'idle' | 'recording' | 'stopping' | 'error'>('idle');
const recordingError = ref('');
const activeRecorder = ref<MediaRecorder | null>(null);
const activeRecordingStream = ref<MediaStream | null>(null);
const pendingSend = ref(false);
const recordingSeconds = ref(0);
let recordingTimer: ReturnType<typeof setInterval> | null = null;

function startRecordingTimer() {
  recordingSeconds.value = 0;
  stopRecordingTimer();
  recordingTimer = setInterval(() => {
    recordingSeconds.value += 1;
  }, 1000);
}

function stopRecordingTimer() {
  if (recordingTimer) {
    clearInterval(recordingTimer);
    recordingTimer = null;
  }
}

const recordingElapsed = computed(() => {
  const total = recordingSeconds.value;
  const minutes = Math.floor(total / 60);
  const seconds = total % 60;
  return `${minutes}:${seconds.toString().padStart(2, '0')}`;
});

const audioRecordingSupported = computed(() => isAudioRecordingSupported());

async function scrollToBottom() {
  await nextTick();
  const el = scrollContainer.value;
  if (el) el.scrollTop = el.scrollHeight;
}

onMounted(scrollToBottom);
watch(() => props.messages.length, scrollToBottom);

function stopRecordingStream() {
  activeRecordingStream.value?.getTracks().forEach((track) => track.stop());
  activeRecordingStream.value = null;
}

function appendAttachments(files: File[]) {
  const totalSize =
    messageAttachments.value.reduce((acc, file) => acc + file.size, 0) +
    files.reduce((acc, file) => acc + file.size, 0);

  if (totalSize > MAX_TOTAL_ATTACHMENT_SIZE) {
    toast.error('O tamanho total dos anexos não pode exceder 25MB');
    return false;
  }

  messageAttachments.value = [...messageAttachments.value, ...files];
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

const send = () => {
  // Se estiver gravando, finaliza a gravação e o envio acontece após o onstop
  // anexar o áudio (sem precisar parar manualmente antes).
  if (recorderState.value === 'recording') {
    pendingSend.value = true;
    stopAudioRecording();
    return;
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
  recordingError.value = '';
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

// Enquanto grava, o botão de microfone mantém o foco; sem isto, o Enter
// dispararia o "click" nativo do botão (parar) em vez de enviar o áudio.
const onRecordButtonKeydown = (event: KeyboardEvent) => {
  if (event.key !== 'Enter' || event.shiftKey || event.isComposing) return;
  if (recorderState.value === 'recording') {
    event.preventDefault();
    send();
  }
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

async function startAudioRecording() {
  if (props.isSending || props.isLocked || recorderState.value === 'recording') {
    return;
  }

  recordingError.value = '';

  if (!audioRecordingSupported.value) {
    recorderState.value = 'error';
    recordingError.value = 'Gravação de áudio indisponível neste navegador.';
    toast.error(recordingError.value);
    return;
  }

  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    const chunks: BlobPart[] = [];
    const preferredMimeType = pickSupportedAudioMimeType();
    let recorder: MediaRecorder;

    try {
      recorder = preferredMimeType
        ? new MediaRecorder(stream, { mimeType: preferredMimeType })
        : new MediaRecorder(stream);
    } catch {
      recorder = new MediaRecorder(stream);
    }

    activeRecordingStream.value = stream;
    activeRecorder.value = recorder;

    recorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        chunks.push(event.data);
      }
    };

    recorder.onerror = () => {
      stopRecordingTimer();
      pendingSend.value = false;
      recorderState.value = 'error';
      recordingError.value = 'Não foi possível gravar o áudio.';
      toast.error(recordingError.value);
      stopRecordingStream();
      activeRecorder.value = null;
    };

    recorder.onstop = () => {
      stopRecordingTimer();
      stopRecordingStream();
      activeRecorder.value = null;

      if (chunks.length > 0) {
        const audioBlob = new Blob(chunks, {
          type: recorder.mimeType || preferredMimeType || 'audio/webm',
        });
        const audioFile = buildRecordedAudioFile(audioBlob);
        appendAttachments([audioFile]);
      }

      if (recorderState.value !== 'error') {
        recorderState.value = 'idle';
      }

      // Envio solicitado durante a gravação: agora que o áudio já foi anexado,
      // dispara o envio real (o próprio send() valida o conteúdo).
      if (pendingSend.value) {
        pendingSend.value = false;
        nextTick(send);
      }
    };

    recorder.start();
    startRecordingTimer();
    recorderState.value = 'recording';
  } catch (error: any) {
    stopRecordingTimer();
    pendingSend.value = false;
    stopRecordingStream();
    activeRecorder.value = null;
    recorderState.value = 'error';
    recordingError.value =
      error?.name === 'NotAllowedError'
        ? 'Permissão de microfone negada.'
        : 'Não foi possível acessar o microfone.';
    toast.error(recordingError.value);
  }
}

function stopAudioRecording() {
  if (!activeRecorder.value || recorderState.value !== 'recording') {
    return;
  }

  recorderState.value = 'stopping';
  activeRecorder.value.stop();
}

onUnmounted(() => {
  const recorder = activeRecorder.value;
  if (recorder && recorder.state !== 'inactive') {
    recorder.ondataavailable = null;
    recorder.onstop = null;
    recorder.onerror = null;
    recorder.stop();
  }
  stopRecordingTimer();
  stopRecordingStream();
});
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
        v-if="recorderState === 'recording' || recorderState === 'stopping' || recordingError"
        class="flex items-center justify-between rounded-lg border px-3 py-2 text-sm"
        :class="
          recorderState === 'error'
            ? 'border-red-200 bg-red-50 text-red-700'
            : 'border-blue-200 bg-blue-50 text-blue-700'
        "
      >
        <span v-if="recorderState === 'recording'" class="flex items-center gap-2">
          <span class="recording-dot" />
          Gravando áudio... {{ recordingElapsed }}
        </span>
        <span v-else-if="recorderState === 'stopping'">Finalizando áudio...</span>
        <span v-else>{{ recordingError }}</span>
        <Button
          v-if="recorderState === 'recording'"
          type="button"
          variant="ghost"
          size="icon"
          class="h-8 w-8 text-red-600 hover:bg-red-100 hover:text-red-700"
          @click="stopAudioRecording"
        >
          <Square class="h-4 w-4 fill-current" />
        </Button>
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
            :disabled="
              (!newMessage.trim() &&
                messageAttachments.length === 0 &&
                recorderState !== 'recording') ||
              isSending ||
              recorderState === 'stopping'
            "
            :title="recorderState === 'recording' ? 'Enviar áudio' : 'Enviar mensagem'"
            variant="default"
          >
            <Send class="h-4 w-4" />
          </Button>
          <input
            ref="messageAttachmentInput"
            type="file"
            multiple
            :accept="CHAT_ATTACHMENT_ACCEPT"
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
          <Button
            type="button"
            :disabled="
              isSending ||
              isLocked ||
              recorderState === 'stopping' ||
              !audioRecordingSupported
            "
            :title="
              audioRecordingSupported
                ? recorderState === 'recording'
                  ? 'Parar gravação'
                  : 'Gravar áudio'
                : 'Gravação de áudio indisponível neste navegador'
            "
            variant="outline"
            size="icon"
            @click="recorderState === 'recording' ? stopAudioRecording() : startAudioRecording()"
            @keydown="onRecordButtonKeydown"
          >
            <Square v-if="recorderState === 'recording'" class="h-4 w-4 fill-current text-red-600" />
            <Mic v-else class="h-4 w-4" />
          </Button>
        </div>
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
