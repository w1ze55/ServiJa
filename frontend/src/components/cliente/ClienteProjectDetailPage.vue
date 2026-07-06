<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import {
  ArrowLeft,
  Clock,
  MapPin,
  MessageSquare,
  CheckCircle,
  AlertCircle,
  Lock,
  Star,
  FileText,
  Upload,
  Loader2,
} from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Card from '../ui/card.vue';
import Badge from '../ui/badge.vue';
import DeliveryAdjustmentModal from './DeliveryAdjustmentModal.vue';
import Avatar from '../ui/avatar.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import AvatarImage from '../ui/avatar-image.vue';
import Progress from '../ui/progress.vue';
import Tabs from '../ui/tabs.vue';
import TabsContent from '../ui/tabs-content.vue';
import TabsList from '../ui/tabs-list.vue';
import TabsTrigger from '../ui/tabs-trigger.vue';
import FileAttachment from '../shared/FileAttachment.vue';
import AttachmentUploadField from '../shared/AttachmentUploadField.vue';
import ProjectChatPanel from '../shared/ProjectChatPanel.vue';
import Dialog from '../ui/dialog.vue';
import DialogContent from '../ui/dialog-content.vue';
import DialogDescription from '../ui/dialog-description.vue';
import DialogFooter from '../ui/dialog-footer.vue';
import DialogHeader from '../ui/dialog-header.vue';
import DialogTitle from '../ui/dialog-title.vue';
import { useClientRequestStore, type ServiceRequest } from '@/stores/clientRequestStore';
import { useDeliveryAdjustmentStore } from '@/stores/deliveryAdjustmentStore';
import ServiceRequestTimeline from '../service/ServiceRequestTimeline.vue';
import { useServiceRequestWebSocket, type ServiceRequestNotification } from '@/composables/useServiceRequestWebSocket';
import { formatApiDate } from '@/utils/apiDate';
import { toast } from '@/utils/toast';
import { getStatusLabel } from '@/utils/statusTranslations';
import api from '@/api/config/axios';

interface Props {
  onNavigate: (page: string, params?: any) => void;
  requestId: string;
}

const props = defineProps<Props>();
const router = useRouter();
const store = useClientRequestStore();
const adjustmentStore = useDeliveryAdjustmentStore();

const request = ref<ServiceRequest | null>(null);
const freelancerRating = ref<number | null>(null);
const freelancerTotalReviews = ref(0);
const freelancerCompletedJobs = ref(0);
const hasFreelancerRating = computed(
  () => freelancerRating.value != null && freelancerTotalReviews.value > 0,
);
const activeTab = ref('details');
const activeFileTab = ref<'client' | 'freelancer'>('client');
const showAdjustmentModal = ref(false);
const isUploadingFiles = ref(false);
const showUploadDialog = ref(false);
const uploadFiles = ref<File[]>([]);
const uploadProgress = ref(0);
let wsConnection: ReturnType<typeof useServiceRequestWebSocket> | null = null;

const clientAttachments = computed(() => request.value?.attachments || []);
const professionalAttachments = computed(() => [
  ...(request.value?.freelancerResponse?.attachments || []),
  ...(request.value?.freelancerAttachments || []),
]);
const adjustmentAlreadyUsed = computed(() => adjustmentStore.adjustments.length > 0);
const pendingAdjustment = computed(() => {
  if (!request.value) return null;
  return (
    adjustmentStore.adjustments.find(
      (adjustment) =>
        adjustment.serviceRequestId.toString() === props.requestId &&
        (adjustment.status === 'PENDING' || adjustment.status === 'ACKNOWLEDGED')
    ) ?? null
  );
});

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

const loadAdjustments = async () => {
  if (!props.requestId) return;
  await adjustmentStore.getAdjustmentsByServiceRequest(parseInt(props.requestId));
};

const loadFreelancerRating = async () => {
  const freelancerId = request.value?.freelancerId;
  if (!freelancerId) return;
  try {
    const response = await api.get(`/freelancer/${freelancerId}/profile`);
    freelancerRating.value = response.data.rating ?? null;
    freelancerTotalReviews.value = response.data.totalReviews ?? 0;
    freelancerCompletedJobs.value = response.data.completedJobs ?? 0;
  } catch {
    freelancerRating.value = null;
    freelancerTotalReviews.value = 0;
    freelancerCompletedJobs.value = 0;
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
  const relevantTypes = ['SERVICE_REQUEST_STATUS_CHANGED', 'ADJUSTMENT_STATUS_CHANGED'];
  if (
    !relevantTypes.includes(notification.type) ||
    String(notification.serviceRequestId) !== props.requestId
  ) {
    return;
  }

  try {
    await Promise.all([loadData(true), loadAdjustments()]);
  } catch (error) {
    console.error('Erro ao recarregar detalhes do projeto após atualização:', error);
  }
};

onMounted(async () => {
  await loadData();
  await loadFreelancerRating();
  await loadAdjustments();

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
    await loadAdjustments();
  }
});

const formatCurrency = (value?: number | null) => {
  if (value == null) return 'A combinar';
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value);
};

const formatDate = (dateString: string) => {
  return formatApiDate(dateString, {
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

const handleApproveDelivery = () => {
  props.onNavigate('cliente-delivery-approval', { requestId: props.requestId });
};

const handleOpenAdjustmentModal = () => {
  showAdjustmentModal.value = true;
};

const handleRequestAdjustments = async (reason: string) => {
  const success = await adjustmentStore.requestAdjustment(parseInt(props.requestId), reason);

  if (success) {
    toast.success('Solicitação de ajustes enviada ao freelancer.');
    await loadAdjustments();
    await loadData(true);
    return true;
  }

  toast.error('Erro ao enviar solicitação. Tente novamente.');
  return false;
};

const handleGoToChat = () => {
  router.push(`/chat/${props.requestId}`);
};

const handleUploadFileRejected = (message: string) => {
  toast.error(message);
};

const openUploadDialog = () => {
  if (isUploadingFiles.value) return;
  uploadFiles.value = [];
  uploadProgress.value = 0;
  showUploadDialog.value = true;
};

const handleUploadFiles = async () => {
  if (!request.value) {
    toast.error('Projeto não encontrado');
    return;
  }

  if (uploadFiles.value.length === 0) {
    toast.error('Selecione pelo menos um arquivo');
    return;
  }

  isUploadingFiles.value = true;
  uploadProgress.value = 0;
  let progressInterval: ReturnType<typeof setInterval> | null = null;

  try {
    const formData = new FormData();
    uploadFiles.value.forEach((file) => formData.append('files', file));

    progressInterval = setInterval(() => {
      if (uploadProgress.value < 90) uploadProgress.value += 10;
    }, 300);

    const uploadResponse = await api.post('/upload/images', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });

    if (progressInterval) {
      clearInterval(progressInterval);
      progressInterval = null;
    }

    const urls = uploadResponse.data.urls || [];
    if (!urls.length) {
      throw new Error('Nenhum arquivo foi enviado');
    }

    await api.post(`/service-requests/${request.value.id}/contractor-attachments`, { urls });

    uploadProgress.value = 100;
    toast.success(uploadFiles.value.length === 1 ? 'Arquivo enviado com sucesso.' : 'Arquivos enviados com sucesso.');
    showUploadDialog.value = false;
    await loadData(true);
    activeTab.value = 'files';
    activeFileTab.value = 'client';
  } catch (error) {
    if (progressInterval) {
      clearInterval(progressInterval);
    }
    console.error('Erro ao enviar arquivos do projeto:', error);
    toast.error('Erro ao enviar arquivos. Tente novamente.');
  } finally {
    isUploadingFiles.value = false;
  }
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
      <div v-if="request?.status === 'delivered' && !pendingAdjustment" class="flex items-center gap-2">
        <Button
          @click="handleOpenAdjustmentModal"
          variant="warning"
        >
          <AlertCircle class="h-4 w-4 mr-2" />
          Solicitar Ajustes
        </Button>
        <Button @click="handleApproveDelivery" variant="success">
          <CheckCircle class="h-4 w-4 mr-2" />
          Aprovar Entrega
        </Button>
      </div>
    </div>

    <div v-if="pendingAdjustment" class="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
      <div class="flex items-start gap-3">
        <AlertCircle class="h-5 w-5 text-yellow-600 mt-0.5 shrink-0" />
        <div>
          <h4 class="text-sm font-semibold text-yellow-800 mb-1">
            Ajustes solicitados — aguardando reenvio do profissional
          </h4>
          <p class="text-sm text-yellow-700">
            Você poderá aprovar a entrega assim que o profissional reenviar o trabalho com os ajustes solicitados.
          </p>
        </div>
      </div>
    </div>

    <Card v-if="request" class="p-6 bg-white border shadow-sm">
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
            <p class="font-semibold text-[#263238]">{{ formatApiDate(request.createdAt, { day: '2-digit', month: '2-digit', year: 'numeric' }) }}</p>
          </div>
        </div>
      </div>
    </Card>

    <div v-if="request" class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <div class="lg:col-span-2 space-y-6">
        <Tabs v-model="activeTab">
          <TabsList class="tabs-list grid w-full grid-cols-4">
            <TabsTrigger value="details">
              Detalhes
            </TabsTrigger>
            <TabsTrigger value="messages">
              Mensagens
            </TabsTrigger>
            <TabsTrigger value="history">
              Histórico
            </TabsTrigger>
            <TabsTrigger value="files">
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
                <MapPin class="h-5 w-5 text-blue-500 shrink-0" />
                <p>{{ request.address || 'Local não informado' }}</p>
              </div>
            </Card>
          </TabsContent>

          <TabsContent value="messages" class="mt-4">
            <Card class="p-4">
              <h3 class="font-bold text-[#263238] mb-4">Conversa com {{ request.freelancerName }}</h3>
              <ProjectChatPanel
                :service-request-id="Number(request.id)"
                current-user-role="client"
                :service-status="request.status"
              />
            </Card>
          </TabsContent>

          <TabsContent value="history" class="mt-4">
            <ServiceRequestTimeline :request-id="request.id" title="Linha do Tempo do Projeto" />
          </TabsContent>

          <TabsContent value="files" class="mt-4">
            <Card class="p-6">
              <div class="flex items-center justify-between mb-4">
                <h3 class="text-lg font-semibold text-[#263238]">Arquivos do Projeto</h3>
                <Button
                  variant="outline"
                  :disabled="isUploadingFiles"
                  @click="openUploadDialog"
                >
                  <Loader2 v-if="isUploadingFiles" class="h-4 w-4 mr-2 animate-spin" />
                  <Upload v-else class="h-4 w-4 mr-2" />
                  {{ isUploadingFiles ? 'Enviando...' : 'Enviar Arquivos' }}
                </Button>
              </div>

              <div class="flex border-b border-gray-200 mb-4">
                <button
                  @click="activeFileTab = 'client'"
                  :class="[
                    'px-4 py-2 text-sm font-medium border-b-2 transition-colors -mb-px',
                    activeFileTab === 'client'
                      ? 'border-[#286AD8] text-[#286AD8]'
                      : 'border-transparent text-[#435574] hover:text-[#263238]'
                  ]"
                >
                  Cliente
                  <Badge v-if="clientAttachments.length > 0" variant="secondary" class="ml-2">
                    {{ clientAttachments.length }}
                  </Badge>
                </button>
                <button
                  @click="activeFileTab = 'freelancer'"
                  :class="[
                    'px-4 py-2 text-sm font-medium border-b-2 transition-colors -mb-px',
                    activeFileTab === 'freelancer'
                      ? 'border-[#286AD8] text-[#286AD8]'
                      : 'border-transparent text-[#435574] hover:text-[#263238]'
                  ]"
                >
                  Freelancer
                  <Badge v-if="professionalAttachments.length > 0" variant="secondary" class="ml-2">
                    {{ professionalAttachments.length }}
                  </Badge>
                </button>
              </div>

              <div v-if="activeFileTab === 'client'">
                <div v-if="clientAttachments.length === 0" class="text-center py-8">
                  <FileText class="h-12 w-12 text-gray-300 mx-auto mb-3" />
                  <p class="text-[#435574]">Nenhum arquivo enviado pelo cliente</p>
                </div>
                <div v-else class="space-y-2">
                  <FileAttachment
                    v-for="attachment in clientAttachments"
                    :key="attachment.id"
                    :name="attachment.name"
                    :url="attachment.url"
                    :size="attachment.size"
                    variant="blue"
                    sender-info="Enviado pelo cliente"
                  />
                </div>
              </div>

              <div v-if="activeFileTab === 'freelancer'">
                <div v-if="professionalAttachments.length === 0" class="text-center py-8">
                  <FileText class="h-12 w-12 text-gray-300 mx-auto mb-3" />
                  <p class="text-[#435574]">Nenhum arquivo enviado pelo freelancer</p>
                </div>
                <div v-else class="space-y-2">
                  <FileAttachment
                    v-for="attachment in professionalAttachments"
                    :key="attachment.id"
                    :name="attachment.name"
                    :url="attachment.url"
                    :size="attachment.size"
                    :description="attachment.description"
                    variant="amber"
                    sender-info="Enviado pelo profissional"
                  />
                </div>
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
              <template v-if="hasFreelancerRating">
                <div class="flex items-center justify-center gap-1">
                  <Star class="h-3 w-3 fill-yellow-400 text-yellow-400" />
                  <span class="font-bold">{{ freelancerRating!.toFixed(1) }}</span>
                </div>
                <p class="text-[10px] text-gray-400">{{ freelancerTotalReviews }} {{ freelancerTotalReviews === 1 ? 'avaliação' : 'avaliações' }}</p>
              </template>
              <p v-else class="text-xs text-gray-400">Sem avaliações</p>
            </div>
            <div class="text-center">
              <p class="text-xs text-[#435574] uppercase mb-1 font-semibold tracking-tighter">Serviços</p>
              <p v-if="freelancerCompletedJobs > 0" class="text-xs text-gray-400">{{ freelancerCompletedJobs }} concluídos</p>
              <p v-else class="text-xs text-gray-400">Nenhum serviço concluído</p>
            </div>
          </div>

          <Button variant="outline" class="w-full mt-6 rounded-full border-primary text-primary" @click="handleGoToChat">
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

    <DeliveryAdjustmentModal
      v-model:open="showAdjustmentModal"
      :service-name="request?.service"
      :adjustment-already-used="adjustmentAlreadyUsed"
      :is-loading="adjustmentStore.isLoading"
      :on-submit="handleRequestAdjustments"
      :on-go-to-chat="handleGoToChat"
    />

    <Dialog :open="showUploadDialog" @update:open="(val) => (showUploadDialog = val)">
      <DialogContent class="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Enviar Arquivos</DialogTitle>
          <DialogDescription>
            Faça upload de arquivos relacionados ao projeto
            <template v-if="request"> "{{ request.service }}"</template>
          </DialogDescription>
        </DialogHeader>

        <div class="space-y-4">
          <AttachmentUploadField
            v-model:files="uploadFiles"
            label="Arquivos *"
            empty-message="Clique para fazer upload ou arraste os arquivos"
            helper-text="PDF, JPG, PNG, DOC, DOCX - Máximo 10 MB por arquivo"
            accept=".pdf,.jpg,.jpeg,.png,.doc,.docx"
            :allowed-mime-types="[
              'application/pdf',
              'image/jpeg',
              'image/png',
              'application/msword',
              'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
            ]"
            :allowed-extensions="['pdf', 'jpg', 'jpeg', 'png', 'doc', 'docx']"
            :disabled="isUploadingFiles"
            @file-rejected="handleUploadFileRejected"
          />

          <div v-if="isUploadingFiles" class="space-y-2">
            <div class="flex items-center justify-between text-sm">
              <span class="text-[#435574]">Enviando arquivos...</span>
              <span class="text-[#263238]">{{ uploadProgress }}%</span>
            </div>
            <Progress :value="uploadProgress" class="h-2" />
          </div>
        </div>

        <DialogFooter>
          <Button @click="showUploadDialog = false" variant="outline" :disabled="isUploadingFiles">
            Cancelar
          </Button>
          <Button
            @click="handleUploadFiles"
            :disabled="isUploadingFiles || uploadFiles.length === 0"
            variant="default"
          >
            <Loader2 v-if="isUploadingFiles" class="h-4 w-4 mr-2 animate-spin" />
            <Upload v-else class="h-4 w-4 mr-2" />
            {{ isUploadingFiles ? 'Enviando...' : 'Enviar Arquivos' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<style scoped>
.prose {
  line-height: 1.6;
}

@media (max-width: 768px) {
  .tabs-list {
    grid-template-columns: repeat(2, 1fr) !important;
    gap: 0.25rem;
  }
}
</style>
