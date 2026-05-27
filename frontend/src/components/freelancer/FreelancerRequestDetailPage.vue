<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import {
  ArrowLeft,
  Calendar,
  MapPin,
  Clock,
  FileText,
  Download,
  Send,
  Plus,
  Trash2,
  AlertCircle,
  CheckCircle,
  User,
  MessageSquare,
  Milestone as MilestoneIcon,
  Phone,
  Mail,
  Check,
  FileEdit,
  Lock,
  Eye,
  Paperclip,
  XCircle,
  X,
  Sparkles,
  History,
  CreditCard,
} from 'lucide-vue-next';
import AttachmentUploadField from '../shared/AttachmentUploadField.vue';
import FileAttachment from '../shared/FileAttachment.vue';
import Button from '../ui/button.vue';
import Input from '../ui/input.vue';
import Label from '../ui/label.vue';
import Card from '../ui/card.vue';
import Badge from '../ui/badge.vue';
import Textarea from '../ui/textarea.vue';
import Dialog from '../ui/dialog.vue';
import DialogContent from '../ui/dialog-content.vue';
import DialogDescription from '../ui/dialog-description.vue';
import DialogFooter from '../ui/dialog-footer.vue';
import DialogHeader from '../ui/dialog-header.vue';
import DialogTitle from '../ui/dialog-title.vue';
import AlertDialog from '../ui/alert-dialog.vue';
import AlertDialogAction from '../ui/alert-dialog-action.vue';
import AlertDialogCancel from '../ui/alert-dialog-cancel.vue';
import AlertDialogContent from '../ui/alert-dialog-content.vue';
import AlertDialogDescription from '../ui/alert-dialog-description.vue';
import AlertDialogFooter from '../ui/alert-dialog-footer.vue';
import AlertDialogHeader from '../ui/alert-dialog-header.vue';
import AlertDialogTitle from '../ui/alert-dialog-title.vue';
import Avatar from '../ui/avatar.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import AvatarImage from '../ui/avatar-image.vue';
import Separator from '../ui/separator.vue';
import WorkModelBadge from '../shared/WorkModelBadge.vue';
import { useProposalStore, type Proposal, type Milestone } from '@/stores/proposalStore';
import ServiceRequestTimeline from '../service/ServiceRequestTimeline.vue';
import ProjectChatPanel from '../shared/ProjectChatPanel.vue';
import { useRouter } from 'vue-router';
import { useServiceRequestWebSocket } from '@/composables/useServiceRequestWebSocket';
import { toast } from '@/utils/toast';
import { getStatusLabel } from '@/utils/statusTranslations';
import api from '@/api/config/axios';
import { validateValueRange, extractApiErrorMessage } from '@/utils/valueValidation';

const router = useRouter();

interface Props {
  onNavigate: (page: string, params?: any) => void;
  requestId?: string;
}

const props = defineProps<Props>();
const proposalStore = useProposalStore();

const activeSection = ref<'details' | 'messages' | 'history' | 'attachments'>('details');
const activeFileTab = ref<'client' | 'freelancer'>('client');
const showSendProposalDialog = ref(false);
const showConfirmDialog = ref(false);
const showRejectDialog = ref(false);
const showRejectCounterDialog = ref(false);
const showCompleteDialog = ref(false);
const rejectReason = ref('');
const rejectCounterReason = ref('');
const isLoading = ref(false);

const proposal = ref<Proposal | null>(null);

const MAX_EXECUTION_DAYS = 365;
const PLATFORM_FEE_PERCENTAGE = 0.05;

const proposalForm = ref({
  value: '',
  executionDays: '',
  scope: '',
  notes: '',
  attachments: [] as File[],
});

const milestones = ref<Array<Omit<Milestone, 'status'> & { status?: Milestone['status'] }>>([]);
const milestoneForm = ref({
  title: '',
  description: '',
});
const showAddMilestone = ref(false);


// WebSocket
let wsConnection: any = null;

const getExecutionTimeLabel = (days: number) => {
  if (!days || days <= 0) return '';

  const clamped = Math.min(days, MAX_EXECUTION_DAYS);
  let remainingDays = clamped;

  const months = Math.floor(remainingDays / 30);
  remainingDays -= months * 30;
  const weeks = Math.floor(remainingDays / 7);

  const parts: string[] = [];
  if (months > 0) {
    parts.push(`${months} mês${months > 1 ? 'es' : ''}`);
  }
  if (weeks > 0) {
    parts.push(`${weeks} semana${weeks > 1 ? 's' : ''}`);
  }

  if (parts.length === 0) {
    parts.push(`${clamped} dia${clamped > 1 ? 's' : ''}`);
  }

  return parts.join(' e ');
};

const executionTimeLabel = computed(() => {
  const days = parseInt(proposalForm.value.executionDays);
  if (isNaN(days) || days <= 0) return '';
  return getExecutionTimeLabel(days);
});

const normalizeDisplayValue = (value?: string | null) => {
  if (typeof value !== 'string') return '';

  const trimmedValue = value.trim();
  if (!trimmedValue) return '';

  const loweredValue = trimmedValue.toLowerCase();
  if (['null', 'undefined', 'invalid date'].includes(loweredValue)) {
    return '';
  }

  return trimmedValue;
};

const formatValidDate = (value?: string | null) => {
  const normalizedValue = normalizeDisplayValue(value);
  if (!normalizedValue) return null;

  const parsedDate = new Date(normalizedValue);
  if (Number.isNaN(parsedDate.getTime())) return null;

  return parsedDate.toLocaleDateString('pt-BR');
};

const requestedDateDisplay = computed(() => {
  const formattedDesiredDate = formatValidDate(proposal.value?.desiredDate);
  if (formattedDesiredDate) return formattedDesiredDate;

  const normalizedDeadline = normalizeDisplayValue(proposal.value?.deadline);
  if (!normalizedDeadline) return 'Não informada';

  return formatValidDate(normalizedDeadline) || normalizedDeadline;
});

onMounted(async () => {
  if (proposalStore.proposals.length === 0) {
    await proposalStore.loadProposals();
  }

  if (props.requestId) {
    proposal.value = proposalStore.getProposalById(props.requestId) || null;
    if (proposal.value) {
      proposalStore.markAsViewed(props.requestId);
      await proposalStore.loadHistory(props.requestId);
    }
  }

  // Conectar WebSocket
  const userEmail = getUserEmail();
  if (userEmail) {
    wsConnection = useServiceRequestWebSocket(userEmail);
    wsConnection.connect();
    window.addEventListener('service-request-notification', handleServiceRequestNotification);
  }
});

import { onUnmounted } from 'vue';
onUnmounted(() => {
  window.removeEventListener('service-request-notification', handleServiceRequestNotification);
  if (wsConnection) {
    wsConnection.disconnect();
  }
});

watch(() => props.requestId, async (newId) => {
  if (newId) {
    if (proposalStore.proposals.length === 0) {
      await proposalStore.loadProposals();
    }
    proposal.value = proposalStore.getProposalById(newId) || null;
    if (proposal.value) {
      proposalStore.markAsViewed(newId);
      await proposalStore.loadHistory(newId);
    }
  }
});

// Extrair email do usuário do JWT
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

// Lidar com notificações via WebSocket
const handleServiceRequestNotification = async (event: any) => {
  const notification = event.detail;

  // Só processa se for para esta solicitação
  if (proposal.value && String(notification.serviceRequestId) === String(proposal.value.id)) {
    if (notification.type === 'SERVICE_REQUEST_STATUS_CHANGED') {
      if (notification.status === 'COMPLETED') {
        toast.success('🎉 Serviço Concluído! O cliente confirmou a entrega.');
      } else {
        toast.info(`Status atualizado: ${getStatusLabel(notification.status)}`);
      }

      // Recarregar dados
      const currentId = proposal.value.id;
      await proposalStore.loadProposals();
      proposal.value = proposalStore.getProposalById(currentId) || null;
      if (proposal.value) {
        await proposalStore.loadHistory(proposal.value.id);
      }
    }

    if (notification.type === 'SERVICE_COMPLETED_REVIEW_PENDING') {
      toast.success('Serviço concluído pelo cliente!');
    }

    if (notification.type === 'NEW_REVIEW') {
      toast.info(notification.reviewerName + ' enviou uma avaliação!');
    }
  }
};

const clientMessagesCount = computed(() =>
  proposal.value?.messages?.filter((m) => m.sender === 'client').length || 0
);

const isClientCancelled = computed(
  () => proposal.value?.status === 'archived' && !!proposal.value?.cancelReason?.trim()
);

const cancellationReason = computed(() => proposal.value?.cancelReason?.trim() || '');

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
};

const formatCurrency = (value?: string | number | null) => {
  if (value == null) return 'A combinar';
  if (typeof value === 'number') {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(value);
  }
  const numbers = value.replace(/\D/g, '');
  const formatted = (parseFloat(numbers) / 100).toLocaleString('pt-BR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
  return `R$ ${formatted}`;
};

const handleCurrencyInput = (value: string) => {
  const numbers = value.replace(/\D/g, '');
  if (numbers) {
    return formatCurrency(numbers);
  }
  return '';
};

const parseCurrencyValue = (value: string): number => {
  const numbers = value.replace(/[^\d,]/g, '').replace(',', '.');
  return parseFloat(numbers) || 0;
};

const roundCurrency = (value: number) => Math.round(value * 100) / 100;

const proposalGrossValue = computed(() => parseCurrencyValue(proposalForm.value.value));
const proposalPlatformFee = computed(() => roundCurrency(proposalGrossValue.value * PLATFORM_FEE_PERCENTAGE));
const proposalNetValue = computed(() =>
  roundCurrency(Math.max(0, proposalGrossValue.value - proposalPlatformFee.value))
);
const shouldShowProposalNetValue = computed(() => proposalGrossValue.value > 0);

const handleExecutionDaysInput = (value: string) => {
  const clean = value.replace(/\D/g, '');
  const digitsOnly = clean.slice(0, 3);

  if (!digitsOnly) {
    proposalForm.value.executionDays = '';
    return;
  }

  let days = parseInt(digitsOnly, 10);

  if (isNaN(days) || days <= 0) {
    proposalForm.value.executionDays = '';
    return;
  }

  if (days > MAX_EXECUTION_DAYS) {
    days = MAX_EXECUTION_DAYS;
    if (proposalForm.value.executionDays !== String(MAX_EXECUTION_DAYS)) {
      toast.error(`O prazo máximo é de ${MAX_EXECUTION_DAYS} dias.`);
    }
  }

  proposalForm.value.executionDays = String(days);
};

const handleExecutionDaysKeydown = (event: KeyboardEvent) => {
  const allowedKeys = [
    'Backspace',
    'Tab',
    'ArrowLeft',
    'ArrowRight',
    'Delete',
    'Home',
    'End',
  ];

  if (allowedKeys.includes(event.key)) {
    return;
  }

  // Bloquear qualquer tecla que não seja dígito
  if (!/^[0-9]$/.test(event.key)) {
    event.preventDefault();
    return;
  }

  const current = proposalForm.value.executionDays || '';
  const digitsCount = current.replace(/\D/g, '').length;

  // Impedir digitação acima de 3 dígitos
  if (digitsCount >= 3) {
    event.preventDefault();
  }
};

const validateProposal = () => {
  const errors: string[] = [];

  if (!proposalForm.value.value) errors.push('Valor da proposta é obrigatório');
  if (!proposalForm.value.executionDays) errors.push('Prazo de execução é obrigatório');
  if (!proposalForm.value.scope) errors.push('Escopo do trabalho é obrigatório');

  const value = parseCurrencyValue(proposalForm.value.value);
  if (isNaN(value) || value <= 0) {
    errors.push('Valor da proposta deve ser um número válido maior que zero');
  } else {
    const rangeError = validateValueRange(value);
    if (rangeError) errors.push(rangeError);
  }

  const days = parseInt(proposalForm.value.executionDays);
  if (isNaN(days) || days <= 0) {
    errors.push('Prazo de execução deve ser um número válido maior que zero');
  } else if (days > MAX_EXECUTION_DAYS) {
    errors.push(`Prazo de execução não pode ser maior que ${MAX_EXECUTION_DAYS} dias`);
  }

  return errors;
};

const handleAddMilestone = () => {
  if (!milestoneForm.value.title.trim()) {
    toast.error('Informe o título do marco');
    return;
  }

  const newMilestone = {
    id: Date.now().toString(),
    title: milestoneForm.value.title.trim(),
    description: milestoneForm.value.description.trim(),
    status: 'pending' as const,
  };

  milestones.value = [...milestones.value, newMilestone];
  milestoneForm.value = { title: '', description: '' };
  showAddMilestone.value = false;
  toast.success('Marco adicionado com sucesso');
};

const handleRemoveMilestone = (id: string) => {
  milestones.value = milestones.value.filter((m) => m.id !== id);
  toast.success('Marco removido');
};

const handleProposalAttachmentReject = (message: string) => {
  toast.error(message);
};

const handlePrepareProposal = () => {
  const errors = validateProposal();

  if (errors.length > 0) {
    toast.error(errors[0]);
    return;
  }

  showSendProposalDialog.value = false;
  showConfirmDialog.value = true;
};

const handleConfirmSendProposal = async () => {
  if (!proposal.value) return;

  isLoading.value = true;

  try {
    let attachmentUrls: string[] = [];

    // 1. Upload de arquivos se existirem
    if (proposalForm.value.attachments.length > 0) {
      const formData = new FormData();
      proposalForm.value.attachments.forEach((file) => formData.append('files', file));

      const uploadResponse = await api.post('/upload/images', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      attachmentUrls = uploadResponse.data.urls || [];
    }

    const parsedDays = parseInt(proposalForm.value.executionDays);
    const safeExecutionDays = Math.min(
      MAX_EXECUTION_DAYS,
      Math.max(1, isNaN(parsedDays) ? 1 : parsedDays)
    );
    
    const responseData = {
      value: parseCurrencyValue(proposalForm.value.value),
      executionDays: safeExecutionDays,
      scope: proposalForm.value.scope,
      notes: proposalForm.value.notes || undefined,
      milestones: milestones.value.length > 0 
        ? milestones.value.map(m => ({
            ...m,
            status: 'pending' as const,
          }))
        : undefined,
      attachments: attachmentUrls.map((url, idx) => ({
        id: `att-new-${Date.now()}-${idx}`,
        name: proposalForm.value.attachments[idx]?.name || 'Arquivo',
        url,
        size: '',
        type: 'image/jpeg' // Simplificado para upload de imagens
      }))
    };

    await proposalStore.sendResponse(proposal.value.id, responseData as any);
    
    toast.success('Proposta enviada com sucesso!');
    showConfirmDialog.value = false;
    showSendProposalDialog.value = false;
    
    proposalForm.value = {
      value: '',
      executionDays: '',
      scope: '',
      notes: '',
      attachments: [],
    };
    milestones.value = [];
    
    proposal.value = proposalStore.getProposalById(proposal.value.id) || null;
  } catch (err: any) {
    console.error('Erro ao enviar proposta:', err);
    const backendMsg = extractApiErrorMessage(err);
    toast.error(backendMsg || 'Erro ao enviar proposta. Verifique os dados e tente novamente.');
  } finally {
    isLoading.value = false;
  }
};

const handleOpenRejectDialog = () => {
  rejectReason.value = '';
  showRejectDialog.value = true;
};

const handleRejectProposal = async () => {
  if (!proposal.value) return;

  if (!rejectReason.value.trim()) {
    toast.error('Por favor, informe o motivo da recusa');
    return;
  }

  isLoading.value = true;
  const success = await proposalStore.rejectProposal(proposal.value.id, rejectReason.value);
  isLoading.value = false;

  if (success) {
    toast.success('Solicitação recusada');
    showRejectDialog.value = false;
    proposal.value = proposalStore.getProposalById(proposal.value.id) || null;
  } else {
    toast.error('Erro ao recusar solicitação');
  }
};

const handleCompleteProposal = async () => {
  if (!proposal.value) return;

  isLoading.value = true;
  const success = await proposalStore.completeProposal(proposal.value.id);
  isLoading.value = false;

  if (success) {
    toast.success('Serviço marcado como entregue! Aguardando confirmação do cliente.');
    showCompleteDialog.value = false;
    proposal.value = proposalStore.getProposalById(proposal.value.id) || null;
  } else {
    toast.error('Erro ao marcar como entregue');
  }
};

const handleUpdateMilestoneStatus = async (milestoneId: string, status: 'pending' | 'in_progress' | 'completed') => {
  if (!proposal.value) return;

  const success = await proposalStore.updateMilestoneStatus(proposal.value.id, milestoneId, status);

  if (success) {
    toast.success('Marco atualizado');
    proposal.value = proposalStore.getProposalById(proposal.value.id) || null;
  } else {
    toast.error('Erro ao atualizar marco');
  }
};


const handleAcceptCounterProposal = async () => {
  if (!proposal.value) return;

  isLoading.value = true;
  const success = await proposalStore.acceptCounterProposal(proposal.value.id);
  isLoading.value = false;

  if (success) {
    toast.success('Contra proposta aceita com sucesso!');
    proposal.value = proposalStore.getProposalById(proposal.value.id) || null;
  } else {
    toast.error('Erro ao aceitar contra proposta');
  }
};

const handleRejectCounterProposal = async () => {
  if (!proposal.value) return;

  isLoading.value = true;
  const success = await proposalStore.rejectCounterProposal(proposal.value.id, rejectCounterReason.value);
  isLoading.value = false;

  if (success) {
    toast.success('Contra proposta rejeitada. Mantendo termos originais.');
    showRejectCounterDialog.value = false;
    rejectCounterReason.value = '';
    proposal.value = proposalStore.getProposalById(proposal.value.id) || null;
  } else {
    toast.error('Erro ao rejeitar contra proposta');
  }
};

const getStatusBadge = (currentProposal: Proposal) => {
  const badges: Record<string, { class: string; label: string; icon: any }> = {
    pending: { class: 'bg-blue-100 text-blue-700', label: 'Pendente', icon: Clock },
    negotiating: { class: 'bg-purple-100 text-purple-700', label: 'Negociando', icon: MessageSquare },
    accepted: { class: 'bg-green-100 text-green-700', label: 'Aceita', icon: CheckCircle },
    in_progress: { class: 'bg-purple-100 text-purple-700', label: 'Em Andamento', icon: Clock },
    delivered: { class: 'bg-blue-100 text-blue-700', label: 'Entregue', icon: Send },
    rejected: { class: 'bg-red-100 text-red-700', label: 'Recusada', icon: XCircle },
    completed: { class: 'bg-teal-100 text-teal-700', label: 'Concluída', icon: CheckCircle },
    archived: { class: 'bg-gray-100 text-gray-700', label: 'Arquivada', icon: FileText },
  };

  if (currentProposal.status === 'archived' && currentProposal.cancelReason?.trim()) {
    return {
      class: 'bg-gray-100 text-gray-700',
      label: 'Cancelada',
      icon: XCircle,
    };
  }

  return badges[currentProposal.status] || badges.pending;
};
</script>

<template>
  <div class="max-w-5xl mx-auto space-y-6">
    <!-- Back Button -->
    <Button @click="props.onNavigate('freelancer-proposals')" variant="ghost" class="mb-2">
      <ArrowLeft class="h-4 w-4 mr-2" />
      Voltar às Solicitações
    </Button>

    <!-- Loading -->
    <div v-if="proposalStore.isLoading" class="flex justify-center py-12">
      <div class="animate-spin h-8 w-8 border-4 border-[#286AD8] border-t-transparent rounded-full"></div>
    </div>

    <!-- Not Found State -->
    <Card v-else-if="!proposal" class="p-12 text-center">
      <FileText class="h-12 w-12 text-gray-400 mx-auto mb-4" />
      <h3 class="text-lg font-semibold text-[#263238] mb-2">Solicitação não encontrada</h3>
      <p class="text-[#435574]">A solicitação que você procura não existe ou foi removida.</p>
      <Button @click="props.onNavigate('freelancer-proposals')" variant="outline" class="mt-4">Voltar</Button>
    </Card>

    <template v-else>
      <!-- Header Card -->
      <Card class="p-6">
        <div class="flex items-start justify-between flex-wrap gap-4">
          <div class="flex-1">
            <div class="flex items-center gap-3 mb-2 flex-wrap">
              <h1 class="text-2xl font-bold text-[#263238]">{{ proposal.service }}</h1>
              <Badge :class="getStatusBadge(proposal).class">
                <component :is="getStatusBadge(proposal).icon" class="h-3 w-3 mr-1" />
                {{ getStatusBadge(proposal).label }}
              </Badge>
            </div>
            <div class="flex items-center gap-4 text-sm text-[#435574] flex-wrap">
              <Badge variant="secondary">{{ proposal.category }}</Badge>
              <span class="flex items-center gap-1">
                <Calendar class="h-4 w-4" />
                Recebido em {{ new Date(proposal.receivedAt).toLocaleDateString('pt-BR') }}
              </span>
              <span v-if="proposal.address" class="flex items-center gap-1">
                <MapPin class="h-4 w-4" />
                {{ proposal.address }}
              </span>
            </div>
          </div>
          <div class="flex items-center gap-2">
            <Button
              v-if="proposal.status === 'pending'"
              @click="handleOpenRejectDialog"
              variant="outline"
              class="text-red-600 border-red-200 hover:bg-red-50"
            >
              <XCircle class="h-4 w-4 mr-2" />
              Recusar
            </Button>
            <Button
              v-if="(proposal.status === 'pending' || proposal.status === 'negotiating') && !proposal.freelancerResponse"
              @click="showSendProposalDialog = true"
              variant="default"
            >
              <Send class="h-4 w-4 mr-2" />
              {{ proposal.freelancerResponse ? 'Atualizar Orçamento' : 'Enviar Orçamento' }}
            </Button>
          </div>
        </div>

        <!-- Delivered Status Alert -->
        <div v-if="proposal.status === 'delivered'"
             class="mt-4 p-4 rounded-lg border border-blue-100 bg-blue-50 text-blue-900 flex items-start gap-3">
          <Clock class="h-5 w-5 mt-0.5 flex-shrink-0 text-blue-600" />
          <div>
            <p class="font-semibold text-sm">Aguardando Confirmação</p>
            <p class="text-sm opacity-90">Você marcou este serviço como entregue. O cliente foi notificado e deve confirmar a conclusão para finalizar o processo.</p>
          </div>
        </div>

        <!-- Rejection Reason Alert -->
        <div v-if="proposal.status === 'rejected' && proposal.rejectReason"
             class="mt-4 p-4 rounded-lg border border-red-100 bg-red-50 text-red-900 flex items-start gap-3">
          <AlertCircle class="h-5 w-5 mt-0.5 flex-shrink-0 text-red-600" />
          <div>
            <p class="font-semibold text-sm">Motivo da Recusa</p>
            <p class="text-sm opacity-90 italic">"{{ proposal.rejectReason }}"</p>
          </div>
        </div>

        <div
          v-if="isClientCancelled"
          class="mt-4 p-4 rounded-lg border border-gray-200 bg-gray-50 text-gray-900 flex items-start gap-3"
        >
          <AlertCircle class="h-5 w-5 mt-0.5 flex-shrink-0 text-gray-600" />
          <div>
            <p class="font-semibold text-sm">Solicitação cancelada pelo cliente</p>
            <p v-if="cancellationReason" class="text-sm opacity-90 italic">"{{ cancellationReason }}"</p>
          </div>
        </div>
      </Card>

      <!-- Navigation Tabs -->
      <div class="flex gap-2 border-b border-gray-200 pb-0">
        <button
          @click="activeSection = 'details'"
          :class="`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
            activeSection === 'details'
              ? 'border-[#286AD8] text-[#286AD8]'
              : 'border-transparent text-[#435574] hover:text-[#263238]'
          }`"
        >
          Detalhes
        </button>
        <button
          @click="activeSection = 'messages'"
          :class="`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
            activeSection === 'messages'
              ? 'border-[#286AD8] text-[#286AD8]'
              : 'border-transparent text-[#435574] hover:text-[#263238]'
          }`"
        >
          Mensagens
          <Badge
            v-if="clientMessagesCount > 0"
            class="ml-1 bg-primary text-white text-xs"
          >
            {{ clientMessagesCount }}
          </Badge>
        </button>
        <button
          @click="activeSection = 'history'"
          :class="`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
            activeSection === 'history'
              ? 'border-[#286AD8] text-[#286AD8]'
              : 'border-transparent text-[#435574] hover:text-[#263238]'
          }`"
        >
          Histórico
        </button>
        <button
          @click="activeSection = 'attachments'"
          :class="`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
            activeSection === 'attachments'
              ? 'border-[#286AD8] text-[#286AD8]'
              : 'border-transparent text-[#435574] hover:text-[#263238]'
          }`"
        >
          Anexos
        </button>
      </div>

      <!-- Details Section -->
      <div v-if="activeSection === 'details'" class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Main Info -->
        <div class="lg:col-span-2 space-y-6">

          <!-- Description -->
          <Card class="p-6">
            <h3 class="font-semibold text-[#263238] mb-3">Descrição do Serviço</h3>
            <p class="text-[#435574] whitespace-pre-line break-words">{{ proposal.description }}</p>
          </Card>

          <!-- Counter Proposal from Client -->
          <Card v-if="proposal.counterValue" class="p-6 border-blue-200 bg-blue-50/30">
            <div class="flex items-center gap-2 mb-4">
              <FileEdit class="h-5 w-5 text-blue-600" />
              <h3 class="font-semibold text-[#263238]">Contra Proposta do Cliente</h3>
            </div>
            
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4">
              <div class="bg-white rounded-lg p-4 border">
                <p class="text-sm text-[#435574] mb-1">Novo Valor Proposto</p>
                <p class="text-xl font-bold text-green-600">
                  {{ formatCurrency(proposal.counterValue) }}
                </p>
              </div>
              <div class="bg-white rounded-lg p-4 border">
                <p class="text-sm text-[#435574] mb-1">Novo Prazo Proposto</p>
                <p class="text-xl font-bold text-[#263238]">
                  {{ proposal.counterDays }} dias
                </p>
              </div>
            </div>
            
            <div v-if="proposal.counterNotes" class="mb-4">
              <p class="text-sm font-medium text-[#263238] mb-1">Observações do Cliente:</p>
              <p class="text-sm text-[#435574] italic">"{{ proposal.counterNotes }}"</p>
            </div>
            
            <div class="mb-4 p-3 bg-blue-100/50 rounded text-sm text-blue-800">
              <p>O cliente sugeriu novos termos. Você pode aceitá-los para atualizar seu orçamento ou manter sua proposta inicial.</p>
            </div>

            <div class="flex gap-3 flex-wrap">
              <Button
                @click="handleAcceptCounterProposal"
                :disabled="isLoading"
                variant="success"
                class="flex-1 min-w-[150px]"
              >
                <Check class="h-4 w-4 mr-2" />
                Aceitar Contra Proposta
              </Button>
              <Button
                @click="showRejectCounterDialog = true"
                variant="outline"
                :disabled="isLoading"
                class="flex-1 min-w-[150px]"
              >
                <XCircle class="h-4 w-4 mr-2" />
                Manter Proposta Inicial
              </Button>
            </div>
          </Card>

          <!-- My Proposal (if sent) -->
          <Card v-if="proposal.freelancerResponse" class="p-6 border-blue-200 bg-blue-50/30">
            <div class="flex items-center gap-2 mb-4">
              <Send class="h-5 w-5 text-blue-600" />
              <h3 class="font-semibold text-[#263238]">Sua Proposta Enviada</h3>
            </div>
            
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4">
              <div class="bg-white rounded-lg p-4 border">
                <p class="text-sm text-[#435574] mb-1">Valor proposto</p>
                <p class="text-xl font-bold text-green-600">
                  {{ formatCurrency(proposal.freelancerResponse.value) }}
                </p>
              </div>
              <div class="bg-white rounded-lg p-4 border">
                <p class="text-sm text-[#435574] mb-1">Prazo de execução</p>
                <p class="text-xl font-bold text-[#263238]">
                  {{ proposal.freelancerResponse.executionDays }} dias
                </p>
              </div>
            </div>

            <div class="mb-4">
              <p class="text-sm font-medium text-[#263238] mb-2">Escopo do trabalho</p>
              <p class="text-sm text-[#435574] whitespace-pre-line break-words">
                {{ proposal.freelancerResponse.scope }}
              </p>
            </div>

            <div v-if="proposal.freelancerResponse.notes" class="mb-4">
              <p class="text-sm font-medium text-[#263238] mb-2">Observações</p>
              <p class="text-sm text-[#435574]">{{ proposal.freelancerResponse.notes }}</p>
            </div>

            <p class="text-xs text-[#435574] mb-4">
              Enviada em {{ new Date(proposal.freelancerResponse.sentAt).toLocaleDateString('pt-BR', {
                day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'
              }) }}
            </p>

            <!-- Milestones -->
            <div v-if="proposal.freelancerResponse.milestones && proposal.freelancerResponse.milestones.length > 0" class="pt-4 border-t">
              <h4 class="font-semibold text-[#263238] mb-3">Marcos de Entrega</h4>
              <div class="space-y-3">
                <Card
                  v-for="milestone in proposal.freelancerResponse.milestones"
                  :key="milestone.id"
                  class="p-4 bg-white"
                >
                  <div class="flex items-start justify-between">
                    <div class="flex-1">
                      <div class="flex items-center gap-2 mb-1">
                        <MilestoneIcon class="h-4 w-4 text-[#286AD8]" />
                        <h5 class="font-medium text-[#263238]">{{ milestone.title }}</h5>
                        <Badge
                          :class="
                            milestone.status === 'completed' ? 'bg-green-100 text-green-700' :
                            milestone.status === 'in_progress' ? 'bg-blue-100 text-blue-700' :
                            'bg-gray-100 text-gray-700'
                          "
                        >
                          {{ milestone.status === 'completed' ? 'Concluído' :
                             milestone.status === 'in_progress' ? 'Em andamento' : 'Pendente' }}
                        </Badge>
                      </div>
                      <p v-if="milestone.description" class="text-sm text-[#435574] mb-2">
                        {{ milestone.description }}
                      </p>
                    </div>
                  </div>
                </Card>
              </div>
            </div>
          </Card>
        </div>

        <!-- Sidebar -->
        <div class="space-y-6">
          <Card class="p-6">
            <h3 class="font-semibold text-[#263238] mb-4">Resumo</h3>
            <div class="space-y-4">
              <div>
                <p class="text-sm text-[#435574]">
                  {{ ['accepted', 'in_progress', 'completed'].includes(proposal.status) && proposal.freelancerResponse ? 'Valor Acordado' : 'Orçamento Estimado' }}
                </p>
                <p class="text-lg font-bold text-green-600">
                  {{ formatCurrency(['accepted', 'in_progress', 'completed'].includes(proposal.status) && proposal.freelancerResponse ? proposal.freelancerResponse.value : proposal.budget) }}
                </p>
              </div>
              <Separator />
              <div>
                <p class="text-sm text-[#435574]">
                  {{ ['accepted', 'in_progress', 'completed'].includes(proposal.status) && proposal.freelancerResponse ? 'Prazo Acordado' : 'Prazo Desejado' }}
                </p>
                <p class="font-medium text-[#263238]">
                  {{ ['accepted', 'in_progress', 'completed'].includes(proposal.status) && proposal.freelancerResponse ? proposal.freelancerResponse.executionDays + ' dias' : requestedDateDisplay }}
                </p>
              </div>
              <Separator />
              <div>
                <p class="text-sm text-[#435574]">Categoria</p>
                <p class="font-medium text-[#263238]">{{ proposal.category }}</p>
              </div>
              <Separator />
              <div>
                <p class="text-sm text-[#435574]">Modelo de trabalho</p>
                <WorkModelBadge :work-model="proposal.workModel" />
              </div>
            </div>
          </Card>
                    <!-- Client Info Card -->
                    <Card class="p-6">
            <h3 class="font-semibold text-[#263238] mb-4">Cliente</h3>
            <div class="flex items-center gap-4">
              <Avatar class="h-16 w-16">
                <AvatarImage :src="proposal.clientAvatar" :alt="proposal.clientName" />
                <AvatarFallback class="bg-[#286AD8] text-white text-lg">
                  {{ getInitials(proposal.clientName) }}
                </AvatarFallback>
              </Avatar>
              <div class="flex-1">
                <h3 class="text-lg font-semibold text-[#263238] mb-1">{{ proposal.clientName }}</h3>
                <div class="flex items-center gap-4 text-sm text-[#435574] flex-wrap">
                  <span class="flex items-center gap-1">
                    <MapPin class="h-4 w-4" />
                    {{ proposal.clientLocation }}
                  </span>
                  <span v-if="proposal.clientEmail" class="flex items-center gap-1">
                    <Mail class="h-4 w-4" />
                    {{ proposal.clientEmail }}
                  </span>
                  <span v-if="proposal.clientPhone" class="flex items-center gap-1">
                    <Phone class="h-4 w-4" />
                    {{ proposal.clientPhone }}
                  </span>
                </div>
              </div>
            </div>
          </Card>
        </div>
      </div>

      <!-- Messages Section -->
      <Card v-if="activeSection === 'messages'" class="p-6">
        <h3 class="font-semibold text-[#263238] mb-4">Mensagens</h3>

        <ProjectChatPanel
          :service-request-id="Number(proposal.id)"
          current-user-role="freelancer"
          :service-status="proposal.status"
        />
      </Card>

      <!-- History Section -->
      <ServiceRequestTimeline
        v-if="activeSection === 'history'"
        :request-id="proposal.id"
        title="Histórico de Atividades"
      />

      <!-- Attachments Section -->
      <Card v-if="activeSection === 'attachments'" class="p-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-semibold text-[#263238]">Arquivos do Projeto</h3>
        </div>

        <!-- Tabs -->
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
            <Badge v-if="proposal.attachments && proposal.attachments.length > 0" variant="secondary" class="ml-2">
              {{ proposal.attachments.length }}
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
            <Badge v-if="proposal.freelancerResponse?.attachments && proposal.freelancerResponse.attachments.length > 0" variant="secondary" class="ml-2">
              {{ proposal.freelancerResponse.attachments.length }}
            </Badge>
          </button>
        </div>

        <!-- Client Files Tab -->
        <div v-if="activeFileTab === 'client'">
          <div v-if="!proposal.attachments || proposal.attachments.length === 0" class="text-center py-8">
            <FileText class="h-12 w-12 text-gray-300 mx-auto mb-3" />
            <p class="text-[#435574]">Nenhum arquivo enviado pelo cliente</p>
          </div>
          <div v-else class="space-y-2">
            <FileAttachment
              v-for="attachment in proposal.attachments"
              :key="attachment.id"
              :name="attachment.name"
              :url="attachment.url"
              :size="attachment.size"
              sender-info="Enviado pelo cliente"
              variant="blue"
            />
          </div>
        </div>

        <!-- Freelancer Files Tab -->
        <div v-if="activeFileTab === 'freelancer'">
          <div v-if="!proposal.freelancerResponse?.attachments || proposal.freelancerResponse.attachments.length === 0" class="text-center py-8">
            <FileText class="h-12 w-12 text-gray-300 mx-auto mb-3" />
            <p class="text-[#435574]">Nenhum arquivo enviado por você</p>
          </div>
          <div v-else class="space-y-2">
            <FileAttachment
              v-for="attachment in proposal.freelancerResponse.attachments"
              :key="attachment.id"
              :name="attachment.name"
              :url="attachment.url"
              :size="attachment.size"
              sender-info="Enviado por você"
              variant="blue"
            />
          </div>
        </div>
      </Card>
    </template>

    <!-- Send Proposal Dialog -->
    <Dialog :open="showSendProposalDialog" @update:open="(val) => showSendProposalDialog = val">
      <DialogContent class="sm:max-w-3xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{{ proposal?.freelancerResponse ? 'Atualizar Orçamento' : 'Enviar Orçamento' }}</DialogTitle>
          <DialogDescription>
            Preencha os detalhes do seu orçamento para este serviço
          </DialogDescription>
        </DialogHeader>

        <div class="space-y-6">
          <!-- Value and Deadline -->
          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-2">
              <Label for="value">Valor Total da Proposta *</Label>
              <Input
                id="value"
                :model-value="proposalForm.value"
                @update:model-value="(val) => proposalForm.value = handleCurrencyInput(val as string)"
                placeholder="R$ 0,00"
              />
              <div
                v-if="shouldShowProposalNetValue"
                class="rounded-lg border border-blue-200 bg-blue-50 px-3 py-2 text-sm"
              >
                <p class="text-gray-600 mb-1">
                  Valor a receber: {{ formatCurrency(proposalNetValue) }}
                </p>
                <p class="text-gray-600">
                  Já considerando 5% da plataforma ({{ formatCurrency(proposalPlatformFee) }}).
                </p>
              </div>
            </div>

            <div class="space-y-2">
              <Label for="executionDays">Prazo de Execução (dias) *</Label>
              <div class="relative">
                <Input
                  id="executionDays"
                  type="text"
                  maxlength="3"
                  inputmode="numeric"
                  :model-value="proposalForm.executionDays"
                  @update:model-value="(val) => handleExecutionDaysInput(val as string)"
                  @keydown="handleExecutionDaysKeydown"
                  placeholder="Quantidade de dias"
                  class="pr-24"
                />
                <div class="absolute right-3 top-1/2 -translate-y-1/2 flex items-center gap-2 pointer-events-none">
                  <span class="text-xs text-gray-400 font-medium">dia(s)</span>
                  <Badge v-if="executionTimeLabel" variant="secondary" class="bg-blue-50 text-[#286AD8] border-[#286AD8]/20 px-2 h-6 text-[10px]">
                    {{ executionTimeLabel }}
                  </Badge>
                </div>
              </div>
            </div>
          </div>

          <!-- Scope -->
          <div class="space-y-2">
            <Label for="scope">Escopo do Trabalho *</Label>
            <Textarea
              id="scope"
              :model-value="proposalForm.scope"
              @update:model-value="(val) => proposalForm.scope = val as string"
              placeholder="Descreva detalhadamente o que será realizado..."
              :rows="6"
            />
          </div>

          <!-- Milestones -->
          <div class="space-y-3">
            <div class="flex items-center justify-between">
              <Label>Marcos de Entrega (opcional)</Label>
              <Button
                @click="showAddMilestone = !showAddMilestone"
                variant="outline"
                size="sm"
              >
                <Plus class="h-4 w-4 mr-2" />
                Adicionar Marco
              </Button>
            </div>

            <Card v-if="showAddMilestone" class="p-4 bg-gray-50">
              <div class="space-y-3">
                <Input
                  :model-value="milestoneForm.title"
                  @update:model-value="(val) => milestoneForm.title = val as string"
                  placeholder="Título do marco"
                />
                <Textarea
                  :model-value="milestoneForm.description"
                  @update:model-value="(val) => milestoneForm.description = val as string"
                  placeholder="Descrição do marco"
                  :rows="2"
                />
                <div class="flex gap-2">
                  <Button @click="handleAddMilestone" size="sm" class="flex-1">
                    Adicionar
                  </Button>
                  <Button
                    @click="showAddMilestone = false"
                    variant="outline"
                    size="sm"
                  >
                    Cancelar
                  </Button>
                </div>
              </div>
            </Card>

            <div v-if="milestones.length > 0" class="space-y-2">
              <Card
                v-for="(milestone, index) in milestones"
                :key="milestone.id"
                class="p-4"
              >
                <div class="flex items-start justify-between">
                  <div class="flex-1">
                    <div class="flex items-center gap-2 mb-1">
                      <MilestoneIcon class="h-4 w-4 text-[#286AD8]" />
                      <h4 class="font-medium text-[#263238]">
                        {{ index + 1 }}. {{ milestone.title }}
                      </h4>
                    </div>
                    <p v-if="milestone.description" class="text-sm text-[#435574] mb-2">
                      {{ milestone.description }}
                    </p>
                  </div>
                  <Button
                    @click="handleRemoveMilestone(milestone.id)"
                    variant="ghost"
                    size="icon"
                    class="text-red-600 hover:text-red-700 hover:bg-red-50"
                  >
                    <Trash2 class="h-4 w-4" />
                  </Button>
                </div>
              </Card>
            </div>
          </div>

          <!-- Notes -->
          <div class="space-y-2">
            <Label for="notes">Observações (opcional)</Label>
            <Textarea
              id="notes"
              :model-value="proposalForm.notes"
              @update:model-value="(val) => proposalForm.notes = val as string"
              placeholder="Informações adicionais, condições especiais, etc."
              :rows="3"
            />
          </div>

          <!-- Attachments -->
          <div class="space-y-2">
            <AttachmentUploadField
              v-model:files="proposalForm.attachments"
              label="Anexos (opcional)"
              empty-message="Adicione documentos, certificados ou imagens"
              helper-text="PDF, JPG, PNG, DOC, DOCX até 10MB cada"
              accept=".pdf,.jpg,.jpeg,.png,.doc,.docx"
              :allowed-mime-types="[
                'application/pdf',
                'image/jpeg',
                'image/png',
                'application/msword',
                'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
              ]"
              :allowed-extensions="['pdf', 'jpg', 'jpeg', 'png', 'doc', 'docx']"
              @file-rejected="handleProposalAttachmentReject"
            />
          </div>
        </div>

        <DialogFooter>
          <Button @click="showSendProposalDialog = false" variant="outline">
            Cancelar
          </Button>
          <Button @click="handlePrepareProposal" variant="default">
            <Send class="h-4 w-4 mr-2" />
            Enviar Proposta
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- Confirmation Dialog -->
    <AlertDialog :open="showConfirmDialog" @update:open="(val) => showConfirmDialog = val">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Confirmar Envio da Proposta</AlertDialogTitle>
          <AlertDialogDescription>
            Você está prestes a enviar uma proposta de
            <strong>{{ proposalForm.value }}</strong> com prazo de
            <strong>{{ proposalForm.executionDays }} dias</strong>.
            <template v-if="shouldShowProposalNetValue">
              <br />
              <br />
              Valor a receber após a taxa da plataforma (5%):
              <strong>{{ formatCurrency(proposalNetValue) }}</strong>.
            </template>
            <template v-if="milestones.length > 0">
              <br />
              <br />
              A proposta inclui <strong>{{ milestones.length }}</strong>
              {{ milestones.length === 1 ? 'marco' : 'marcos' }} de entrega.
            </template>
            <br />
            <br />
            Deseja confirmar o envio?
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel @click="showConfirmDialog = false">Revisar</AlertDialogCancel>
          <AlertDialogAction
            @click="handleConfirmSendProposal"
            :disabled="isLoading"
          >
            {{ isLoading ? 'Enviando...' : 'Confirmar Envio' }}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <!-- Reject Dialog -->
    <Dialog :open="showRejectDialog" @update:open="(val) => showRejectDialog = val">
      <DialogContent class="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Recusar Solicitação</DialogTitle>
          <DialogDescription>
            Informe o motivo da recusa para "{{ proposal?.service }}"
          </DialogDescription>
        </DialogHeader>
        <div class="space-y-4">
          <div class="space-y-2">
            <Label for="reject-reason">Motivo da recusa *</Label>
            <Textarea
              id="reject-reason"
              :model-value="rejectReason"
              @update:model-value="(val) => rejectReason = val as string"
              placeholder="Ex: Não trabalho com esse tipo de serviço, agenda indisponível, etc."
              :rows="4"
            />
          </div>
        </div>
        <DialogFooter>
          <Button @click="showRejectDialog = false" variant="outline">
            Cancelar
          </Button>
          <Button @click="handleRejectProposal" variant="destructive" :disabled="isLoading">
            {{ isLoading ? 'Recusando...' : 'Confirmar Recusa' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- Reject Counter Proposal Dialog -->
    <Dialog :open="showRejectCounterDialog" @update:open="(val) => showRejectCounterDialog = val">
      <DialogContent class="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Manter Proposta Inicial</DialogTitle>
          <DialogDescription>
            Informe o motivo ao cliente (opcional)
          </DialogDescription>
        </DialogHeader>
        <div class="space-y-4">
          <div class="space-y-2">
            <Label for="reject-counter-reason">Motivo</Label>
            <Textarea
              id="reject-counter-reason"
              :model-value="rejectCounterReason"
              @update:model-value="(val) => rejectCounterReason = val as string"
              placeholder="Ex: Custos de materiais, agenda, etc."
              :rows="4"
            />
          </div>
        </div>
        <DialogFooter>
          <Button @click="showRejectCounterDialog = false" variant="outline">
            Cancelar
          </Button>
          <Button @click="handleRejectCounterProposal" variant="default" :disabled="isLoading">
            {{ isLoading ? 'Enviando...' : 'Confirmar e Manter Inicial' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- Complete Dialog -->
    <AlertDialog :open="showCompleteDialog" @update:open="(val) => showCompleteDialog = val">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Confirmar Entrega</AlertDialogTitle>
          <AlertDialogDescription>
            Tem certeza que deseja marcar o serviço "{{ proposal?.service }}" como entregue?
            <br /><br />
            Esta ação indica que o trabalho foi finalizado pelo seu lado. O cliente será notificado para confirmar a conclusão e liberar o processo final.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel @click="showCompleteDialog = false">Cancelar</AlertDialogCancel>
          <AlertDialogAction
            @click="handleCompleteProposal"
            :disabled="isLoading"
          >
            {{ isLoading ? 'Enviando...' : 'Confirmar Entrega' }}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>
