<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import {
  ArrowLeft,
  Clock,
  MapPin,
  MessageSquare,
  CheckCircle,
  AlertCircle,
  FileText,
  Upload,
  Loader2,
  Calendar,
  DollarSign,
  Check,
} from 'lucide-vue-next';
import AttachmentUploadField from '../shared/AttachmentUploadField.vue';
import FileAttachment from '../shared/FileAttachment.vue';
import ChatBox from '../shared/ChatBox.vue';
import ServiceRequestTimeline from '../service/ServiceRequestTimeline.vue';
import Button from '../ui/button.vue';
import Card from '../ui/card.vue';
import Badge from '../ui/badge.vue';
import Checkbox from '../ui/checkbox.vue';
import Progress from '../ui/progress.vue';
import Avatar from '../ui/avatar.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import AvatarImage from '../ui/avatar-image.vue';
import Tabs from '../ui/tabs.vue';
import TabsContent from '../ui/tabs-content.vue';
import TabsList from '../ui/tabs-list.vue';
import TabsTrigger from '../ui/tabs-trigger.vue';
import AlertDialog from '../ui/alert-dialog.vue';
import AlertDialogAction from '../ui/alert-dialog-action.vue';
import AlertDialogCancel from '../ui/alert-dialog-cancel.vue';
import AlertDialogContent from '../ui/alert-dialog-content.vue';
import AlertDialogDescription from '../ui/alert-dialog-description.vue';
import AlertDialogFooter from '../ui/alert-dialog-footer.vue';
import AlertDialogHeader from '../ui/alert-dialog-header.vue';
import AlertDialogTitle from '../ui/alert-dialog-title.vue';
import Dialog from '../ui/dialog.vue';
import DialogContent from '../ui/dialog-content.vue';
import DialogDescription from '../ui/dialog-description.vue';
import DialogFooter from '../ui/dialog-footer.vue';
import DialogHeader from '../ui/dialog-header.vue';
import DialogTitle from '../ui/dialog-title.vue';
import { useProposalStore, type Milestone, type Proposal } from '@/stores/proposalStore';
import { useDeliveryAdjustmentStore } from '@/stores/deliveryAdjustmentStore';
import {
  useServiceRequestWebSocket,
  type ServiceRequestNotification,
} from '@/composables/useServiceRequestWebSocket';
import { formatApiDate } from '@/utils/apiDate';
import { getStatusLabel } from '@/utils/statusTranslations';
import { toast } from '@/utils/toast';
import api from '@/api/config/axios';

interface Props {
  onNavigate: (page: string, params?: any) => void;
  requestId?: string;
}

const props = defineProps<Props>();
const proposalStore = useProposalStore();
const adjustmentStore = useDeliveryAdjustmentStore();

const project = ref<Proposal | null>(null);
const activeTab = ref<'details' | 'messages' | 'history' | 'files' | 'milestones'>('details');
const activeFileTab = ref<'client' | 'freelancer'>('client');
const showCompleteDialog = ref(false);
const showCompleteAdjustmentDialog = ref(false);
const showUploadDialog = ref(false);
const isCompleting = ref(false);
const isCompletingAdjustment = ref(false);
const isUploading = ref(false);
const isSendingMessage = ref(false);
const uploadProgress = ref(0);
const uploadFiles = ref<File[]>([]);
const uploadDescription = ref('');

let wsConnection: ReturnType<typeof useServiceRequestWebSocket> | null = null;

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

const loadData = async () => {
  if (!props.requestId) return;

  await Promise.all([proposalStore.loadProposals(), adjustmentStore.getReceived()]);
  project.value = proposalStore.getProposalById(props.requestId) || null;

  if (project.value) {
    await proposalStore.loadHistory(project.value.id);
  }

  if (activeTab.value === 'milestones' && !hasMilestones.value) {
    activeTab.value = 'details';
  }
};

const handleServiceRequestNotification = async (event: Event) => {
  const notification = (event as CustomEvent<ServiceRequestNotification>).detail;
  if (props.requestId && String(notification.serviceRequestId) !== String(props.requestId)) {
    return;
  }

  switch (notification.type) {
    case 'SERVICE_REQUEST_STATUS_CHANGED':
    case 'SERVICE_COMPLETED_REVIEW_PENDING':
    case 'ADJUSTMENTS_REQUESTED':
    case 'ADJUSTMENT_STATUS_CHANGED':
    case 'ADJUSTMENT_DELETED':
    case 'SERVICE_REQUEST_CANCELLED':
      await loadData();
      break;
    default:
      break;
  }
};

onMounted(async () => {
  await loadData();

  const userEmail = getUserEmail();
  if (userEmail) {
    wsConnection = useServiceRequestWebSocket(userEmail);
    wsConnection.connect().catch((error) => {
      console.error('Erro ao conectar WebSocket do detalhe do projeto do freelancer:', error);
    });
    window.addEventListener('service-request-notification', handleServiceRequestNotification);
  }
});

onUnmounted(() => {
  window.removeEventListener('service-request-notification', handleServiceRequestNotification);
  wsConnection?.disconnect();
});

watch(() => props.requestId, async (newId, oldId) => {
  if (newId && newId !== oldId) {
    await loadData();
  }
});

const projectMilestones = computed(() => project.value?.freelancerResponse?.milestones ?? []);
const hasMilestones = computed(() => projectMilestones.value.length > 0);
const completedMilestones = computed(
  () => projectMilestones.value.filter((milestone) => milestone.status === 'completed').length
);

const pendingAdjustment = computed(() => {
  if (!project.value) return null;
  return adjustmentStore.adjustments.find(
    (adjustment) =>
      adjustment.serviceRequestId.toString() === project.value!.id &&
      (adjustment.status === 'PENDING' || adjustment.status === 'ACKNOWLEDGED')
  );
});

const projectValue = computed(() => project.value?.freelancerResponse?.value ?? project.value?.budget ?? null);
const platformFee = computed(() => (projectValue.value == null ? null : projectValue.value * 0.05));
const netValue = computed(() =>
  projectValue.value == null || platformFee.value == null ? null : projectValue.value - platformFee.value
);

const clientAttachments = computed(() => project.value?.attachments ?? []);
const freelancerAttachments = computed(() => [
  ...(project.value?.freelancerResponse?.attachments ?? []),
  ...(project.value?.freelancerAttachments ?? []),
]);

const projectProgress = computed(() => {
  if (hasMilestones.value) {
    return Math.round((completedMilestones.value / projectMilestones.value.length) * 100);
  }

  if (project.value?.status === 'completed' || project.value?.status === 'delivered') {
    return 100;
  }

  if (project.value?.status === 'in_progress') {
    return 66;
  }

  return 0;
});

const currentStepLabel = computed(() => {
  if (pendingAdjustment.value) return 'Cliente solicitou ajustes na entrega';
  if (project.value?.status === 'delivered') return 'Entrega enviada, aguardando aprovacao do cliente';
  if (project.value?.status === 'completed') return 'Projeto concluido';
  if (hasMilestones.value) return `${completedMilestones.value} de ${projectMilestones.value.length} marcos concluidos`;
  if (project.value?.status === 'in_progress') return 'Trabalho em andamento';
  return 'Projeto em acompanhamento';
});

const statusColor = computed(() => {
  if (pendingAdjustment.value) return 'bg-amber-100 text-amber-700';

  const colors: Record<string, string> = {
    delivered: 'bg-amber-100 text-amber-700',
    in_progress: 'bg-blue-100 text-blue-700',
    accepted: 'bg-green-100 text-green-700',
    completed: 'bg-teal-100 text-teal-700',
  };

  return project.value ? colors[project.value.status] || 'bg-gray-100 text-gray-700' : 'bg-gray-100 text-gray-700';
});

const canCompleteDelivery = computed(
  () => project.value?.status === 'in_progress' && !pendingAdjustment.value
);
const canCompleteAdjustment = computed(() => Boolean(pendingAdjustment.value));

const remainingDays = computed(() => {
  if (!project.value) return null;

  const startDate = new Date(project.value.freelancerResponse?.sentAt || project.value.receivedAt);
  if (Number.isNaN(startDate.getTime())) return null;

  startDate.setHours(0, 0, 0, 0);
  const parsedDeadline = parseInt(project.value.deadline || '', 10);
  const executionDays =
    project.value.freelancerResponse?.executionDays ||
    (!Number.isNaN(parsedDeadline) ? parsedDeadline : 30);
  const deadlineDate = new Date(startDate);
  deadlineDate.setDate(deadlineDate.getDate() + executionDays);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return Math.round((deadlineDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
});

const formatCurrency = (value?: number | null) => {
  if (value == null) return 'A combinar';
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value);
};

const formatDate = (dateString?: string | null) => {
  if (!dateString) return 'Nao informado';
  return formatApiDate(dateString, {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
};

const getInitials = (name: string) =>
  name
    .split(' ')
    .map((part) => part[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);

const getMilestoneStatusConfig = (status: Milestone['status']) => {
  switch (status) {
    case 'completed':
      return {
        label: 'Concluido',
        class: 'bg-green-100 text-green-700 hover:bg-green-100',
      };
    case 'in_progress':
      return {
        label: 'Em andamento',
        class: 'bg-blue-100 text-blue-700 hover:bg-blue-100',
      };
    default:
      return {
        label: 'Pendente',
        class: 'bg-gray-100 text-gray-600 hover:bg-gray-100',
      };
  }
};

const goBack = () => {
  props.onNavigate('freelancer-deliveries');
};

const openMessagesTab = () => {
  activeTab.value = 'messages';
};

const handleSendMessage = async (message: string, attachments: File[]) => {
  if (!project.value) return;

  isSendingMessage.value = true;
  const success = await proposalStore.sendMessage(project.value.id, message, attachments);
  isSendingMessage.value = false;

  if (success) {
    toast.success('Mensagem enviada');
    return;
  }

  toast.error('Erro ao enviar mensagem');
};

const handleMilestoneComplete = async (milestone: Milestone) => {
  if (!project.value) return;

  const newStatus = milestone.status === 'completed' ? 'pending' : 'completed';
  const success = await proposalStore.updateMilestoneStatus(project.value.id, milestone.id, newStatus);

  if (success) {
    project.value = proposalStore.getProposalById(project.value.id) || project.value;
    const label = newStatus === 'completed' ? 'marcado como concluido' : 'desmarcado';
    toast.success(`Marco "${milestone.title}" ${label}.`);
    return;
  }

  toast.error('Erro ao atualizar marco.');
};

const handleCompleteDelivery = async () => {
  if (!project.value) return;

  isCompleting.value = true;
  const currentId = project.value.id;

  try {
    const success = await proposalStore.completeProposal(currentId);

    if (success) {
      toast.success('Entrega concluida com sucesso! O cliente sera notificado.');
      showCompleteDialog.value = false;
      await loadData();
      return;
    }

    toast.error('Erro ao concluir a entrega. Tente novamente.');
  } catch {
    toast.error('Erro ao concluir a entrega. Tente novamente.');
  } finally {
    isCompleting.value = false;
  }
};

const handleCompleteAdjustment = async () => {
  if (!project.value || !pendingAdjustment.value) return;

  isCompletingAdjustment.value = true;
  const currentId = project.value.id;

  try {
    const successAdjustment = await adjustmentStore.updateStatus(
      pendingAdjustment.value.id,
      'COMPLETED',
      'Ajustes realizados e nova entrega enviada.'
    );

    if (successAdjustment) {
      await proposalStore.completeProposal(currentId);
      toast.success('Ajustes finalizados e enviados ao cliente para nova avaliacao.');
      showCompleteAdjustmentDialog.value = false;
      await loadData();
      return;
    }

    toast.error('Erro ao finalizar ajuste. Tente novamente.');
  } catch {
    toast.error('Erro ao finalizar ajuste. Tente novamente.');
  } finally {
    isCompletingAdjustment.value = false;
  }
};

const handleUploadFileRejected = (message: string) => {
  toast.error(message);
};

const openUploadDialog = () => {
  uploadFiles.value = [];
  uploadDescription.value = '';
  uploadProgress.value = 0;
  isUploading.value = false;
  showUploadDialog.value = true;
};

const handleUploadFiles = async () => {
  if (!project.value) {
    toast.error('Projeto nao encontrado');
    return;
  }

  if (uploadFiles.value.length === 0) {
    toast.error('Selecione pelo menos um arquivo');
    return;
  }

  isUploading.value = true;
  uploadProgress.value = 0;
  let progressInterval: ReturnType<typeof setInterval> | null = null;

  try {
    const formData = new FormData();
    uploadFiles.value.forEach((file) => formData.append('files', file));

    progressInterval = setInterval(() => {
      if (uploadProgress.value < 90) uploadProgress.value += 10;
    }, 300);

    const response = await api.post('/upload/images', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });

    if (progressInterval) {
      clearInterval(progressInterval);
      progressInterval = null;
    }

    const urls = response.data.urls || [];
    if (urls.length > 0) {
      await api.post(`/service-requests/${project.value.id}/freelancer-attachments`, {
        attachments: urls.map((url: string, index: number) => ({
          url,
          name: uploadFiles.value[index]?.name || 'Arquivo',
          description: uploadDescription.value.trim() || null,
        })),
      });
    }

    uploadProgress.value = 100;
    toast.success('Arquivos enviados com sucesso!');
    showUploadDialog.value = false;
    activeTab.value = 'files';
    activeFileTab.value = 'freelancer';
    await loadData();
  } catch {
    toast.error('Erro ao enviar arquivos. Tente novamente.');
  } finally {
    if (progressInterval) {
      clearInterval(progressInterval);
    }
    isUploading.value = false;
  }
};
</script>

<template>
  <div class="max-w-7xl mx-auto space-y-6 p-6">
    <div class="flex items-center gap-4 mb-4">
      <Button @click="goBack" variant="ghost" size="icon">
        <ArrowLeft class="h-5 w-5" />
      </Button>

      <div class="flex-1">
        <div class="flex items-center gap-3 flex-wrap">
          <h1 class="text-2xl font-bold text-[#263238]">
            {{ project?.service || 'Carregando...' }}
          </h1>
          <Badge v-if="project" :class="statusColor">
            {{ pendingAdjustment ? 'Ajustes solicitados' : getStatusLabel(project.status) }}
          </Badge>
        </div>
        <p class="text-[#435574]">Acompanhamento detalhado do projeto</p>
      </div>

      <div v-if="project" class="flex items-center gap-2 flex-wrap justify-end">
        <Button v-if="canCompleteAdjustment" @click="showCompleteAdjustmentDialog = true" variant="warning">
          <CheckCircle class="h-4 w-4 mr-2" />
          Finalizar Ajustes
        </Button>

        <Button v-else-if="canCompleteDelivery" @click="showCompleteDialog = true" variant="default">
          <Check class="h-4 w-4 mr-2" />
          Concluir Entrega
        </Button>
      </div>
    </div>

    <div v-if="proposalStore.isLoading && !project" class="flex items-center justify-center py-16">
      <Loader2 class="h-8 w-8 text-[#286AD8] animate-spin mr-3" />
      <span class="text-[#435574]">Carregando projeto...</span>
    </div>

    <Card
      v-else-if="!project"
      class="p-12 flex flex-col items-center justify-center text-center min-h-[320px]"
    >
      <div class="inline-flex p-6 bg-gray-100 rounded-full mb-6">
        <FileText class="h-16 w-16 text-gray-400" />
      </div>
      <h2 class="text-xl font-semibold text-[#263238] mb-2">Projeto nao encontrado</h2>
      <p class="text-[#435574] mb-6">
        Nao foi possivel localizar esse projeto na sua lista de entregas.
      </p>
      <Button @click="goBack" variant="default">Voltar para projetos</Button>
    </Card>

    <template v-else>
      <Card class="p-6 bg-white border shadow-sm">
        <div class="flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div class="flex-1 space-y-2">
            <div class="flex items-center justify-between text-sm mb-1">
              <span class="font-medium text-[#263238] flex items-center gap-2">
                <Clock class="h-4 w-4 text-blue-600" />
                {{ currentStepLabel }}
              </span>
              <span class="font-bold text-blue-700">{{ projectProgress }}%</span>
            </div>
            <Progress :value="projectProgress" class="h-3 shadow-inner" />
            <p class="text-xs text-[#435574] mt-2">
              Status atual: <span class="font-semibold">{{ getStatusLabel(project.status) }}</span>
            </p>
          </div>

          <div class="flex items-center gap-8 px-6 border-l hidden md:flex">
            <div class="text-center">
              <p class="text-xs text-[#435574] uppercase tracking-wider mb-1">Início</p>
              <p class="font-semibold text-[#263238]">{{ formatDate(project.receivedAt) }}</p>
            </div>
            <div class="text-center">
              <p class="text-xs text-[#435574] uppercase tracking-wider mb-1">Prazo</p>
              <p class="font-semibold text-[#263238]">
                {{ project.freelancerResponse?.executionDays || parseInt(project.deadline) || 30 }} dias
              </p>
            </div>
            <div class="text-center">
              <p class="text-xs text-[#435574] uppercase tracking-wider mb-1">Valor</p>
              <p class="font-semibold text-[#263238]">{{ formatCurrency(projectValue) }}</p>
            </div>
          </div>
        </div>
      </Card>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div class="lg:col-span-2 space-y-6">
          <div v-if="pendingAdjustment" class="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
            <div class="flex items-start gap-3">
              <AlertCircle class="h-5 w-5 text-yellow-600 mt-0.5" />
              <div>
                <h4 class="text-sm font-semibold text-yellow-800 mb-1">
                  O cliente solicitou ajustes na entrega
                </h4>
                <p class="text-sm text-yellow-700 whitespace-pre-wrap">
                  {{ pendingAdjustment.reason }}
                </p>
              </div>
            </div>
          </div>

          <Tabs v-model="activeTab">
            <TabsList
              class="tabs-list grid w-full gap-1"
              :class="hasMilestones ? 'grid-cols-2 md:grid-cols-5' : 'grid-cols-2 md:grid-cols-4'"
            >
              <TabsTrigger value="details">Detalhes</TabsTrigger>
              <TabsTrigger value="messages">Mensagens</TabsTrigger>
              <TabsTrigger value="history">Histórico</TabsTrigger>
              <TabsTrigger value="files">Arquivos</TabsTrigger>
              <TabsTrigger v-if="hasMilestones" value="milestones">Marcos</TabsTrigger>
            </TabsList>

            <TabsContent value="details" class="mt-4 space-y-4">
              <Card class="p-6">
                <h3 class="font-bold text-[#263238] mb-4 flex items-center gap-2">
                  <FileText class="h-5 w-5 text-blue-600" />
                  Escopo e Descrição
                </h3>
                <div class="prose prose-blue max-w-none text-[#435574]">
                  <p class="whitespace-pre-line">{{ project.description }}</p>
                </div>

                <div v-if="project.freelancerResponse?.scope" class="mt-8 pt-6 border-t font-medium text-[#263238]">
                  <h4 class="mb-3">Sua proposta:</h4>
                  <p class="text-sm text-[#435574] italic bg-gray-50 p-4 rounded-lg border border-dashed">
                    "{{ project.freelancerResponse.scope }}"
                  </p>
                </div>
              </Card>

              <Card class="p-6">
                <h3 class="font-bold text-[#263238] mb-2">Informações de Localização</h3>
                <div class="flex items-start gap-3 text-[#435574]">
                  <MapPin class="h-5 w-5 text-blue-500 shrink-0" />
                  <p>{{ project.address || project.clientLocation || 'Local não informado' }}</p>
                </div>
              </Card>
            </TabsContent>

            <TabsContent value="messages" class="mt-4">
              <Card class="p-6 min-h-[500px] flex flex-col">
                <div class="mb-4 border-b pb-4">
                  <h3 class="font-bold text-[#263238]">Conversa com {{ project.clientName }}</h3>
                </div>
                <ChatBox
                  :messages="project.messages || []"
                  current-user-role="freelancer"
                  :is-locked="project.status === 'completed'"
                  :is-sending="isSendingMessage"
                  :service-status="project.status"
                  @send-message="handleSendMessage"
                />
              </Card>
            </TabsContent>

            <TabsContent value="history" class="mt-4">
              <ServiceRequestTimeline :request-id="project.id" title="Linha do Tempo do Projeto" />
            </TabsContent>

            <TabsContent value="files" class="mt-4">
              <Card class="p-6">
                <div class="flex items-center justify-between mb-4">
                  <h3 class="text-lg font-semibold text-[#263238]">Arquivos do Projeto</h3>
                  <Button @click="openUploadDialog" variant="outline" :disabled="isUploading">
                    <Loader2 v-if="isUploading" class="h-4 w-4 mr-2 animate-spin" />
                    <Upload v-else class="h-4 w-4 mr-2" />
                    {{ isUploading ? 'Enviando...' : 'Enviar Arquivos' }}
                  </Button>
                </div>

                <div class="flex border-b border-gray-200 mb-4">
                  <button
                    @click="activeFileTab = 'client'"
                    :class="[
                      'px-4 py-2 text-sm font-medium border-b-2 transition-colors -mb-px',
                      activeFileTab === 'client'
                        ? 'border-[#286AD8] text-[#286AD8]'
                        : 'border-transparent text-[#435574] hover:text-[#263238]',
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
                        : 'border-transparent text-[#435574] hover:text-[#263238]',
                    ]"
                  >
                    Freelancer
                    <Badge v-if="freelancerAttachments.length > 0" variant="secondary" class="ml-2">
                      {{ freelancerAttachments.length }}
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
                  <div v-if="freelancerAttachments.length === 0" class="text-center py-8">
                    <FileText class="h-12 w-12 text-gray-300 mx-auto mb-3" />
                    <p class="text-[#435574]">Nenhum arquivo enviado pelo freelancer</p>
                  </div>
                  <div v-else class="space-y-2">
                    <FileAttachment
                      v-for="attachment in freelancerAttachments"
                      :key="attachment.id"
                      :name="attachment.name"
                      :url="attachment.url"
                      :size="attachment.size"
                      :description="attachment.description"
                      variant="amber"
                      sender-info="Enviado por voce"
                    />
                  </div>
                </div>
              </Card>
            </TabsContent>

            <TabsContent v-if="hasMilestones" value="milestones" class="mt-4">
              <Card class="p-6">
                <div class="flex items-start justify-between gap-4 mb-4">
                  <div>
                    <h3 class="text-lg font-semibold text-[#263238]">Marcos do Projeto</h3>
                    <p class="text-sm text-[#435574]">
                      {{ completedMilestones }} de {{ projectMilestones.length }}
                      {{ projectMilestones.length === 1 ? 'marco concluido' : 'marcos concluidos' }}
                    </p>
                  </div>
                  <Badge class="bg-white text-[#263238] border border-slate-200 hover:bg-white">
                    {{ projectProgress }}%
                  </Badge>
                </div>

                <div class="space-y-3">
                  <label
                    v-for="milestone in projectMilestones"
                    :key="milestone.id"
                    class="flex items-start gap-3 rounded-lg border border-slate-200 bg-white px-3 py-3"
                  >
                    <Checkbox
                      class="mt-0.5"
                      :checked="milestone.status === 'completed'"
                      :disabled="project.status === 'completed'"
                      @update:checked="() => handleMilestoneComplete(milestone)"
                    />

                    <div class="min-w-0 flex-1">
                      <div class="flex items-center gap-2 flex-wrap">
                        <p
                          :class="[
                            'text-sm font-medium',
                            milestone.status === 'completed'
                              ? 'text-slate-500 line-through'
                              : 'text-[#263238]',
                          ]"
                        >
                          {{ milestone.title }}
                        </p>
                        <Badge :class="getMilestoneStatusConfig(milestone.status).class" class="text-xs">
                          {{ getMilestoneStatusConfig(milestone.status).label }}
                        </Badge>
                      </div>
                      <p v-if="milestone.description" class="text-sm text-[#435574] mt-1">
                        {{ milestone.description }}
                      </p>
                    </div>
                  </label>
                </div>
              </Card>
            </TabsContent>
          </Tabs>
        </div>

        <div class="space-y-6">
          <Card class="p-6 text-center">
            <h3 class="font-bold text-[#263238] mb-4 text-left">Cliente</h3>
            <Avatar class="h-24 w-24 mx-auto mb-4 border-4 border-blue-50">
              <AvatarImage :src="project.clientAvatar" />
              <AvatarFallback class="bg-amber-100 text-amber-700 text-2xl font-bold">
                {{ getInitials(project.clientName || 'Cliente') }}
              </AvatarFallback>
            </Avatar>
            <p class="font-bold text-[#263238] text-lg">{{ project.clientName || 'Cliente' }}</p>
            <p class="text-sm text-[#435574] mb-4">{{ project.category }}</p>

            <div class="pt-4 border-t text-left space-y-3">
              <div class="flex items-start gap-2 text-sm text-[#435574]">
                <MapPin class="h-4 w-4 text-blue-500 mt-0.5 shrink-0" />
                <span>{{ project.clientLocation || 'Local nao informado' }}</span>
              </div>
              <div class="flex items-center gap-2 text-sm text-[#435574]">
                <Calendar class="h-4 w-4 text-blue-500 shrink-0" />
                <span>Inicio: {{ formatDate(project.receivedAt) }}</span>
              </div>
            </div>

            <Button variant="outline" class="w-full mt-6 rounded-full border-primary text-primary" @click="openMessagesTab">
              <MessageSquare class="h-4 w-4 mr-2" />
              Abrir Chat
            </Button>
          </Card>

          <Card class="p-6">
            <h3 class="font-bold text-[#263238] mb-4">Resumo Financeiro</h3>
            <div class="space-y-4">
              <div class="flex justify-between items-center text-sm">
                <span class="text-[#435574]">Valor do serviço</span>
                <span class="font-semibold">{{ formatCurrency(projectValue) }}</span>
              </div>
              <div class="flex justify-between items-center text-sm">
                <span class="text-[#435574]">Taxa da plataforma (5%)</span>
                <span class="font-semibold text-amber-600">{{ formatCurrency(platformFee) }}</span>
              </div>
              <div class="pt-4 border-t flex justify-between items-center">
                <span class="font-bold text-[#263238]">Valor a receber</span>
                <span class="text-xl font-bold text-blue-700">{{ formatCurrency(netValue) }}</span>
              </div>
            </div>

            <div class="p-4 bg-gray-50 rounded-lg flex items-start gap-2">
              <DollarSign class="h-4 w-4 text-green-500 mt-1 shrink-0" />
              <p class="text-[10px] text-[#435574]">
                O valor será liberado para pagamento assim que o projeto for marcado como concluído e aprovado pelo cliente.
              </p>
            </div>
          </Card>

          <Card class="p-6">
            <h3 class="font-bold text-[#263238] mb-4">Prazo</h3>
            <div class="space-y-4">
              <div>
                <p class="text-xs text-[#435574] mb-0.5">Prazo de entrega</p>
                <p class="text-sm font-medium text-[#263238]">
                  {{ project.freelancerResponse?.executionDays || parseInt(project.deadline) || 30 }} dias
                </p>
              </div>
              <div>
                <p class="text-xs text-[#435574] mb-0.5">Tempo restante</p>
                <p
                  :class="[
                    'text-sm font-medium',
                    remainingDays != null && remainingDays < 0
                      ? 'text-red-600'
                      : remainingDays != null && remainingDays <= 7
                        ? 'text-amber-600'
                        : 'text-green-600',
                  ]"
                >
                  <template v-if="remainingDays == null">Nao informado</template>
                  <template v-else-if="remainingDays < 0">{{ Math.abs(remainingDays) }} dias atrasado</template>
                  <template v-else-if="remainingDays === 0">Vence hoje</template>
                  <template v-else>{{ remainingDays }} dias restantes</template>
                </p>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </template>

    <AlertDialog :open="showCompleteDialog" @update:open="(val) => (showCompleteDialog = val)">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Concluir Entrega do Projeto</AlertDialogTitle>
          <AlertDialogDescription>
            <template v-if="project">
              Voce esta prestes a marcar o projeto <strong>"{{ project.service }}"</strong> como concluido.
              <br /><br />
              O cliente sera notificado e podera revisar a entrega. Certifique-se de que todos os arquivos
              e marcos estao finalizados antes de confirmar.
              <br /><br />
              <span class="text-amber-600 font-medium">Esta acao nao pode ser desfeita.</span>
            </template>
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel @click="showCompleteDialog = false" :disabled="isCompleting">
            Cancelar
          </AlertDialogCancel>
          <AlertDialogAction @click="handleCompleteDelivery" :disabled="isCompleting">
            <Loader2 v-if="isCompleting" class="h-4 w-4 mr-2 animate-spin" />
            <Check v-else class="h-4 w-4 mr-2" />
            {{ isCompleting ? 'Concluindo...' : 'Confirmar Conclusao' }}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <AlertDialog
      :open="showCompleteAdjustmentDialog"
      @update:open="(val) => (showCompleteAdjustmentDialog = val)"
    >
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Finalizar e Reenviar Projeto</AlertDialogTitle>
          <AlertDialogDescription>
            <template v-if="project">
              Voce esta prestes a finalizar os ajustes do projeto
              <strong>"{{ project.service }}"</strong>. <br /><br />
              Certifique-se de que todas as correcoes solicitadas pelo cliente foram realizadas e que voce fez
              o upload dos novos arquivos, caso necessario. <br /><br />
              O projeto sera novamente enviado para avaliacao e aprovacao final do cliente.
            </template>
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel @click="showCompleteAdjustmentDialog = false" :disabled="isCompletingAdjustment">
            Cancelar
          </AlertDialogCancel>
          <AlertDialogAction
            @click="handleCompleteAdjustment"
            :disabled="isCompletingAdjustment"
            class="bg-warning text-warning-foreground hover:bg-warning/90"
          >
            <Loader2 v-if="isCompletingAdjustment" class="h-4 w-4 mr-2 animate-spin" />
            <CheckCircle v-else class="h-4 w-4 mr-2" />
            {{ isCompletingAdjustment ? 'Finalizando...' : 'Confirmar Envio' }}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <Dialog :open="showUploadDialog" @update:open="(val) => (showUploadDialog = val)">
      <DialogContent class="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Enviar Arquivos</DialogTitle>
          <DialogDescription>
            Faca upload de arquivos relacionados ao projeto
            <template v-if="project"> "{{ project.service }}"</template>
          </DialogDescription>
        </DialogHeader>

        <div class="space-y-4">
          <AttachmentUploadField
            v-model:files="uploadFiles"
            v-model:description="uploadDescription"
            description-enabled
            label="Arquivos *"
            empty-message="Clique para fazer upload ou arraste os arquivos"
            helper-text="PDF, JPG, PNG, ZIP - Maximo 10 MB por arquivo"
            @file-rejected="handleUploadFileRejected"
          />

          <div v-if="isUploading" class="space-y-2">
            <div class="flex items-center justify-between text-sm">
              <span class="text-[#435574]">Enviando arquivos...</span>
              <span class="text-[#263238]">{{ uploadProgress }}%</span>
            </div>
            <Progress :value="uploadProgress" class="h-2" />
          </div>
        </div>

        <DialogFooter>
          <Button @click="showUploadDialog = false" variant="outline" :disabled="isUploading">
            Cancelar
          </Button>
          <Button @click="handleUploadFiles" :disabled="isUploading || uploadFiles.length === 0" variant="default">
            <Loader2 v-if="isUploading" class="h-4 w-4 mr-2 animate-spin" />
            <Upload v-else class="h-4 w-4 mr-2" />
            {{ isUploading ? 'Enviando...' : 'Enviar Arquivos' }}
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
</style>
