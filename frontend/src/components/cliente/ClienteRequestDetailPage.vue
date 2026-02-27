<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue';
import {
  ArrowLeft,
  Clock,
  DollarSign,
  MapPin,
  Calendar,
  MessageSquare,
  CheckCircle,
  XCircle,
  AlertCircle,
  FileText,
  Send,
  User,
  History,
} from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Input from '../ui/input.vue';
import Card from '../ui/card.vue';
import Badge from '../ui/badge.vue';
import Avatar from '../ui/avatar.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import Separator from '../ui/separator.vue';
import Textarea from '../ui/textarea.vue';
import Label from '../ui/label.vue';
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
import { useClientRequestStore, type ServiceRequest } from '@/stores/clientRequestStore';
import ServiceRequestTimeline from '../service/ServiceRequestTimeline.vue';
import { toast } from '@/utils/toast';

interface Props {
  onNavigate: (page: string, params?: any) => void;
  requestId: string;
}

const props = defineProps<Props>();
const store = useClientRequestStore();

const request = ref<ServiceRequest | null>(null);
const newMessage = ref('');
const isSendingMessage = ref(false);
const showCancelDialog = ref(false);
const cancelReason = ref('');
const showRejectDialog = ref(false);
const rejectReason = ref('');
const activeSection = ref<'details' | 'messages' | 'history'>('details');

onMounted(async () => {
  if (store.requests.length === 0) {
    await store.loadRequests();
  }
  request.value = store.getRequestById(props.requestId) || null;
  if (request.value) {
    await store.loadHistory(props.requestId);
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

const formatShortDate = (dateString: string) => {
  const date = new Date(dateString);
  return date.toLocaleDateString('pt-BR', {
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

const handleAcceptResponse = async () => {
  if (!request.value) return;

  const success = await store.acceptFreelancerResponse(request.value.id);
  if (success) {
    toast.success('Proposta aceita com sucesso!');
  } else {
    toast.error('Erro ao aceitar proposta');
  }
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
    negotiating: { bg: 'bg-amber-100', text: 'text-amber-700' },
    accepted: { bg: 'bg-green-100', text: 'text-green-700' },
    completed: { bg: 'bg-teal-100', text: 'text-teal-700' },
    rejected: { bg: 'bg-red-100', text: 'text-red-700' },
    cancelled: { bg: 'bg-gray-100', text: 'text-gray-700' },
  };
  return colors[status] || colors.pending;
};

const getStatusLabel = (status: string) => {
  const labels: Record<string, string> = {
    pending: 'Pendente',
    sent: 'Enviada',
    negotiating: 'Negociando',
    accepted: 'Aceita',
    rejected: 'Recusada',
    completed: 'Concluída',
    cancelled: 'Cancelada',
  };
  return labels[status] || status;
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
              <Badge :class="`${getStatusColor(request.status).bg} ${getStatusColor(request.status).text}`">
                {{ getStatusLabel(request.status) }}
              </Badge>
            </div>
            <div class="flex items-center gap-4 text-sm text-[#435574] flex-wrap">
              <span class="flex items-center gap-1">
                <Calendar class="h-4 w-4" />
                Criada em {{ formatDate(request.createdAt) }}
              </span>
              <span v-if="request.address" class="flex items-center gap-1">
                <MapPin class="h-4 w-4" />
                {{ request.address }}
              </span>
            </div>
          </div>
          <div class="flex items-center gap-2">
            <Button
              v-if="request.status === 'pending' || request.status === 'sent'"
              @click="showCancelDialog = true"
              variant="outline"
              class="text-red-600 border-red-200 hover:bg-red-50"
            >
              <XCircle class="h-4 w-4 mr-2" />
              Cancelar
            </Button>
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
            v-if="request.messages && request.messages.length > 0"
            class="ml-1 bg-[#286AD8] text-white text-xs"
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
          <Card v-if="request.freelancerResponse" class="p-6 border-amber-200 bg-amber-50/30">
            <div class="flex items-center gap-2 mb-4">
              <Send class="h-5 w-5 text-amber-600" />
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
              class="flex items-center gap-3 pt-4 border-t"
            >
              <Button
                @click="handleAcceptResponse"
                class="bg-green-600 hover:bg-green-700 flex-1"
              >
                <CheckCircle class="h-4 w-4 mr-2" />
                Aceitar Proposta
              </Button>
              <Button
                @click="handleOpenRejectDialog"
                variant="outline"
                class="text-red-600 border-red-200 hover:bg-red-50 flex-1"
              >
                <XCircle class="h-4 w-4 mr-2" />
                Recusar
              </Button>
            </div>

            <div v-else-if="request.status === 'accepted'" class="pt-4 border-t">
              <div class="flex items-center gap-2 text-green-700">
                <CheckCircle class="h-5 w-5" />
                <span class="font-medium">Proposta aceita</span>
              </div>
            </div>
          </Card>

          <!-- Attachments -->
          <Card v-if="request.attachments && request.attachments.length > 0" class="p-6">
            <h3 class="font-semibold text-[#263238] mb-3">Anexos</h3>
            <div class="space-y-2">
              <div
                v-for="attachment in request.attachments"
                :key="attachment.id"
                class="flex items-center gap-3 p-3 bg-gray-50 rounded-lg"
              >
                <FileText class="h-5 w-5 text-[#286AD8]" />
                <div class="flex-1 min-w-0">
                  <p class="text-sm font-medium text-[#263238] truncate">{{ attachment.name }}</p>
                  <p class="text-xs text-[#435574]">{{ attachment.size }}</p>
                </div>
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
                <p class="text-sm text-[#435574]">Orçamento</p>
                <p class="text-lg font-bold text-green-600">{{ formatCurrency(request.budget) }}</p>
              </div>
              <Separator />
              <div>
                <p class="text-sm text-[#435574]">Prazo solicitado</p>
                <p class="font-medium text-[#263238]">{{ request.deadline }}</p>
              </div>
              <Separator />
              <div>
                <p class="text-sm text-[#435574]">Categoria</p>
                <p class="font-medium text-[#263238]">{{ request.category }}</p>
              </div>
              <Separator />
              <div>
                <p class="text-sm text-[#435574]">Prioridade</p>
                <Badge
                  :class="
                    request.priority === 'high'
                      ? 'bg-red-100 text-red-700'
                      : request.priority === 'medium'
                      ? 'bg-yellow-100 text-yellow-700'
                      : 'bg-gray-100 text-gray-700'
                  "
                >
                  {{ request.priority === 'high' ? 'Alta' : request.priority === 'medium' ? 'Média' : 'Baixa' }}
                </Badge>
              </div>
              <template v-if="request.desiredDate">
                <Separator />
                <div>
                  <p class="text-sm text-[#435574]">Data desejada</p>
                  <p class="font-medium text-[#263238]">{{ formatShortDate(request.desiredDate) }}</p>
                </div>
              </template>
            </div>
          </Card>

          <!-- Freelancer Info -->
          <Card v-if="request.freelancerName" class="p-6">
            <h3 class="font-semibold text-[#263238] mb-4">Profissional</h3>
            <div class="flex items-center gap-3 mb-3">
              <Avatar class="h-12 w-12">
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

        <div v-if="!request.messages || request.messages.length === 0" class="text-center py-8">
          <MessageSquare class="h-10 w-10 text-gray-300 mx-auto mb-3" />
          <p class="text-[#435574]">Nenhuma mensagem ainda</p>
        </div>

        <div v-else class="space-y-4 max-h-96 overflow-y-auto mb-4">
          <div
            v-for="msg in request.messages"
            :key="msg.id"
            :class="`flex ${msg.sender === 'client' ? 'justify-end' : 'justify-start'}`"
          >
            <div
              :class="`max-w-[80%] rounded-lg p-3 ${
                msg.sender === 'client'
                  ? 'bg-[#286AD8] text-white'
                  : 'bg-gray-100 text-[#263238]'
              }`"
            >
              <p class="text-xs font-medium mb-1 opacity-80">{{ msg.senderName }}</p>
              <p class="text-sm">{{ msg.message }}</p>
              <p class="text-xs mt-1 opacity-60">{{ formatDate(msg.timestamp) }}</p>
            </div>
          </div>
        </div>

        <!-- Message Input -->
        <div
          v-if="request.status !== 'cancelled' && request.status !== 'rejected' && request.status !== 'completed'"
          class="flex gap-2"
        >
          <Textarea
            :model-value="newMessage"
            @update:model-value="(val) => newMessage = val as string"
            placeholder="Digite sua mensagem..."
            :rows="2"
            class="flex-1"
            @keydown.enter.ctrl="handleSendMessage"
          />
          <Button
            @click="handleSendMessage"
            class="bg-[#286AD8] hover:bg-[#435574] self-end"
            :disabled="!newMessage.trim() || isSendingMessage"
          >
            <Send class="h-4 w-4" />
          </Button>
        </div>
      </Card>

      <!-- History Section -->
      <ServiceRequestTimeline
        v-if="activeSection === 'history'"
        :entries="[...(request.auditLog || [])].reverse()"
        title="Histórico de Atividades"
      />
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
            <Label for="cancel-reason-detail">Motivo (opcional)</Label>
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
          <Button @click="handleCancelRequest" class="bg-red-600 hover:bg-red-700">
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
          <Button @click="handleRejectResponse" class="bg-red-600 hover:bg-red-700">
            Confirmar Recusa
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>
