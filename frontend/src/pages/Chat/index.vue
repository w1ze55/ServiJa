<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import MainLayout from '@/components/layout/MainLayout.vue';
import type { ChatMessage } from '@/components/shared/ChatBox.vue';
import FileAttachment from '@/components/shared/FileAttachment.vue';
import Textarea from '@/components/ui/textarea.vue';
import Button from '@/components/ui/button.vue';
import { chatService } from '@/api/services/chatService';
import type { ConversationDTO, MessageDTO } from '@/api/services/chatService';
import { useChatWebSocket } from '@/composables/useChatWebSocket';
import { uploadFiles } from '@/api/uploadService';
import { useNavigation } from '@/utils/navigation';
import { toast } from '@/utils/toast';
import {
  ArrowLeft,
  MessageSquare,
  Search,
  Send,
  Paperclip,
  X,
  Lock,
  MoreVertical,
  Download,
  ExternalLink,
  ChevronLeft,
  ChevronRight,
} from 'lucide-vue-next';

const IMAGE_EXT_RE = /\.(jpg|jpeg|png|gif|webp|bmp|svg)(\?.*)?$/i;
function isImageUrl(url?: string | null) {
  return !!url && IMAGE_EXT_RE.test(url);
}
function isImageFile(file: File) {
  return file.type.startsWith('image/');
}

const route = useRoute();
const router = useRouter();
const { navigate, logout } = useNavigation();

const user = ref<any>(null);

// Lista de conversas (sidebar)
const conversations = ref<ConversationDTO[]>([]);
const loadingList = ref(true);
const searchQuery = ref('');

// Conversa ativa
const activeConversation = ref<ConversationDTO | null>(null);
const messages = ref<ChatMessage[]>([]);
const loadingChat = ref(false);
const error = ref('');

// Composer
const newMessage = ref('');
const messageAttachments = ref<File[]>([]);
const attachmentPreviews = ref<Record<string, string>>({});
const messageAttachmentInput = ref<HTMLInputElement | null>(null);
const isSending = ref(false);

// Lightbox
const lightboxIndex = ref<number>(-1);
const isDownloading = ref(false);

const conversationImages = computed<string[]>(() => {
  const urls: string[] = [];
  for (const msg of messages.value) {
    if (!msg.attachments) continue;
    for (const att of msg.attachments) {
      if (isImageUrl(att.url)) urls.push(att.url);
    }
  }
  return urls;
});

const lightboxUrl = computed<string | null>(() => {
  const i = lightboxIndex.value;
  if (i < 0 || i >= conversationImages.value.length) return null;
  return conversationImages.value[i];
});

const hasMultiple = computed(() => conversationImages.value.length > 1);
const canPrev = computed(() => lightboxIndex.value > 0);
const canNext = computed(
  () => lightboxIndex.value >= 0 && lightboxIndex.value < conversationImages.value.length - 1
);

function openLightbox(url: string) {
  const i = conversationImages.value.indexOf(url);
  lightboxIndex.value = i >= 0 ? i : -1;
}
function closeLightbox() { lightboxIndex.value = -1; }
function prevImage() { if (canPrev.value) lightboxIndex.value -= 1; }
function nextImage() { if (canNext.value) lightboxIndex.value += 1; }

function filenameFromUrl(url: string) {
  try {
    const u = new URL(url, window.location.href);
    const last = u.pathname.split('/').filter(Boolean).pop();
    return last ? decodeURIComponent(last) : 'imagem';
  } catch {
    return 'imagem';
  }
}

async function downloadFromUrl(url: string) {
  if (isDownloading.value) return;
  isDownloading.value = true;
  try {
    const res = await fetch(url, { mode: 'cors' });
    if (!res.ok) throw new Error('Falha ao baixar');
    const blob = await res.blob();
    const objectUrl = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = objectUrl;
    a.download = filenameFromUrl(url);
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(objectUrl), 1000);
  } catch {
    toast.error('Não foi possível baixar a imagem');
    window.open(url, '_blank', 'noopener');
  } finally {
    isDownloading.value = false;
  }
}

function fileKey(file: File) {
  return `${file.name}:${file.size}:${file.lastModified}`;
}

// Mobile: alterna sidebar vs. conversa
const showSidebarMobile = ref(false);

const messagesContainer = ref<HTMLElement | null>(null);

const { onMessage, connect, disconnect } = useChatWebSocket();

const currentUserRole = computed<'client' | 'freelancer'>(() =>
  user.value?.type === 'provider' ? 'freelancer' : 'client'
);

const activeServiceRequestId = computed(() => {
  const param = route.params.serviceRequestId;
  if (!param) return null;
  const n = Number(param);
  return Number.isFinite(n) ? n : null;
});

function displayName(email: string) {
  if (!email) return '';
  return email.split('@')[0].replace(/[._]/g, ' ');
}

function initials(email: string) {
  const name = displayName(email);
  return name.charAt(0).toUpperCase() || '?';
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
  if (isNaN(d.getTime())) return '';
  const now = new Date();
  const sameDay =
    d.getDate() === now.getDate() &&
    d.getMonth() === now.getMonth() &&
    d.getFullYear() === now.getFullYear();
  return sameDay
    ? d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
    : d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' });
}

function formatDayDivider(iso: string) {
  const d = new Date(iso);
  if (isNaN(d.getTime())) return '';
  const today = new Date();
  const yesterday = new Date();
  yesterday.setDate(today.getDate() - 1);
  const sameDay = (a: Date, b: Date) =>
    a.getDate() === b.getDate() &&
    a.getMonth() === b.getMonth() &&
    a.getFullYear() === b.getFullYear();
  if (sameDay(d, today)) return 'Hoje';
  if (sameDay(d, yesterday)) return 'Ontem';
  return d.toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  });
}

function dayKey(iso: string) {
  const d = new Date(iso);
  if (isNaN(d.getTime())) return '';
  return `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`;
}

function formatHour(iso: string) {
  const d = new Date(iso);
  if (isNaN(d.getTime())) return iso;
  return d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
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

const filteredConversations = computed(() => {
  const q = searchQuery.value.toLowerCase().trim();
  const base = [...conversations.value].sort((a, b) => {
    const ta = a.lastMessage?.sentAt ?? a.updatedAt ?? a.createdAt;
    const tb = b.lastMessage?.sentAt ?? b.updatedAt ?? b.createdAt;
    return new Date(tb).getTime() - new Date(ta).getTime();
  });
  if (!q) return base;
  return base.filter((c) => otherParty(c).toLowerCase().includes(q));
});

// Agrupa mensagens por dia para renderizar divisores
const groupedMessages = computed(() => {
  const groups: Array<{ key: string; label: string; items: ChatMessage[] }> = [];
  let currentKey = '';
  for (const m of messages.value) {
    const k = dayKey(m.timestamp);
    if (k !== currentKey) {
      groups.push({ key: k, label: formatDayDivider(m.timestamp), items: [m] });
      currentKey = k;
    } else {
      groups[groups.length - 1].items.push(m);
    }
  }
  return groups;
});

async function loadConversations() {
  try {
    conversations.value = await chatService.listConversations();
  } catch (e) {
    console.error('Erro ao carregar conversas', e);
  } finally {
    loadingList.value = false;
  }
}

async function openConversation(serviceRequestId: number, opts: { pushRoute?: boolean } = {}) {
  loadingChat.value = true;
  error.value = '';
  messages.value = [];
  disconnect();

  if (opts.pushRoute !== false) {
    router.replace(`/chat/${serviceRequestId}`);
  }
  showSidebarMobile.value = false;

  try {
    const conv = await chatService.getOrCreateConversation(serviceRequestId);
    activeConversation.value = conv;

    const page = await chatService.getMessages(conv.id);
    messages.value = page.content.map(toDisplay);

    await chatService.markAsRead(conv.id);
    // Zera badge local
    const idx = conversations.value.findIndex((c) => c.id === conv.id);
    if (idx >= 0) {
      const role = user.value?.type === 'provider' ? 'provider' : 'contractor';
      const updated = {
        ...conversations.value[idx],
        unreadCount: { ...conversations.value[idx].unreadCount, [role]: 0 },
      };
      conversations.value.splice(idx, 1, updated);
    } else {
      conversations.value = [conv, ...conversations.value];
    }

    onMessage.value = (incoming: MessageDTO) => {
      if (!activeConversation.value) return;
      if (incoming.conversationId !== activeConversation.value.id) return;
      if (messages.value.some((m) => m.id === incoming.id)) return;
      messages.value.push(toDisplay(incoming));
      chatService.markAsRead(activeConversation.value.id).catch(() => {});
      // Atualiza preview na lista
      const i = conversations.value.findIndex((c) => c.id === incoming.conversationId);
      if (i >= 0) {
        conversations.value.splice(i, 1, {
          ...conversations.value[i],
          lastMessage: {
            content: incoming.content,
            senderId: incoming.senderId,
            sentAt: incoming.sentAt,
          },
        });
      }
    };
    connect(conv.id);
    loadingChat.value = false;
    await scrollToBottom({ instant: true });
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '';
    if (msg.toLowerCase().includes('habilitado')) {
      error.value = 'O chat ainda não está disponível. Aguarde a confirmação do pagamento.';
    } else {
      error.value = 'Erro ao carregar o chat.';
    }
    activeConversation.value = null;
    loadingChat.value = false;
  }
}

async function scrollToBottom(opts: { instant?: boolean } = {}) {
  await nextTick();
  const el = messagesContainer.value;
  if (!el) return;
  el.scrollTop = el.scrollHeight;
  if (opts.instant) {
    // Garante após carregamento de imagens (que mudam a altura do scroll)
    requestAnimationFrame(() => {
      const elNow = messagesContainer.value;
      if (elNow) elNow.scrollTop = elNow.scrollHeight;
    });
  }
}

watch(
  () => messages.value.length,
  () => scrollToBottom()
);

watch(
  () => route.params.serviceRequestId,
  (val) => {
    if (!val) return;
    const id = Number(val);
    if (!Number.isFinite(id)) return;
    if (activeConversation.value?.serviceRequestId === id) return;
    openConversation(id, { pushRoute: false });
  }
);

function onKeydown(e: KeyboardEvent) {
  if (!lightboxUrl.value) return;
  if (e.key === 'Escape') closeLightbox();
  else if (e.key === 'ArrowLeft') prevImage();
  else if (e.key === 'ArrowRight') nextImage();
}

onBeforeUnmount(() => {
  for (const url of Object.values(attachmentPreviews.value)) URL.revokeObjectURL(url);
  window.removeEventListener('keydown', onKeydown);
});

onMounted(async () => {
  window.addEventListener('keydown', onKeydown);
  const userData = localStorage.getItem('user');
  if (!userData) {
    router.push('/login');
    return;
  }
  user.value = JSON.parse(userData);

  await loadConversations();

  if (activeServiceRequestId.value !== null) {
    await openConversation(activeServiceRequestId.value, { pushRoute: false });
  }
});

const handleAttachmentChange = (event: Event) => {
  const target = event.target as HTMLInputElement;
  if (!target.files) return;
  const newFiles = Array.from(target.files);
  const totalSize =
    messageAttachments.value.reduce((acc, f) => acc + f.size, 0) +
    newFiles.reduce((acc, f) => acc + f.size, 0);
  if (totalSize > 25 * 1024 * 1024) {
    toast.error('O tamanho total dos anexos não pode exceder 25MB');
    return;
  }
  for (const file of newFiles) {
    if (isImageFile(file)) {
      const key = fileKey(file);
      if (!attachmentPreviews.value[key]) {
        attachmentPreviews.value[key] = URL.createObjectURL(file);
      }
    }
  }
  messageAttachments.value = [...messageAttachments.value, ...newFiles];
  if (messageAttachmentInput.value) messageAttachmentInput.value.value = '';
};

function revokePreview(file: File) {
  const key = fileKey(file);
  const url = attachmentPreviews.value[key];
  if (url) {
    URL.revokeObjectURL(url);
    delete attachmentPreviews.value[key];
  }
}

const removeAttachment = (index: number) => {
  const [removed] = messageAttachments.value.splice(index, 1);
  if (removed) revokePreview(removed);
};

function clearAttachments() {
  for (const f of messageAttachments.value) revokePreview(f);
  messageAttachments.value = [];
}

async function handleSend() {
  if (isSending.value || !activeConversation.value) return;
  if (!newMessage.value.trim() && messageAttachments.value.length === 0) {
    toast.error('Digite uma mensagem ou adicione um anexo');
    return;
  }
  if (activeConversation.value.locked) return;
  isSending.value = true;

  const text = newMessage.value.trim();
  const files = [...messageAttachments.value];
  newMessage.value = '';
  clearAttachments();

  try {
    if (text) {
      const msg = await chatService.sendMessage(activeConversation.value.id, text);
      if (!messages.value.some((m) => m.id === msg.id)) {
        messages.value.push(toDisplay(msg));
      }
    }
    if (files.length > 0) {
      const urls = await uploadFiles(files);
      for (const url of urls) {
        const asImage = isImageUrl(url);
        const msg = await chatService.sendMessage(
          activeConversation.value.id,
          asImage ? 'Imagem' : 'Arquivo',
          asImage ? 'image' : 'file',
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

function onComposerKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    handleSend();
  }
}

const otherPartyEmail = computed(() =>
  activeConversation.value ? otherParty(activeConversation.value) : ''
);

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
    <div class="w-full">
      <div
        class="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden flex h-[calc(100vh-160px)] min-h-[560px]"
      >
        <!-- ============ SIDEBAR (lista de conversas) ============ -->
        <aside
          :class="[
            'w-full md:w-[320px] md:flex-shrink-0 border-r border-gray-200 flex-col bg-white',
            activeConversation && !showSidebarMobile ? 'hidden md:flex' : 'flex',
          ]"
        >
          <div class="p-4 border-b border-gray-100">
            <h1 class="text-lg font-semibold text-gray-900 mb-3">Mensagens</h1>
            <div class="relative">
              <Search class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
              <input
                v-model="searchQuery"
                placeholder="Buscar conversa..."
                class="w-full pl-9 pr-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          <div class="flex-1 overflow-y-auto">
            <!-- Loading -->
            <div v-if="loadingList" class="p-3 space-y-2">
              <div v-for="i in 5" :key="i" class="h-16 bg-gray-100 rounded-lg animate-pulse" />
            </div>

            <!-- Vazio -->
            <div
              v-else-if="filteredConversations.length === 0"
              class="text-center py-12 px-4 text-gray-400"
            >
              <MessageSquare class="h-10 w-10 mx-auto mb-3 text-gray-200" />
              <p class="text-sm font-medium">Nenhuma conversa</p>
              <p class="text-xs mt-1">
                As conversas aparecem após a confirmação do pagamento.
              </p>
            </div>

            <!-- Lista -->
            <ul v-else class="py-1">
              <li v-for="conv in filteredConversations" :key="conv.id">
                <button
                  @click="openConversation(conv.serviceRequestId)"
                  :class="[
                    'w-full flex items-center gap-3 px-3 py-3 hover:bg-gray-50 transition-colors text-left border-l-2',
                    activeConversation?.id === conv.id
                      ? 'bg-blue-50/60 border-blue-600'
                      : 'border-transparent',
                  ]"
                >
                  <div
                    class="flex-shrink-0 h-10 w-10 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 font-semibold text-sm"
                  >
                    {{ initials(otherParty(conv)) }}
                  </div>
                  <div class="flex-1 min-w-0">
                    <div class="flex items-center justify-between gap-2">
                      <span class="font-medium text-sm text-gray-900 truncate capitalize">
                        {{ displayName(otherParty(conv)) || otherParty(conv) }}
                      </span>
                      <span class="text-[10px] text-gray-400 flex-shrink-0">
                        {{ formatTime(conv.lastMessage?.sentAt) }}
                      </span>
                    </div>
                    <div class="flex items-center justify-between gap-2 mt-0.5">
                      <p class="text-xs text-gray-500 truncate">
                        {{ conv.lastMessage?.content || 'Conversa iniciada' }}
                      </p>
                      <span
                        v-if="unreadForMe(conv) > 0"
                        class="flex-shrink-0 h-5 min-w-5 px-1.5 rounded-full bg-blue-600 text-white text-[10px] font-semibold flex items-center justify-center"
                      >
                        {{ unreadForMe(conv) > 9 ? '9+' : unreadForMe(conv) }}
                      </span>
                    </div>
                    <p class="text-[10px] text-gray-400 mt-0.5">
                      Solicitação #{{ conv.serviceRequestId }}
                    </p>
                  </div>
                </button>
              </li>
            </ul>
          </div>
        </aside>

        <!-- ============ CONVERSA ATIVA ============ -->
        <section
          :class="[
            'flex-1 flex-col min-w-0 bg-gray-50',
            (activeConversation || error) && !showSidebarMobile ? 'flex' : 'hidden md:flex',
          ]"
        >
          <!-- Empty state (nenhuma conversa selecionada) -->
          <div
            v-if="!activeConversation && !loadingChat && !error"
            class="flex-1 flex flex-col items-center justify-center text-center px-8 text-gray-400"
          >
            <div class="h-16 w-16 rounded-full bg-blue-100 flex items-center justify-center mb-4">
              <MessageSquare class="h-7 w-7 text-blue-600" />
            </div>
            <h2 class="text-lg font-semibold text-gray-700">Selecione uma conversa</h2>
            <p class="text-sm mt-1 max-w-sm">
              Escolha uma conversa na lista ao lado para começar a trocar mensagens.
            </p>
          </div>

          <!-- Erro -->
          <div
            v-else-if="error"
            class="flex-1 flex flex-col items-center justify-center text-center px-8"
          >
            <Lock class="h-10 w-10 text-gray-300 mb-3" />
            <p class="text-sm text-gray-500 max-w-sm">{{ error }}</p>
            <button
              @click="
                error = '';
                activeConversation = null;
                router.replace('/chat');
              "
              class="mt-4 text-sm text-blue-600 hover:underline"
            >
              Voltar
            </button>
          </div>

          <template v-else-if="activeConversation">
            <!-- Header da conversa -->
            <header
              class="flex items-center gap-3 px-4 py-3 bg-white border-b border-gray-200 flex-shrink-0"
            >
              <button
                @click="showSidebarMobile = true"
                class="md:hidden p-1 -ml-1 rounded-full hover:bg-gray-100"
                title="Voltar"
              >
                <ArrowLeft class="h-5 w-5 text-gray-600" />
              </button>
              <div
                class="flex-shrink-0 h-10 w-10 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 font-semibold"
              >
                {{ initials(otherPartyEmail) }}
              </div>
              <div class="flex-1 min-w-0">
                <h2 class="font-semibold text-gray-900 truncate capitalize">
                  {{ displayName(otherPartyEmail) || otherPartyEmail }}
                </h2>
                <p class="text-xs text-gray-500">
                  Solicitação #{{ activeConversation.serviceRequestId }}
                </p>
              </div>
              <button
                class="p-2 rounded-full hover:bg-gray-100 text-gray-500"
                title="Mais opções"
              >
                <MoreVertical class="h-5 w-5" />
              </button>
            </header>

            <!-- Loading -->
            <div v-if="loadingChat" class="flex-1 flex items-center justify-center text-gray-400 text-sm">
              Carregando mensagens...
            </div>

            <!-- Lista de mensagens -->
            <div
              v-else
              ref="messagesContainer"
              class="flex-1 overflow-y-auto px-4 py-4 space-y-4"
              style="background-image: radial-gradient(circle at 1px 1px, rgba(0,0,0,0.04) 1px, transparent 0); background-size: 16px 16px;"
            >
              <div v-if="messages.length === 0" class="text-center text-gray-400 py-16">
                <MessageSquare class="h-10 w-10 mx-auto mb-3 text-gray-200" />
                <p class="text-sm">Nenhuma mensagem ainda. Diga olá!</p>
              </div>

              <template v-for="group in groupedMessages" :key="group.key">
                <!-- Divisor de dia -->
                <div class="flex justify-center">
                  <span
                    class="text-[11px] font-medium text-gray-600 bg-white border border-gray-200 px-3 py-1 rounded-full shadow-sm"
                  >
                    {{ group.label }}
                  </span>
                </div>

                <div
                  v-for="msg in group.items"
                  :key="msg.id"
                  :class="['flex', msg.sender === currentUserRole ? 'justify-end' : 'justify-start']"
                >
                  <div
                    :class="[
                      'max-w-[75%] rounded-2xl px-3.5 py-2 shadow-sm',
                      msg.sender === currentUserRole
                        ? 'bg-[#286AD8] text-white rounded-br-sm'
                        : 'bg-white text-gray-800 border border-gray-100 rounded-bl-sm',
                    ]"
                  >
                    <p
                      v-if="msg.sender !== currentUserRole"
                      class="text-[11px] font-semibold mb-0.5 text-blue-600 capitalize"
                    >
                      {{ msg.senderName }}
                    </p>
                    <p
                      v-if="msg.message && !(msg.attachments?.length && (msg.message === 'Imagem' || msg.message === 'Arquivo'))"
                      class="text-sm whitespace-pre-wrap break-words"
                    >{{ msg.message }}</p>
                    <div v-if="msg.attachments && msg.attachments.length > 0" class="mt-1.5 space-y-1.5">
                      <template v-for="att in msg.attachments" :key="att.id">
                        <button
                          v-if="isImageUrl(att.url)"
                          type="button"
                          @click="openLightbox(att.url)"
                          class="block rounded-lg overflow-hidden border border-black/5 hover:opacity-90 transition-opacity max-w-[260px]"
                        >
                          <img
                            :src="att.url"
                            :alt="att.name"
                            class="block max-h-[260px] w-auto object-cover"
                            loading="lazy"
                          />
                        </button>
                        <FileAttachment
                          v-else
                          :name="att.name"
                          :url="att.url"
                          :size="att.size"
                          variant="compact"
                          :class="
                            msg.sender === currentUserRole
                              ? 'bg-white/10 border-white/20 text-white'
                              : ''
                          "
                        />
                      </template>
                    </div>
                    <p
                      :class="[
                        'text-[10px] mt-1 text-right',
                        msg.sender === currentUserRole ? 'text-white/70' : 'text-gray-400',
                      ]"
                    >
                      {{ formatHour(msg.timestamp) }}
                    </p>
                  </div>
                </div>
              </template>
            </div>

            <!-- Composer -->
            <footer class="flex-shrink-0 bg-white border-t border-gray-200 p-3">
              <div
                v-if="activeConversation.locked"
                class="flex items-center justify-center gap-2 py-3 px-4 bg-gray-50 rounded-lg border border-dashed border-gray-200"
              >
                <Lock class="h-4 w-4 text-[#286AD8]" />
                <p class="text-sm text-[#435574]">
                  O chat será liberado após a confirmação do pagamento.
                </p>
              </div>

              <div v-else class="space-y-2">
                <div v-if="messageAttachments.length > 0" class="flex flex-wrap gap-2">
                  <template v-for="(file, index) in messageAttachments" :key="fileKey(file)">
                    <!-- Preview de imagem -->
                    <div
                      v-if="isImageFile(file)"
                      class="relative group rounded-lg overflow-hidden border border-blue-100 bg-blue-50"
                    >
                      <img
                        :src="attachmentPreviews[fileKey(file)]"
                        :alt="file.name"
                        class="h-20 w-20 object-cover"
                      />
                      <button
                        @click="removeAttachment(index)"
                        class="absolute top-1 right-1 h-5 w-5 rounded-full bg-black/60 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"
                        title="Remover anexo"
                      >
                        <X class="h-3 w-3" />
                      </button>
                    </div>

                    <!-- Chip para arquivos não-imagem -->
                    <div
                      v-else
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
                  </template>
                </div>

                <div class="flex items-end gap-2">
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
                    title="Anexar arquivo"
                  >
                    <Paperclip class="h-4 w-4" />
                  </Button>
                  <Textarea
                    v-model="newMessage"
                    placeholder="Digite sua mensagem..."
                    :rows="1"
                    class="flex-1 resize-none min-h-9 max-h-32 py-1.5 leading-6"
                    @keydown="onComposerKeydown"
                  />
                  <Button
                    @click="handleSend"
                    :disabled="
                      (!newMessage.trim() && messageAttachments.length === 0) || isSending
                    "
                    variant="default"
                    size="icon"
                    title="Enviar"
                  >
                    <Send class="h-4 w-4" />
                  </Button>
                </div>
              </div>
            </footer>
          </template>
        </section>
      </div>
    </div>

    <!-- Lightbox de imagem -->
    <div
      v-if="lightboxUrl"
      class="fixed inset-0 z-[100] bg-black/95 flex items-center justify-center p-4"
      @click.self="closeLightbox"
    >
      <!-- Contador -->
      <div
        v-if="conversationImages.length > 1"
        class="absolute top-4 left-4 px-3 py-1.5 rounded-full bg-white/10 text-white text-sm font-medium"
      >
        {{ lightboxIndex + 1 }} / {{ conversationImages.length }}
      </div>

      <!-- Ações -->
      <div class="absolute top-4 right-4 flex items-center gap-2">
        <button
          @click="downloadFromUrl(lightboxUrl)"
          :disabled="isDownloading"
          class="h-10 w-10 rounded-full bg-white/10 hover:bg-white/20 text-white flex items-center justify-center disabled:opacity-50"
          title="Baixar"
        >
          <Download class="h-5 w-5" />
        </button>
        <a
          :href="lightboxUrl"
          target="_blank"
          rel="noopener"
          class="h-10 w-10 rounded-full bg-white/10 hover:bg-white/20 text-white flex items-center justify-center"
          title="Abrir em nova guia"
        >
          <ExternalLink class="h-5 w-5" />
        </a>
        <button
          @click="closeLightbox"
          class="h-10 w-10 rounded-full bg-white/10 hover:bg-white/20 text-white flex items-center justify-center"
          title="Fechar"
        >
          <X class="h-5 w-5" />
        </button>
      </div>

      <!-- Seta anterior -->
      <button
        v-if="hasMultiple"
        @click.stop="prevImage"
        :disabled="!canPrev"
        class="absolute left-4 top-1/2 -translate-y-1/2 h-12 w-12 rounded-full bg-white/10 text-white flex items-center justify-center transition-opacity enabled:hover:bg-white/20 disabled:opacity-30 disabled:cursor-not-allowed"
        title="Imagem anterior"
      >
        <ChevronLeft class="h-6 w-6" />
      </button>

      <!-- Seta próxima -->
      <button
        v-if="hasMultiple"
        @click.stop="nextImage"
        :disabled="!canNext"
        class="absolute right-4 top-1/2 -translate-y-1/2 h-12 w-12 rounded-full bg-white/10 text-white flex items-center justify-center transition-opacity enabled:hover:bg-white/20 disabled:opacity-30 disabled:cursor-not-allowed"
        title="Próxima imagem"
      >
        <ChevronRight class="h-6 w-6" />
      </button>

      <img
        :src="lightboxUrl"
        :key="lightboxUrl"
        alt="Visualização"
        class="max-w-full max-h-full rounded-lg shadow-2xl object-contain"
        @click.stop
      />
    </div>
  </MainLayout>
</template>
