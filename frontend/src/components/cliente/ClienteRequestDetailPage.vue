<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue';
import {
  ArrowLeft,
  Clock,
  DollarSign,
  MapPin,
  Calendar,
  MessageSquare,
  CheckCircle,
  XCircle,
  User,
  History,
  FileEdit,
  Lock,
  CreditCard,
  AlertCircle,
  FileText,
  Eye,
  Download,
  Sparkles,
} from 'lucide-vue-next';
import FileAttachment from '../shared/FileAttachment.vue';
import Button from '../ui/button.vue';
import Input from '../ui/input.vue';
import Card from '../ui/card.vue';
import Badge from '../ui/badge.vue';
import Avatar from '../ui/avatar.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import AvatarImage from '../ui/avatar-image.vue';
import Separator from '../ui/separator.vue';
import Textarea from '../ui/textarea.vue';
import Label from '../ui/label.vue';
import Dialog from '../ui/dialog.vue';
import DialogContent from '../ui/dialog-content.vue';
import DialogDescription from '../ui/dialog-description.vue';
import DialogFooter from '../ui/dialog-footer.vue';
import DialogHeader from '../ui/dialog-header.vue';
import DialogTitle from '../ui/dialog-title.vue';
import WorkModelBadge from '../shared/WorkModelBadge.vue';
import AlertDialog from '../ui/alert-dialog.vue';
import AlertDialogAction from '../ui/alert-dialog-action.vue';
import AlertDialogCancel from '../ui/alert-dialog-cancel.vue';
import AlertDialogContent from '../ui/alert-dialog-content.vue';
import AlertDialogDescription from '../ui/alert-dialog-description.vue';
import AlertDialogFooter from '../ui/alert-dialog-footer.vue';
import AlertDialogHeader from '../ui/alert-dialog-header.vue';
import AlertDialogTitle from '../ui/alert-dialog-title.vue';
import { useClientRequestStore, type ServiceRequest } from '@/stores/clientRequestStore';
import ServiceRequestTimeline from '../service/ServiceRequestTimeline.vue';
import ChatBox from '../shared/ChatBox.vue';
import { formatApiDate } from '@/utils/apiDate';
import { toast } from '@/utils/toast';
import { getStatusLabel } from '@/utils/statusTranslations';
import { validateValueRange, extractApiErrorMessage } from '@/utils/valueValidation';

interface Props {
  onNavigate: (page: string, params?: any) => void;
  requestId: string;
}

const props = defineProps<Props>();
const store = useClientRequestStore();

const request = ref<ServiceRequest | null>(null);
const isSendingMessage = ref(false);
const showCancelDialog = ref(false);
const cancelReason = ref('');
const showRejectDialog = ref(false);
const rejectReason = ref('');
const showCounterProposalDialog = ref(false);
const counterValue = ref(0);
const counterDays = ref(0);
const counterNotes = ref('');
const isSubmittingCounter = ref(false);
const activeSection = ref<'details' | 'messages' | 'history' | 'attachments'>('details');
const showCelebrationModal = ref(false);
const activeFileTab = ref<'client' | 'freelancer'>('client');
const clientAttachments = computed(() => request.value?.attachments || []);
const professionalAttachments = computed(() => [
  ...(request.value?.freelancerResponse?.attachments || []),
  ...(request.value?.freelancerAttachments || []),
]);

const handleOpenCounterProposalDialog = () => {
  if (!request.value || !request.value.freelancerResponse) return;
  counterValue.value = request.value.freelancerResponse.value;
  counterDays.value = request.value.freelancerResponse.executionDays;
  counterNotes.value = '';
  showCounterProposalDialog.value = true;
};

const loadData = async () => {
  const loadedRequest = await store.loadRequestById(props.requestId);
  if (loadedRequest) {
    request.value = loadedRequest;
    await store.loadHistory(props.requestId);
    return;
  }

  request.value = store.getRequestById(props.requestId) || null;
  if (request.value) {
    await store.loadHistory(props.requestId);
  }
};

const handleSendCounterProposal = async () => {
  if (!request.value) return;

  if (counterValue.value <= 0 || counterDays.value <= 0) {
    toast.error('Informe um valor e prazo válidos');
    return;
  }

  const rangeError = validateValueRange(counterValue.value);
  if (rangeError) {
    toast.error(rangeError);
    return;
  }

  isSubmittingCounter.value = true;
  try {
    await store.sendCounterProposal(
      request.value.id,
      counterValue.value,
      counterDays.value,
      counterNotes.value
    );
    toast.success('Contra proposta enviada com sucesso!');
    showCounterProposalDialog.value = false;
  } catch (err: any) {
    console.error('Erro ao enviar contra proposta:', err);
    const backendMsg = extractApiErrorMessage(err);
    toast.error(backendMsg || 'Erro ao enviar contra proposta. Verifique os dados e tente novamente.');
  } finally {
    isSubmittingCounter.value = false;
  }
};

onMounted(async () => {
  await loadData();
});

watch(() => props.requestId, async (newId, oldId) => {
  if (newId && newId !== oldId) {
    await loadData();
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

const formatShortDate = (dateString: string) => {
  return formatApiDate(dateString, {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
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
  props.onNavigate('requests');
};

const handleSendMessage = async (message: string, attachments: File[]) => {
  if (!request.value) return;

  isSendingMessage.value = true;
  const success = await store.sendMessage(
    request.value.id,
    message,
    attachments.length > 0 ? attachments : undefined
  );
  isSendingMessage.value = false;

  if (success) {
    toast.success('Mensagem enviada');
  } else {
    toast.error('Erro ao enviar mensagem');
  }
};

const wasCounterProposalRejected = computed(() => {
  if (!request.value) return false;
  return request.value.status === 'negotiating' &&
         !request.value.counterValue &&
         request.value.auditLog?.some(log => log.operationKey === 'COUNTER_PROPOSAL_REJECTED');
});

const handleAcceptResponse = async () => {
  if (!request.value) return;

  const success = await store.acceptFreelancerResponse(request.value.id);
  if (success) {
    toast.success('Proposta aceita com sucesso!');
    // Mostrar modal de celebração
    setTimeout(() => {
      showCelebrationModal.value = true;
    }, 500);
  } else {
    toast.error('Erro ao aceitar proposta');
  }
};

const handleGoToCheckout = () => {
  if (!request.value) return;
  showCelebrationModal.value = false;
  // Navegar para checkout
  window.location.href = `/checkout?requestId=${request.value.id}`;
};

const handleOpenRejectDialog = () => {
  rejectReason.value = '';
  showRejectDialog.value = true;
};

const handleRejectResponse = async () => {
  if (!request.value || !rejectReason.value.trim()) {
    toast.error('Informe o motivo da recusa');
    return;
  }

  const success = await store.rejectFreelancerResponse(request.value.id, rejectReason.value);
  if (success) {
    toast.success('Proposta recusada');
    showRejectDialog.value = false;
  } else {
    toast.error('Erro ao recusar proposta');
  }
};

const handleCancelRequest = async () => {
  if (!request.value) return;

  if (!cancelReason.value.trim()) {
    toast.error('Informe o motivo do cancelamento');
    return;
  }

  const success = await store.cancelRequest(request.value.id, cancelReason.value);
  if (success) {
    toast.success('Solicitação cancelada');
    showCancelDialog.value = false;
  } else {
    toast.error('Erro ao cancelar solicitação');
  }
};

const getStatusColor = (status: string) => {
  const colors: Record<string, { bg: string; text: string }> = {
    pending: { bg: 'bg-blue-100', text: 'text-blue-700' },
    sent: { bg: 'bg-blue-100', text: 'text-blue-700' },
    negotiating: { bg: 'bg-blue-100', text: 'text-blue-700' },
    accepted: { bg: 'bg-green-100', text: 'text-green-700' },
    in_progress: { bg: 'bg-purple-100', text: 'text-purple-700' },
    delivered: { bg: 'bg-blue-100', text: 'text-blue-700' },
    completed: { bg: 'bg-teal-100', text: 'text-teal-700' },
    rejected: { bg: 'bg-red-100', text: 'text-red-700' },
    cancelled: { bg: 'bg-gray-100', text: 'text-gray-700' },
  };
  return colors[status] || colors.pending;
};
</script>

<template>
  <div class="max-w-5xl mx-auto space-y-6">
    <!-- Back Button -->
    <Button @click="goBack" variant="ghost" class="mb-2">
      <ArrowLeft class="h-4 w-4 mr-2" />
      Voltar às Solicitações
    </Button>

    <!-- Loading -->
    <div v-if="store.isLoading" class="flex justify-center py-12">
      <div class="animate-spin h-8 w-8 border-4 border-[#286AD8] border-t-transparent rounded-full"></div>
    </div>

    <!-- Not Found -->
    <Card v-else-if="!request" class="p-12 text-center">
      <FileText class="h-12 w-12 text-gray-400 mx-auto mb-4" />
      <h3 class="text-lg font-semibold text-[#263238] mb-2">Solicitação não encontrada</h3>
      <p class="text-[#435574]">A solicitação que você procura não existe ou foi removida.</p>
      <Button @click="goBack" variant="outline" class="mt-4">Voltar</Button>
    </Card>

    <template v-else>
      <!-- Header Card -->
      <Card class="p-6">
        <div class="flex items-start justify-between flex-wrap gap-4">
          <div class="flex-1">
            <div class="flex items-center gap-3 mb-2 flex-wrap">
              <h1 class="text-2xl font-bold text-[#263238]">{{ request.service }}</h1>
              <Badge v-if="request" :class="`${getStatusColor(request.status).bg} ${getStatusColor(request.status).text}`">
                {{ getStatusLabel(request.status) }}
              </Badge>
            </div>
            <div class="flex items-center gap-4 text-sm text-[#435574] flex-wrap">
              <span v-if="request" class="flex items-center gap-1">
                <Calendar class="h-4 w-4" />
                Criada em {{ formatDate(request.createdAt) }}
              </span>
              <span v-if="request?.address" class="flex items-center gap-1">
                <MapPin class="h-4 w-4" />
                {{ request.address }}
              </span>
            </div>
          </div>
          <div class="flex items-center gap-2">
            <Button
              v-if="request?.status === 'pending' || request?.status === 'sent'"
              @click="showCancelDialog = true"
              variant="outline"
              class="text-red-600 border-red-200 hover:bg-red-50"
            >
              <XCircle class="h-4 w-4 mr-2" />
              Cancelar
            </Button>
          </div>
        </div>

        <!-- Payment Pending Alert (quando status é ACCEPTED e pagamento não confirmado) -->
        <div 
          v-if="
            request.status === 'accepted' &&
            !request.auditLog?.some((log) => log.operationKey === 'PAYMENT_CONFIRMED')
          " 
          class="mt-4 p-4 rounded-lg border-2 border-green-300 bg-gradient-to-r from-green-50 to-blue-50 flex items-start gap-3"
        >
          <div class="flex-shrink-0 w-10 h-10 bg-green-600 rounded-full flex items-center justify-center">
            <CreditCard class="h-5 w-5 text-white" />
          </div>
          <div class="flex-1">
            <p class="font-semibold text-green-900 mb-1">Ação Necessária: Pagamento Pendente</p>
            <p class="text-sm text-green-800 mb-3">
              Sua proposta foi aceita! Complete o pagamento agora para garantir o serviço e iniciar o trabalho com <span class="font-semibold">{{ request.freelancerName }}</span>.
            </p>
            <Button
              @click="handleGoToCheckout"
              variant="success"
              size="sm"
            >
              <CreditCard class="h-4 w-4 mr-2" />
              Realizar Pagamento Agora
            </Button>
          </div>
        </div>

        <!-- Rejection/Cancellation Reason Alert -->
        <div v-if="(request.status === 'rejected' && request.rejectReason) || (request.status === 'cancelled' && request.cancelReason)" 
             class="mt-4 p-4 rounded-lg border flex items-start gap-3"
             :class="request.status === 'rejected' ? 'bg-red-50 border-red-100 text-red-900' : 'bg-gray-50 border-gray-100 text-gray-900'">
          <AlertCircle class="h-5 w-5 mt-0.5 flex-shrink-0" :class="request.status === 'rejected' ? 'text-red-600' : 'text-gray-600'" />
          <div>
            <p class="font-semibold text-sm">
              {{ request.status === 'rejected' ? 'Motivo da Recusa' : 'Motivo do Cancelamento' }}
            </p>
            <p class="text-sm opacity-90 italic">"{{ request.status === 'rejected' ? request.rejectReason : request.cancelReason }}"</p>
          </div>
        </div>
      </Card>

      <!-- Navigation Tabs -->
      <div class="flex gap-2 border-b border-gray-200 pb-0 items-center justify-between">
        <div class="flex gap-2">
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
              v-if="request.messages && request.messages.length > 0"
              class="ml-1 bg-primary text-white text-xs"
            >
              {{ request.messages.length }}
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
            Anexo
          </button>
        </div>
        
      </div>

      <!-- Info Alert: Aguardando entrega do profissional -->
      <div v-if="request.status === 'in_progress'" 
           class="mt-4 p-4 rounded-lg border border-blue-100 bg-blue-50 text-blue-900 flex items-start gap-3">
        <Clock class="h-5 w-5 mt-0.5 flex-shrink-0 text-blue-600" />
        <div>
          <p class="font-semibold text-sm">Serviço em Andamento</p>
          <p class="text-sm opacity-90">O profissional está trabalhando no seu pedido. Assim que ele finalizar a entrega, você poderá aprová-la pelo detalhe do projeto.</p>
        </div>
      </div>

      <!-- Details Section -->
      <div v-if="activeSection === 'details'" class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Main Info -->
        <div class="lg:col-span-2 space-y-6">
          <!-- Description -->
          <Card class="p-6">
            <h3 class="font-semibold text-[#263238] mb-3">Descrição do Serviço</h3>
            <p class="text-[#435574] whitespace-pre-line">{{ request.description }}</p>
          </Card>

          <!-- Freelancer Response -->
          <Card v-if="request.freelancerResponse" class="p-6 border-blue-200 bg-blue-50/30">
            <div class="flex items-center gap-2 mb-4">
              <Send class="h-5 w-5 text-blue-600" />
              <h3 class="font-semibold text-[#263238]">Resposta do Profissional</h3>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4">
              <div class="bg-white rounded-lg p-4 border">
                <p class="text-sm text-[#435574] mb-1">Valor proposto</p>
                <p class="text-xl font-bold text-green-600">
                  {{ formatCurrency(request.freelancerResponse.value) }}
                </p>
              </div>
              <div class="bg-white rounded-lg p-4 border">
                <p class="text-sm text-[#435574] mb-1">Prazo de execução</p>
                <p class="text-xl font-bold text-[#263238]">
                  {{ request.freelancerResponse.executionDays }} dias
                </p>
              </div>
            </div>

            <div class="mb-4">
              <p class="text-sm font-medium text-[#263238] mb-2">Escopo do trabalho</p>
              <p class="text-sm text-[#435574] whitespace-pre-line">
                {{ request.freelancerResponse.scope }}
              </p>
            </div>

            <div v-if="request.freelancerResponse.notes" class="mb-4">
              <p class="text-sm font-medium text-[#263238] mb-2">Observações</p>
              <p class="text-sm text-[#435574]">{{ request.freelancerResponse.notes }}</p>
            </div>

            <p class="text-xs text-[#435574] mb-4">
              Recebida em {{ formatDate(request.freelancerResponse.sentAt) }}
            </p>

            <!-- Action Buttons -->
            <div
              v-if="request.status === 'negotiating'"
              class="space-y-4 pt-4 border-t"
            >
              <!-- Counter Proposal Summary -->
              <div v-if="request.counterValue" class="p-4 bg-blue-50 border border-blue-100 rounded-lg">
                <p class="text-sm font-medium text-blue-900 mb-1">Você enviou uma contra proposta:</p>
                <p class="text-sm text-blue-700">
                  <span class="font-bold">{{ formatCurrency(request.counterValue) }}</span> | 
                  <span class="font-bold">{{ request.counterDays }} dias</span>
                </p>
                <p v-if="request.counterNotes" class="text-xs text-blue-600 mt-1 italic">
                  "{{ request.counterNotes }}"
                </p>
              </div>

              <!-- Rejection Alert -->
              <div v-if="wasCounterProposalRejected" class="p-4 bg-blue-50 border border-blue-100 rounded-lg flex items-start gap-3">
                <AlertCircle class="h-5 w-5 text-blue-600 shrink-0 mt-0.5" />
                <div>
                  <p class="text-sm font-medium text-blue-900">Contra proposta recusada</p>
                  <p class="text-xs text-blue-700 mt-1">
                    O profissional preferiu manter a proposta original. Você pode aceitá-la ou recusar.
                  </p>
                </div>
              </div>

              <div class="flex items-center gap-3 flex-wrap">
                <Button
                  @click="handleAcceptResponse"
                  variant="success"
                  class="flex-1 min-w-[150px]"
                >
                  <CheckCircle class="h-4 w-4 mr-2" />
                  Aceitar Proposta
                </Button>
                <Button
                  @click="handleOpenRejectDialog"
                  variant="outline"
                  class="text-red-600 border-red-200 hover:bg-red-50 flex-1 min-w-[150px]"
                >
                  <XCircle class="h-4 w-4 mr-2" />
                  Recusar
                </Button>
                <Button
                  v-if="!request.counterValue && !wasCounterProposalRejected"
                  @click="handleOpenCounterProposalDialog"
                  variant="outline"
                  class="text-primary border-primary flex-1 min-w-[150px]"
                >
                  <FileEdit class="h-4 w-4 mr-2" />
                  Contra Proposta
                </Button>
              </div>
            </div>

            <div v-else-if="request.status === 'accepted'" class="pt-4 border-t">
              <div class="flex items-center gap-2 text-green-700">
                <CheckCircle class="h-5 w-5" />
                <span class="font-medium">Proposta aceita</span>
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
                  {{ ['accepted', 'in_progress', 'completed'].includes(request.status) && request.freelancerResponse ? 'Valor Acordado' : 'Orçamento' }}
                </p>
                <p class="text-lg font-bold text-green-600">
                  {{ formatCurrency(['accepted', 'in_progress', 'completed'].includes(request.status) && request.freelancerResponse ? request.freelancerResponse.value : request.budget) }}
                </p>
              </div>
              <Separator />
              <div>
                <p class="text-sm text-[#435574]">Data desejada</p>
                <p class="font-medium text-[#263238]">
                  {{ request.desiredDate ? formatShortDate(request.desiredDate) : request.deadline }}
                </p>
              </div>
              <Separator />
              <div>
                <p class="text-sm text-[#435574]">Categoria</p>
                <p class="font-medium text-[#263238]">{{ request.category }}</p>
              </div>
              <Separator />
              <div>
                <p class="text-sm text-[#435574]">Modelo de trabalho</p>
                <WorkModelBadge :work-model="request.workModel" />
              </div>
            </div>
          </Card>

          <!-- Freelancer Info -->
          <Card v-if="request.freelancerName" class="p-6">
            <h3 class="font-semibold text-[#263238] mb-4">Profissional</h3>
            <div class="flex items-center gap-3 mb-3">
              <Avatar class="h-12 w-12">
                <AvatarImage :src="request.freelancerAvatar" />
                <AvatarFallback class="bg-[#286AD8] text-white">
                  {{ getInitials(request.freelancerName) }}
                </AvatarFallback>
              </Avatar>
              <div>
                <p class="font-medium text-[#263238]">{{ request.freelancerName }}</p>
                <p v-if="request.freelancerLocation" class="text-sm text-[#435574]">
                  {{ request.freelancerLocation }}
                </p>
              </div>
            </div>
          </Card>
        </div>
      </div>

      <!-- Messages Section -->
      <Card v-if="activeSection === 'messages'" class="p-6">
        <h3 class="font-semibold text-[#263238] mb-4">Mensagens</h3>

        <ChatBox
          :messages="request.messages || []"
          current-user-role="client"
          :is-locked="request.status !== 'in_progress' && request.status !== 'delivered'"
          :service-status="request.status"
          :is-sending="isSendingMessage"
          @send-message="handleSendMessage"
        />
      </Card>

      <ServiceRequestTimeline
        v-if="activeSection === 'history'"
        :request-id="request.id"
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

        <!-- Client Files Tab -->
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
              sender-info="Enviado por você"
              variant="blue"
            />
          </div>
        </div>

        <!-- Freelancer Files Tab -->
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
              sender-info="Enviado pelo profissional"
              variant="blue"
            />
          </div>
        </div>
      </Card>
    </template>

    <!-- Cancel Dialog -->
    <Dialog :open="showCancelDialog" @update:open="(val) => showCancelDialog = val">
      <DialogContent class="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Cancelar Solicitação</DialogTitle>
          <DialogDescription>
            Tem certeza que deseja cancelar esta solicitação?
          </DialogDescription>
        </DialogHeader>
        <div class="space-y-4">
          <div class="space-y-2">
            <Label for="cancel-reason-detail">Motivo do cancelamento *</Label>
            <Textarea
              id="cancel-reason-detail"
              :model-value="cancelReason"
              @update:model-value="(val) => cancelReason = val as string"
              placeholder="Por que está cancelando?"
              :rows="3"
            />
          </div>
        </div>
        <DialogFooter>
          <Button @click="showCancelDialog = false" variant="outline">Manter</Button>
          <Button @click="handleCancelRequest" variant="destructive">
            Confirmar Cancelamento
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- Reject Response Dialog -->
    <Dialog :open="showRejectDialog" @update:open="(val) => showRejectDialog = val">
      <DialogContent class="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Recusar Proposta</DialogTitle>
          <DialogDescription>
            Informe o motivo para recusar a proposta do profissional
          </DialogDescription>
        </DialogHeader>
        <div class="space-y-4">
          <div class="space-y-2">
            <Label for="reject-reason">Motivo da recusa *</Label>
            <Textarea
              id="reject-reason"
              :model-value="rejectReason"
              @update:model-value="(val) => rejectReason = val as string"
              placeholder="Ex: Valor acima do orçamento, prazo longo, etc."
              :rows="4"
            />
          </div>
        </div>
        <DialogFooter>
          <Button @click="showRejectDialog = false" variant="outline">Cancelar</Button>
          <Button @click="handleRejectResponse" variant="destructive">
            Confirmar Recusa
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- Counter Proposal Dialog -->
    <Dialog :open="showCounterProposalDialog" @update:open="(val) => showCounterProposalDialog = val">
      <DialogContent class="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Enviar Contra Proposta</DialogTitle>
          <DialogDescription>
            Sugira um novo valor ou prazo para o serviço "{{ request?.service }}"
          </DialogDescription>
        </DialogHeader>
        <div class="space-y-4 py-2">
          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-2">
              <Label for="counter-value">Novo Valor (R$)</Label>
              <Input
                id="counter-value"
                type="number"
                v-model="counterValue"
                step="0.01"
              />
            </div>
            <div class="space-y-2">
              <Label for="counter-days">Novo Prazo (dias)</Label>
              <Input
                id="counter-days"
                type="number"
                v-model="counterDays"
              />
            </div>
          </div>
          <div class="space-y-2">
            <Label for="counter-notes">Observações</Label>
            <Textarea
              id="counter-notes"
              v-model="counterNotes"
              placeholder="Explique sua proposta ou peça algum ajuste no escopo..."
              :rows="3"
            />
          </div>
        </div>
        <DialogFooter>
          <Button @click="showCounterProposalDialog = false" variant="outline">Cancelar</Button>
          <Button 
            @click="handleSendCounterProposal" 
            variant="default"
            :disabled="isSubmittingCounter"
          >
            {{ isSubmittingCounter ? 'Enviando...' : 'Enviar Contra Proposta' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- Celebration Modal - "Hora de Fechar!" -->
    <Dialog :open="showCelebrationModal" @update:open="(val) => showCelebrationModal = val">
      <DialogContent class="sm:max-w-lg">
        <div class="text-center space-y-6 py-4">
          <!-- Animated Icon -->
          <div class="relative inline-flex">
            <div class="absolute inset-0 animate-ping opacity-75">
              <div class="w-24 h-24 bg-green-200 rounded-full mx-auto"></div>
            </div>
            <div class="relative w-24 h-24 bg-gradient-to-br from-green-400 to-green-600 rounded-full mx-auto flex items-center justify-center">
              <CheckCircle class="h-12 w-12 text-white" />
            </div>
          </div>

          <!-- Title -->
          <div>
            <h2 class="text-3xl font-bold text-[#263238] mb-2 flex items-center justify-center gap-2">
              <Sparkles class="h-6 w-6 text-yellow-500" />
              Hora de Fechar!
              <Sparkles class="h-6 w-6 text-yellow-500" />
            </h2>
            <p class="text-lg text-green-600 font-semibold">Proposta Aceita com Sucesso!</p>
          </div>

          <!-- Message -->
          <div class="bg-gradient-to-r from-green-50 to-blue-50 rounded-lg p-6 space-y-3">
            <p class="text-[#263238]">
              Parabéns! Você acaba de aceitar a proposta de
              <span class="font-semibold">{{ request?.freelancerName }}</span>
            </p>
            <p class="text-sm text-[#435574]">
              Para garantir o serviço e iniciar o trabalho, realize o pagamento agora mesmo.
            </p>
          </div>

          <!-- Service Summary -->
          <div v-if="request?.freelancerResponse" class="bg-white border-2 border-green-200 rounded-lg p-4 space-y-2">
            <div class="flex justify-between items-center">
              <span class="text-sm text-[#435574]">Valor do serviço:</span>
              <span class="text-lg font-bold text-green-600">
                {{ formatCurrency(request.freelancerResponse.value) }}
              </span>
            </div>
            <div class="flex justify-between items-center">
              <span class="text-sm text-[#435574]">Prazo de execução:</span>
              <span class="font-semibold text-[#263238]">
                {{ request.freelancerResponse.executionDays }} dias
              </span>
            </div>
          </div>

          <!-- Action Buttons -->
          <div class="space-y-3 pt-4">
            <Button
              @click="handleGoToCheckout"
              variant="success"
              class="w-full py-6 text-lg"
              size="lg"
            >
              <CreditCard class="h-6 w-6 mr-2" />
              Ir para Pagamento
            </Button>
            <Button
              @click="showCelebrationModal = false"
              variant="ghost"
              class="w-full"
            >
              Fazer Depois
            </Button>
          </div>

          <!-- Trust Badge -->
          <div class="flex items-center justify-center gap-2 text-xs text-[#435574]">
            <Lock class="h-4 w-4 text-green-600" />
            <span>Pagamento 100% seguro e protegido</span>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  </div>
</template>
