<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import {
  ArrowLeft,
  Clock,
  MapPin,
  MessageSquare,
  CheckCircle,
  Lock,
  Sparkles,
  Send,
  FileText,
  Upload,
} from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Input from '../ui/input.vue';
import Card from '../ui/card.vue';
import Badge from '../ui/badge.vue';
import Avatar from '../ui/avatar.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import AvatarImage from '../ui/avatar-image.vue';
import Progress from '../ui/progress.vue';
import Tabs from '../ui/tabs.vue';
import TabsContent from '../ui/tabs-content.vue';
import TabsList from '../ui/tabs-list.vue';
import TabsTrigger from '../ui/tabs-trigger.vue';
import FileAttachment from '../shared/FileAttachment.vue';
import { useClientRequestStore, type ServiceRequest } from '@/stores/clientRequestStore';
import ServiceRequestTimeline from '../service/ServiceRequestTimeline.vue';
import { useServiceRequestWebSocket, type ServiceRequestNotification } from '@/composables/useServiceRequestWebSocket';
import { toast } from '@/utils/toast';
import { getStatusLabel } from '@/utils/statusTranslations';

interface Props {
  onNavigate: (page: string, params?: any) => void;
  requestId: string;
}

const props = defineProps<Props>();
const store = useClientRequestStore();

const request = ref<ServiceRequest | null>(null);
const activeTab = ref('details');
const newMessage = ref('');
const isSendingMessage = ref(false);
let wsConnection: ReturnType<typeof useServiceRequestWebSocket> | null = null;

const clientAttachments = computed(() => request.value?.attachments || []);
const professionalAttachments = computed(() => [
  ...(request.value?.freelancerResponse?.attachments || []),
  ...(request.value?.freelancerAttachments || []),
]);
const hasProjectAttachments = computed(() =>
  clientAttachments.value.length > 0 || professionalAttachments.value.length > 0
);

const loadData = async (preserveCurrent = false) => {
  const loadedRequest = await store.loadRequestById(props.requestId);
  if (loadedRequest) {
    request.value = loadedRequest;
    await store.loadHistory(props.requestId);
    return;
  }

  if (!preserveCurrent) {
    request.value = store.getRequestById(props.requestId) || null;
    if (request.value) {
      await store.loadHistory(props.requestId);
    }
  }
};

const getUserEmail = () => {
  const token = localStorage.getItem('token');
  if (!token) return null;

  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.sub || payload.email;
  } catch (error) {
    console.error('Erro ao extrair email do token:', error);
    return null;
  }
};

const handleServiceRequestNotification = async (event: Event) => {
  const notification = (event as CustomEvent<ServiceRequestNotification>).detail;
  if (
    notification.type !== 'SERVICE_REQUEST_STATUS_CHANGED' ||
    String(notification.serviceRequestId) !== props.requestId
  ) {
    return;
  }

  try {
    await loadData(true);
  } catch (error) {
    console.error('Erro ao recarregar detalhes do projeto após atualização:', error);
  }
};

onMounted(async () => {
  await loadData();

  const userEmail = getUserEmail();
  if (userEmail) {
    wsConnection = useServiceRequestWebSocket(userEmail);
    wsConnection.connect().catch((error) => {
      console.error('Erro ao conectar WebSocket do detalhe do projeto:', error);
    });
    window.addEventListener('service-request-notification', handleServiceRequestNotification);
  }
});

onUnmounted(() => {
  window.removeEventListener('service-request-notification', handleServiceRequestNotification);
  if (wsConnection) {
    wsConnection.disconnect();
  }
});

watch(() => props.requestId, async (newId, oldId) => {
  if (newId && newId !== oldId) {
    await loadData();
  }
});

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value);
};

const formatDate = (dateString: string) => {
  const date = new Date(dateString);
  return date.toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
};

const goBack = () => {
  props.onNavigate('cliente-projects');
};

const handleSendMessage = async () => {
  if (!request.value || !newMessage.value.trim()) return;

  isSendingMessage.value = true;
  const success = await store.sendMessage(request.value.id, newMessage.value.trim());
  isSendingMessage.value = false;

  if (success) {
    newMessage.value = '';
    toast.success('Mensagem enviada');
  } else {
    toast.error('Erro ao enviar mensagem');
  }
};

const handleApproveDelivery = () => {
  props.onNavigate('cliente-delivery-approval', { requestId: props.requestId });
};

const progress = computed(() => {
  if (!request.value) return 0;
  switch (request.value.status) {
    case 'accepted': return 33;
    case 'in_progress': return 66;
    case 'delivered':
    case 'completed': return 100;
    default: return 0;
  }
});

const currentStepLabel = computed(() => {
  if (!request.value) return '';
  switch (request.value.status) {
    case 'accepted': return 'Aguardando início do trabalho';
    case 'in_progress': return 'Trabalho em andamento';
    case 'delivered': return 'Revisão da entrega';
    case 'completed': return 'Projeto concluído';
    default: return '';
  }
});

const getStatusColor = (status: string) => {
  const colors: Record<string, string> = {
    delivered: 'bg-amber-100 text-amber-700',
    in_progress: 'bg-blue-100 text-blue-700',
    accepted: 'bg-green-100 text-green-700',
    completed: 'bg-teal-100 text-teal-700',
  };
  return colors[status] || 'bg-gray-100 text-gray-700';
};
</script>

<template>
  <div class="max-w-7xl mx-auto space-y-6 p-6">
    <div class="flex items-center gap-4 mb-4">
      <Button @click="goBack" variant="ghost" size="icon">
        <ArrowLeft class="h-5 w-5" />
      </Button>
      <div class="flex-1">
        <div class="flex items-center gap-3">
          <h1 class="text-2xl font-bold text-[#263238]">{{ request?.service || 'Carregando...' }}</h1>
          <Badge v-if="request" :class="getStatusColor(request.status)">
            {{ getStatusLabel(request.status) }}
          </Badge>
        </div>
        <p class="text-[#435574]">Acompanhamento detalhado do seu projeto</p>
      </div>
      <div v-if="request?.status === 'delivered'" class="flex items-center gap-2">
        <Button @click="handleApproveDelivery" class="bg-green-600 hover:bg-green-700">
          <CheckCircle class="h-4 w-4 mr-2" />
          Aprovar Entrega
        </Button>
      </div>
    </div>

    <Card v-if="request" class="p-6 bg-gradient-to-r from-blue-50 to-white border-blue-100 shadow-sm">
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div class="flex-1 space-y-2">
          <div class="flex items-center justify-between text-sm mb-1">
            <span class="font-medium text-[#263238] flex items-center gap-2">
              <Clock class="h-4 w-4 text-blue-600" />
              {{ currentStepLabel }}
            </span>
            <span class="font-bold text-blue-700">{{ progress }}%</span>
          </div>
          <Progress :model-value="progress" class="h-3 shadow-inner" />
          <p class="text-xs text-[#435574] mt-2">
            Status atual: <span class="font-semibold">{{ getStatusLabel(request.status) }}</span>
          </p>
        </div>

        <div class="flex items-center gap-8 px-6 border-l hidden md:flex">
          <div class="text-center">
            <p class="text-xs text-[#435574] uppercase tracking-wider mb-1">Início</p>
            <p class="font-semibold text-[#263238]">{{ new Date(request.createdAt).toLocaleDateString('pt-BR') }}</p>
          </div>
          <div class="text-center">
            <p class="text-xs text-[#435574] uppercase tracking-wider mb-1">Investimento</p>
            <p class="font-bold text-green-600">{{ formatCurrency(request.freelancerResponse?.value || request.budget) }}</p>
          </div>
        </div>
      </div>
    </Card>

    <div v-if="request" class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <div class="lg:col-span-2 space-y-6">
        <Tabs v-model="activeTab">
          <TabsList class="grid w-full grid-cols-4 bg-white border shadow-sm">
            <TabsTrigger value="details" class="data-[state=active]:text-blue-600 data-[state=active]:border-b-2 data-[state=active]:border-blue-600">
              Detalhes
            </TabsTrigger>
            <TabsTrigger value="messages" class="data-[state=active]:text-blue-600 data-[state=active]:border-b-2 data-[state=active]:border-blue-600">
              Mensagens
            </TabsTrigger>
            <TabsTrigger value="history" class="data-[state=active]:text-blue-600 data-[state=active]:border-b-2 data-[state=active]:border-blue-600">
              Histórico
            </TabsTrigger>
            <TabsTrigger value="files" class="data-[state=active]:text-blue-600 data-[state=active]:border-b-2 data-[state=active]:border-blue-600">
              Anexo
            </TabsTrigger>
          </TabsList>

          <TabsContent value="details" class="mt-4 space-y-4">
            <Card class="p-6">
              <h3 class="font-bold text-[#263238] mb-4 flex items-center gap-2">
                <FileText class="h-5 w-5 text-blue-600" />
                Escopo e Descrição
              </h3>
              <div class="prose prose-blue max-w-none text-[#435574]">
                <p class="whitespace-pre-line">{{ request.description }}</p>
              </div>

              <div v-if="request.freelancerResponse?.scope" class="mt-8 pt-6 border-t font-medium text-[#263238]">
                <h4 class="mb-3">Resposta do Profissional:</h4>
                <p class="text-sm text-[#435574] italic bg-gray-50 p-4 rounded-lg border border-dashed">
                  "{{ request.freelancerResponse.scope }}"
                </p>
              </div>
            </Card>

            <Card class="p-6">
              <h3 class="font-bold text-[#263238] mb-4">Informações de Localização</h3>
              <div class="flex items-start gap-3 text-[#435574]">
                <MapPin class="h-5 w-5 text-red-500 shrink-0" />
                <p>{{ request.address || 'Local não informado' }}</p>
              </div>
            </Card>
          </TabsContent>

          <TabsContent value="messages" class="mt-4">
            <Card class="p-0 overflow-hidden flex flex-col min-h-[500px]">
              <div class="p-4 border-b bg-gray-50 flex items-center justify-between">
                <h3 class="font-bold text-[#263238]">Conversa com {{ request.freelancerName }}</h3>
                <Badge variant="outline" class="bg-white">Online</Badge>
              </div>

              <div class="flex-1 p-6 space-y-4 overflow-y-auto max-h-[400px]">
                <div v-if="!request.messages || request.messages.length === 0" class="text-center py-12">
                  <MessageSquare class="h-12 w-12 text-gray-300 mx-auto mb-4" />
                  <p class="text-[#435574]">Nenhuma mensagem trocada ainda.</p>
                </div>

                <div v-for="msg in request.messages" :key="msg.id" :class="`flex ${msg.sender === 'client' ? 'justify-end' : 'justify-start'}`">
                  <div :class="`max-w-[80%] rounded-2xl px-4 py-2 ${msg.sender === 'client' ? 'bg-blue-600 text-white rounded-tr-none' : 'bg-gray-100 text-[#263238] rounded-tl-none border'}`">
                    <p class="text-xs mb-1 opacity-70 font-semibold">{{ msg.senderName }}</p>
                    <p class="text-sm">{{ msg.message }}</p>
                    <p class="text-[10px] text-right mt-1 opacity-60">{{ formatDate(msg.timestamp) }}</p>
                  </div>
                </div>
              </div>

              <div class="p-4 border-t bg-white">
                <div class="flex gap-2">
                  <Input v-model="newMessage" placeholder="Digite sua mensagem..." @keyup.enter="handleSendMessage" class="flex-1 rounded-full px-6" />
                  <Button @click="handleSendMessage" :disabled="!newMessage.trim() || isSendingMessage" class="bg-blue-600 hover:bg-blue-700 rounded-full w-12 h-12 p-0 flex items-center justify-center">
                    <Send class="h-5 w-5 text-white" />
                  </Button>
                </div>
              </div>
            </Card>
          </TabsContent>

          <TabsContent value="history" class="mt-4">
            <ServiceRequestTimeline :request-id="request.id" title="Linha do Tempo do Projeto" />
          </TabsContent>

          <TabsContent value="files" class="mt-4">
            <Card class="p-6">
              <div class="flex items-center justify-between mb-6">
                <h3 class="font-bold text-[#263238]">Anexos do Projeto</h3>
                <Button variant="outline" size="sm" class="text-blue-600 border-blue-200">
                  <Upload class="h-4 w-4 mr-2" />
                  Enviar Arquivo
                </Button>
              </div>

              <div v-if="!hasProjectAttachments" class="text-center py-12 border-2 border-dashed rounded-xl">
                <FileText class="h-12 w-12 text-gray-300 mx-auto mb-4" />
                <p class="text-[#435574]">Nenhum arquivo enviado para este projeto.</p>
              </div>

              <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <FileAttachment
                  v-for="file in clientAttachments"
                  :key="file.id"
                  :name="file.name"
                  :url="file.url"
                  :size="file.size"
                  sender-info="Enviado por você"
                  variant="blue"
                />

                <FileAttachment
                  v-for="file in professionalAttachments"
                  :key="file.id"
                  :name="file.name"
                  :url="file.url"
                  :size="file.size"
                  :description="file.description"
                  sender-info="Enviado pelo profissional"
                  variant="amber"
                />
              </div>
            </Card>
          </TabsContent>
        </Tabs>
      </div>

      <div class="space-y-6">
        <Card class="p-6 text-center">
          <h3 class="font-bold text-[#263238] mb-4 text-left">Profissional</h3>
          <Avatar class="h-24 w-24 mx-auto mb-4 border-4 border-blue-50">
            <AvatarImage :src="request.freelancerAvatar" />
            <AvatarFallback class="bg-blue-600 text-white text-2xl font-bold">
              {{ getInitials(request.freelancerName || 'Freelancer') }}
            </AvatarFallback>
          </Avatar>
          <p class="font-bold text-[#263238] text-lg">{{ request.freelancerName || 'Profissional' }}</p>
          <p class="text-sm text-[#435574] mb-4">{{ request.category }}</p>

          <div class="grid grid-cols-2 gap-4 pt-4 border-t">
            <div class="text-center border-r">
              <p class="text-xs text-[#435574] uppercase mb-1 font-semibold tracking-tighter">Avaliação</p>
              <div class="flex items-center justify-center gap-1">
                <Sparkles class="h-3 w-3 text-amber-500" />
                <span class="font-bold">5.0</span>
              </div>
            </div>
            <div class="text-center">
              <p class="text-xs text-[#435574] uppercase mb-1 font-semibold tracking-tighter">Visto por último</p>
              <p class="text-xs font-bold text-gray-400">Hoje</p>
            </div>
          </div>

          <Button variant="outline" class="w-full mt-6 rounded-full border-blue-200 text-blue-600 hover:bg-blue-50" @click="activeTab = 'messages'">
            <MessageSquare class="h-4 w-4 mr-2" />
            Abrir Chat
          </Button>
        </Card>

        <Card class="p-6">
          <h3 class="font-bold text-[#263238] mb-4">Resumo Financeiro</h3>
          <div class="space-y-4">
            <div class="flex justify-between items-center text-sm">
              <span class="text-[#435574]">Subtotal</span>
              <span class="font-semibold">{{ formatCurrency(request.freelancerResponse?.value || request.budget) }}</span>
            </div>
            <div class="flex justify-between items-center text-sm">
              <span class="text-[#435574]">Taxa de Serviço</span>
              <span class="font-semibold text-green-600">R$ 0,00</span>
            </div>
            <div class="pt-4 border-t flex justify-between items-center">
              <span class="font-bold text-[#263238]">Total</span>
              <span class="text-xl font-bold text-blue-700">{{ formatCurrency(request.freelancerResponse?.value || request.budget) }}</span>
            </div>
          </div>

          <div class="mt-6 p-4 bg-gray-50 rounded-lg flex items-start gap-2">
            <Lock class="h-4 w-4 text-amber-500 mt-1 shrink-0" />
            <p class="text-[10px] text-[#435574]">
              Seu pagamento está seguro. O valor só será liberado ao profissional após sua confirmação final de entrega.
            </p>
          </div>
        </Card>
      </div>
    </div>
  </div>
</template>

<style scoped>
.prose {
  line-height: 1.6;
}
</style>
